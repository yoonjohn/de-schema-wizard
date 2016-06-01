package com.deleidos.dmf.web;

import com.deleidos.analytics.websocket.api.BaseWebSocketMessage;
import com.fasterxml.jackson.databind.JsonNode;

public class SchemaWizardLongRunningTaskFacade extends BaseWebSocketMessage {
	
	public SchemaWizardLongRunningTaskFacade(JsonNode jsonNode) {
		// get necessary fields from object sent by front end
	}

	@Override
	public void processMessage() throws Exception {
		// this method will automatically be multithreaded
		// create analyzer parameters and start it up here
	}

}
