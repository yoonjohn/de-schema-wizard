package com.deleidos.dmf.framework;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.pdfbox.io.IOUtils;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.deleidos.dmf.analyzer.AnalyzerProgressUpdater;
import com.deleidos.dmf.exception.AnalyticsInitializationRuntimeException;
import com.deleidos.dmf.exception.AnalyticsRuntimeException;
import com.deleidos.dmf.progressbar.ProgressBar;
import com.deleidos.dp.h2.H2SampleDataAccessObject;
import com.deleidos.dp.profiler.api.Profiler;

public class AnalyticsEmbeddedDocumentExtractor implements EmbeddedDocumentExtractor {
	private static final Logger logger = Logger.getLogger(AnalyticsEmbeddedDocumentExtractor.class);
	//private boolean shouldParseEmbedded;
	private TikaProfilerParameters parentParameters;
	private boolean isAllContentExtracted;
	private List<ExtractedContent> extractedContents;

	public AnalyticsEmbeddedDocumentExtractor(TikaProfilerParameters params) {
		this.parentParameters = params;
		if(params instanceof TikaSchemaProfilableParameters) {
			this.isAllContentExtracted = true;
		} else {
			this.isAllContentExtracted = false;
		}
		this.extractedContents = new ArrayList<ExtractedContent>();
		//this.shouldParseEmbedded = true;
	}

	@Override
	public boolean shouldParseEmbedded(Metadata metadata) {
		return !isAllContentExtracted;
	}

	@Override
	public void parseEmbedded(InputStream stream, ContentHandler handler, Metadata metadata, boolean outputHtml)
			throws SAXException, IOException {	

		try {

			File extractedContentDirectory = getOrCreateExtractedContentDirectory();
			parentParameters.setExtractedContentDir(extractedContentDirectory.getAbsolutePath());
			String name = generateEmbeddedResourceName(metadata);

			Detector detector = parentParameters.get(Detector.class, new AnalyticsDefaultDetector());


			logger.info("Extracting embedded content: " + name + ".");
			TemporaryResources tmp = new TemporaryResources();
			TikaInputStream tis = TikaInputStream.get(stream, tmp);
			try {
				MediaType type = detector.detect(tis, metadata);
				metadata.set(Metadata.CONTENT_TYPE, type.toString());

				File embeddedDocumentFile = new File(parentParameters.getExtractedContentDir(), name);
				embeddedDocumentFile = writeToFile(embeddedDocumentFile, tis, handler, metadata);

				if(embeddedDocumentFile.length() == 0) {
					throw new AnalyticsInitializationRuntimeException("Embedded document " + name + " is empty.");
				}
				metadata.set(Metadata.RESOURCE_NAME_KEY, name);
				ExtractedContent content = new ExtractedContent(embeddedDocumentFile, metadata);
				extractedContents.add(content);

				Parser nestedParser = parentParameters.get(Parser.class);
				if(!(nestedParser instanceof AnalyticsDefaultParser)) {
					throw new AnalyticsInitializationRuntimeException("Parser is not anlytics default parser.");
				} else {

					Parser nestedContentExtractionParser = ((AnalyticsDefaultParser)nestedParser).getParser(metadata, parentParameters);
					if(nestedContentExtractionParser instanceof TikaProfilableParser) {
						//logger.info("Nested parser is an instance of TikaProfilable.  All extractable content fully removed from " + name + ".");
						return;
					} else {
						logger.info("Further extraction with " + nestedContentExtractionParser.getClass().getSimpleName() +".");
						FileInputStream fis = new FileInputStream(embeddedDocumentFile);
						TemporaryResources nestedTmp = new TemporaryResources();
						TikaInputStream nestedTis = TikaInputStream.get(fis, nestedTmp);

						AnalyticsEmbeddedDocumentExtractor nestedAnalyticsExtractor = new AnalyticsEmbeddedDocumentExtractor(parentParameters);
						TikaProfilerParameters params = initializeTikaProfilingParameters(nestedTis, handler, metadata);
						params.setExtractedContentDir(parentParameters.getExtractedContentDir());
						params.set(EmbeddedDocumentExtractor.class, nestedAnalyticsExtractor);
						nestedContentExtractionParser.parse(nestedTis, handler, metadata, params);
						this.extractedContents.addAll(nestedAnalyticsExtractor.getExtractedContents());
					}
				}			
			} catch (Exception e) {
				logger.error(e);
				throw new AnalyticsRuntimeException("Error extracting embedded document.", e);
			} finally {
				tmp.dispose();
			}

		} catch (TikaException e) {
			logger.error("Error parsing embedded document.", e);
		} 
	} 

