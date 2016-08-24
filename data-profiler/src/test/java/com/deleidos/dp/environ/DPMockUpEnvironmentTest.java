package com.deleidos.dp.environ;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;

import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.h2.H2DataAccessObject;
import com.deleidos.dp.interpretation.IEConfig;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.deleidos.hd.h2.H2TestDatabase;

public abstract class DPMockUpEnvironmentTest {
	private static final Logger logger = Logger.getLogger(DPMockUpEnvironmentTest.class);
	private static boolean running = false;

	@BeforeClass
	public static void setupUnitTestingEnvironment() throws DataAccessException, ClassNotFoundException, SQLException, InterruptedException {
		if(!running) {
			InterpretationEngineFacade.setInstance(IEConfig.BUILTIN_CONFIG);
			logger.info("Setting up built-in Interpretation Engine.");
			H2TestDatabase h2Test = new H2TestDatabase();
			h2Test.startTestServer(h2Test.getConfig());
			H2DataAccessObject.setInstance(h2Test);
			running = true;
		}
	}
}