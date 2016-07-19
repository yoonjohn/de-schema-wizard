package com.deleidos.dmf.parser;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.deleidos.dmf.parser.CEFTikaParser;

public class CEFTikaParserTest extends ParserTest {
	private Logger logger = Logger.getLogger(CEFTikaParserTest.class);
	String header = "Apr 26 00:00:02 127.0.0.1 CEF:0|Blue Coat|Proxy SG||TCP_HIT|TCP_HIT|Medium|";
	String content = "|| eventId=340260681108843 mrt=1272254394059 app=http in=338 out=1882 customerID=SDwW76BwBABCE9T5UIF38dA\\\\=\\\\= customerURI=/All Customers/Technology/Seville categorySignificance=/Informational categoryBehavior=/Execute/Response categoryDeviceGroup=/Proxy categoryOutcome=/Success categoryObject=/Host/Resource/File modelConfidence=0 severity=5 relevance=10 assetCriticality=0 priority=6 art=1272254335932 cat=main deviceSeverity=304 act=OBSERVED rt=1272254336000 src=10.41.48.255 sourceZoneID=M0mY23yQBABCadpEq8I+Jng\\\\=\\\\= sourceZoneURI=/All Zones/Customers/Seville/NORAM/US/VA/Suffolk/1282/LAN_Zone_7_10.41.48.1-10.41.55.254 suser=- sourceGeoCountryCode=US sourceGeoLocationInfo=Suffolk slong=77.0 slat=37.0 sourceGeoPostalCode=20167 sourceGeoRegionCode=VA dhost=www.npr.org dst=216.35.221.76 destinationZoneID=MsmcHVQ8BABCAUdgm7Ba7Kw\\\\=\\\\= destinationZoneURI=/All Zones/ArcSight System/Public Address Space Zones/216.0.0.0-222.255.255.255 destinationGeoCountryCode=US destinationGeoLocationInfo=Cary dlong=-78.7789001464 dlat=35.7509002685 destinationGeoPostalCode=27511 destinationGeoRegionCode=NC request=http://www.npr.org:80/?refresh\\\\=true requestMethod=GET requestClientApplication=Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; InfoPath.1; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729) cs1=- locality=1 cs3Label=r-supplier-ip cs2Label=sc-filter-category cs1Label=x-virus-id ahost=10.110.10.10 agt=10.110.10.10 av=4.7.1.5242.0 atz=America/New_York aid=FRJcLCgBABCAAVBfa4xEIA\\\\=\\\\= at=superagent_ng dvc=149.8.131.14 deviceZoneID=MP1Cv5fsAABCB5LYyFrIszg\\\\=\\\\= deviceZoneURI=/All Zones/ArcSight System/Public Address Space Zones/128.0.0.0-169.253.255.255 deviceZoneExternalID=128.0.0.0-169.253.255.255 dtz=UTC eventAnnotationStageUpdateTime=1272254394076 eventAnnotationModificationTime=1272254394076 eventAnnotationAuditTrail=1,1271884756723,root,Queued,,,,\\\\n eventAnnotationVersion=1 eventAnnotationFlags=0 eventAnnotationEndTime=1272254336000 eventAnnotationManagerReceiptTime=1272254394059 ad.arcSightEventPath=3Sgs7+BwBABD03C-vEJ0Wog\\\\=\\\\=";

	public boolean debug = false;
	
	@Before
	@Override
	public void setup() {
		CEFTikaParser cef = new CEFTikaParser();
		setParser(cef);
	}

	@Test
	public void testHeaderSplit() {
		CEFTikaParser parser = new CEFTikaParser();
		List list = parser.splitHeaderFields(header);
		Map<String,String> map = new HashMap<String,String>();
		parser.loadHeaderToMap(map, list);
		/*for(String key : map.keySet()) {
			System.out.println(key + ": " + map.get(key));
		}*/
		assertTrue(map.containsKey("Device Vendor"));
	}

