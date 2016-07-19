package com.deleidos.dmf.parser;

import org.junit.Before;

public class XMLTikaParserTest extends ParserTest {

	@Before
	@Override
	public void setup() throws Exception {
		setParser(new XMLTikaParser());
	}
	
}
