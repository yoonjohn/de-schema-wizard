package com.deleidos.hd.scheduler;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.apache.log4j.Logger;

/**
 * This class creates a Scheduler based on the built-in Java
 * ScheduledExecutorService which checks for stale data in the H2 database and
 * removes it.
 * 
 * @author yoonj1
 *
 */
public class DeletionScheduler {
	// Final Variables
	private final Logger logger = Logger.getLogger(DeletionScheduler.class);
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private final int INITIAL_DELAY = 60;
	private final int DELAY = 60;
	
	// Variables
	private Connection dbConnection;

	// Queries
	private final String QUERY_STALE_GUIDS = "SELECT guid FROM deletion_queue" + " WHERE (in_progress = false)"
			+ " OR (last_update < DATEADD('DAY', -1, CURRENT_DATE));";
	private final String QUERY_SCHEMA_BY_GUID = "SELECT * FROM schema_model" + " WHERE schema_model.s_guid = ?;";
	private final String QUERY_SAMPLE_BY_GUID = "SELECT * FROM data_sample" + " WHERE data_sample.ds_guid = ?;";
	private final String DELETE_SCHEMA_BY_GUID = "DELETE FROM schema_model WHERE s_guid = ?";
	private final String DELETE_SAMPLE_BY_GUID = "DELETE FROM data_sample WHERE ds_guid = ?";

	public DeletionScheduler(Connection dbConnection) {
		this.dbConnection = dbConnection;
	}
	
	/**
	 * Begins the schedule of clearing the stale data out of the H2 Database.
	 */
	public void clearStaleData() {
		final Runnable h2Cleaner = new Runnable() {
			public void run() {
				runDeletionScript();
			}
		};

		// The schedule of the job - this is used.
		@SuppressWarnings("unused")
		final ScheduledFuture<?> cleaningSchedule = scheduler.scheduleAtFixedRate(h2Cleaner, INITIAL_DELAY, DELAY,
				MINUTES);
	}

	// Private methods
	/**
	 * Does the job of gathering stale GUIDs (GUIDs that are not in progress OR
	 * older than 24 hours), distinguishing it between a Schema and a Data
	 * Sample, and deleting the resulting variable.
	 */
	private void runDeletionScript() {
		ArrayList<String> staleGuids = getStaleGuids();

		try {
			for (String guid : staleGuids) {
				if (isSchema(guid)) {
					deleteSchema(guid);
				} else if (isSample(guid)) {
					deleteSample(guid);
				}
			}
		} catch (SQLException e) {
			logger.error("Error executing Schema or Data Sample check.");
		}
	}

	/**
	 * Retrieves GUIDs that are not in progress or older than 24 hours.
	 * 
	 * @return An ArrayList<String> of stale GUIDs 
	 */
	private ArrayList<String> getStaleGuids() {
		ArrayList<String> staleGuids = new ArrayList<String>();

		try {
			PreparedStatement ppst = dbConnection.prepareStatement(QUERY_STALE_GUIDS);
			ResultSet rs = ppst.executeQuery();
			ppst.close();

			while (rs.next()) {
				staleGuids.add(rs.getString("guid"));
			}
		} catch (SQLException e) {
			logger.error("Error retrieving stale guids.");
			e.printStackTrace();
		}

		return staleGuids;
	}

	/**
	 * 
	 * @param guid
	 *            The GUID to be checked.
	 * @return true if the GUID is a Schema; false if it is not.
	 * @throws SQLException
	 */
	private boolean isSchema(String guid) throws SQLException {
		PreparedStatement ppst = dbConnection.prepareStatement(QUERY_SCHEMA_BY_GUID);
		ppst.setString(1, guid);
		ResultSet rs = ppst.executeQuery();
		ppst.close();

		if (rs.next()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 
	 * @param guid
	 *            The GUID to be checked.
	 * @return true if the GUID is a Data Sample; false if it is not.
	 * @throws SQLException
	 */
	private boolean isSample(String guid) throws SQLException {
		PreparedStatement ppst = dbConnection.prepareStatement(QUERY_SAMPLE_BY_GUID);
		ppst.setString(1, guid);
		ResultSet rs = ppst.executeQuery();
		ppst.close();

		if (rs.next()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 
	 * @param guid
	 *            The GUID to delete.
	 * @throws SQLException
	 */
	private void deleteSchema(String guid) throws SQLException {
		PreparedStatement ppst = dbConnection.prepareStatement(DELETE_SCHEMA_BY_GUID);
		ppst.setString(1, guid);
		ppst.execute();
		ppst.close();
	}

	/**
	 * 
	 * @param guid
	 *            The GUID to delete.
	 * @throws SQLException
	 */
	private void deleteSample(String guid) throws SQLException {
		PreparedStatement ppst = dbConnection.prepareStatement(DELETE_SAMPLE_BY_GUID);
		ppst.setString(1, guid);
		ppst.execute();
		ppst.close();
	}
}
