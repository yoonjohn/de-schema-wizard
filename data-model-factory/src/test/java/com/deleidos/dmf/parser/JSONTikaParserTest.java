package com.deleidos.dmf.parser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.framework.AbstractAnalyticsParser;
import com.deleidos.dp.profiler.api.ProfilerRecord;

public class JSONTikaParserTest extends ParserTest {
	
	@Before
	@Override
	public void setup() throws Exception {
		JSONTikaParser jsonParser = new JSONTikaParser();
		setParser(jsonParser);
	}

}
