package com.deleidos.dmf.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.deleidos.analytics.websocket.WebSocketServer;
import com.deleidos.analytics.websocket.api.WebSocketApiPlugin;
import com.deleidos.analytics.websocket.api.WebSocketEventListener;
import com.deleidos.analytics.websocket.api.WebSocketMessage;
import com.deleidos.analytics.websocket.api.WebSocketMessageFactory;
import com.deleidos.dmf.progressbar.ProgressBar;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class SchemaWizardWebSocketUtility implements WebSocketApiPlugin, WebSocketMessageFactory, WebSocketEventListener {
	private static SchemaWizardWebSocketUtility INSTANCE = null;
	private static final Logger logger = Logger.getLogger(SchemaWizardWebSocketUtility.class);
	private BiMap<String, String> sessionSocketBidirectionalMapping;
	private Map<String, SessionData> sessionIdToSessionDataMapping;
	//will have to synchronize these fields with multi-client

	protected SchemaWizardWebSocketUtility() {
		sessionSocketBidirectionalMapping = HashBiMap.create();
		sessionIdToSessionDataMapping = new HashMap<String, SessionData>();
	}

	public static SchemaWizardWebSocketUtility getInstance(SchemaWizardWebSocketUtility testUtility) {
		if(INSTANCE == null) {
			INSTANCE = testUtility;
			logger.info("Registering plugin with web socket server.");
			WebSocketServer.getInstance().registerPlugin(INSTANCE);
		}
		return INSTANCE;
	}

	public static SchemaWizardWebSocketUtility getInstance() {
		if(INSTANCE == null) {
			INSTANCE = new SchemaWizardWebSocketUtility();
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
	public void updateProgress(ProgressBar updateBean, String sessionId) {
		try {
			if(sessionId != null) {
				if(sessionSocketBidirectionalMapping.containsKey(sessionId)) {
					WebSocketServer.getInstance().send(updateBean, sessionSocketBidirectionalMapping.get(sessionId));
				}
			} 
		} catch (Exception e) {
			logger.error(e);
			logger.error("Progress update failed to send to session " + sessionId + ".");
		}
	}

	public void incrementProgressState(String sessionId) {

	}

	public void updateProgress(float currentUpdatePercentageCompleted, String sessionId) {

	}

	public void associateSessionAndSocket(String sessionId, String socketId) {
		logger.debug("Session "+sessionId+" associated with socket " +socketId + "." );
		sessionSocketBidirectionalMapping.put(sessionId, socketId);
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
					SchemaWizardWebSocketUtility.getInstance().associateSessionAndSocket(sessionIdString, webSocketId);
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

	@Override
	public void onWebSocketClose(String webSocketId) {
		String associatedSessionId = sessionSocketBidirectionalMapping.inverse().get(webSocketId);
		sessionSocketBidirectionalMapping.remove(associatedSessionId);
		logger.debug("Socket " + webSocketId + " with associated session " + associatedSessionId + " closed.");
	}

	@Override
	public void onWebSocketConnect(String webSocketId) {
		logger.debug("Socket " + webSocketId + " opened.  Awaiting session ID.");
	}

}
