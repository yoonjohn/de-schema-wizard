/*
 * Copyright 2011 Junar SpA.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.deleidos.dmf.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.ContentHandler;

import com.deleidos.dmf.exception.AnalyticsParsingRuntimeException;
import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.framework.AbstractAnalyticsParser;
import com.deleidos.dmf.framework.TikaProfilerParameters;
import com.deleidos.dmf.splitter.Splitter;
import com.deleidos.dmf.splitter.TextlineWithQuotesSplitter;
import com.deleidos.dp.enums.GroupingBehavior;
import com.deleidos.dp.profiler.DefaultProfilerRecord;
import com.deleidos.dp.profiler.api.ProfilerRecord;

import au.com.bytecode.opencsv.CSVReader;

/**
 * A CSVParser for the analytics framework.  Auto-detects the separator (either ',' or ';' or '\t') and parses 
 * each record based on that separator.
 * based on class by Marco Salgado A. < marco.salgado[at]junar.com >
 * Editted by @author Greg Lee
 */
public class CSVTikaParser extends AbstractAnalyticsParser {
	private Logger logger = Logger.getLogger(CSVTikaParser.class);
	private char separator;
	private String[] headerFields = null;
	private Splitter splitter;

	private static final Set<MediaType> SUPPORTED_TYPES = 
			Collections.singleton(MediaType.text("csv"));

	public static final MediaType CSV_MIME_TYPE = MediaType.text("csv");

	public Set<MediaType> getSupportedTypes(ParseContext context) {
		return SUPPORTED_TYPES;
	}

	public CSVTikaParser() {
		splitter = new TextlineWithQuotesSplitter();
	}

	@Override
	public void preParse(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		splitter.setInputStream(inputStream);
		try {
			separator = detectSeparators(inputStream);
			CSVReader lReader = new CSVReader(new InputStreamReader(new ByteArrayInputStream(splitter.split().getBytes())), separator);
			headerFields = lReader.readNext();
		} catch (IOException e) {
			logger.error("Header fields failed to be loaded due to IOException.");
			logger.error(e);
		}
	}

	@Override
	public ProfilerRecord getNextProfilerRecord(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		try {
			String nextSplit = splitter.split();
			if(nextSplit == null) {
				return null;
			}
			context.setCharsRead(context.getCharsRead()+nextSplit.length());
			CSVReader lReader = new CSVReader(new InputStreamReader(new ByteArrayInputStream(nextSplit.getBytes())), separator);
			String[] splits = lReader.readNext();
			EmptyStringsAreNullsProfilerRecords record = loadFieldsWithHeaderTemplate(headerFields, splits);
			return record;
		} catch (IOException e) {
			throw new AnalyticsTikaProfilingException(e);
		}
	}

	private class Separators {
		private char fSeparatorChar;
		private int  fFieldCount;

		public Separators(char pSeparator) {
			fSeparatorChar = pSeparator;
		}
		public void setSeparator(char pSeparator) {
			fSeparatorChar = pSeparator;
		}

		public void setCount(int pCount) {
			fFieldCount = pCount;
		}
		public char getSeparator() {
			return fSeparatorChar;
		}
		public int getCount() {
			return fFieldCount;
		}        
	}

	private char detectSeparators(InputStream inputStream) throws IOException {
		int lMaxValue = 0;
		char lCharMax = ','; 
		inputStream.mark(2048);

		ArrayList<Separators> lSeparators = new ArrayList<Separators>();        
		lSeparators.add(new Separators(','));
		lSeparators.add(new Separators(';'));
		lSeparators.add(new Separators('\t'));

		Iterator<Separators> lIterator = lSeparators.iterator();
		while (lIterator.hasNext()) {
			Separators lSeparator = lIterator.next();
			CSVReader lReader = new CSVReader(new InputStreamReader(inputStream), lSeparator.getSeparator());
			String[] lLine;
			lLine = lReader.readNext();
			lSeparator.setCount(lLine.length);

			if (lSeparator.getCount() > lMaxValue) {
				lMaxValue = lSeparator.getCount();
				lCharMax = lSeparator.getSeparator();
			}
			inputStream.reset();
		}

		return lCharMax;
	}

	private EmptyStringsAreNullsProfilerRecords loadFieldsWithHeaderTemplate(String[] headerFields, String[] pRow) {
		EmptyStringsAreNullsProfilerRecords profilerRecord = new EmptyStringsAreNullsProfilerRecords();
		if(headerFields.length != pRow.length) {
			logger.error("Invalid CSV format detected.");
			throw new AnalyticsParsingRuntimeException("Invalid CSV format detected.", this);
		}
		for(int i = 0; i < headerFields.length; i++) {
			profilerRecord.put(headerFields[i].trim(), pRow[i]);
		}
		return profilerRecord;
	}
	
	private static class EmptyStringsAreNullsProfilerRecords extends DefaultProfilerRecord {
		private static final long serialVersionUID = -2244595877110727189L;

		@Override
		public Map<String, List<Object>> normalizeRecord(GroupingBehavior groupingBehavior) {
			Map<String, List<Object>> recordMap = super.normalizeRecord(groupingBehavior);
			// loop through everything and replace emptry strings with null
			recordMap.replaceAll((k,v) -> {
				v.replaceAll(x-> "".equals(x) ? null : x);
				return v;
			});
			return recordMap;
		}
	}
}