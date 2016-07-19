package com.deleidos.dp.reversegeocoding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;

public class MockReverseGeocoder implements ReverseGeocoder.ReverseGeocodingWorker {
	private static final Logger logger = Logger.getLogger(MockReverseGeocoder.class);
	private List<String> testIsoCodes;
	private List<Integer> testIsoCodeCounts;
	private int counter;
	public static int SLEEP_MILLIS = 0;

	public MockReverseGeocoder() {
		testIsoCodes = new ArrayList<String>();
		testIsoCodeCounts = new ArrayList<Integer>();
		testIsoCodes.addAll(Arrays.asList("United States", "Russia", "China"));
		testIsoCodeCounts.addAll(Arrays.asList(0,0,0));
		counter = 0;
	}

	@Override
	public List<String> getCountryCodesFromCoordinateList(List<Double[]> coordinates) throws DataAccessException {
		synchronized(InterpretationEngineFacade.getInstance()) {
			List<String> countryCodes = new ArrayList<String>();
			for(Double[] coordinate : coordinates) {
				try {
					Thread.sleep(SLEEP_MILLIS);
				} catch (InterruptedException e) {
					logger.error(e);
				}
				String code = testIsoCodes.get(counter);
				testIsoCodeCounts.set(counter, testIsoCodeCounts.get(counter)+1);
				counter = ++counter%3;
				countryCodes.add(code);
			}
			return countryCodes;
		}
	}


	public List<String> getTestIsoCodes() {
		return testIsoCodes;
	}

	public void setTestIsoCodes(List<String> testIsoCodes) {
		this.testIsoCodes = testIsoCodes;
	}

	public List<Integer> getTestIsoCodeCounts() {
		return testIsoCodeCounts;
	}

	public void setTestIsoCodeCounts(List<Integer> testIsoCodeCounts) {
		this.testIsoCodeCounts = testIsoCodeCounts;
	}

}
