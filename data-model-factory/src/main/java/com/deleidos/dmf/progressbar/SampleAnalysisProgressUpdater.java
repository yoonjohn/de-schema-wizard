package com.deleidos.dmf.progressbar;

import org.apache.log4j.Logger;

import com.deleidos.dmf.analyzer.AnalyzerParameters;
import com.deleidos.dmf.analyzer.AnalyzerProgressUpdater;
import com.deleidos.dmf.exception.AnalyticsInitializationRuntimeException;
import com.deleidos.dmf.framework.TikaSampleProfilableParameters;
import com.deleidos.dmf.handler.AnalyticsProgressTrackingContentHandler;
import com.deleidos.dmf.web.SchemaWizardWebSocketUtility;
import com.deleidos.dp.profiler.SampleProfiler;
import com.deleidos.dp.profiler.api.Profiler;

public class SampleAnalysisProgressUpdater implements AnalyzerProgressUpdater {
	private static final Logger logger = Logger.getLogger(SampleAnalysisProgressUpdater.class);
	private boolean sendUpdatesToProgressBar;
	private TikaSampleProfilableParameters params;
	private long total;

	@Override
	public void init(AnalyzerParameters parameters) {
		if(!(parameters instanceof TikaSampleProfilableParameters)) {
			throw new AnalyticsInitializationRuntimeException("Parameters not an instance of TikaProfilerParameters.");
		} else {
			params = (TikaSampleProfilableParameters) parameters;
			try {
				total = params.getStreamLength();
				if(total > 0) {
					sendUpdatesToProgressBar = true;
				} else {
					sendUpdatesToProgressBar = false;
				}
			} catch (Exception e) {
				logger.error(e);
				sendUpdatesToProgressBar = false;
			}
			sendUpdatesToProgressBar = true;
		}
	}

	@Override
	public void handleProgressUpdate(int progress) {
		float percentCompleted = (float) progress / (float) total;
		int numeratorUpdate = 
				(params.getProgress().getCurrentState().getStartValue())
				+ (int)((percentCompleted * params.getProgress().getCurrentState().rangeLength()));
		params.getProgress().updateCurrentSampleNumerator(numeratorUpdate);
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
