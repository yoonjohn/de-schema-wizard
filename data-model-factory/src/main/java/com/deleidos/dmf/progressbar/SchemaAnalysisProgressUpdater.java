package com.deleidos.dmf.progressbar;

import org.apache.log4j.Logger;

import com.deleidos.dmf.analyzer.AnalyzerParameters;
import com.deleidos.dmf.analyzer.AnalyzerProgressUpdater;
import com.deleidos.dmf.exception.AnalyticsInitializationRuntimeException;
import com.deleidos.dmf.framework.AbstractAnalyticsParser;
import com.deleidos.dmf.framework.TikaSchemaProfilableParameters;
import com.deleidos.dmf.web.SchemaWizardWebSocketUtility;
import com.deleidos.dp.profiler.SchemaProfiler;
import com.deleidos.dp.profiler.api.Profiler;

public class SchemaAnalysisProgressUpdater implements AnalyzerProgressUpdater {
	private static final Logger logger = Logger.getLogger(SchemaAnalysisProgressUpdater.class);
	private boolean sendUpdatesToProgressBar;
	private TikaSchemaProfilableParameters params;
	private SchemaProfiler profiler;

	@Override
	public void init(AnalyzerParameters parameters) {
		if(!(parameters instanceof TikaSchemaProfilableParameters)) {
			throw new AnalyticsInitializationRuntimeException("Parameters not an instance of Schema Parameters.");
		} else {
			try {
				params = (TikaSchemaProfilableParameters) parameters;
				profiler = (SchemaProfiler) params.get(Profiler.class);
			} catch (Exception e) {
				sendUpdatesToProgressBar = false;
				logger.error("Disabling detailed progress updates.", e);
			}
			sendUpdatesToProgressBar = true;
		}
	}

	@Override
	public void handleProgressUpdate(int progress) {
		float percentCompleted = (float) progress / (float) AbstractAnalyticsParser.RECORD_LIMIT;
		int numeratorUpdate = 
				(params.getProgress().getCurrentState().getStartValue())
				+ (int)((percentCompleted * params.getProgress().getCurrentState().rangeLength()));
		if(numeratorUpdate != params.getProgress().getNumerator()%100) {

			params.getProgress().updateCurrentSampleNumerator(numeratorUpdate);
		}
	}

	@Override
	public void updateProgress() {
		if(!sendUpdatesToProgressBar) {
			return;
		} else {
			SchemaWizardWebSocketUtility.getInstance().updateProgress(params.getProgress(), params.getSessionId());	
		}
	}

}
