package com.deleidos.dmf.detector;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.tika.detect.Detector;
import org.apache.tika.detect.TextDetector;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.pkg.ZipContainerDetector;

import com.deleidos.dmf.framework.AbstractMarkSupportedAnalyticsDetector;

import au.com.bytecode.opencsv.CSVReader;

/**
 * CSVDetector built in accordance with RFC 4180.  Returns media type "text/csv" if the file complies with the specified format.
 * @author leegc
 *
 */
public class CSVDetector extends AbstractMarkSupportedAnalyticsDetector {
	private static final Logger logger = Logger.getLogger(CSVDetector.class);
	public static final MediaType CONTENT_TYPE = MediaType.text("csv");
	private char separator = ',';
	public StringBuilder err = new StringBuilder();
	private int startAvailable;
	protected BufferedReader br;
	
	@Override
	public Set<MediaType> getDetectableTypes() {
		return Collections.singleton(CONTENT_TYPE);
	}

	@Override
	public MediaType analyticsDetect(InputStream inputStream, Metadata metadata) throws IOException {
		LineValidator lv = new LineValidator();
		startAvailable = inputStream.available();
		if(startAvailable==0) return null;
		br = new BufferedReader(new InputStreamReader(inputStream));
		separator = detectSeparators(br);
		if(separator == 0) {
			return null;
		}
		while(!lv.complete) {
			lv.loadNextCompleteCSVLine(br);
			int commaCount = lv.popCommaCount();
			if(!lv.validate(commaCount)) {
				return null;
			}
		}
		return CONTENT_TYPE;
	}
	
	private class LineValidator {
		private int matcherCount = -1;
		private int commaCount;
		private boolean complete = false;
		private boolean firstFieldState = true;
		private boolean newFieldState = true;
		private boolean doubleQuoteEnclosedState = false;
		private boolean quotedState = false;
		private boolean isValid = true;

		public boolean validate(int commaCount) {
			if(complete) {
				if(isValid == true) {
					return true;
				} else {
					err.append("Stream completed but invalid format detected.");
					return false;
				}
			} else {
				if(commaCount == matcherCount) { 
					return true;
				} else {
					err.append("Line separator count does not match header.");
					return false;
				}
			}
		}

		public void resetStates() {
			firstFieldState = true;
			newFieldState = true;
			doubleQuoteEnclosedState = false;
			quotedState = false;
			isValid = true;
		}

		private void setInvalid() {
			isValid = false;
		}

		public boolean getIsValid() {
			return isValid;
		}

		public boolean load(String line) {
			StringReader stringReader = new StringReader(line);
			if(line == null) {
				complete = true;
				if(doubleQuoteEnclosedState || quotedState || newFieldState) {
					return false;
				} else {
					return true;
				}
			}
			boolean valid = true;
			int n = 0;
			char lastChar = (char)n;
			try {
				while((n = stringReader.read()) > -1) {
					char c = (char)n;
					if(newFieldState) {
						if(c==' '){
							//ignore whitespace
						} else if(c=='\"') {
							newFieldState = false;
							doubleQuoteEnclosedState = true;
							quotedState = true;
						} else if(c == separator) {
							commaCount++;
							newFieldState = true;
						} else if(n == 10 || n == 13) {
							if(firstFieldState) {
								//skip empty line
							} else {
								err.append("Invalid format: separator detected at the end of a line.");
								setInvalid();
								break;
							}
						} else {
							newFieldState = false;
							doubleQuoteEnclosedState = false;
						}
					} else {
						if(doubleQuoteEnclosedState) {
							if(quotedState) {
								if(c == '\"') {
									quotedState = false;
								} 
							} else {
								if(c == '\"') {
									quotedState = true;
								} else if(c == separator) {
									commaCount++;
									newFieldState = true;
								} else if(n == 13 || n == 10) {

								}
							}
						} else { 
							if(c=='\"') {
								setInvalid();
								err.append("Ivalid format: Quotation detected in the middle of a field name.");
								break;
							} else if(c==separator) {
								commaCount++;
								newFieldState = true;
							} else if(n == 10 || n == 13) {
								if(matcherCount > -1) {
									if(commaCount == 0) {
										err.append("Invalid format: First line detected with 0 separators.");
										setInvalid();
									} else {
										if(commaCount != matcherCount) {
											err.append("Separator count on line does not match set count.  Set count: " + matcherCount + " and separators on line: " + commaCount);
											setInvalid();
										}
									}
								} 
								break;
							}
						}
					}
					lastChar = c;
				}
			} catch (IOException e) {
				logger.error(e);
			}
			if(!quotedState) {
				if(matcherCount < 1) {
					if(commaCount == 0) {
						setInvalid();
					}
					if(lastChar==separator) {
						setInvalid();
					}
					setMatchCount(commaCount);
				}
				newFieldState = true;
				firstFieldState = true;
			} else {
				firstFieldState = false;
			}
			if(valid == false) {
				setInvalid();
			}
			return valid;
		}

		private void setMatchCount(int matchCount) {
			this.matcherCount = matchCount; 
		}

		public boolean hasCompletedCSVLine() {
			return firstFieldState;
		}

		public int popCommaCount() {
			int c = commaCount;
			commaCount = 0;
			return c;
		}
		
		private void loadNextCompleteCSVLine(BufferedReader br) throws IOException {
			StringBuilder fullCSVLine = new StringBuilder();
			String line;
			do {
				line = br.readLine();
				if(line == null) {
					complete = true;
					break;
				}
				fullCSVLine.append(line);
				load(line);		
			} while(!hasCompletedCSVLine());
			String f = fullCSVLine.toString();
		}
	}

	/**
	 * TODO could use this to auto detect separators
	 * @param pBuffered
	 * @return
	 * @throws IOException
	 */
	private char detectSeparators(BufferedReader pBuffered) throws IOException {
		final int lookAheadBufferSize = 4096;
		int lMaxValue = 0;
		char lCharMax = ','; 
		pBuffered.mark(lookAheadBufferSize);
		char[] cbuf = new char[lookAheadBufferSize];
		pBuffered.read(cbuf);
		
		boolean hasLineBreak = false;
		for(char c : cbuf) {
			if(c == '\r' || c == '\n' || String.valueOf(c) == "\r\n") {
				hasLineBreak = true;
			}
		}
		if(!hasLineBreak) {
			return 0;
		}

		ArrayList<Separators> lSeparators = new ArrayList<Separators>();        
		lSeparators.add(new Separators(','));
		lSeparators.add(new Separators(';'));
		lSeparators.add(new Separators('\t'));

		Iterator<Separators> lIterator = lSeparators.iterator();
		while (lIterator.hasNext()) {
			CharArrayReader charArrayReader = new CharArrayReader(cbuf);
			Separators lSeparator = lIterator.next();
			CSVReader lReader = new CSVReader(charArrayReader, lSeparator.getSeparator());
			String[] lLine;
			lLine = lReader.readNext();
			lSeparator.setCount(lLine.length);

			if (lSeparator.getCount() > lMaxValue) {
				lMaxValue = lSeparator.getCount();
				lCharMax = lSeparator.getSeparator();
			}
		}
		pBuffered.reset();

		return lCharMax;
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

	/**
	 * Close if less than 30% of the first 2000 characters are printable characters. 
	 */
	@Override
	public boolean closeOnBinaryDetection(InputStream inputStream) throws IOException {
		boolean binary = testIsBinary(inputStream, 2000, .30f);
		if(binary) return true;
		return false;
	}


}
