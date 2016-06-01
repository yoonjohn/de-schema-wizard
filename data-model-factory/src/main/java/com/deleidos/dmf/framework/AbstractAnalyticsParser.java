package com.deleidos.dmf.framework;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.json.JSONObject;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.deleidos.dmf.analyzer.AnalyzerProgressUpdater;
import com.deleidos.dmf.exception.AnalyticsInitializationRuntimeException;
import com.deleidos.dmf.exception.AnalyticsParsingRuntimeException;
import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.exception.AnalyzerException;
import com.deleidos.dmf.progressbar.ProgressState;
import com.deleidos.dp.profiler.DefaultProfilerRecord;
import com.deleidos.dp.profiler.SampleProfiler;
import com.deleidos.dp.profiler.SampleReverseGeocodingProfiler;
import com.deleidos.dp.profiler.SchemaProfiler;
import com.deleidos.dp.profiler.api.Profiler;
import com.deleidos.dp.profiler.api.ProfilerRecord;

/**
 * The Analytics implementation of Tika Parsers.  This class is a protective abstract class that handles all loading 
 * metrics data into the appropriate profiler.  The idea of this class and its subclasses
 * is to convert records within a stream into {@link ProfilerRecord}.  This delegates record splitting functionality 
 * to subclasses and allows the framework to process streams or large files given a functional parser.  
 * <b> You must add the fully qualified class name to this 
 * project's src/main/resources/META-INF/services/org.apache.tika.parser.Parser file. </b>
 * @author leegc
 *
 */
public abstract class AbstractAnalyticsParser implements TikaProfilableParser {
	/**
	 * 
	 */
	private static final long serialVersionUID = -463118690334548751L;
	private static final Logger logger = Logger.getLogger(AbstractAnalyticsParser.class);
	public static final int RECORD_LIMIT = 1000000;
	public static final long UPDATE_FREQUENCY_IN_MILLIS = 500;
	private boolean interrupt = false;
	private Profiler profiler;
	private AnalyzerProgressUpdater progressUpdater;
	private TikaProfilerParameters params;

	/**
	 *  Parse method implemented for all Analytics parsers.  Subclasses' getNextProfilerRecord() methods will be called until
	 *  it returns null, interruptParse() is called, or an Exception is thrown.  When adding to the Analytics framework, 
	 *  one may override this method for more control of the progress updates or profiling method.
	 */
	@Override
	public void parse(InputStream stream, ContentHandler handler,
			Metadata metadata, ParseContext context) throws IOException, SAXException, TikaException {
		if(!(context instanceof TikaProfilerParameters)) {
			throw new AnalyticsInitializationRuntimeException("Context not instance of Tika Profiling Parameters.");
		}
		TikaProfilerParameters params = (TikaProfilerParameters) context;

		try {
			ProfilerRecord record = null;
			long startTime = System.currentTimeMillis();	

			for(int i = 0; i < RECORD_LIMIT; i++) {
				record = getNextProfilerRecord(stream, handler, metadata, params);
				if(record == null || interrupt) {
					if(interrupt) {
						logger.warn("Parsing interrupted after " + i + " records.");
					}
					break;
				} 
				loadToProfiler(record);

				if(System.currentTimeMillis() - startTime > UPDATE_FREQUENCY_IN_MILLIS) {
					progressUpdater.updateProgress();
					startTime = System.currentTimeMillis();
				}
			}
		} catch (AnalyticsTikaProfilingException e) {
			throw new AnalyticsParsingRuntimeException("Profiling exception from parser "+this.getClass().getName()+".", e, this);
		} 
	}

	public void initializeGlobalVariables(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) {

		profiler = context.get(Profiler.class);
		if(profiler == null) {
			throw new AnalyticsInitializationRuntimeException("Profiler not defined in context.");
		}

		if(!(context instanceof TikaProfilerParameters)) {
			throw new AnalyticsInitializationRuntimeException("Context is not an instance of TikaProfilerParameters.");
		}
		params = (TikaProfilerParameters) context;

		progressUpdater = context.get(AnalyzerProgressUpdater.class);
		if(progressUpdater == null) {
			throw new AnalyticsInitializationRuntimeException("Progress updater not defined in context.");
		} else {
			progressUpdater.init(params);
			if(profiler instanceof SampleProfiler) {
				((SampleProfiler)profiler).setProgressUpdateListener(progressUpdater);
			} else if(profiler instanceof SampleReverseGeocodingProfiler) {
				((SampleReverseGeocodingProfiler)profiler).setProgressUpdateListener(progressUpdater);
			} else if(profiler instanceof SchemaProfiler) {
				((SchemaProfiler)profiler).setProgressUpdateListener(progressUpdater);
			}
		}

	}

