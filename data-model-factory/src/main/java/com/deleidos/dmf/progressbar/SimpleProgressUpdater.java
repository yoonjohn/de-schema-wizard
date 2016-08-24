package com.deleidos.dmf.progressbar;

import java.util.Optional;

import org.apache.log4j.Logger;

import com.deleidos.dmf.web.SchemaWizardSessionUtility;
import com.deleidos.dp.profiler.api.ProfilingProgressUpdateHandler;

public class SimpleProgressUpdater implements ProfilingProgressUpdateHandler {
	private static final Logger logger = Logger.getLogger(SimpleProgressUpdater.class);
	private final ProgressBarManager progressBar;
	private final long totalRecords;
	private final String sessionId;
	private Optional<DescriptionCallback> descriptionCallback = Optional.empty();
	
	public SimpleProgressUpdater(String sessionId, ProgressBarManager progressBar, long recordCount) {
		this(sessionId, progressBar, recordCount, true);
	}
	
	public SimpleProgressUpdater(String sessionId, ProgressBarManager progressBar, long recordCount, boolean smoothUpdates) {
		this.progressBar = progressBar;
		this.totalRecords = recordCount;
		this.sessionId = sessionId;
	}

	@Override
	public void handleProgressUpdate(long progress) {
		float percentCompleted = (float) progress / (float) totalRecords;
		int numeratorUpdate = 
				(int)progressBar.getCurrentState().getStartValue() + 
				(int)((percentCompleted * progressBar.getCurrentState().rangeLength()));
		if(numeratorUpdate != 0) {
			if(progressBar.updateNumerator(numeratorUpdate)) {
				descriptionCallback.ifPresent(x->progressBar.getCurrentState().setDescription(x.determineDescription(this, progress)));
			}
		}
		SchemaWizardSessionUtility.getInstance().updateProgress(progressBar, sessionId);
		
	}

	public ProgressBarManager getProgressBar() {
		return progressBar;
	}

	public long getTotalRecords() {
		return totalRecords;
	}

	public interface DescriptionCallback {
		public String determineDescription(SimpleProgressUpdater progressUpdater, long progress);
	}

	public Optional<DescriptionCallback> getDescriptionCallback() {
		return descriptionCallback;
	}

	public void setDescriptionCallback(DescriptionCallback descriptionCallback) {
		this.descriptionCallback = Optional.of(descriptionCallback);
	}
	
	public void removeDescriptionCallback() {
		this.descriptionCallback = Optional.empty();
	}
}