	public void initAlreadyExtractedContentFromDisk() throws IOException, TikaException {
		// contents are already on disk
		// don't extract, just detect metadata and add to extracted content list

		Detector detector = parentParameters.get(Detector.class, new AnalyticsDefaultDetector());

		if(parentParameters.getExtractedContentDir() == null) {
			logger.info("No extracted content found.");
		} else {
			File extractedDir = new File(parentParameters.getExtractedContentDir());
			for(File extractedFile : extractedDir.listFiles()) {
				TemporaryResources tmp = new TemporaryResources();
				TikaInputStream tis = TikaInputStream.get(new FileInputStream(extractedFile), tmp);
				try {
					Metadata reinitializedMetadata = new Metadata();
					MediaType type = detector.detect(tis, reinitializedMetadata);
					reinitializedMetadata.set(Metadata.CONTENT_TYPE, type.toString());
					reinitializedMetadata.set(Metadata.RESOURCE_NAME_KEY, extractedFile.getName());
					ExtractedContent content = new ExtractedContent(extractedFile, reinitializedMetadata);
					extractedContents.add(content);
				} finally {
					tmp.dispose();
				}
			}
		}
	}

	private String generateEmbeddedResourceName(Metadata metadata) {
		final String defaultName = "embedded-document";

		String embeddedDocumentName = (metadata.get(Metadata.RESOURCE_NAME_KEY) != null) ? metadata.get(Metadata.RESOURCE_NAME_KEY) : defaultName;

		File embeddedDocumentFile = new File(embeddedDocumentName);
		embeddedDocumentName = embeddedDocumentFile.getName();
		if(embeddedDocumentName.contains(File.separator)) {
			embeddedDocumentName = embeddedDocumentName.substring(0, embeddedDocumentName.lastIndexOf(File.separator));
		}
		Set<String> files = new HashSet<String>();
		for(String file : new File(parentParameters.getExtractedContentDir()).list()) {
			files.add(file);
		}
		embeddedDocumentName = H2SampleDataAccessObject.generateNewSampleName(embeddedDocumentName, files);

		return embeddedDocumentName;
	}

	private File getOrCreateExtractedContentDirectory() throws IOException {
		if(parentParameters.get(File.class) == null) {
			logger.error("File not found in context.  Embedded document parsing requires a file in context.");
			throw new IOException("Required file context not found for Embedded document parser.");
		}
		File fileLocation = parentParameters.get(File.class);

		File extractedContentDirectory;
		if(parentParameters.getExtractedContentDir() == null) {
			extractedContentDirectory = new File(fileLocation.getParent(), fileLocation.getName().substring(0, fileLocation.getName().lastIndexOf('.')));
			if(extractedContentDirectory.isDirectory() && extractedContentDirectory.exists()) {
				Set<String> existingSampleNames = new HashSet<String>();
				for(String file : extractedContentDirectory.getParentFile().list()) {
					existingSampleNames.add(file);
				}
				String extractedName = H2SampleDataAccessObject.generateNewSampleName(extractedContentDirectory.getName(), existingSampleNames);
				extractedContentDirectory = new File(fileLocation.getParent(), extractedName);
			}
			if(!extractedContentDirectory.mkdir()) {
				throw new IOException("Embedded document extraction failed because " + extractedContentDirectory + " could not be created.");
			} 
			logger.info("Embedded documents being extracted into " + extractedContentDirectory.getAbsolutePath() + ".");
		} else {
			//logger.info("Using " + parentParameters.getExtractedContentDir() + ".");
			extractedContentDirectory = new File(parentParameters.getExtractedContentDir());
		}
		return extractedContentDirectory;
	}

