package com.deleidos.dp.interpretation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class IEConfig {
	private static final Logger logger = Logger.getLogger(IEConfig.class);
	public static String CONFIG_RESOURCE_NAME = "/build.properties";
	public static final String SW_CONFIG_PROPERTIES = "SW_CONFIG_PROPERTIES";
	public static final String BUILT_IN_OVERRIDE = "default";
	private static final String PROPERTIES_IE_URL = "ie.url";
	private static final String ENV_IE_PORT = "SW_IE_PORT";
	private String url;
	private boolean fakeGeocode = false;
	private String filePath = null;
	
	public static IEConfig dynamicConfig(String url) {
		IEConfig dynamic = new IEConfig();
		dynamic.setUrl(url);
		return dynamic;
	}
	
	public IEConfig() {
		File file = new File("~" + File.separator + CONFIG_RESOURCE_NAME);
		if(file.exists()) {
			filePath = file.getAbsolutePath();
		} else {
			filePath = (System.getenv(SW_CONFIG_PROPERTIES) != null) ? System.getenv(SW_CONFIG_PROPERTIES) : null;
		}
	}
	
	public IEConfig load() throws IOException {
		Properties properties = new Properties();
		if(filePath == null) {
			properties.load(getClass().getResourceAsStream(CONFIG_RESOURCE_NAME));
		} else {
			logger.info("Grabbing IE config from " + this.filePath + ".");
			FileInputStream fis = new FileInputStream(this.filePath);
			properties.load(fis);
			fis.close();
		}
		url = properties.getProperty(PROPERTIES_IE_URL);
		loadFromEnv();
		return this;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		if(url != null && url.contains("tcp")) {
			url = url.replaceFirst("tcp", "http");
		}
		this.url = url;
	}
	
	private void loadFromEnv() {
		setUrl(System.getenv(ENV_IE_PORT) != null ? System.getenv(ENV_IE_PORT) : getUrl());
	}
	
	public boolean useBuiltin() {
		return getUrl() == null || getUrl().equals(BUILT_IN_OVERRIDE);
	}
	
	public boolean isFakeGeocode() {
		return fakeGeocode;
	}

	public void setFakeGeocode(boolean fakeGeocode) {
		this.fakeGeocode = fakeGeocode;
	}

	public static IEConfig BUILTIN_CONFIG = new IEConfig() {
		{
			setUrl(null);
			setFakeGeocode(true);
		}
	};
}
