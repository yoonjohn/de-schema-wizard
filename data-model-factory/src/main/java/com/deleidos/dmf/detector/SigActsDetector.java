package com.deleidos.dmf.detector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import com.deleidos.dmf.framework.AbstractMarkSupportedAnalyticsDetector;

/**
 * Detector for SigActs files.
 * 
 * @author yoonj1
 */
@SuppressWarnings("serial")
public class SigActsDetector extends AbstractMarkSupportedAnalyticsDetector {
	private static final Logger logger = Logger.getLogger(SigActsDetector.class);
	public static final MediaType CONTENT_TYPE = MediaType.application("sigacts");
	protected InputStreamReader isReader;
	private static final String NAME_VALUE_DELIMITER = ":";
	private static final String FILE_CONTENT = "FILE_CONTENT";
	@Override
	public Set<MediaType> getDetectableTypes() {
		return Collections.singleton(CONTENT_TYPE);
	}

	@Override
	public MediaType analyticsDetect(InputStream inputStream, Metadata metadata) throws IOException {
		if (inputStream.available() == 0) {
			return null;
		}

		isReader = new InputStreamReader(inputStream, "UTF-8");

		if (readRecord() != null) {
			return CONTENT_TYPE;
		} else {
			return null;
		}
	}

	@Override
	public boolean closeOnBinaryDetection(InputStream inputStream) throws IOException {
		boolean binary = testIsBinary(inputStream, 2000, .30f);
		if (binary) {
			return true;
		} else {
			return false;
		}
	}

	// Private methods
	private HashMap<String, String> parseRecord() {
		HashMap<String, String> map = new HashMap<String, String>();
		BufferedReader bReader = new BufferedReader(isReader);
		StringBuffer fileContent = new StringBuffer();
		String line;

		try {
			while ((line = bReader.readLine()) != null) {
				fileContent.append(line);
				String[] nvPair = line.split(NAME_VALUE_DELIMITER, 2);
				if (nvPair != null && nvPair.length == 2) {
					map.put(nvPair[0].trim(), nvPair[1].trim());
				}
			}
			map.put(FILE_CONTENT, fileContent.toString());
		} catch (IOException e) {
			logger.error(e);
		} 

		return map;
	}

	private String readRecord() {
		HashMap<String, String> map = new HashMap<String, String>();
		map = parseRecord();

		if (map.get("SERIAL") != null) {
			if (map.get("DTG_DT") != null) {
				if (map.get("EVENT TYPE 1") != null) {
					if (map.get("EVENT CATEGORY 1") != null) {
						if (map.get("REGIONAL COMMAND") != null) {
							if (map.get("EVENT TYPE 2") != null) {
								if (map.get("CF WIA") != null) {
									if (map.get("CF KIA") != null) {
										if (map.get("HN WIA") != null) {
											if (map.get("HN KIA") != null) {
												if (map.get("CIV WIA") != null) {
													if (map.get("CIV KIA") != null) {
														if (map.get("ENEMY WIA") != null) {
															if (map.get("ENEMY KIA") != null) {
																if (map.get("DETAINED") != null) {
																	if (map.get("MGRS") != null) {
																		if (map.get("LAT") != null) {
																			if (map.get("LONG") != null) {
																				return "SigActs";
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		} 
		return null;
	}
}
