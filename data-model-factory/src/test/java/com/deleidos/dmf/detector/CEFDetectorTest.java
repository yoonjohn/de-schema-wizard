package com.deleidos.dmf.detector;

import org.junit.Before;

public class CEFDetectorTest extends DetectorTest {
	private CEFDetector cefDetector;
	
	@Override
	@Before
	public void setup() {
		cefDetector =  new CEFDetector();
		setDetector(cefDetector);
	}
}
