package com.deleidos.dmf.framework;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.pdfbox.util.PDFStreamEngine;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.DefaultParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.SecureContentHandler;
import org.jnetpcap.packet.JRegistry;
import org.jnetpcap.packet.RegistryHeaderErrors;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.deleidos.dmf.analyzer.Analyzer;
import com.deleidos.dmf.exception.AnalyticsInitializationRuntimeException;
import com.deleidos.dmf.exception.AnalyticsParsingRuntimeException;
import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.exception.AnalyticsUndetectableTypeException;
import com.deleidos.dmf.exception.AnalyticsUnsupportedParserException;
import com.deleidos.dmf.framework.AnalyticsEmbeddedDocumentExtractor.ExtractedContent;
import com.deleidos.dmf.handler.AnalyticsProgressTrackingContentHandler;
import com.deleidos.dmf.parser.JNetPcapTikaParser;
import com.deleidos.dmf.parser.pcap.ext.Wireless80211;
import com.deleidos.dmf.parser.pcap.ext.Wireless80211RadioTap;
import com.deleidos.dmf.progressbar.ProgressBar;
import com.deleidos.dmf.progressbar.ProgressState;
import com.deleidos.dmf.web.SchemaWizardWebSocketUtility;
import com.deleidos.dmf.exception.AnalyzerException;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.profiler.AbstractReverseGeocodingProfiler;
import com.deleidos.dp.profiler.api.Profiler;

