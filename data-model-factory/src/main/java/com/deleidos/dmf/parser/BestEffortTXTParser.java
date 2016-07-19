package com.deleidos.dmf.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Set;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.ContentHandler;

import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.framework.AbstractAnalyticsParser;
import com.deleidos.dmf.framework.TikaProfilerParameters;
import com.deleidos.dp.profiler.DefaultProfilerRecord;
import com.deleidos.dp.profiler.api.ProfilerRecord;

/**
 * Best effort last resort plain text parser
 * @author leegc
 *
 */
public class BestEffortTXTParser extends AbstractAnalyticsParser {
	private final String COLON = ":";

	@Override
	public Set<MediaType> getSupportedTypes(ParseContext context) {
		return Collections.singleton(MediaType.TEXT_PLAIN);
	}

	@Override
	public ProfilerRecord getNextProfilerRecord(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

			DefaultProfilerRecord record = new DefaultProfilerRecord();

			String key = null;
			String line = null;

			while ((line = br.readLine()) != null) {
				if (line.contains(COLON)) {
					String[] keyValue = line.split(COLON, 2);
					if (keyValue.length == 2) {
						key = keyValue[0].trim();
						record.put(key, keyValue[1].trim());
					} else {
						if (key != null) {
							record.put(key, record.get(key) + System.lineSeparator() + line);
						}
					}
				} else {
					if (key != null) {
						record.put(key, record.get(key) + System.lineSeparator() + line);
					}
				}
			}

			return record;
		} catch (IOException e) {
			throw new AnalyticsTikaProfilingException(e);
		}
	}


}
