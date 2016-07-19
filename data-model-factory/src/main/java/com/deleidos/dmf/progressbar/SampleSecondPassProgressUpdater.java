package com.deleidos.dmf.progressbar;

import org.apache.log4j.Logger;

import com.deleidos.dmf.analyzer.AnalyzerParameters;
import com.deleidos.dmf.analyzer.AnalyzerProgressUpdater;
import com.deleidos.dmf.exception.AnalyticsInitializationRuntimeException;
import com.deleidos.dmf.framework.TikaSampleAnalyzerParameters;
import com.deleidos.dmf.web.SchemaWizardWebSocketUtility;
import com.deleidos.dp.profiler.SampleSecondPassProfiler;
import com.deleidos.dp.profiler.api.Profiler;

public class SampleSecondPassProgressUpdater implements AnalyzerProgressUpdater {
	private static final Logger logger = Logger.getLogger(SampleSecondPassProgressUpdater.class);
	private boolean sendUpdatesToProgressBar = false;
	private TikaSampleAnalyzerParameters params;
	private SampleSecondPassProfiler profiler;
	private int total;

	@Override
	public void init(AnalyzerParameters parameters) {
		final int approximateProgressUpdatesForGeocoding = 10;
		if(!(parameters instanceof TikaSampleAnalyzerParameters)) {
			throw new AnalyticsInitializationRuntimeException("Parameters not an instance of Sample Parameters for geocoding pass.");
		} else {
			try {
				params = (TikaSampleAnalyzerParameters) parameters;
				profiler = (SampleSecondPassProfiler) params.get(Profiler.class);
				if(params.getReverseGeocodingCallsEstimate() > 0) {
					int batchSize = params.getReverseGeocodingCallsEstimate()/approximateProgressUpdatesForGeocoding;
					batchSize = (batchSize > 500) ? 500 : batchSize;
					batchSize = (batchSize == 0) ? 5 : batchSize;
					profiler.setMinimumBatchSize(batchSize);
				}
				total = params.getReverseGeocodingCallsEstimate();
				sendUpdatesToProgressBar = true;
			} catch (Exception e) {
				logger.error(e);
				sendUpdatesToProgressBar = false;
			}
		}
	}

	@Override
	public void handleProgressUpdate(int progress) {
		float percentCompleted = (float) progress / (float) total;
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
