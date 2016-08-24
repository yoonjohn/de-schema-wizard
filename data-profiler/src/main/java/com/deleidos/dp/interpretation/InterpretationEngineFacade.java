package com.deleidos.dp.interpretation;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.interpretation.builtin.BuiltinInterpretationEngine;
import com.deleidos.dp.profiler.api.ProfilingProgressUpdateHandler;

public class InterpretationEngineFacade {
	private static final String IE_ADDR_KEY = "SW_IE_PORT";
	private static final String DEFAULT_IE_ADDR = "http://localhost:5000";
	private static final Logger logger = Logger.getLogger(InterpretationEngineFacade.class);
	private static InterpretationEngine interpretationEngine = null;

	public static InterpretationEngine setInstance(IEConfig config) throws DataAccessException {
		if(config.useBuiltin()) {
			interpretationEngine = new BuiltinInterpretationEngine(config.isFakeGeocode());
			logger.info("Using built-in interpretation engine.");
		} else {
			interpretationEngine = new HttpInterpretationEngine(config);
		}
		return interpretationEngine;
	}

	public static InterpretationEngine getInstance() throws DataAccessException {
		if(interpretationEngine == null) {
			try {
				IEConfig config = new IEConfig().load();
				if(config.useBuiltin()) {
					interpretationEngine = new BuiltinInterpretationEngine(false);
				} else {
					interpretationEngine = new HttpInterpretationEngine(config);
				}
			} catch(IOException e) {
				logger.error(e);
				logger.error("Configuration files not found.");
				interpretationEngine = new BuiltinInterpretationEngine(false);
			}
		}
		return interpretationEngine;
	}

	public static void interpretInline(DataSample dataSample, String domainName, ProfilingProgressUpdateHandler progressUpdater) throws DataAccessException {
		Map<String, Profile> profile = dataSample.getDsProfile();
		InterpretationEngine interpretationEngine = InterpretationEngineFacade.getInstance();
		double timeoutMultiplier = 
				(interpretationEngine instanceof HttpInterpretationEngine) ?
				((HttpInterpretationEngine)interpretationEngine).getConfig().getMultiplier() : 1;
		// depends on number of interpretations -- needs to be dynamically calculated
		// need to do some benchmarking to see how size of domain affects estimate
		int timeEstimate = profile.size() * 50;
		int timeoutOverEstimate = (int)(timeEstimate * timeoutMultiplier);
		profile.putAll(interpretationEngine.interpret(domainName, profile, timeoutOverEstimate));
	}
	
	public static Optional<HttpInterpretationEngine> getHttpInterpretationEngine() {
		if(interpretationEngine instanceof HttpInterpretationEngine) {
			return Optional.of((HttpInterpretationEngine)interpretationEngine);
		}
		return Optional.empty();
	}
}
