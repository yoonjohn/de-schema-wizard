package com.deleidos.hd.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import org.apache.log4j.Logger;

public class H2Queries {
	private static final Logger logger = Logger.getLogger(H2Queries.class);
	private static final String ADD_DATA_SAMPLE = "INSERT INTO data_sample VALUES (NULL, ?, ?, ?, ?, ? ,? ,?, ?, ?, ?);";
	private static final String ADD_GUID = "INSERT INTO guid_list VALUES (?)";
	
	public static void addPlaceholderSample(Connection connection, String guid, String message) throws SQLException {
		logger.info("Adding failed analysis guid " + guid + " with message " + message + ".  This is not an error message.");
		addSample(connection, guid, guid, null, Timestamp.from(Instant.now()), message, null, null, null, 0, 0);
	}

	private static void addSample(Connection connection, String guid, String name, String version, Timestamp timestamp, String description,
			String fileName, String fileType, String extractedContentDir, int recordsParsedCount, int fileSize) throws SQLException {
		PreparedStatement ppst = connection.prepareStatement(ADD_DATA_SAMPLE);
		ppst.setString(1, guid);
		ppst.setString(2, name);
		ppst.setString(3, fileName);
		ppst.setString(4, fileType);
		ppst.setString(5, version);
		ppst.setTimestamp(6, timestamp);
		ppst.setString(7, description);
		ppst.setString(8, extractedContentDir);
		ppst.setInt(9, recordsParsedCount);
		ppst.setInt(10, fileSize);
		ppst.execute();

		PreparedStatement ppst2 = connection.prepareStatement(ADD_GUID);
		ppst2.setString(1, guid);
		ppst2.execute();
	}
}
