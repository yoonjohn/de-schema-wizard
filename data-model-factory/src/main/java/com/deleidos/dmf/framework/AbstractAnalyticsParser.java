package com.deleidos.dmf.framework;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.deleidos.dmf.exception.AnalyticsCancelledWorkflowException;
import com.deleidos.dmf.exception.AnalyticsInitializationRuntimeException;
import com.deleidos.dmf.exception.AnalyticsParsingRuntimeException;
import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.exception.AnalyzerException;
import com.deleidos.dmf.web.SchemaWizardSessionUtility;
import com.deleidos.dp.profiler.SampleProfiler;
import com.deleidos.dp.profiler.SampleSecondPassProfiler;
import com.deleidos.dp.profiler.SchemaProfiler;
import com.deleidos.dp.profiler.api.Profiler;
import com.deleidos.dp.profiler.api.ProfilerRecord;
import com.deleidos.dp.profiler.api.ProfilingProgressUpdateHandler;

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
	private boolean interrupt = false;
	private Profiler profiler;
	private TikaProfilerParameters params;
	private ProgressUpdatingBehavior progressBehavior;
	private ProfilingProgressUpdateHandler progressUpdater;

	public enum ProgressUpdatingBehavior {
		BY_CHARACTERS_READ, BY_RECORD_COUNT, BY_COMMON_FIELD_OCCURANCES
	}

	/**
	 *  Parse method implemented for all Analytics parsers.  Subclasses' getNextProfilerRecord() methods will be called until
	 *  it returns null, interruptParse() is called, or an Exception is thrown.  When adding to the Analytics framework, 
	 *  one should not need to override this method.
	 */
	@Override
	public void parse(InputStream stream, ContentHandler handler,
			Metadata metadata, ParseContext context) throws IOException, SAXException, TikaException {
		if(!(context instanceof TikaProfilerParameters)) {
			throw new AnalyticsInitializationRuntimeException("Context not an instance of Tika Profiling Parameters.");
		}
		TikaProfilerParameters params = (TikaProfilerParameters) context;

		try {
			ProfilerRecord record = null;

			for(int i = 0; i < RECORD_LIMIT; i++) {
				record = getNextProfilerRecord(stream, handler, metadata, params);
				if(record == null || interrupt) {
					if(interrupt) {
						logger.warn("Parsing interrupted after " + i + " records.");
					}
					break;
				} 
				loadToProfiler(record);

				if(SchemaWizardSessionUtility.getInstance().isCancelled(params.getSessionId())) {
					return; // cancel has occurred, but cant throw exception here
				}
				// only update progress at this level during the first pass (accumulators handle additional updates)
				// eventually we add progress updater callback to parser api
				switch(progressBehavior) {
				case BY_CHARACTERS_READ: progressUpdater.handleProgressUpdate(params.getCharsRead()); break;
				case BY_RECORD_COUNT: progressUpdater.handleProgressUpdate(i); break;
				case BY_COMMON_FIELD_OCCURANCES: break; // progress is handled in accumulators
				}
			}
		} catch (AnalyticsTikaProfilingException e) {
			throw new AnalyticsParsingRuntimeException("Profiling exception from parser "+this.getClass().getName()+".", e, this);
		} 
	}

	public void initializeGlobalVariables(InputStream stream, ContentHandler handler, 
			Metadata metadata, ParseContext context) {

		profiler = context.get(Profiler.class);
		if(profiler == null) {
			throw new AnalyticsInitializationRuntimeException("Profiler not defined in context.");
		}

		if(!(context instanceof TikaProfilerParameters)) {
			throw new AnalyticsInitializationRuntimeException("Context is not an instance of TikaProfilerParameters.");
		}
		params = (TikaProfilerParameters) context;
		progressBehavior = params.getProgressUpdatingBehavior();
		if(progressBehavior == null) {
			progressBehavior = ProgressUpdatingBehavior.BY_CHARACTERS_READ;
		}

		progressUpdater = context.get(ProfilingProgressUpdateHandler.class);
		if(progressUpdater == null) {
			throw new AnalyticsInitializationRuntimeException("Progress updater not defined in context.");
		} else {
			if(profiler instanceof SampleProfiler) {
				((SampleProfiler)profiler).setProgressUpdateListener(progressUpdater);
			} else if(profiler instanceof SampleSecondPassProfiler) {
				((SampleSecondPassProfiler)profiler).setProgressUpdateListener(progressUpdater);
			} else if(profiler instanceof SchemaProfiler) {
				((SchemaProfiler)profiler).setProgressUpdateListener(progressUpdater);
			}
		}

	}

	/**
	 * Optional method that will run once before the parse method is called.  Useful for
	 * headers.  This method will not be called if parseAllRecords returns true.  
	 * This method has an empty body in AnalyticsTikaParser.java.
	 * 
	 * @param inputStream The stream that will be parsed.
	 * @param handler the handler context passed to the AnalyticsTikaParser
	 * @param metadata The metadata of the given stream (at minimum contains Metadata.CONTENT_TYPE and SampleProfiler.SOURCE_NAME)
	 * @param context the parsing context passed to the AnalyticsTikaParser
	 * @param splitter The splitter that will split the stream.
	 * @throws AnalyticsTikaProfilingException a checked exception that should stop the parsing and be reported
	 */
	public void preParse(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		return;
	}

	private void profileAllRecords(TikaProfilerParameters profilableParameters) throws AnalyticsTikaProfilingException, SAXException, IOException {
		InputStream inputStream = profilableParameters.getStream();
		ContentHandler handler = profilableParameters.getHandler();
		Metadata metadata = profilableParameters.getMetadata();

		long t1 = System.currentTimeMillis();
		try {
			initializeGlobalVariables(inputStream, handler, metadata, profilableParameters);

			preParse(inputStream, handler, metadata, profilableParameters);

			parse(inputStream, handler, metadata, profilableParameters);
			if(SchemaWizardSessionUtility.getInstance().isCancelled(profilableParameters.getSessionId())) {
				throw new AnalyticsCancelledWorkflowException("Workflow cancelled during parsing.");
			}

			postParse(handler, metadata, profilableParameters);

			long t2 = System.currentTimeMillis();
			logger.debug("Parsing for " + getSupportedTypes(profilableParameters) + " took " + (t2 - t1) + " millis.");	
		} catch (TikaException e) {
			if(SchemaWizardSessionUtility.getInstance().isCancelled(profilableParameters.getSessionId())) {
				throw new AnalyticsCancelledWorkflowException("Workflow cancelled during parsing.");
			}
			long t2 = System.currentTimeMillis();
			logger.error("Parsing for " + getSupportedTypes(profilableParameters) + " failed after " + (t2 - t1) + " millis.");
			throw new AnalyticsTikaProfilingException(e);
		}
	}

	/**
	 * Optional method that will run once after the parse method is called.
	 * @param handler
	 * @param metadata
	 * @param context
	 * @throws AnalyticsTikaProfilingException a checked exception that should stop the parsing and be reported
	 */
	public void postParse(ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		return;
	}

	public void loadToProfiler(ProfilerRecord record) {
		profiler.load(record);
	}

	/**
	 * The method that should be implemented when adding parsers to the framework.  Read the stream and parse a single
	 * record out of it.  The ProfilerRecord result is a map of field keys to a list of its values.  This method allows
	 * the framework to gather metrics in an efficient, "unlimited" manner.  Keys in the object are used as headers, or names,
	 * for each field, and the values are used to accumulate metrics.  Subclasses can simply use the 
	 * DefaultProfilerRecord in the com.deleidos.dp.profiler package 
	 * to accumulate records, though they may implement a different strategy if desired.  Return null when parsing has
	 * been completed.
	 *  
	 * @return a profiler record, or null if parsing is completed.
	 */
	public abstract ProfilerRecord getNextProfilerRecord(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws AnalyticsTikaProfilingException;

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

	public ProfilingProgressUpdateHandler getProgressUpdater() {
		return progressUpdater;
	}

	public void setProgressUpdater(ProfilingProgressUpdateHandler progressUpdater) {
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
	public TikaSampleAnalyzerParameters runSampleAnalysis(TikaSampleAnalyzerParameters sampleProfilableParams)
			throws AnalyzerException {
		try {
			profileAllRecords(sampleProfilableParams);
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
	public TikaSchemaAnalyzerParameters runSchemaAnalysis(TikaSchemaAnalyzerParameters schemaProfilableParams)
			throws AnalyzerException {
		try {
			profileAllRecords(schemaProfilableParams);
		} catch (SAXException | IOException e) {
			throw new AnalyticsTikaProfilingException(e);
		} 
		return schemaProfilableParams;
	}


}
