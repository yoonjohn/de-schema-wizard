package com.deleidos.dp.beans;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONObject;
import org.junit.Test;

import com.deleidos.dp.deserializors.SerializationUtility;


public class RegionDataBeanTest {
	private Logger logger = Logger.getLogger(RegionDataBeanTest.class);

	@Test
	public void testOutputBean() throws JsonGenerationException, JsonMappingException, IOException {
		RegionData rd = new RegionData();
		rd.setLatitudeKey("lat");
		rd.setLongitudeKey("lon");
		rd.setCols(new ArrayList<ColsEntry>(Arrays.asList(new ColsEntry("Country"), new ColsEntry("Frequency"))));
		rd.setRows(new ArrayList<RowEntry>(Arrays.asList(
				new RowEntry("United States",300), 
				new RowEntry("Russia",500),
				new RowEntry("Canada",100), 
				new RowEntry("Brazil",1000), 
				new RowEntry("Germany",200)
				)));
		JSONObject result = new JSONObject(SerializationUtility.serialize(rd));
		JSONObject json = new JSONObject("{\r\n" + 
				"                \"cols\": [{\r\n" + 
				"                                \"label\": \"Country\"\r\n" + 
				"                }, {\r\n" + 
				"                                \"label\": \"Frequency\"\r\n" + 
				"                }],\r\n" + 
				"                \"rows\": [{\r\n" + 
				"                                \"c\": [{\r\n" + 
				"                                                \"v\": \"United States\"\r\n" + 
				"                                }, {\r\n" + 
				"                                                \"v\": 300\r\n" + 
				"                                }]\r\n" + 
				"                }, {\r\n" + 
				"                                \"c\": [{\r\n" + 
				"                                                \"v\": \"Russia\"\r\n" + 
				"                                }, {\r\n" + 
				"                                                \"v\": 500\r\n" + 
				"                                }]\r\n" + 
				"                }, {\r\n" + 
				"                                \"c\": [{\r\n" + 
				"                                                \"v\": \"Canada\"\r\n" + 
				"                                }, {\r\n" + 
				"                                                \"v\": 100\r\n" + 
				"                                }]\r\n" + 
				"                }, {\r\n" + 
				"                                \"c\": [{\r\n" + 
				"                                                \"v\": \"Brazil\"\r\n" + 
				"                                }, {\r\n" + 
				"                                                \"v\": 1000\r\n" + 
				"                                }]\r\n" + 
				"                }, {\r\n" + 
				"                                \"c\": [{\r\n" + 
				"                                                \"v\": \"Germany\"\r\n" + 
				"                                }, {\r\n" + 
				"                                                \"v\": 200\r\n" + 
				"                                }]\r\n" + 
				"                }]\r\n" + 
				"}");
		json.put("latitude-key", "lat");
		json.put("longitude-key", "lon");
		assertTrue(result.similar(json));
		logger.info("Bean matches specification");
	}
}