	private void callProfileAllRecords(TikaProfilerParameters profilableParameters) throws AnalyticsTikaProfilingException, SAXException, IOException {
		InputStream inputStream = profilableParameters.getStream();
		ContentHandler handler = profilableParameters.getHandler();
		Metadata metadata = profilableParameters.getMetadata();
		ParseContext context = profilableParameters;

		try {
			long t1 = System.currentTimeMillis();

			initializeGlobalVariables(inputStream, handler, metadata, context);
			boolean isParsingPass = (getParams().getProgress().getCurrentState()
					.equals(ProgressState.sampleParsingStage)) ? true : false;
			if(isParsingPass) {
				//getParams().getProgress().setCurrentState(ProgressState.LOCK);

				profileAllRecords(profilableParameters.getStream(), profilableParameters.getHandler(),
						profilableParameters.getMetadata(), profilableParameters);

				//getParams().getProgress().setCurrentState(ProgressState.UNLOCKED);
			} else {
				profileAllRecords(inputStream, handler, metadata, profilableParameters);
			}

			long t2 = System.currentTimeMillis();
			logger.debug("Parsing for " + getSupportedTypes(context) + " took " + (t2 - t1) + " millis.");	
		} catch (TikaException e) {
			throw new AnalyticsTikaProfilingException(e);
		}
	}

	/*public static void callProfileAllRecords(TikaProfilableParser profilableParser, TikaProfilerParameters profilableParameters) 
			throws AnalyticsTikaProfilingException, SAXException, IOException {
		InputStream inputStream = profilableParameters.getStream();
		ContentHandler handler = profilableParameters.getHandler();
		Metadata metadata = profilableParameters.getMetadata();
		ParseContext context = profilableParameters;

		try {
			long t1 = System.currentTimeMillis();

			if(profilableParser instanceof AbstractAnalyticsParser) {
				AbstractAnalyticsParser analyticsParser = (AbstractAnalyticsParser) profilableParser;
				analyticsParser.initializeGlobalVariables(inputStream, handler, metadata, context);
				ProgressState progressState = analyticsParser.getParams().getProgress().getCurrentState();
				boolean isFirstPass = (progressState.equals(ProgressState.STAGE2)) ? true : false;
				if(isFirstPass) {
					analyticsParser.getParams().getProgress().setCurrentState(ProgressState.LOCK);

					analyticsParser.profileAllRecords(profilableParameters.getStream(), profilableParameters.getHandler(),
							profilableParameters.getMetadata(), profilableParameters);

					analyticsParser.getParams().getProgress().setCurrentState(ProgressState.UNLOCKED);
				} else {
					analyticsParser.profileAllRecords(inputStream, handler, metadata, context);
				}
			} else {

				AnalyticsProgressUpdater progressUpdater = context.get(AnalyticsProgressUpdater.class);
				Profiler profiler = context.get(Profiler.class);

				if(progressUpdater == null || profiler == null) {
					throw new AnalyticsInitializationRuntimeException(
							"Progress updater or profiler not set in context: "+profilableParser.getClass().getName()+".");
				}

				long startTime = System.currentTimeMillis();

				ProfilerRecord record = null;
				for(int i = 0; i < RECORD_LIMIT; i++) {

					record = profilableParser.getNextProfilerRecord(inputStream, handler, metadata, context);
					if(record == null) {
						break;
					} else if(System.currentTimeMillis() - startTime > UPDATE_FREQUENCY_IN_MILLIS) {
						progressUpdater.updateProgress(inputStream, handler, metadata, context);
					}

					profiler.load(record);

				}


			}
			long t2 = System.currentTimeMillis();
			logger.debug("Parsing for " + profilableParser.getSupportedTypes(context) + " took " + (t2 - t1) + " millis.");	
		} catch (TikaException e) {
			throw new AnalyticsTikaProfilingException(e);
		}

	}*/

	/**
	 * Optional method that will run once before any Splitter methods are called or any bytes are read.  Useful for
	 * headers.  This method will not be called if parseAllRecords returns true.  
	 * This method has an empty body in AnalyticsTikaParser.java.
	 * 
	 * @param inputStream The stream that will be parsed.
	 * @param handler the handler context passed to the AnalyticsTikaParser
	 * @param metadata The metadata of the given stream (at minimum contains Metadata.CONTENT_TYPE and SampleProfiler.SOURCE_NAME)
	 * @param context the parsing context passed to the AnalyticsTikaParser
	 * @param splitter The splitter that will split the stream.
	 */
	public void preParse(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) {
		return;
	}

	public void postParse(ContentHandler handler, Metadata metadata, TikaProfilerParameters context) {
		return;
	}

	public void loadToProfiler(ProfilerRecord record) {
		profiler.load(record);
	}

	/**
	 * Manually push a JSON Object to the sample or schema handler.
	 * @param recordCharArray A JSON Object (as a character array) that is a fully flattened record from the stream.
	 * @throws org.xml.sax.SAXException - any SAX exception, possibly wrapping another exception

	protected void loadToProfiler(ProfilerRecord record) throws SAXException {
		profiler.load(record);
	} */

