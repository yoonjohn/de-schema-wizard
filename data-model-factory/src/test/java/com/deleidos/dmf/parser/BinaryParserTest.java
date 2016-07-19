package com.deleidos.dmf.parser;

import org.junit.Before;

public class BinaryParserTest extends ParserTest {

	@Before
	@Override
	public void setup() throws Exception {
		BinaryParser binaryParser = new BinaryParser();
		binaryParser.setMediaType("image/jpeg");
		binaryParser.setName("test-binary");
		this.setParser(binaryParser);
		this.setDebugOutput(true);
	}
	
	

}
