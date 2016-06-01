package com.deleidos.dmf.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tika.mime.MediaType;
import org.junit.After;
import org.junit.Before;

import com.deleidos.dmf.analyzer.TikaAnalyzer;
import com.deleidos.dmf.analyzer.workflows.DefinedTestResource;
import com.deleidos.dmf.detector.CEFDetector;
import com.deleidos.dmf.detector.CSVDetector;
import com.deleidos.dmf.detector.DiplomaticCableDetector;
import com.deleidos.dmf.detector.IIRDetector;
import com.deleidos.dmf.detector.JSONDetector;
import com.deleidos.dmf.detector.SPOTDetector;
import com.deleidos.dmf.detector.SigActsDetector;
import com.deleidos.dmf.parser.PCAPTikaParser;

/**
 * Class to load resources for tests.  As of 12/14/2015, this classes requires a running H2 Database, or it will throw a NullPointerException.
 * Test classes that want to use 'defined resources' should extend this class.  For information regarding defined resources, see DefinedTestResource.java
 * @author leegc
 *
 */
public class ResourceLoader {
	private Logger logger = Logger.getLogger(ResourceLoader.class);
	public boolean debug = false;
	public boolean localBulkTest = false;
	protected String localBulkTestPath = "C:\\Users\\leegc\\Documents\\Data";
	//protected Map<String, String> sources;
	protected List<DefinedTestResource> streamSources;
	//protected static TikaWorker worker;
	//protected static TikaAnalyzer analyzer;

	public void loadToFiles(File file, String expectedDataType, String expectedBodyContentType) throws FileNotFoundException {
		for(String s : file.list()) {
			File f = new File(file, s);
			if(f.isFile()) {
				streamSources.add(new DefinedTestResource(f.getPath(), expectedDataType, expectedBodyContentType, new FileInputStream(f), true, true));
			} else {
				loadToFiles(f, f.getName(), null);
			}
		}
	}