public class AnalyticsDefaultParser extends DefaultParser implements Analyzer<TikaSampleAnalyzerParameters, TikaSchemaAnalyzerParameters> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2117902573967865092L;
	private static final Logger logger = Logger.getLogger(AnalyticsDefaultParser.class);
	private AnalyticsDefaultDetector detector;
	private AnalyticsEmbeddedDocumentExtractor extractor;
	public final int INTERNAL_BUFFER_MAX_LENGTH = -1;

	public AnalyticsDefaultParser(AnalyticsDefaultDetector detector, TikaProfilerParameters parameters) {
		this.detector = detector;

		try {
			Class.forName("org.jnetpcap.packet.JRegistry", false, this.getClass().getClassLoader());
			try {
				JRegistry.register(Wireless80211RadioTap.class);
				JRegistry.register(Wireless80211.class);
			} catch (RegistryHeaderErrors e) {
				logger.error(e);
				logger.error("Could not add JNetPcapTikaParser.  Parser will not be available.");
			}
		} catch (ClassNotFoundException e) {
			logger.error("Could not find JnetPcap classes.  Not adding JnetPcapTikaParser to available parsers.");
		}

		PDFParserConfig pc = new PDFParserConfig();
		pc.setExtractInlineImages(true);
		parameters.set(PDFParserConfig.class, pc);
		Logger pdfLogger = Logger.getLogger(PDFStreamEngine.class);
		pdfLogger.setLevel(Level.OFF);

		this.extractor = new AnalyticsEmbeddedDocumentExtractor(parameters);
	}

	public AnalyticsDefaultParser excludeParsers(List<Parser> excludedParsers, ParseContext context) {
		for(Parser p : excludedParsers) {
			if(this.getParsers(context).containsKey(p.getSupportedTypes(context))) {
				this.getParsers(context).remove(p.getSupportedTypes(context));
				logger.warn(p.getSupportedTypes(context) + " parser excluded from available parsers.");
			}
		}
		return this;
	}

	@Override
	public void parse(
			InputStream stream, ContentHandler handler,
			Metadata metadata, ParseContext context)
					throws IOException, SAXException, TikaException {
		long t1 = System.currentTimeMillis();

		// error checking
		if(context == null) {
			throw new AnalyticsInitializationRuntimeException("Parse context is null.");
		} else if(!(context instanceof TikaProfilerParameters)) {
			throw new AnalyticsInitializationRuntimeException("Parse context is not an instance of AnalyticsTikaParameters.");
		}


		boolean reverseGeocodingPass = false;
		if(context.get(Profiler.class) != null) {
			Profiler profiler = context.get(Profiler.class);
			if(profiler instanceof AbstractReverseGeocodingProfiler) {
				reverseGeocodingPass = true;
			}
		} else {
			logger.warn("No profiler handler found in context.");
		}
		if(context.get(File.class) == null) {
			logger.warn("File passed in parsing context.");
		}

		TemporaryResources tmp = new TemporaryResources();
		TikaInputStream tis = TikaInputStream.get(stream, tmp);

		try {	
			TikaProfilerParameters params = extractor.initializeTikaProfilingParameters(tis, handler, metadata);
			analyticsParse(tis, handler, metadata, params, reverseGeocodingPass);
			logger.info("Finished parsing.");
		} catch (AnalyticsParsingRuntimeException e) {
			logger.error("Runtime exception from " + e.getParser() + " parser for type " + e.getParser().getSupportedTypes(context) +".", e);
		} catch (AnalyticsInitializationRuntimeException e) {
			logger.error("Initialization exception while attempting to parse.", e);
		} catch (AnalyzerException | DataAccessException e) {
			logger.error(e);
			throw new TikaException("Tika Exception wrapping Analytics Exception: " + e.getMessage(), e);
		} finally {
			tmp.dispose();
		}

		long t2 = System.currentTimeMillis();
		logger.debug("Parsing for " + metadata.get(Metadata.CONTENT_TYPE).toString() + " took " + (t2 - t1) + " millis.");

	}

	private void analyticsDetect(TikaInputStream tis, ContentHandler handler, Metadata metadata,
			TikaProfilerParameters params, boolean bodyContentParsing)
					throws IOException,	SAXException, AnalyticsUndetectableTypeException, TikaException {

		long detectorT1 = System.currentTimeMillis();
		MediaType type = detector.detect(tis, metadata);
		if(type == null) {
			if(!bodyContentParsing) {
				throw new AnalyticsUndetectableTypeException("Type not detected by any detectors.");
			} else {
				logger.warn("Body content was not detected to have a type.");
				metadata.set(AnalyticsDefaultDetector.HAS_BODY_CONTENT, Boolean.FALSE.toString());
			}
		} else if(type.equals(MediaType.OCTET_STREAM)) {
			if(!bodyContentParsing) {
				throw new AnalyticsUndetectableTypeException("Could not detect type as anything other than octet stream.");
			} else {
				logger.warn("Body content was not detected to have a type.");
				metadata.set(AnalyticsDefaultDetector.HAS_BODY_CONTENT, Boolean.FALSE.toString());
			}
		} else {
			long detectorT2 = System.currentTimeMillis();
			logger.debug("Detecting for " + type + " took " + (detectorT2 - detectorT1) + " millis.");
			metadata.set(Metadata.CONTENT_TYPE, type.toString());
		}
	}

	public List<Detector> getAvailableDetectors() {
		return detector.getDetectors();
	}

	public Map<MediaType, Parser> getBodyContentParsers(ParseContext context) {
		return super.getParsers(context);
	}

	@Override
	public Parser getParser(Metadata metadata, ParseContext context) {
		return super.getParser(metadata, context);
	}

	public TikaProfilableParser getProfilableParser(Metadata metadata, ParseContext context) throws AnalyticsUnsupportedParserException {
		Parser p = super.getParser(metadata, context);
		if(p instanceof TikaProfilableParser) {
			return (TikaProfilableParser) p;
		} else {
			throw new AnalyticsUnsupportedParserException("No supported analytics parser found for " + metadata.get(Metadata.CONTENT_TYPE) + ".");
		}
	}

	private void analyticsParse(TikaInputStream tis, ContentHandler analyticsHandler, Metadata metadata, TikaProfilerParameters analyticsParams,
			boolean reverseGeocodingPass) throws SAXException, TikaException, IOException, AnalyzerException, DataAccessException {
		//sync the input stream with the profiler parameters (want to make this cleaner)

		if(!reverseGeocodingPass) {
			analyticsDetect(tis, analyticsHandler, metadata, analyticsParams, false);
			analyticsParams.getProgress().setCurrentState(ProgressState.sampleParsingStage);
			analyticsParams.getProgress().updateCurrentSampleNumerator(ProgressState.sampleParsingStage.getStartValue());
			SchemaWizardWebSocketUtility.getInstance().updateProgress(analyticsParams.getProgress(), analyticsParams.getSessionId());
		} else {
			analyticsParams.getProgress().updateCurrentSampleNumerator(ProgressState.geocodingStage.getStartValue());
			SchemaWizardWebSocketUtility.getInstance().updateProgress(analyticsParams.getProgress(), analyticsParams.getSessionId());
		}

		detector.disableProgressUpdates();

		Parser parser = getParser(metadata, analyticsParams);

		if(parser instanceof TikaProfilableParser) {
			// easy - detected type is profilable, let the instance of AbstractAnalyticsParser handle everything
			// give TikaProfilableParsers control of parsing the stream
			profilableParse((TikaProfilableParser)parser, analyticsParams);
		} else  {
			// parser is not a profilable (usually means it's a built in tika parser), need to
			// 1. attempt to parse the plain text body contents - needs to be disabled for certain types
			// 2. extract all binary contents - handled by AnalyticsEmbeddedDocumentExtractor
			boolean shouldExtractBodyContent = Boolean.valueOf(metadata.get(AnalyticsDefaultDetector.HAS_BODY_CONTENT));

			TikaInputStream bodyStream = extractProfilableContent(parser, tis, analyticsHandler, metadata, analyticsParams);
			// get body contents as a new stream (in memory for now, need catch exception after certain size and write to file)


			analyticsParams.getProgress().updateCurrentSampleNumerator(ProgressState.sampleParsingStage.getStartValue()+5);
			SchemaWizardWebSocketUtility.getInstance().updateProgress(analyticsParams.getProgress(), analyticsParams.getSessionId());

			int i = 0;
			if(shouldExtractBodyContent) {
				// set remaining split to extract content size + 1 (for body content)
				if(!reverseGeocodingPass) {
					analyticsParams.getProgress().setCurrentStateSplits(extractor.getExtractedContents().size()+1);
					i = 1;
				}

				Metadata bodyMetadata = new Metadata();
				TikaProfilerParameters reinitializedParams = extractor.initializeTikaProfilingParameters(bodyStream, analyticsHandler, bodyMetadata);
				analyticsDetect(bodyStream, analyticsHandler, bodyMetadata, reinitializedParams, true);
				Parser bodyParser = getParser(bodyMetadata, reinitializedParams);
				if(bodyParser instanceof TikaProfilableParser) {
					profilableParse((TikaProfilableParser)bodyParser, reinitializedParams);
					// profile the body content
				} else {
					logger.error("Body content could not be profiled.");
					throw new AnalyticsUnsupportedParserException("Format detected as " +
							analyticsParams.getMetadata().get(Metadata.CONTENT_TYPE) + " cannot be parsed.");
				}
			} else {
				if(!reverseGeocodingPass) {
					analyticsParams.getProgress().setCurrentStateSplits(extractor.getExtractedContents().size());
				}
			}

			// embedded documents are already written to disk (from AnalyticsEmbeddedDocumentParser())
			// doing all of these before profiling them allows us to gauge progress
			// writing them to disk means we don't have to re-parse for reverse geocoding or schema passes
			// examples: files from a zip, images from a pdf, etc. 
			// embedded document extraction must unfortunately be parser dependent, but that's what Tika is fo
			// loop through extracted embedded documents (stream and metadata held in AnalyticsEmbeddedDocumentExtractor) 
			// profile them, but they may have plain text body content as well (which is not extracted to disk)
			// so we need to use the same method as the base file)
			// example zip contains pdf which contains parseable text

			logger.info("Using content from " +extractor.getParentParameters().getExtractedContentDir()+ ".  Profiling extracted contents.");


			for(ExtractedContent extractedContent : extractor.getExtractedContents()) {

				embeddedDocumentBodyContentParse(extractedContent, analyticsHandler);

				if(!reverseGeocodingPass) {
					analyticsParams.getProgress().setCurrentStateSplitIndex(i);
					analyticsParams.getProgress().updateCurrentSampleNumerator(ProgressState.sampleParsingStage.getEndValue());
					SchemaWizardWebSocketUtility.getInstance().updateProgress(analyticsParams.getProgress(), analyticsParams.getSessionId());
				}
				i++;
			}
			analyticsParams.getProgress().setCurrentStateSplits(1);
		} 

	}

	private void embeddedDocumentBodyContentParse(ExtractedContent extractedContent, ContentHandler analyticsHandler) throws IOException, SAXException, TikaException, AnalyzerException, DataAccessException {
		FileInputStream fis = new FileInputStream(extractedContent.getExtractedFile());
		TemporaryResources tmp = new TemporaryResources();
		TikaInputStream extractedTis = TikaInputStream.get(fis, tmp);

		try {

			TikaProfilerParameters extractedContentParams = extractor.initializeTikaProfilingParameters(extractedTis, analyticsHandler, extractedContent.getMetadata());

			Metadata extractedMetadata = extractedContent.getMetadata();
			//extractedMetadata.set(Metadata.RESOURCE_NAME_KEY, extractedContent.getExtractedFile().getAbsolutePath());
			
			Parser extractedContentParser = getParser(extractedMetadata, extractedContentParams);
			if(extractedContentParser instanceof TikaProfilableParser) {
				profilableParse((TikaProfilableParser)extractedContentParser, extractedContentParams);
			} else {
				boolean shouldExtractEmbeddedDocumentBodyContent = Boolean.valueOf(extractedMetadata.get(AnalyticsDefaultDetector.HAS_BODY_CONTENT));

				TikaInputStream embeddedDocumentBodyStream = 
						extractProfilableContent(extractedContentParser, extractedTis, analyticsHandler, extractedMetadata, extractedContentParams);
				// get body contents as a new stream (in memory for now, need catch exception after certain size and write to file)

				if(shouldExtractEmbeddedDocumentBodyContent) {
					Metadata embeddedDocumentBodyMetadata = new Metadata();
					TikaProfilerParameters reinitializedEmbeddedDocumentParams = 
							extractor.initializeTikaProfilingParameters(embeddedDocumentBodyStream, analyticsHandler, embeddedDocumentBodyMetadata);
					//analyticsParams.getProgress().updateCurrentSampleNumerator(ProgressState.sampleParsingStage.getStartValue() + 10);
					//SchemaWizardWebSocketUtility.getInstance().updateProgress(analyticsParams.getProgress(), analyticsParams.getSessionId());
					analyticsDetect(embeddedDocumentBodyStream, analyticsHandler, embeddedDocumentBodyMetadata, reinitializedEmbeddedDocumentParams, true);
					Parser embeddedDocumentBodyParser = getParser(embeddedDocumentBodyMetadata, reinitializedEmbeddedDocumentParams);
					if(embeddedDocumentBodyParser instanceof TikaProfilableParser) {
						profilableParse((TikaProfilableParser)embeddedDocumentBodyParser, reinitializedEmbeddedDocumentParams);
						// profile the body content
					} else {
						throw new AnalyticsUnsupportedParserException("Body content could not be profiled for embedded document.");
					}
				} else {
					throw new AnalyticsUnsupportedParserException("Body content is not set to be extracted for embedded document.");
				}
				// handle embedded documents with a plain text body
			}
			// avoid recursion
			//analyticsParse(extractedTis, analyticsHandler, extractedMetadata, params, reverseGeocodingPass);

		} finally {
			tmp.dispose();
		}
	}

	private void profilableParse(TikaProfilableParser profilableParser, TikaProfilerParameters analyticsParams) throws IOException, AnalyzerException, DataAccessException {
		if(analyticsParams instanceof TikaSampleAnalyzerParameters) {
			profilableParser.runSampleAnalysis((TikaSampleAnalyzerParameters)analyticsParams);
		} else if(analyticsParams instanceof TikaSchemaAnalyzerParameters) {
			profilableParser.runSchemaAnalysis((TikaSchemaAnalyzerParameters)analyticsParams);
		} else {
			throw new AnalyticsTikaProfilingException("Parameters not an instance of sample or schema profilable parameters.");
		}
	}


	private TikaInputStream extractProfilableContent(Parser parser, TikaInputStream stream, ContentHandler frameworkHandler, Metadata metadata,
			final TikaProfilerParameters context) throws SAXException, TikaException, IOException {

		// if string buffer length is reached, write to file
		// special content handler?

		InputStream bodyInputStream;
		AnalyticsProgressTrackingContentHandler bch = new AnalyticsProgressTrackingContentHandler((INTERNAL_BUFFER_MAX_LENGTH));
		try {
			logger.info("Extracting content of " + metadata.get(Metadata.CONTENT_TYPE) + " file.");
			// simple body content extraction when body fits into a string buffer 

			SecureContentHandler sch = (bch != null) ? new SecureContentHandler(bch, stream) : null;
			parser.parse((TikaInputStream)stream, sch, metadata, context);

			extractor.setAreContentsExtracted(true);

			String s = bch.toString();
			bodyInputStream = new ByteArrayInputStream(s.getBytes());

		} catch (SAXException e) {
			logger.error("Failed to write body contents to internal string buffer.  Attempting to write to a temporary body content file.");

			stream.reset();

			String filePath = null;
			if(context.get(File.class) != null) {
				filePath = context.get(File.class).getAbsolutePath();
			} else {
				filePath = context.getUploadFileDir() + "-" +System.currentTimeMillis();
			}
			String fileNoExtension = filePath.substring(0, filePath.lastIndexOf('.'));

			if(!(new File(fileNoExtension).mkdir())) {
				throw new IOException("Unable to create temporary directory " + fileNoExtension + " to store body contents.");
			} else {
				File bodyContentFile = new File(fileNoExtension, fileNoExtension + "-body.txt");
				FileWriter fileWriter = new FileWriter(bodyContentFile);
				bch = new AnalyticsProgressTrackingContentHandler(fileWriter);

				try {
					SecureContentHandler sch = (bch != null) ? new SecureContentHandler(bch, stream) : null;
					parser.parse((TikaInputStream)stream, sch, metadata, context);

					fileWriter.close();

					bodyInputStream = new FileInputStream(bodyContentFile);
				} catch (Exception e2) {
					logger.error("Failed writing body contents to file.");
					throw new AnalyticsParsingRuntimeException(e2.getMessage(), e2, this);
				}
			}
		}

		TikaInputStream tis = TikaInputStream.get(bodyInputStream);

		return tis;

	}

	@Override
	public TikaSampleAnalyzerParameters runSampleAnalysis(TikaSampleAnalyzerParameters sampleProfilableParams)
			throws IOException, AnalyzerException {
		try {
			if(sampleProfilableParams.isReverseGeocodingPass()) {
				extractor.setAreContentsExtracted(true);
			}
			sampleProfilableParams.set(EmbeddedDocumentExtractor.class, extractor);
			sampleProfilableParams.set(File.class, sampleProfilableParams.get(File.class));
			sampleProfilableParams.set(Detector.class, detector);
			sampleProfilableParams.set(Parser.class, this);
			parse(sampleProfilableParams.getStream(), sampleProfilableParams.getHandler(), sampleProfilableParams.getMetadata(), sampleProfilableParams);
			sampleProfilableParams.setMediaType(sampleProfilableParams.getMetadata().get(Metadata.CONTENT_TYPE));
			sampleProfilableParams.getProgress().setCurrentState(ProgressState.geocodingStage);
			SchemaWizardWebSocketUtility.getInstance().updateProgress(sampleProfilableParams.getProgress(), sampleProfilableParams.getSessionId());
			if(sampleProfilableParams.getProfilerBean().getDsProfile().isEmpty()) {
				throw new AnalyticsUnsupportedParserException("Parsing finished without an exception, but no keys were extracted.");
			}
			return (TikaSampleAnalyzerParameters) sampleProfilableParams;
		} catch (SAXException | TikaException e) {
			if(e.getCause() instanceof AnalyticsUndetectableTypeException) {
				throw (AnalyticsUndetectableTypeException) e.getCause();
			} else if(e.getCause() instanceof AnalyticsUnsupportedParserException) {
				throw (AnalyticsUnsupportedParserException) e.getCause();
			} else {
				throw new AnalyticsTikaProfilingException(e);
			}
		}
	}

	@Override
	public TikaSchemaAnalyzerParameters runSchemaAnalysis(TikaSchemaAnalyzerParameters schemaProfilableParams)
			throws IOException, AnalyzerException {
		try {
			schemaProfilableParams.set(EmbeddedDocumentExtractor.class, extractor);
			schemaProfilableParams.set(File.class, schemaProfilableParams.get(File.class));
			schemaProfilableParams.set(Detector.class, detector);
			schemaProfilableParams.set(Parser.class, this);
			try {
				extractor.initAlreadyExtractedContentFromDisk();
				extractor.setAreContentsExtracted(true);
			} catch (IOException e) {
				logger.error("Contents expected to be on disk were not found.  Attempting to re-extract contents from original file.");
				logger.error(e);
				extractor.setAreContentsExtracted(false);
			}
			parse(schemaProfilableParams.getStream(), schemaProfilableParams.getHandler(), schemaProfilableParams.getMetadata(), schemaProfilableParams);
			schemaProfilableParams.getProgress().setCurrentState(ProgressState.complete);
			SchemaWizardWebSocketUtility.getInstance().updateProgress(schemaProfilableParams.getProgress(), schemaProfilableParams.getSessionId());
			return (TikaSchemaAnalyzerParameters) extractor.getParentParameters();
		} catch (SAXException | TikaException e) {
			throw new AnalyticsTikaProfilingException(e);
		}
	}

	public AnalyticsEmbeddedDocumentExtractor getExtractor() {
		return extractor;
	}

	public void setExtractor(AnalyticsEmbeddedDocumentExtractor extractor) {
		this.extractor = extractor;
	}

}
