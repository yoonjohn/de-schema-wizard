package com.deleidos.dp.profiler;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.profiler.api.Profiler;


public class DeterminationTest {
	Logger logger = Logger.getLogger(DeterminationTest.class);
	
	@Test
	public void uniqueTest() {
		String name = appendWithStructuredObjectAppender("asdiOutput","asdiMessage","boundaryCrossingUpdate","nxcm:boundaryPosition","nxce:latLong","nxce:latitude","nxce:latitudeDMS","degrees");
		String name2 = appendWithStructuredObjectAppender("asdiOutput","asdiMessage","boundaryCrossingUpdate","nxcm:boundaryPosition","nxce:latLong","nxce:longitude","nxce:longitudeDMS","degrees");
		Map<String, Profile> p = new HashMap<String, Profile>();
		p.put(name, new Profile());
		p.put(name2, new Profile());
		p = DisplayNameHelper.determineDisplayNames(p);
		p.forEach((k,v) -> logger.info(k+"->"+v.getDisplayName()));
		assertTrue(p.get(name).getDisplayName().equals(appendWithStructuredObjectAppender("nxce:latitudeDMS","degrees")));
		assertTrue(p.get(name2).getDisplayName().equals(appendWithStructuredObjectAppender("nxce:longitudeDMS","degrees")));
	}
	
	@Test
	public void displayNameWorkingTest() {
		Map<String, Profile> p = new HashMap<String, Profile>();
		p.put(appendWithStructuredObjectAppender("waypoints", "lat"), new Profile());
		p.put(appendWithStructuredObjectAppender("waypoints", "lon"), new Profile());
		p.put(appendWithStructuredObjectAppender("lat"), new Profile());
		p.put(appendWithStructuredObjectAppender("lon"), new Profile());
		p = DisplayNameHelper.determineDisplayNames(p);
		p.forEach((k,v) -> logger.info(k+"->"+v.getDisplayName()));
		assertTrue(p.get(appendWithStructuredObjectAppender("lat")).getDisplayName()
				.equals(appendWithStructuredObjectAppender("lat")));
		assertTrue(p.get(appendWithStructuredObjectAppender("lon")).getDisplayName()
				.equals(appendWithStructuredObjectAppender("lon")));
		assertTrue(p.get(appendWithStructuredObjectAppender("waypoints", "lat")).getDisplayName()
				.equals(appendWithStructuredObjectAppender("waypoints", "lat")));
		assertTrue(p.get(appendWithStructuredObjectAppender("waypoints", "lon")).getDisplayName()
				.equals(appendWithStructuredObjectAppender("waypoints", "lon")));
	}
	
	private String appendWithStructuredObjectAppender(String ... strings) {
		StringBuilder sb = new StringBuilder();
		for(String s : strings) {
			sb.append(s+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER);
		}
		String r = sb.toString();
		return r.substring(0, r.lastIndexOf(DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER));
	}

	@Test
	public void testDetermineDisplayNames() {
		Map<String, Profile> p = new HashMap<String, Profile>();
		p.put(appendWithStructuredObjectAppender("catalog","book","id"), new Profile());
		p.put(appendWithStructuredObjectAppender("catalog","book","author"), new Profile());
		p.put(appendWithStructuredObjectAppender("catalog","book","author","name"), new Profile());
		p.put(appendWithStructuredObjectAppender("catalog","book","title"), new Profile());
		p.put(appendWithStructuredObjectAppender("catalog","title"), new Profile());
		p.put(appendWithStructuredObjectAppender("catalog","id"), new Profile());
		p.put(appendWithStructuredObjectAppender("catalog","author"), new Profile());
		Map<String, Profile> p2 = DisplayNameHelper.determineDisplayNames(p);
		p2.forEach((k,v) -> logger.info(k+"->"+v.getDisplayName()));
		assertTrue(p2.get(appendWithStructuredObjectAppender("catalog","id")).getDisplayName().equals(appendWithStructuredObjectAppender("catalog","id")));
		assertTrue(p2.get(appendWithStructuredObjectAppender("catalog","title")).getDisplayName().equals(appendWithStructuredObjectAppender("catalog","title")));
		assertTrue(p2.get(appendWithStructuredObjectAppender("catalog","author")).getDisplayName().equals(appendWithStructuredObjectAppender("catalog","author")));
		assertTrue(p2.get(appendWithStructuredObjectAppender("catalog","book","author","name")).getDisplayName().equals("name"));
		assertTrue(p2.get(appendWithStructuredObjectAppender("catalog","book","author")).getDisplayName().equals(appendWithStructuredObjectAppender("book","author")));
		assertTrue(p2.get(appendWithStructuredObjectAppender("catalog","book","id")).getDisplayName().equals(appendWithStructuredObjectAppender("book","id")));
		assertTrue(p2.get(appendWithStructuredObjectAppender("catalog","book","title")).getDisplayName().equals(appendWithStructuredObjectAppender("book","title")));
	}

