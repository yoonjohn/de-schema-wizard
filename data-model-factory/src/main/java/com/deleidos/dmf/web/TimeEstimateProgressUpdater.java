package com.deleidos.dmf.web;

import org.apache.log4j.Logger;

import com.deleidos.dmf.progressbar.ProgressBarManager;
import com.deleidos.dmf.progressbar.SimpleProgressUpdater;

public class TimeEstimateProgressUpdater extends SimpleProgressUpdater implements Runnable {
	private static final Logger logger = Logger.getLogger(TimeEstimateProgressUpdater.class);
	private final long startTime;
	private final long endTime;
	private volatile boolean isDone;

	public TimeEstimateProgressUpdater(String sessionId, ProgressBarManager progress, long estimateMillis) {
		super(sessionId, progress, estimateMillis);
		this.startTime = System.currentTimeMillis();
		this.endTime =  startTime + estimateMillis;
		logger.debug("start " + startTime + " end " + endTime);
		isDone = false;
	}

	@Override
	public void run() {
		long current = System.currentTimeMillis();
		synchronized(this.getProgressBar()) {
			while(current < endTime && !isDone) {
				long dif = current - startTime;
				handleProgressUpdate(dif);
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					logger.error("Unexpected interrupt.  Ending thread.", e);
					return;
				}
				current = System.currentTimeMillis();
			}
		}
		logger.debug("done");
	}

	public boolean isDone() {
		return isDone;
	}

	public void setDone() {
		this.isDone = true;
	}

}
