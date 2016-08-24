package com.deleidos.dmf.parser;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.deleidos.dmf.analyzer.workflows.DefinedTestResource;

//Ignore this test class because it depends on a native library

public class JnetPcapParserTest extends ParserTest {
	private static final Logger logger = Logger.getLogger(JnetPcapParserTest.class);
	public boolean debug = false;
	Map<String, String> resources;

	@Before
	@Override
	public void setup() throws Exception {
		setParser(new JNetPcapTikaParser());
		setDebugOutput(false);
		for(DefinedTestResource dtr : super.streamSources) {
			if(dtr.getExpectedType().equals(JNetPcapTikaParser.CONTENT_TYPE.toString())) {
				String name = dtr.getFilePath().substring(dtr.getFilePath().lastIndexOf('/'));
				File tmp = File.createTempFile("pcap-test-file-"+name , String.valueOf(System.currentTimeMillis()));
				FileUtils.copyInputStreamToFile(dtr.getStream(), tmp);
				dtr.setFilePath(tmp.getAbsolutePath());
				dtr.setStream(new FileInputStream(tmp));
			}
		}
	}
}
