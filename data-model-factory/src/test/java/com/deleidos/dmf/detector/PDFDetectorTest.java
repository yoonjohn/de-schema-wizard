package com.deleidos.dmf.detector;

import java.util.Collections;

import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;
import org.junit.Before;

import com.deleidos.dmf.framework.AnalyticsDetectorWrapper;

public class PDFDetectorTest extends DetectorTest {

	@Before
	@Override
	public void setup() {
		setDetector(new AnalyticsDetectorWrapper(MimeTypes.getDefaultMimeTypes(), Collections.singleton(MediaType.application("pdf"))));
	}
	
}