	@Test
	public void testHeaderSplitWithEscapedPipe() {
		String header = "Apr 26 00:00:02 127.0.0.1 CEF:0|Bl\\\\ue Coat|Pro\\|xy SG||TCP_HIT|TCP_HIT|Medium|";
		CEFTikaParser parser = new CEFTikaParser();
		List list = parser.splitHeaderFields(header);
		Map<String,String> map = new HashMap<String,String>();
		parser.loadHeaderToMap(map, list);
		/*for(String key : map.keySet()) {
			System.out.println(key + ": " + map.get(key));
		}*/
		assertTrue(map.get("Device Product").equals("Pro\\|xy SG"));
	}

	@Test
	public void testContentSplit() {
		CEFTikaParser parser = new CEFTikaParser();
		List<String[]> list = parser.splitContentFields(content);
		Map<String,String> map = new HashMap<String,String>();
		parser.loadContentToMap(map, list);
		/*for(String key : map.keySet()) {
			System.out.println(key + ": " + map.get(key));
		}*/
		assertTrue(map.containsKey("destinationGeoPostalCode"));
	}

	@Ignore 
	@Test
	public void testContentWithEscapedSplit() {
		String content = "|| eventId=340260681108843 mrt=1272254394059 app\\\\=http in=338 out=1882 customerID=SDwW76BwBABCE9T5UIF38dA\\\\=\\\\= customerURI=/All Customers/Technology/Seville categorySignificance=/Informational categoryBehavior=/Execute/Response categoryDeviceGroup=/Proxy categoryOutcome=/Success categoryObject=/Host/Resource/File modelConfidence=0 severity=5 relevance=10 assetCriticality=0 priority=6 art=1272254335932 cat=main deviceSeverity=304 act=OBSERVED rt=1272254336000 src=10.41.48.255 sourceZoneID=M0mY23yQBABCadpEq8I+Jng\\\\=\\\\= sourceZoneURI=/All Zones/Customers/Seville/NORAM/US/VA/Suffolk/1282/LAN_Zone_7_10.41.48.1-10.41.55.254 suser=- sourceGeoCountryCode=US sourceGeoLocationInfo=Suffolk slong=77.0 slat=37.0 sourceGeoPostalCode=20167 sourceGeoRegionCode=VA dhost=www.npr.org dst=216.35.221.76 destinationZoneID=MsmcHVQ8BABCAUdgm7Ba7Kw\\\\=\\\\= destinationZoneURI=/All Zones/ArcSight System/Public Address Space Zones/216.0.0.0-222.255.255.255 destinationGeoCountryCode=US destinationGeoLocationInfo=Cary dlong=-78.7789001464 dlat=35.7509002685 destinationGeoPostalCode=27511 destinationGeoRegionCode=NC request=http://www.npr.org:80/?refresh\\\\=true requestMethod=GET requestClientApplication=Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; InfoPath.1; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729) cs1=- locality=1 cs3Label=r-supplier-ip cs2Label=sc-filter-category cs1Label=x-virus-id ahost=10.110.10.10 agt=10.110.10.10 av=4.7.1.5242.0 atz=America/New_York aid=FRJcLCgBABCAAVBfa4xEIA\\\\=\\\\= at=superagent_ng dvc=149.8.131.14 deviceZoneID=MP1Cv5fsAABCB5LYyFrIszg\\\\=\\\\= deviceZoneURI=/All Zones/ArcSight System/Public Address Space Zones/128.0.0.0-169.253.255.255 deviceZoneExternalID=128.0.0.0-169.253.255.255 dtz=UTC eventAnnotationStageUpdateTime=1272254394076 eventAnnotationModificationTime=1272254394076 eventAnnotationAuditTrail=1,1271884756723,root,Queued,,,,\\\\n eventAnnotationVersion=1 eventAnnotationFlags=0 eventAnnotationEndTime=1272254336000 eventAnnotationManagerReceiptTime=1272254394059 ad.arcSightEventPath=3Sgs7+BwBABD03C-vEJ0Wog\\\\=\\\\=";

		CEFTikaParser parser = new CEFTikaParser();
		List<String[]> list = parser.splitContentFields(content);
		Map<String,String> map = new HashMap<String,String>();
		parser.loadContentToMap(map, list);
		for(String key : map.keySet()) {
			logger.info(key + ": " + map.get(key));
		}
		assertTrue(map.containsKey("app\\\\="));
	}

}