	@Test
	public void testDetermineDate() {
		DetailType t = MetricsCalculationsFacade.determineStringDetailType("2000-10-10");
		assertTrue(t == DetailType.DATE_TIME);
		logger.info("Date successfully detected");
	}

	@Test
	public void testDetermineBoolean() {
		DetailType t = MetricsCalculationsFacade.determineStringDetailType("true");
		assertTrue(t == DetailType.BOOLEAN);
		logger.info("Boolean successfully detected.");
	}

	@Test
	public void testDeterminePhrase() {
		DetailType t = MetricsCalculationsFacade.determineStringDetailType("hello there");
		assertTrue(t == DetailType.PHRASE);
		logger.info("Phrase successfully detected.");
	}

	@Test
	public void testDetermineTerm() {
		DetailType t = MetricsCalculationsFacade.determineStringDetailType("hello");
		assertTrue(t == DetailType.TERM);
		logger.info("Term successfully detected.");
	}

	@Test
	public void testDetermineInteger() {
		DetailType t = MetricsCalculationsFacade.determineNumberDetailType("43");
		assertTrue(t == DetailType.INTEGER);
		logger.info("Integer successfully detected.");
	}

	@Test
	public void testDetermineDecimal() {
		DetailType t = MetricsCalculationsFacade.determineNumberDetailType("43.5");
		assertTrue(t == DetailType.DECIMAL);
		logger.info("Decimal successfully detected.");
	}

	@Test
	public void testDetermineExponent() {
		DetailType t = MetricsCalculationsFacade.determineNumberDetailType("43.2E5");
		assertTrue(t == DetailType.EXPONENT);
		logger.info("Exponent successfully detected.");
	}

	@Test
	public void testDetermineString() {
		MainType t = MetricsCalculationsFacade.determineDataType("hello");
		assertTrue(t == MainType.STRING);
		logger.info("String successfully detected.");
	}

	@Test
	public void testDetermineNumber() {
		MainType t = MetricsCalculationsFacade.determineDataType(123);
		assertTrue(t == MainType.NUMBER);
		logger.info("Number successfully detected.");
	}

	@Test
	public void testDetermineExponentAsNumber() {
		MainType t = MetricsCalculationsFacade.determineDataType("400E51");
		assertTrue(t == MainType.NUMBER);
		logger.info("Number successfully detected.");
	}

	@Test
	public void testDetermineNumberInString() {
		MainType t = MetricsCalculationsFacade.determineDataType("12544362");
		assertTrue(t == MainType.NUMBER);
		logger.info("Number in string format successfully detected.");
	}

	@Test
	public void testDetermineNumberWithDecimalInString() {
		MainType t = MetricsCalculationsFacade.determineDataType("12544362.3");
		DetailType dt = MetricsCalculationsFacade.determineDetailType(t, "12544362.3");
		assertTrue(t == MainType.NUMBER && dt == DetailType.DECIMAL);
		logger.info("Decimal in string format successfully detected.");
	}

	@Test
	public void testDetermineNumberWithMultipleDecimalInString() {
		MainType t = MetricsCalculationsFacade.determineDataType("1254436243563.4563456345.3");
		assertTrue(t == MainType.STRING);
		logger.info("Invalid number successfully detected as string.");
	}

	@Test
	public void testDetermineNumberWithExponentInString() {
		MainType t = MetricsCalculationsFacade.determineDataType("125443624356e43");
		assertTrue(t == MainType.NUMBER);
		logger.info("Possible exponent number as string successfully detected.");
	}

	@Test
	public void testDetermineObject() {

		JSONObject o = new JSONObject();
		o.put("h", "hi");
		MainType t = MetricsCalculationsFacade.determineDataType(o);
		assertTrue(t == MainType.OBJECT);
	}

	@Test
	public void testDetermineMap() {
		JSONArray a = new JSONArray();
		a.put("hi");
		MainType t = MetricsCalculationsFacade.determineDataType(a);
		assertTrue(t == MainType.ARRAY);
	}
}
