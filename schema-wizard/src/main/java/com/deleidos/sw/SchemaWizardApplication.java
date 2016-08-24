package com.deleidos.sw;

import org.glassfish.jersey.server.ResourceConfig;

import com.deleidos.analytics.websocket.WebSocketServlet;
import com.deleidos.dmf.accessor.ServiceLayerAccessor;
import com.deleidos.dmf.analyzer.TikaAnalyzer;

/**
 * Implementation of Glassfish's default web application class.  Instantiates SchemaWizardController and WebSocketServlet.
 * @author leegc
 * @see <a href="https://jersey.java.net/documentation/latest/deployment.html">https://jersey.java.net/documentation/latest/deployment.html</a>
 */
public class SchemaWizardApplication extends ResourceConfig {

	public SchemaWizardApplication() {
		register(new SchemaWizardController(new TikaAnalyzer(), new ServiceLayerAccessor()));
		register(new WebSocketServlet());
	}
}
