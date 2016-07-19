package com.deleidos.dmf.detector;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

public class CSVDetectorTest extends DetectorTest {
	private Logger logger = Logger.getLogger(CSVDetectorTest.class);
	private CSVDetector csvDetector;
	ArrayList<String> valid;
	ArrayList<String> invalid;

	@Override
	@Before
	public void setup() {
		csvDetector = new CSVDetector();
		setDetector(csvDetector);
		String simple = "aaa,bbb,ccc";
		String quoted = "\"aaa\",\"bbb\",\"ccc\"\nxxx,yyy,zzz";
		String escapedLineFeed = "\"aaa\",\"b\nbb\",\"ccc\"\nzzz,yyy,zzz";
		String doubleQuoted = "\"aaa\",\"b\"\"bb\",\"ccc\"";
		String other = "A,B,C" + "\n" + "1,2,3" + "\n" + "4,\"5,3\",6";

		valid = new ArrayList<String>();
		valid.add(simple);
		valid.add(quoted);
		valid.add(escapedLineFeed);
		valid.add(doubleQuoted);
		valid.add(other);

		String trailingComma = "aaa,bbb,ccc,";
		String noLeadingQuotation = "aaa\",\"bbb\",\"ccc\"\nxxx,yyy,zzz";
		String improperlyEscapedLineFeed = "\"aaa\",b\nbb,\"ccc\"\nzzz,yyy,zzz";
		String simpleSentence = "I am not proper CSV.";

		invalid = new ArrayList<String>();
		invalid.add(trailingComma);
		invalid.add(noLeadingQuotation);
		invalid.add(improperlyEscapedLineFeed);
		invalid.add(simpleSentence);
	}

	@Test
	public void invalidCSVStringsTest() throws Exception {
		for(String f: invalid) {
			InputStream is = null;

			is = new ByteArrayInputStream(f.getBytes());
			Object type = csvDetector.detect(is, null);
			is.close();
			logger.info(f + ": " + type + " - should be \"null\"");
			assertTrue(type == null);

		}
	}


}
