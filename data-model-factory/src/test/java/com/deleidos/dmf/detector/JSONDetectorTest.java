package com.deleidos.dmf.detector;

import java.io.FileNotFoundException;
import org.junit.Before;
import org.junit.Test;

public class JSONDetectorTest extends DetectorTest {
	JSONDetector jsonDetector;

	@Override
	@Before
	public void setup() {
		jsonDetector = new JSONDetector();
		setDetector(jsonDetector);
	}
}
