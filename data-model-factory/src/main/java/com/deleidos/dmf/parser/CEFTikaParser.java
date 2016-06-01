package com.deleidos.dmf.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.XHTMLContentHandler;
import org.json.JSONObject;
import org.xml.sax.ContentHandler;

import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.framework.AbstractAnalyticsParser;
import com.deleidos.dmf.framework.TikaProfilerParameters;
import com.deleidos.dp.profiler.api.ProfilerRecord;
import com.deleidos.rtws.splitter.LineSplitter;
import com.deleidos.rtws.splitter.Splitter;

public class CEFTikaParser extends AbstractAnalyticsParser {
	private static Logger logger = Logger.getLogger(CEFTikaParser.class);
	private XHTMLContentHandler fXHTML;
	private Splitter splitter;
	private CEFObject cefObject;

	private static final Set<MediaType> SUPPORTED_TYPES = 
			Collections.singleton(MediaType.application("cef"));
	
	@Override
	public Set<MediaType> getSupportedTypes(ParseContext context) {
		return SUPPORTED_TYPES;
	}
	
	public static final String CEF_TYPE = "application/cef";
	
	public CEFTikaParser() {
		splitter = new LineSplitter();
	}

	private CEFObject breakDownLine(String line) {
		
		CEFObject cefObject = new CEFObject();
		StringBuilder firstSevenPipes = new StringBuilder();
		StringBuilder rawKeyValuePairs = new StringBuilder();
		String[] headerSplit = new String[2];
		int pipeCount = 0;
		char lastChar = ' ';
		int i = 0;
		while(i < line.length()) {
			char c = line.charAt(i);
			int n = (int)c;
			if(lastChar != '\\' && c=='|') pipeCount++;
			else if(n == 10 || c == '\n' || c == '\r') break;
			if(pipeCount == 7) {
				rawKeyValuePairs.append(c);
				break;
			}
			lastChar = c;
			firstSevenPipes.append(c);
			i++;
		}
		headerSplit[0] = firstSevenPipes.toString();
		loadHeaderToMap(cefObject.getHeaderMapping(), splitHeaderFields(headerSplit[0]));
		while(i < line.length()) {
			char c = line.charAt(i);
			int n = (int)c;
			if(n == 10 || c == '\n' || c == '\r') break;
			rawKeyValuePairs.append(c);
			lastChar = c;
			i++;
		}
		headerSplit[1] = rawKeyValuePairs.toString();
		loadContentToMap(cefObject.getExtensionMapping(), splitContentFields(headerSplit[1]));
		return cefObject;
	}

	public List<String> splitHeaderFields(String header) {
		final String ESCAPED_BACKSLASH = "\\\\";
		List<String> list = new ArrayList<String>();
		String[] splits = header.split("(?<!\\\\)[\\|]");
		for(int i = 0; i < 7; i++) {
			list.add(splits[i]);
		}
		return list;
	}

	public void loadHeaderToMap(Map<String,String> map, List<String> parsedHeaderFields) {
		if(parsedHeaderFields.size() != 7) {
			logger.error("Header fields not parsed to 7 fields.  Has " + parsedHeaderFields.size() + " instead.");
			return;
		}
		for(int i = 0; i < parsedHeaderFields.size(); i++) {
			String key = null;
			switch(i) {
			case 0: {
				key = "Version";
				break;
			}
			case 1: {
				key = "Device Vendor";
				break;
			}
			case 2: {
				key = "Device Product";
				break;
			}
			case 3: {
				key = "Device Version";
				break;
			}
			case 4: {
				key = "Signature ID";
				break;
			}
			case 5: {
				key = "Name";
				break;
			}
			case 6: {
				key = "Severity";
				break;
			}
			}
			map.put(key, parsedHeaderFields.get(i));
		}
	}

	public List<String[]> splitContentFields(String content) {
		//TODO still need to get escaped equals splitting properly
		List<String[]> list = new ArrayList<String[]>();
		String[] equalSplits = content.split("(?<!\\\\)[\\=]");
		int splitLength = equalSplits.length;
		if(splitLength > 0) {
			String[] keyValuePair;
			for(int i = 1; i < equalSplits.length - 1; i++) {
				keyValuePair = new String[2];
				keyValuePair[0] = equalSplits[i-1].substring(equalSplits[i-1].lastIndexOf(' ')).trim();
				keyValuePair[1] = equalSplits[i].substring(0, equalSplits[i].lastIndexOf(' '));
				list.add(keyValuePair);
			}
			keyValuePair = new String[2];
			keyValuePair[0] = equalSplits[splitLength-2].substring(equalSplits[splitLength-2].lastIndexOf(' ')).trim();
			keyValuePair[1] = equalSplits[splitLength-1].substring(0);
			list.add(keyValuePair);
		}

		return list;
	}

	public void loadContentToMap(Map<String,String> map, List<String[]> content) {
		Iterator<String[]> i = content.iterator();
		while(i.hasNext()) {
			String[] kvPair = i.next();
			map.put(kvPair[0], kvPair[1]);
		}
	}

	class CEFObject {
		Map<String, String> headerMapping;
		Map<String, String> extensionMapping;

		CEFObject() {
			headerMapping = new HashMap<String, String>();
			extensionMapping = new HashMap<String,String>();
		}

		public Map<String, String> getHeaderMapping() {
			return headerMapping;
		}

		public void setHeaderMapping(Map<String, String> headerMapping) {
			this.headerMapping = headerMapping;
		}

		public Map<String, String> getExtensionMapping() {
			return extensionMapping;
		}

		public void setExtensionMapping(Map<String, String> extensionMapping) {
			this.extensionMapping = extensionMapping;
		}
	}
	
	@Override
	public void preParse(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) {
		splitter.setInputStream(inputStream);
	}

	@Override
	public JSONObject parseSingleRecordAsJson(InputStream inputStream,
			ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws IOException {
		String split = splitter.split();
		if(split == null) return null;
		context.setCharsRead(context.getCharsRead()+split.length());
		JSONObject object = new JSONObject();
		CEFObject cef = breakDownLine(split);
		for(String key : cef.getHeaderMapping().keySet()) {
			object.put(key, cef.getHeaderMapping().get(key));
		}
		for(String key : cef.getExtensionMapping().keySet()) {
			object.put(key, cef.getExtensionMapping().get(key));
		}
		return object;
	}

	@Override
	public ProfilerRecord getNextProfilerRecord(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		try {
			return super.flattenedJsonToDefaultProfilerRecord(this.parseSingleRecordAsJson(inputStream, handler, metadata, context), context.getCharsRead());
		} catch (IOException e) {
			throw new AnalyticsTikaProfilingException(e);
		}
	}


}