	public TikaProfilerParameters getParentParameters() {
		return parentParameters;
	}

	public void setParentParameters(TikaProfilerParameters parentParameters) {
		this.parentParameters = parentParameters;
	}

	private File writeToFile(File embeddedDocument, InputStream stream, ContentHandler handler, Metadata metadata) throws IOException {

		FileOutputStream fos = new FileOutputStream(embeddedDocument);
		IOUtils.copy(stream, fos);
		fos.close();

		return embeddedDocument;
	}

	public TikaProfilerParameters initializeTikaProfilingParameters(InputStream inputStream, ContentHandler handler, Metadata metadata) throws IOException {
		TikaProfilerParameters parameters;
		if(parentParameters instanceof TikaSampleProfilableParameters) {
			TikaSampleProfilableParameters sampleParams = (TikaSampleProfilableParameters) parentParameters; 
			parameters = new TikaSampleProfilableParameters(sampleParams.get(Profiler.class), sampleParams.get(AnalyzerProgressUpdater.class), 
					sampleParams.getUploadFileDir(), sampleParams.getGuid(), inputStream, handler, metadata); 
			((TikaSampleProfilableParameters)parameters).setReverseGeocodingCallsEstimate(sampleParams.getReverseGeocodingCallsEstimate());
		} else if(parentParameters instanceof TikaSchemaProfilableParameters) {
			TikaSchemaProfilableParameters schemaParams = (TikaSchemaProfilableParameters) parentParameters;
			parameters = new TikaSchemaProfilableParameters(schemaParams.get(Profiler.class), schemaParams.get(AnalyzerProgressUpdater.class), 
					schemaParams.getUploadFileDir(), schemaParams.getGuid(), schemaParams.getUserModifiedSampleList());
			parameters.setStream(inputStream);
			parameters.setHandler(handler);
			parameters.setMetadata(metadata);
		} else {
			inputStream.close();
			throw new AnalyticsRuntimeException("Parameters not defined as sample or schema parameters.");
		}
		ProgressBar progress = parentParameters.getProgress();
		parameters.setStreamLength(metadata.get(Metadata.CONTENT_LENGTH) != null ? Integer.valueOf(metadata.get(Metadata.CONTENT_LENGTH)) : 0);
		parameters.setProgress(progress);
		File parentFile = parentParameters.get(File.class);
		parameters.set(File.class, parentFile);
		parameters.set(Detector.class, parentParameters.get(Detector.class));
		parameters.set(Parser.class, parentParameters.get(Parser.class));
		parameters.set(EmbeddedDocumentExtractor.class, parentParameters.get(EmbeddedDocumentExtractor.class));
		parameters.set(PDFParserConfig.class, parentParameters.get(PDFParserConfig.class));
		parameters.setSessionId(parentParameters.getSessionId());
		//EmbeddedDocumentExtractor extractor= new ParsingEmbeddedDocumentExtractor(parameters);
		//parameters.set(EmbeddedDocumentExtractor.class, extractor);
		return parameters;
	}

	public class ExtractedContent {
		private File extractedFile;
		private Metadata metadata;
		public ExtractedContent(File file, Metadata metadata) {
			setExtractedFile(file);
			setMetadata(metadata);
		}
		public File getExtractedFile() {
			return extractedFile;
		}
		public void setExtractedFile(File extractedFile) {
			this.extractedFile = extractedFile;
		}
		public Metadata getMetadata() {
			return metadata;
		}
		public void setMetadata(Metadata metadata) {
			this.metadata = metadata;
			this.metadata.set(Metadata.CONTENT_LENGTH, String.valueOf(extractedFile.length())) ;
		}
	}

	public List<ExtractedContent> getExtractedContents() {
		return extractedContents;
	}

	public void setExtractedContents(List<ExtractedContent> extractedContents) {
		this.extractedContents = extractedContents;
	}

	public boolean areContentsExtracted() {
		return isAllContentExtracted;
	}

	public void setAreContentsExtracted(boolean extractedAllContents) {
		this.isAllContentExtracted = extractedAllContents;
	}

}
