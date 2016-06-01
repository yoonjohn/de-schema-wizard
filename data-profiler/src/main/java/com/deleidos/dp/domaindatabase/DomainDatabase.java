package com.deleidos.dp.domaindatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.log4j.Logger;

public final class DomainDatabase {
	public static Logger logger = Logger.getLogger(DomainDatabase.class);
	private static final String DB_DRIVER = "com.mysql.jdbc.Driver";
	private static final BasicDataSource dataSource = new BasicDataSource();
	private static String DB_PORT;
	private static String DB_NAME;
	private static String DB_USER;
	private static String DB_PASSWORD;
	
	static {
		DB_PORT = (System.getenv("SW_DOMAIN_DB_PORT") != null) ? System.getenv("SW_DOMAIN_DB_PORT")
				: "tcp://localhost:3306";
		DB_NAME = (System.getenv("SW_DOMAIN_DB_NAME") != null) ? System.getenv("SW_DOMAIN_DB_NAME") : "mysql";
		DB_USER = (System.getenv("SW_DOMAIN_DB_USER") != null) ? System.getenv("SW_DOMAIN_DB_USER") : "root";
		DB_PASSWORD = (System.getenv("SW_DOMAIN_DB_PASSWORD") != null) ? System.getenv("SW_DOMAIN_DB_PASSWORD")
				: "secret";

		dataSource.setDriverClassName(DB_DRIVER);
		dataSource.setUrl("jdbc:mysql://" + DB_PORT + DB_NAME);
		dataSource.setUsername(DB_USER);
		dataSource.setPassword(DB_PASSWORD);
	}
	
	private DomainDatabase() {
		//
	}

	public static Connection getConnection() {
		Connection connection = null;

		try {
			connection = dataSource.getConnection();
		} catch (SQLException e) {
			logger.error("Unable to get connection.");
			e.printStackTrace();
		}

		return connection;
	}

	public static void test() {
		String createTable = "CREATE TABLE ? ( color varchar(255) );";
		try (Connection connection = DomainDatabase.getConnection();
				PreparedStatement statement = connection.prepareStatement(createTable);) {
			statement.setString(1, "cars");
		} catch (SQLException e) {
			logger.error("Failed to create table.");
			e.printStackTrace();
		}
	}
}
