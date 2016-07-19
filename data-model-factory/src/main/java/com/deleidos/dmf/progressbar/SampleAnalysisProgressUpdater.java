package com.deleidos.dmf.progressbar;

import org.apache.log4j.Logger;

import com.deleidos.dmf.analyzer.AnalyzerParameters;
import com.deleidos.dmf.analyzer.AnalyzerProgressUpdater;
import com.deleidos.dmf.exception.AnalyticsInitializationRuntimeException;
import com.deleidos.dmf.framework.TikaSampleAnalyzerParameters;
import com.deleidos.dmf.handler.AnalyticsProgressTrackingContentHandler;
import com.deleidos.dmf.web.SchemaWizardWebSocketUtility;
import com.deleidos.dp.profiler.SampleProfiler;
import com.deleidos.dp.profiler.api.Profiler;

public class SampleAnalysisProgressUpdater implements AnalyzerProgressUpdater {
	private static final Logger logger = Logger.getLogger(SampleAnalysisProgressUpdater.class);
	private boolean sendUpdatesToProgressBar;
	private TikaSampleAnalyzerParameters params;
	private long total;
	private long progress;

	@Override
	public void init(AnalyzerParameters parameters) {
		if(!(parameters instanceof TikaSampleAnalyzerParameters)) {
			throw new AnalyticsInitializationRuntimeException("Parameters not an instance of TikaProfilerParameters.");
		} else {
			params = (TikaSampleAnalyzerParameters) parameters;
			try {
				total = params.getStreamLength();
				progress = 0;
				if(total > 0) {
					sendUpdatesToProgressBar = true;
				} else {
					sendUpdatesToProgressBar = false;
				}
			} catch (Exception e) {
				logger.error(e);
				sendUpdatesToProgressBar = false;
			}
		}
	}

	@Override
	public void handleProgressUpdate(int progress) {
		this.progress = progress;
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

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

}
