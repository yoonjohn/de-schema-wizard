package com.deleidos.dp.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.Domain;
import com.deleidos.dp.beans.DomainMetaData;

/**
 * This class is here to store any data regarding domains.  Currently unused - 6/17/2016
 * @author leegc
 *
 */
public class H2DomainDataAccessObject {
	private Logger logger = Logger.getLogger(H2DomainDataAccessObject.class);
	private H2DataAccessObject h2;
	private Connection dbConnection;
	
	private static final String ADD_DOMAIN = "INSERT INTO domain VALUES (NULL, ?, ?)";
	private final String ADD_GUID = "INSERT INTO guid_list VALUES (?)";

	public H2DomainDataAccessObject(H2DataAccessObject h2) {
		this.h2 = h2;
		dbConnection = h2.getDBConnection();
	}
	
	public int addDomain(DomainMetaData domain) throws SQLException {
		dbConnection.setAutoCommit(false);
		try {
			PreparedStatement ppst = dbConnection.prepareStatement(ADD_DOMAIN);
			ppst.setString(1, domain.getdName());
			ppst.setString(2, domain.getdId());
			ppst.execute();
			int gen = h2.getGeneratedKey(ppst);
			
			PreparedStatement ppst2 = dbConnection.prepareStatement(ADD_GUID);
			ppst2.setString(1, domain.getdId());
			ppst2.execute();
			dbConnection.commit();
			return gen;
		} catch(SQLException e) {
			dbConnection.rollback();
			return -1;	
		} finally {
			dbConnection.setAutoCommit(true);
		}
	}
}
