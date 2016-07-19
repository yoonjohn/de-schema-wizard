package com.deleidos.dmf.framework;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.deleidos.dmf.loader.ResourceLoader;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.h2.H2DataAccessObject;
import com.deleidos.dp.interpretation.IEConfig;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.deleidos.hd.h2.H2TestDatabase;

/**
 * An abstract class for tests that should run with an embedded H2 instance and Built-in Interpretation Engine.
 * Extend this class and the Interpretation Engine and H2DataAccessObject singletons will be initialized for you.
 * 
 * @author leegc
 *
 */
public abstract class DMFMockUpEnvironmentTest extends ResourceLoader {
	private static final Logger logger = Logger.getLogger(DMFMockUpEnvironmentTest.class);
	private static boolean running = false;

	@BeforeClass
	public static void setupUnitTestingEnvironment() throws DataAccessException, ClassNotFoundException, SQLException, InterruptedException {
		if(!running) {
			InterpretationEngineFacade.setInstance(IEConfig.BUILTIN_CONFIG);
			logger.info("Setting up built-in Interpretation Engine.");
			H2TestDatabase h2Test = new H2TestDatabase().startTestServer();
			h2Test.populateDatabase();
			H2DataAccessObject.setInstance(h2Test).initConnection();
			running = true;
		}
	}
}
