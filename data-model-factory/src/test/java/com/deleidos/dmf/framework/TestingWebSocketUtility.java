package com.deleidos.dmf.framework;

import org.apache.log4j.Logger;

import com.deleidos.dmf.analyzer.workflows.AbstractAnalyzerTestWorkflow;
import com.deleidos.dmf.progressbar.ProgressBarManager;
import com.deleidos.dmf.progressbar.ProgressBarManager.ProgressBar;
import com.deleidos.dmf.web.SchemaWizardSessionUtility;

public class TestingWebSocketUtility extends SchemaWizardSessionUtility {
	private static final Logger logger = Logger.getLogger(TestingWebSocketUtility.class);
	private boolean hasOutputTestInfo;
	private float lastValue;
	private int numCalls;
	private boolean error;
	
	public TestingWebSocketUtility() {
		hasOutputTestInfo = false;
		error = false;
		lastValue = 0;
		numCalls = 0;
		super.setPerformFakeUpdates(false);
	}
	
	@Override
	public void updateProgress(ProgressBarManager updater, String sessionId) {
		numCalls++;
		if(!hasOutputTestInfo) {
			logger.debug("This output shows that a test call was made to update the progress bar.");
			hasOutputTestInfo = true;
		}
		ProgressBar updateBean = updater.asBean();
		float currentValue = (float)updateBean.getNumerator() / (float)updateBean.getDenominator();
		if(currentValue < lastValue) {
			error = true;
			logger.error("Progress bar did not monotonically increase!");
		} else if(currentValue > 1) {
			logger.error("Progress over 1!");
		}
		
		super.updateProgress(updater, AbstractAnalyzerTestWorkflow.testSessionId);
		logger.debug("After " + numCalls + " calls to the fake progress bar, update sent as " + updateBean.getNumerator() + "/" + updateBean.getDenominator());
		
	}
	
	@Override
	public Boolean isCancelled(String sessionId) {
		return false;
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}
}
