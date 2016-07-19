package com.deleidos.dmf.parser;

import org.junit.Before;

public class CSVTikaParserTest extends ParserTest {
	
	@Before
	@Override
	public void setup() throws Exception {
		CSVTikaParser csv = new CSVTikaParser();
		setParser(csv);
	}
}