	@Before
	public void initFiles() throws FileNotFoundException, UnsupportedEncodingException {
		//System.setProperty("java.library.path", "C:\\Users\\leegc\\Downloads\\jnetpcap-1.3.0-1.win64\\jnetpcap-1.3.0\\jnetpcap.dll");
		if(System.getenv().containsKey("DEBUG_LOCAL") && System.getenv("DEBUG_LOCAL").equals("true")) {
			localBulkTest = true;
		} else {
			localBulkTest = false;
		}
		streamSources = new ArrayList<DefinedTestResource>();
		if(localBulkTest) {
			File testingDir = new File(localBulkTestPath);
			loadToFiles(testingDir, "data", null);
		} else {
			//sources = new HashMap<String, String>();

			//cannot define compressed sources here. need to copy them to disk

			addToSources("/iir.zip", MediaType.APPLICATION_ZIP.toString(), true, true);
			addToSources("/json.zip", MediaType.APPLICATION_ZIP.toString(), true, true);
			addToSources("/csv.zip", MediaType.APPLICATION_ZIP.toString(), true, true);
			
			addToSources("/IIR-2012-hard-Manoj.docx", "application/x-tika-ooxml", IIRDetector.CONTENT_TYPE.toString(), true, true);
			
			addToSources("/IIR_2012-1346964722320.pdf", MediaType.application("pdf").toString(), IIRDetector.CONTENT_TYPE.toString(), true, true);
			addToSources("/TIIR-525MIBDE-030-302-103-12_2.pdf", MediaType.application("pdf").toString(), IIRDetector.CONTENT_TYPE.toString(), true, true);

			addToSources("/IIR_2012-13456-text.txt", IIRDetector.CONTENT_TYPE.toString(), true, true);

			addToSources("/README.txt", "data", false, false);

			addToSources("/xml1.xml", MediaType.application("xml").toString(), true, true);
			addToSources("/xml2.txt", MediaType.application("xml").toString(), true, true);
			addToSources("/xml3.xml", MediaType.application("xml").toString(), true, true);
			//addToSources("/simplexml.txt", MediaType.application("xml").toString(), true, true);
			
			addToSources("/site.yml", "data", false, false);

			addToSources("/generated-centos7-standalone.tar", "tar", false, false);

			addToSources("/ceffile", CEFDetector.CONTENT_TYPE.toString());
			
			addToSources("/SPOT1.docx", "application/x-tika-ooxml", SPOTDetector.CONTENT_TYPE.toString(), true, true);
			addToSources("/SPOT2.docx", "application/x-tika-ooxml", SPOTDetector.CONTENT_TYPE.toString(), true, true);
			addToSources("/SPOT3.docx", "application/x-tika-ooxml", SPOTDetector.CONTENT_TYPE.toString(), true, true);
			addToSources("/SPOT4.docx", "application/x-tika-ooxml", SPOTDetector.CONTENT_TYPE.toString(), true, true);
			addToSources("/SPOT5.docx", "application/x-tika-ooxml", SPOTDetector.CONTENT_TYPE.toString(), true, true);
			addToSources("/spot001.txt", SPOTDetector.CONTENT_TYPE.toString(), true, true);
			addToSources("/spot002.txt", SPOTDetector.CONTENT_TYPE.toString(), true, true);
			addToSources("/spot003.txt", SPOTDetector.CONTENT_TYPE.toString(), true, true);
			addToSources("/spot004.txt", SPOTDetector.CONTENT_TYPE.toString(), true, true);
			addToSources("/spot005.txt", SPOTDetector.CONTENT_TYPE.toString(), true, true);
			

			addToSources("/synscan.pcap", PCAPTikaParser.PCAP_TYPE.toString(), true, false);

			addToSources("/simple.txt", "data", false, false);

			addToSources("/TeamsHalf.csv", CSVDetector.CONTENT_TYPE.toString());

			addToSources("/datasink.mongodb-pod.json", JSONDetector.CONTENT_TYPE.toString());
			addToSources("/simplejson.txt", JSONDetector.CONTENT_TYPE.toString());
			addToSources("/complexjson.txt", JSONDetector.CONTENT_TYPE.toString());
			addToSources("/FlightJson.txt", JSONDetector.CONTENT_TYPE.toString());
			addToSources("/FlightPositionJson.txt", JSONDetector.CONTENT_TYPE.toString());
			
			addToSources("/SigActs1.txt", SigActsDetector.CONTENT_TYPE.toString());
			addToSources("/SigActs2.txt", SigActsDetector.CONTENT_TYPE.toString());
			addToSources("/SigActs3.txt", SigActsDetector.CONTENT_TYPE.toString());
			addToSources("/SigActs4.txt", SigActsDetector.CONTENT_TYPE.toString());
			addToSources("/SigActs5.txt", SigActsDetector.CONTENT_TYPE.toString());
			addToSources("/SigActs6.txt", SigActsDetector.CONTENT_TYPE.toString());
			addToSources("/SigActs7.txt", SigActsDetector.CONTENT_TYPE.toString());
			addToSources("/SigActs8.txt", SigActsDetector.CONTENT_TYPE.toString());
			addToSources("/SigActs9.txt", SigActsDetector.CONTENT_TYPE.toString());
			
			addToSources("/DiplomaticCable1.txt", DiplomaticCableDetector.CONTENT_TYPE.toString());
			addToSources("/DiplomaticCable2.txt", DiplomaticCableDetector.CONTENT_TYPE.toString());
			addToSources("/DiplomaticCable3.txt", DiplomaticCableDetector.CONTENT_TYPE.toString());
			addToSources("/DiplomaticCable4.txt", DiplomaticCableDetector.CONTENT_TYPE.toString());
			
			//addToSources("/image0.jpg", "image/jpeg");
			
		}
	}
	
	protected boolean addToSource(DefinedTestResource dtr) {
		return streamSources.add(dtr);
	}
	
	protected void addToSources(String resourceName, String expectedType) throws UnsupportedEncodingException {
		addToSources(resourceName, expectedType, null, true, true);
	}
	
	protected void addToSources(String resourceName, String expectedType, boolean isDetectorReady, boolean isParserReady) throws UnsupportedEncodingException {
		addToSources(resourceName, expectedType, null, isDetectorReady, isParserReady);
	}

	protected void addToSources(String resourceName, String expectedType, String expectedBodyContentType, boolean isDetectorReady, boolean isParserReady) throws UnsupportedEncodingException {
		if(!resourceName.startsWith("/")) {
			resourceName = "/" + resourceName;
		}
		String path = resourceName;
		path = URLDecoder.decode(path, "UTF8");
		InputStream is = getClass().getResourceAsStream(path);
		if(is == null) {
			logger.error("Resource " + path + " not found.  Ignoring.");
		} else {
			streamSources.add(new DefinedTestResource(path, expectedType, expectedBodyContentType, is, isDetectorReady, isParserReady));
		}
		//sources.put(resourceName, expectedType);
	}
}

