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
	private static final String PROPERTIES_IE_TIMEOUT = "ie.timeout";
	private static final String ENV_IE_TIMEOUT = "IE_TIMEOUT";
	private static final String PROPERTIES_IE_TIMEOUT_MULTIPLIER = "ie.timeout.multiplier";
	private static final String ENV_IE_TIMEOUT_MULTIPLIER = "IE_TIMEOUT_MULTIPLIER";
	private int connectionTimeout = 15000;
	private int readTimeout = 15000;
	private double multiplier = 5;
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
		connectionTimeout = Integer.valueOf(properties.getProperty(PROPERTIES_IE_TIMEOUT));
		readTimeout = Integer.valueOf(properties.getProperty(PROPERTIES_IE_TIMEOUT));
		multiplier = Double.valueOf(properties.getProperty(PROPERTIES_IE_TIMEOUT_MULTIPLIER));
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
		try {
			setConnectionTimeout(System.getenv(ENV_IE_TIMEOUT) != null 
					? Integer.valueOf(System.getenv(ENV_IE_TIMEOUT)) : getConnectionTimeout());
			setReadTimeout(getConnectionTimeout());
		} catch (NumberFormatException e) {
			logger.error("Invalid timeout environment variable.", e);
		}
		try {
			setMultiplier(System.getenv(ENV_IE_TIMEOUT_MULTIPLIER) != null
					? Double.valueOf(System.getenv(ENV_IE_TIMEOUT_MULTIPLIER)) : getMultiplier());
		} catch (NumberFormatException e) {
			logger.error("Invalid timeout multiplier variable.", e);
		}

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

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	public double getMultiplier() {
		return multiplier;
	}

	public void setMultiplier(double multiplier) {
		this.multiplier = multiplier;
	}

	public static IEConfig BUILTIN_CONFIG = new IEConfig() {
		{
			setUrl(null);
			setFakeGeocode(true);
		}
	};
}
