package com.deleidos.dmf.detector;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import com.deleidos.dmf.framework.AbstractMarkSupportedAnalyticsDetector;

public class IIRDetector extends AbstractMarkSupportedAnalyticsDetector {
	/**
	 * 
	 */
	private static final Logger logger = Logger.getLogger(IIRDetector.class);

	private static final long serialVersionUID = -253897386870711283L;
	public static final MediaType CONTENT_TYPE = MediaType.application("iir");

	@Override
	public Set<MediaType> getDetectableTypes() {
		return Collections.singleton(CONTENT_TYPE);
	}

	@Override
	public MediaType analyticsDetect(InputStream inputStream, Metadata metadata) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
		if(passesScanForIIR(br)) {
			return CONTENT_TYPE;
		} else {
			return null;
		}
	}

	private boolean passesScanForIIR(BufferedReader br) throws IOException {
		IIRValidator validator = new IIRValidator();
		while(!validator.isComplete()) {
			if(!validator.validate(br)) {
				return false;
			}
		}
		return validator.isValid();
	}

	@Override
	public boolean closeOnBinaryDetection(InputStream inputStream) throws IOException {
		boolean binary = testIsBinary(inputStream, 2000, .20f);
		if(binary) return true;
		return false;
	}

	private class IIRValidator {
		private boolean complete;
		private boolean valid;
		private boolean hasClassification;
		private EssentialElements essentials;
		private Map<String, Boolean> essentialMapping;
		private JaroWinklerDistance j;

		public IIRValidator() {
			j = new JaroWinklerDistance();
			hasClassification = false;
			valid = true;
			complete = false;
			essentials = new EssentialElements();
			essentialMapping = new HashMap<String, Boolean>();
			essentials.forEach(x->essentialMapping.put(x, false));
		}

		public void loadChars(char[] cbuf) throws IOException {
			BufferedReader br = new BufferedReader(new CharArrayReader(cbuf));
			String trimmed = null;
			int lineCount = 0;
			

			final String subjectPrefix = "SUBJECT";
			final String serialPrefix = "SERIAL";
			boolean hasSubject = false;
			boolean hasSerial = false;
			
			
			while((trimmed = br.readLine()) != null) {
				lineCount++;
				if(trimmed.isEmpty()) {
					continue;
				} else {
					/*int colonIndex = trimmed.indexOf(':');
					if(colonIndex > 0) {
						String beforeColon = trimmed.substring(0, colonIndex);
						if(essentials.contains(beforeColon)) {
							System.out.println(beforeColon);
							essentialMapping.put(beforeColon, true);
						}
					}
					if(lineCount > 50) {
						break;
					}*/
					if(trimmed.startsWith(subjectPrefix)) {
						int gotoIndex = (trimmed.length() > 50) ? 50 : trimmed.length() - 1;
						String afterSubject = trimmed.substring(subjectPrefix.length(), gotoIndex);
						if(afterSubject.contains("IIR")) {
							hasSubject = true;
						}
					} else if(trimmed.startsWith(serialPrefix)) {
						int gotoIndex = (trimmed.length() > 50) ? 50 : trimmed.length() - 1;
						String afterSubject = trimmed.substring(serialPrefix.length(), gotoIndex);
						if(afterSubject.contains("IIR")) {
							hasSerial = true;
						}
					}
				}
			}

			complete = true;
			
			if(hasSerial && hasSubject) {
				valid = true;
			} else {
				valid = false;
			}
			
			/*complete = true;
			if(essentials.hasMost(essentialMapping)) {
				valid = true;
			} else {
				valid = false;
			}*/
			/*line = line.trim();
			if (!line.isEmpty() && !line.startsWith("IIR") 
				&& !line.startsWith("RR") && !line.startsWith("SERIAL:") && !line.startsWith("U N P U B L I S H E D")) {

			}
			// if we make it to the "RR" or "SERIAL" line without finding classification, break out
			if (line.startsWith("RR") || line.startsWith("SERIAL:")) {
				if(!hasClassification) {
					valid = false;
				}
			}*/
		}

		public boolean isValid() {
			if(complete && valid) {
				return true;
			} else {
				return valid;
			}
		}

		public boolean validate(BufferedReader br) throws IOException {
			char[] cbuf = new char[1024];
			br.read(cbuf);
			loadChars(cbuf);
			return isValid();
		}

		public boolean isComplete() {
			return complete;
		}
	}

	public static final class EssentialElements extends ArrayList<String> {

		/**
		 * 
		 */
		private static final long serialVersionUID = 7724929235068627114L;

		public EssentialElements() {
			addAll();
		}

		/*@Override
		public String get(int index) {
			int tweakedIndex = (index < 0) ? 0 : index;
			tweakedIndex = (index > this.size()-1) ? this.size()-1 : index;
			return super.get(tweakedIndex);
		}*/

		public boolean hasMost(Map<String, Boolean> containsMap) {
			float amountPresent = 0;
			for(String s : containsMap.keySet()) {
				if(containsMap.get(s)) {
					amountPresent++;
				}
			}
			float percentage = amountPresent/((float)this.size());
			if(percentage > .9f) {
				return true;
			} else {
				return false;
			}
		}

		private void addAll() {
			addAll(Arrays.asList("SERIAL","DATE OF PUBLICATION", "---------- DEPARTMENT OF DEFENSE ----------",
					"COUNTRY OR NONSTATE ENTITY", "SUBJECT", "DATE OF INFORMATION", "CUTOFF", "SUMMARY", "SOURCE NUMBER",
					"SOURCE", "CONTEXT", "WARNING", "TEXT", "COMMENTS", "TOPIC", "FUNCTIONAL CODE", "REQUIREMENT",
					"MGT CODE", "INSTR", "PREP", "JOINT REP", "DATE OF ACQUISITION", "POC", "WARNING", "AGENCY", "U.S. MISSION",
					"MILITARY", "STATE/LOCAL", "CLASSIFIED BY", "REASON", "DERIVED FROM", "DECLASSIFY ON"));
		}

	}


	/*int workingIndex = -1;
	if((workingIndex = line.indexOf('.')) > 0) {
		Integer.getInteger(line.substring(0, workingIndex), null); // will be null if not parsed correctly
	} else {
		if((workingIndex = line.indexOf(':')) > 0) {
			String beforeColon = line.substring(0, workingIndex);
			int approximate = approximateEssentialsIndex;
			for(int i = -1; i < 2; i++) {
				confidence += (.95f - j.getDistance(beforeColon, essentials.get(approximate - i)));
			}
		}
	}*/

}
