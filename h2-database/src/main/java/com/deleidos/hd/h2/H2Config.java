package com.deleidos.hd.h2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

public class H2Config {
	public static String CONFIG_RESOURCE_NAME = "/build.properties";
	public static final String SW_CONFIG_ENV_VAR = "SW_CONFIG_PROPERTIES";
	private static final Logger logger = Logger.getLogger(H2Config.class);
	private static final String PROPERTIES_H2_DRIVER = "h2.driver";
	private static final String PROPERTIES_H2_HOST = "h2.host";
	private static final String PROPERTIES_H2_DIR = "h2.dir";
	private static final String PROPERTIES_H2_NAME = "h2.name";
	private static final String PROPERTIES_H2_USER = "h2.user";
	private static final String PROPERTIES_H2_PORTNUM = "h2.portnum";
	private static final String PROPERTIES_H2_PASSWORD = "h2.passwd";
	private static final String ENV_H2_DRIVER = "H2_DB_DRIVER";
	private static final String ENV_H2_HOST = "H2_DB_HOST";
	private static final String ENV_H2_DIR = "H2_DB_DIR";
	private static final String ENV_H2_NAME = "H2_DB_NAME";
	private static final String ENV_H2_USER = "H2_DB_USER";
	private static final String ENV_H2_PORTNUM = "H2_DB_PORTNUM";
	private static final String ENV_H2_PASSWORD = "H2_DB_PASSWD";
	private static final String ENV_H2_PORT = "H2_DB_PORT";
	private String driver;
	private String host;
	private String dir;
	private String name;
	private String tcpConnectionString;
	private String port;
	private String user;
	private String passwd;
	private String filePath = null;
	
	public H2Config() {
		File file = new File("~" + File.separator + CONFIG_RESOURCE_NAME);
		if(file.exists()) {
			filePath = file.getAbsolutePath();
		} else {
			filePath = (System.getenv(SW_CONFIG_ENV_VAR) != null) ? System.getenv(SW_CONFIG_ENV_VAR) : null;
		}
	}
	
	public H2Config load() throws IOException {
		Properties properties = new Properties();
		if(filePath == null) {
			properties.load(getClass().getResourceAsStream(CONFIG_RESOURCE_NAME));
			logger.info("Using default resource configuration.");
		} else {
			logger.info("Grabbing H2 config from " + this.filePath + ".");
			FileInputStream fis = new FileInputStream(this.filePath);
			properties.load(fis);
			fis.close();
		}
		driver = properties.getProperty(PROPERTIES_H2_DRIVER);
		host = properties.getProperty(PROPERTIES_H2_HOST);
		dir = properties.getProperty(PROPERTIES_H2_DIR);
		name = properties.getProperty(PROPERTIES_H2_NAME);
		user = properties.getProperty(PROPERTIES_H2_USER);
		port = properties.getProperty(PROPERTIES_H2_PORTNUM);
		passwd = properties.getProperty(PROPERTIES_H2_PASSWORD);
		loadFromEnv();
		tcpConnectionString = "tcp://" + host + ":" + port + "/";
		printConfiguration();
		return this;
	}
	
	/**
	 * The environment overloads any configuration file settings.
	 * @return
	 */
	private void loadFromEnv() {
		setDriver(System.getenv(ENV_H2_DRIVER) != null ? System.getenv(ENV_H2_DRIVER) : getDriver());
		setDir(System.getenv(ENV_H2_DIR) != null ? System.getenv(ENV_H2_DIR) : getDir());
		setName(System.getenv(ENV_H2_NAME) != null ? System.getenv(ENV_H2_NAME) : getName());
		setHost(System.getenv(ENV_H2_HOST) != null ? System.getenv(ENV_H2_HOST) : getHost());
		setPasswd(System.getenv(ENV_H2_PASSWORD) != null ? System.getenv(ENV_H2_PASSWORD) : getPasswd());
		setPortNum(System.getenv(ENV_H2_PORTNUM) != null ? System.getenv(ENV_H2_PORTNUM) : getPortNum());
		setUser(System.getenv(ENV_H2_USER) != null ? System.getenv(ENV_H2_USER) : getUser());
		if(System.getenv(ENV_H2_PORT) != null) {
			String tcpPort = System.getenv(ENV_H2_PORT);
			tcpPort = tcpPort.substring(6, tcpPort.length());
			String[] splits = tcpPort.split(":");
			setHost(splits[0]);
			setPortNum(splits[1]);
		}
	}
	
	private void printConfiguration() {
		logger.info("H2Configuration: "
				+ "\n\tdriver = " + driver
				+ "\n\thost = " + host
				+ "\n\tdir = " + dir
				+ "\n\tname = " + name
				+ "\n\tuser = " + user
				+ "\n\ttcpConnectionString = " + tcpConnectionString);
	}
	
	public String getConnectionString() {
		return "jdbc:h2:" + getTcpConnectionString() + getDir() + "/" + getName();
	}

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTcpConnectionString() {
		return tcpConnectionString;
	}

	public void setTcpConnectionString(String tcpConnectionString) {
		this.tcpConnectionString = tcpConnectionString;
	}

	public String getPortNum() {
		return port;
	}

	public void setPortNum(String port) {
		this.port = port;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPasswd() {
		return passwd;
	}

	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}
	
	/**
	 * Unit testing configuration.  Runs in the same JVM as the test.
	 */
	public static final H2Config TEST_CONFIG = new H2Config() {
		{
			setDir("~/test-files/");
			setHost("localhost");
			setName("data");
			setPasswd("");
			setPortNum("9124");
			setTcpConnectionString("tcp://localhost:9124/");
			setUser("sa");
			setDriver(H2Database.DB_DRIVER);
		}
	};
	
	
}
