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
	
	public String bytesToString(byte[] bytes) {
		String byteString = "";
		for(int i = 0; i < bytes.length; i++) {
			byteString += Byte.toUnsignedInt(bytes[i]) + " ";
		}
		return byteString;
	}
	
	public String integerIP4ToString(int ip) {
		final int THIRD_OCTET_MAX = 16777216;
		final int SECOND_OCTET_MAX = 65536;
		final int FIRST_OCTET_MAX = 256;
		int first_octet = 0;
		int second_octet = 0;
		int third_octet = 0;
		int fourth_octet = 0;
		
		fourth_octet = ip / THIRD_OCTET_MAX;
		ip = ip % (fourth_octet*THIRD_OCTET_MAX);
		third_octet = ip / SECOND_OCTET_MAX;
		ip = ip % (third_octet*SECOND_OCTET_MAX);
		second_octet = ip / FIRST_OCTET_MAX;
		ip = ip % (second_octet*FIRST_OCTET_MAX);
		first_octet = ip;
		String value = String.valueOf(fourth_octet) + "." + String.valueOf(third_octet) + "." + String.valueOf(second_octet) + "." + String.valueOf(first_octet);
		return value;
	}

	@Before
	@Override
	public void setup() throws Exception {
		//JRegistry.addBindings(new Wireless80211Header());
		//setParser(new PCAPTikaParser());
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