	/**
	 * Optional method to allow a subclass to handle all the parsing.  Push records to the defined profiler 
	 * using the loadToProfiler() method.  The handler and parse context are passed as arguments for as much control as Tika
	 * gives.  If this method is implemented, the subclass is expected to update the progress bar, and the preParse() method
	 * will not be called.
	 * @param inputStream The given stream, as reset after the detection phase.
	 * @param metadata Metadata given to the Advanced Analytics Tika Parsers
	 * @throws IOException should be thrown if the stream cannot be read
	 * @throws TikaException 
	 */
	private void profileAllRecords(InputStream stream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context)
			throws SAXException, IOException, TikaException {

		try {	

			preParse(stream, handler, metadata, context);			

			parse(stream, handler, metadata, context);

			postParse(handler, metadata, context);

		} catch (Exception e) {
			logger.error(e);
			throw new AnalyticsParsingRuntimeException("Exception caught during analysis parsing in " + getSupportedTypes(context) + " parser.", e, this);
		}

	}

	/**
	 * The method that should be implemented when adding parsers to the framework.  Read the stream and parse a single
	 * record out of it.  The JSON result should be a flattened object of the field keys and field values.  This method allows
	 * the framework to gather metrics in an efficient, "unlimited" manner.  Keys in the object are used as headers, or names,
	 * for each field, and the values are used to accumulate metrics.
	 *  
	 * @param inputStream the stream to be parsed.  This stream should not be closed in any subclass.
	 * @param handler the handler passed to the AnalyticsTikaParser
	 * @param metadata Metadata associated with this stream
	 * @param context the parsing context passed to the AnalyticsTikaParser
	 * @return a fully flattened JSON Object that represents one record, or null if parsing is completed.
	 * @throws IOException If the stream cannot be read.
	 * @deprecated Use {@link #getNextProfilerRecord()} instead
	 */
	public JSONObject parseSingleRecordAsJson(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws IOException{
		return null;
	}

	/**
	 * The method that should be implemented when adding parsers to the framework.  Read the stream and parse a single
	 * record out of it.  The JSON result should be a flattened object of the field keys and field values.  This method allows
	 * the framework to gather metrics in an efficient, "unlimited" manner.  Keys in the object are used as headers, or names,
	 * for each field, and the values are used to accumulate metrics.
	 *  
	 * @return a profiler record, or null if parsing is completed.
	 */
	public abstract ProfilerRecord getNextProfilerRecord(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws AnalyticsTikaProfilingException;

	protected static DefaultProfilerRecord flattenedJsonToDefaultProfilerRecord(JSONObject json, int charsRead) {
		if(json == null || json.keySet().size() == 0) {
			return null;
		} 
		DefaultProfilerRecord defaultProfilerRecord = new DefaultProfilerRecord();
		defaultProfilerRecord.setRecordProgress(charsRead);
		for(String key : json.keySet()) {
			defaultProfilerRecord.put(key, json.get(key));
		}
		return defaultProfilerRecord;
	}

	public void interruptParse() {
		interrupt = true;
	}

	public boolean isInterrupt() {
		return interrupt;
	}

	public void setInterrupt(boolean interrupt) {
		this.interrupt = interrupt;
	}

	public Profiler getProfiler() {
		return profiler;
	}

	public void setProfiler(Profiler profiler) {
		this.profiler = profiler;
	}

	public AnalyzerProgressUpdater getProgressUpdater() {
		return progressUpdater;
	}

	public void setProgressUpdater(AnalyzerProgressUpdater progressUpdater) {
		this.progressUpdater = progressUpdater;
	}

	public TikaProfilerParameters getParams() {
		return params;
	}

	public void setParams(TikaProfilerParameters params) {
		this.params = params;
	}

	/**
	 * Implementation of sampleAnalysis so Analyzers other then TikaAnalyzer may use the parsers if desired.  Need to call
	 * setInputStream, setMetadata, and setContentHandler to use outside of the TikaAnalyzer framework. 
	 */
	@Override
	public TikaSampleProfilableParameters sampleAnalysis(TikaSampleProfilableParameters sampleProfilableParams)
			throws AnalyzerException {
		try {
			callProfileAllRecords(sampleProfilableParams);
		} catch (SAXException | IOException e) {
			throw new AnalyticsTikaProfilingException(e);
		} 
		return sampleProfilableParams;
	}

	/**
	 * Implementation of schemaAnalysis so Analyzers other then TikaAnalyzer may use the parsers if desired.  Need to call
	 * setInputStream, setMetadata, and setContentHandler to use outside of the TikaAnalyzer framework. 
	 */
	@Override
	public TikaSchemaProfilableParameters schemaAnalysis(TikaSchemaProfilableParameters schemaProfilableParams)
			throws AnalyzerException {
		try {
			callProfileAllRecords(schemaProfilableParams);
		} catch (SAXException | IOException e) {
			throw new AnalyticsTikaProfilingException(e);
		} 
		return schemaProfilableParams;
	}
}
