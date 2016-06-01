package com.deleidos.dmf.analyzer;

import com.deleidos.dp.profiler.api.ProfilingProgressUpdateListener;

public interface AnalyzerProgressUpdater extends ProfilingProgressUpdateListener {

	/**
	 * Initialize the progress based on the parameters.  This method will be called before any updateProgress(parameters) calls are made.
	 * @param parameters the parameters with fields that may help an implementation of this interface determine progress intervals.
	 */
	public void init(AnalyzerParameters parameters);
	
	public void updateProgress();
	
	public static AnalyzerProgressUpdater EMPTY_PROGRESS_UPDATER = new EmptyProgressUpdater();
	
	public static class EmptyProgressUpdater implements AnalyzerProgressUpdater {

		@Override
		public void init(AnalyzerParameters parameters) {
			return;
		}

		@Override
		public void handleProgressUpdate(int progress) {
			return;
		}

		@Override
		public void updateProgress() {
			return;
		}
		
	}
}
