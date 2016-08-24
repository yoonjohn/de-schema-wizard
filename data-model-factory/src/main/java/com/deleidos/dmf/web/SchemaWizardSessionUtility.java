package com.deleidos.dmf.web;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.log4j.Logger;

import com.deleidos.analytics.websocket.WebSocketServer;
import com.deleidos.analytics.websocket.api.WebSocketApiPlugin;
import com.deleidos.analytics.websocket.api.WebSocketEventListener;
import com.deleidos.analytics.websocket.api.WebSocketMessage;
import com.deleidos.analytics.websocket.api.WebSocketMessageFactory;
import com.deleidos.dmf.progressbar.ProgressBarManager;
import com.deleidos.dmf.progressbar.ProgressBarManager.ProgressBar;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class SchemaWizardSessionUtility implements WebSocketApiPlugin, WebSocketMessageFactory, WebSocketEventListener {
	private static SchemaWizardSessionUtility INSTANCE = null;
	private static final Logger logger = Logger.getLogger(SchemaWizardSessionUtility.class);
	private final BiMap<String, String> sessionSocketBidirectionalMapping;
	private final Map<String, SessionData> sessionDataMapping;
	private final Map<String, Long> memoryUsageMapping;
	private final ExecutorService executorService;
	private long overheadEstimate;
	private long updateFrequencyMillis = 100;
	private long fakeUpdateDelay = 105;
	private int noticeableProgressJump = 5;
	private int minFakeUpdates = 10;
	private boolean performFakeUpdates = true;
	public final String OVER_ESTIMATE_ENV_VAR = "OVER_ESTIMATE_MULTIPLIER";
	public static double OVER_ESTIMATE_MULTIPLIER;

	protected SchemaWizardSessionUtility() {
		try {
			OVER_ESTIMATE_MULTIPLIER = (System.getenv(OVER_ESTIMATE_ENV_VAR)) != null ? Double.valueOf(System.getenv(OVER_ESTIMATE_ENV_VAR)) : 6;
		} catch (Exception e) {
			OVER_ESTIMATE_MULTIPLIER = 6;
		}
		executorService = Executors.newCachedThreadPool();
		sessionSocketBidirectionalMapping = HashBiMap.create();
		memoryUsageMapping = new ConcurrentHashMap<String, Long>();
		sessionDataMapping = new ConcurrentHashMap<String, SessionData>();
		overheadEstimate = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}

	public static SchemaWizardSessionUtility getInstance(SchemaWizardSessionUtility testUtility) {
		if(INSTANCE == null) {
			INSTANCE = testUtility;
			logger.info("Registering plugin with web socket server.");
		}
		return INSTANCE;
	}

	public static void register() {
		WebSocketServer.getInstance().registerPlugin(SchemaWizardSessionUtility.getInstance());
	}

	public static SchemaWizardSessionUtility getInstance() {
		if(INSTANCE == null) {
			INSTANCE = new SchemaWizardSessionUtility();
			logger.info("Registering plugin with web socket server.");
			WebSocketServer.getInstance().registerPlugin(INSTANCE);
		}
		return INSTANCE;
	}

	/**
	 * Update progress if the socket/session mapping contains the session id.
	 * @param updateBean
	 * @param sessionId
	 */
	public synchronized void updateProgress(ProgressBarManager updater, String sessionId) {
		ProgressBar updateBean = updater.asBean();
		try {
			if(isAssociated(sessionId)) {
				boolean shouldUpdate = sessionDataMapping.get(sessionId).shouldUpdate();
				if(shouldUpdate) {
					if(updateBean.getNumerator() / updateBean.getDenominator() == 1) {
						sessionDataMapping.get(sessionId).setDone(true);
						logger.info("Analysis for session "+sessionId+" determined to be complete.");
					}
					// if the progress jump is large, send a few intermediary updates to smooth it out - side effects
					smoothUpdate(updater, sessionId);					
					WebSocketServer.getInstance().send(updateBean, sessionSocketBidirectionalMapping.get(sessionId));
					sessionDataMapping.get(sessionId).setLastUpdateNumerator(updater.getNumerator());
				}
			} 
		} catch (Exception e) {
			logger.debug("Progress update failed to send to session " + sessionId + ".", e);
			if(sessionDataMapping.containsKey(sessionId)) {
				sessionDataMapping.get(sessionId).setSendingErrors(
						sessionDataMapping.get(sessionId).getSendingErrors()+1);
				if(sessionDataMapping.get(sessionId).getSendingErrors() >= SessionData.ERROR_CUTOFF) {
					logger.error(SessionData.ERROR_CUTOFF + " errors have been caught for session " + sessionId +
							".  Attempting to send on last message and then disabling progress updates.", e);
					try {
						updater.getCurrentState().setDescription("Sorry, there was a problem gauging the progress of your analysis.");
						WebSocketServer.getInstance().send(updateBean, sessionSocketBidirectionalMapping.get(sessionId));
					} catch (Exception e1) {
						logger.error("Exception sending message over websocket.", e);
					}
				}
			}
		}
	}

	public void associateSessionAndSocket(String sessionId, String socketId) {
		logger.debug("Session "+sessionId+" associated with socket " +socketId + "." );
		sessionSocketBidirectionalMapping.put(sessionId, socketId);
		sessionDataMapping.put(sessionId, new SessionData());
	}

	private boolean isAssociated(String sessionId) {
		return sessionSocketBidirectionalMapping.containsKey(sessionId) && sessionDataMapping.containsKey(sessionId);
	}

	@Override
	public List<WebSocketEventListener> getWebSocketEventListeners() {
		return Arrays.asList(this);
	}

	@Override
	public List<WebSocketMessageFactory> getWebSocketMessageFactories() {
		return Arrays.asList(this);
	}

	@Override
	public List<String> getResourcePackages() {
		// unnecessary for now
		// add for service layer accessor
		return null;
	}

	@Override
	public WebSocketMessage buildMessage(String message, String webSocketId) {
		// if sample/schema analysis is started here, progress updating will be handled in the same thread as the analysis
		// handle communications coming from the client
		// as of 2/9, only a session id message
		try {
			JsonNode j = SerializationUtility.getObjectMapper().readTree(message);
			JsonNode sessionId = j.path("sessionId");
			JsonNode request = j.path("request");
			if(sessionId != null) {
				//SchemaWizardWebSocketSession session = SerializationUtility.deserialize(message, SchemaWizardWebSocketSession.class);
				//can use beans if more fields become necessary
				String sessionIdString = sessionId.asText(null);
				if(sessionIdString == null) {
					logger.error("Session Id received from client as null.  Progress bar not successfully initialized.");
				} else {
					SchemaWizardSessionUtility.getInstance().associateSessionAndSocket(sessionIdString, webSocketId);
				}

			} else {
				logger.warn("Received unexpected message: " + message);
			}
		} catch (IOException e) {
			logger.error("Received non parseable message: " + message, e);
		}

		// returning null is fine
		return null; 
	}

	public void endSession(String sessionId) {
		sessionSocketBidirectionalMapping.remove(sessionId);
		sessionDataMapping.remove(sessionId);
		memoryUsageMapping.remove(sessionId);
		overheadEstimate = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}

	@Override
	public void onWebSocketClose(String webSocketId) {
		String associatedSessionId = sessionSocketBidirectionalMapping.inverse().get(webSocketId);
		if(!sessionDataMapping.get(associatedSessionId).isDone()) {
			// set the parameters as cancelled if they were not finished
			// parsers will check these parameters to see if there was a cancellation
			logger.info("Session "+associatedSessionId+" cancelled.");
			sessionDataMapping.get(associatedSessionId).setCancelled(true);
		}
		endSession(associatedSessionId);
		logger.debug("Socket " + webSocketId + " with associated session " + associatedSessionId + " closed.");
	}

	@Override
	public void onWebSocketConnect(String webSocketId) {
		logger.debug("Socket " + webSocketId + " opened.  Awaiting session ID.");
	}

	public Boolean isCancelled(String sessionId) {
		Optional<SessionData> sessionData = Optional.ofNullable(sessionDataMapping.get(sessionId));
		if(sessionData.isPresent()) {
			return sessionData.get().isCancelled();
		} else {
			return true;
		}
	}

	public void waitForAvailableResources(String sessionId, File sampleFile) throws FileUploadException {
		Runtime instance = Runtime.getRuntime();
		long totalMemory = instance.totalMemory();
		if(totalMemory < sampleFile.length()) {
			throw new FileUploadException("File is too large to be processed with current JVM settings.");
		}
		long heapUsageOverEstimate = (long)(((int)sampleFile.length())*OVER_ESTIMATE_MULTIPLIER);
		long t1 = System.currentTimeMillis();
		synchronized (memoryUsageMapping) {
			if(memoryUsageMapping.containsKey(sessionId)) {
				memoryUsageMapping.remove(sessionId);	
			}
		}
		long freeMemory = instance.totalMemory() - overheadEstimate - memoryFromMapping();
		boolean sendUpdates = true;
		while(heapUsageOverEstimate > freeMemory) {
			logger.info("Free memory - " + freeMemory + " < " + heapUsageOverEstimate);

			// need to lock memory mapping so size is locked 
			synchronized(memoryUsageMapping) {
				if(System.currentTimeMillis() - t1 > CancellableFileUploader.MAX_UPLOAD_SECONDS*1000) {
					throw new FileUploadException("File upload timed out while waiting for resources.");
				} else if(memoryUsageMapping.size() == 0 
						|| (memoryUsageMapping.size() == 1 && memoryUsageMapping.containsKey(sessionId))) {
					throw new FileUploadException("There is not enough space in the JVM for this file.");
				}
				if(this.memoryUsageMapping.size() <= 1) {
					overheadEstimate = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				}
			}
			try {
				float num = sessionDataMapping.get(sessionId).getLastUpdateNumerator();
				ProgressBar queuedProgressBarUpdate = new ProgressBar((int)num, "Computing resources are limited. "
						+ String.valueOf(memoryUsageMapping.size()-1) + " other users also awaiting processing.");
				if(sendUpdates) {
					try {
						WebSocketServer.getInstance().send(queuedProgressBarUpdate, 
								sessionSocketBidirectionalMapping.get(sessionId));
					} catch (Exception e) {
						logger.debug("Web socket communications failed.  Attempting to continue analysis.", e);
						sendUpdates = false;
					}
				}
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new FileUploadException("Unexpected threading interruption.", e);
			}

			freeMemory = instance.totalMemory() - overheadEstimate - memoryFromMapping();
		}
		memoryUsageMapping.put(sessionId, heapUsageOverEstimate);
	}

	private synchronized long memoryFromMapping() {
		long sum = 0;
		for(String key : memoryUsageMapping.keySet()) {
			sum += memoryUsageMapping.get(key);
		}
		return sum;
	}


	private void smoothUpdate(ProgressBarManager progressUpdater, String sessionId) throws Exception {
		ProgressBar progressBar = progressUpdater.asBean();
		float previousUpdate = sessionDataMapping.get(sessionId).getLastUpdateNumerator();
		float updateDif = progressUpdater.getNumerator() - previousUpdate;
		// need to lock the progress bar so the websocket doesnt get closed while these updates are happening
		synchronized(progressBar) {
			if(updateDif >= noticeableProgressJump && performFakeUpdates) {
				int fakeUpdates = (int)(updateDif / minFakeUpdates);
				fakeUpdates = (fakeUpdates < minFakeUpdates) ? minFakeUpdates : fakeUpdates;
				final float fakeUpdateProgressInterval = updateDif/fakeUpdates;
				boolean interrupted = false;
				for (int i = 0; i < fakeUpdates  && !interrupted; i++) {
					float numerator = previousUpdate + (fakeUpdateProgressInterval * i);
					String description = progressUpdater.getStateByNumerator(numerator).getDescription();
					ProgressBar ithUpdateProgressBar = new ProgressBar((int)numerator, description);
					WebSocketServer.getInstance().send(ithUpdateProgressBar, sessionSocketBidirectionalMapping.get(sessionId));
					try {
						Thread.sleep(fakeUpdateDelay);
					} catch (InterruptedException e) {
						logger.error("Smooth updater interrupted.  Sending raw update");
						interrupted = true;
					}
				}
			}
		}
	}

	public long getUpdateFrequencyMillis() {
		return updateFrequencyMillis;
	}

	public void setUpdateFrequencyMillis(long updateFrequencyMillis) {
		this.updateFrequencyMillis = updateFrequencyMillis;
	}

	public long getFakeUpdateDelay() {
		return fakeUpdateDelay;
	}

	public void setFakeUpdateDelay(long fakeUpdateDelay) {
		this.fakeUpdateDelay = fakeUpdateDelay;
	}

	public int getNoticeableProgressJump() {
		return noticeableProgressJump;
	}

	public void setNoticeableProgressJump(int noticeableProgressJump) {
		this.noticeableProgressJump = noticeableProgressJump;
	}

	public int getMinFakeUpdates() {
		return minFakeUpdates;
	}

	public void setMinFakeUpdates(int minFakeUpdates) {
		this.minFakeUpdates = minFakeUpdates;
	}

	public boolean isPerformFakeUpdates() {
		return performFakeUpdates;
	}

	public void setPerformFakeUpdates(boolean performFakeUpdates) {
		this.performFakeUpdates = performFakeUpdates;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

}
