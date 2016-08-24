package com.deleidos.dp.histogram;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * Bucket that contains exclusively numerical values.
 * @author leegc
 *
 */
public class NumberBucket extends AbstractBucket {
	public static final int TEMP_DEFAULT_NUMBER_BUCKET_DISPLAY_CUTOFF = 6;
	private static Logger logger = Logger.getLogger(NumberBucket.class);
	@JsonIgnore
	public BigDecimal minBoundary;
	@JsonIgnore
	public BigDecimal maxBoundary;

	private NumberBucket() {
		super();
	}

	public static BigDecimal[] parseLabel(String label) {
		BigDecimal[] minAndMax = new BigDecimal[2];
		String[] splits = label.split(",", 2);
		String minString = null;
		String maxString = null;
		if(splits.length == 2) {
			minString = splits[0].substring(1).trim();
			maxString = splits[1].substring(0, splits[1].length()-1).trim();
		} else {
			minString = splits[0].trim();
			maxString = null;
		}
		minAndMax[0] = new BigDecimal(minString);
		minAndMax[1] = maxString == null ? null : new BigDecimal(maxString);
		return minAndMax;
	}

	public NumberBucket(String longLabel, String shortLabel, BigInteger count) {
		super(count);
		BigDecimal[] minAndMax = parseLabel(longLabel);
		minBoundary = minAndMax[0];
		maxBoundary = minAndMax[1];
		this.setCount(count);
	}

	public static long determineAppropriateNumberOfBuckets(BigDecimal min, BigDecimal max, long distinctValues, int distinctValuesLimit) {
		if(min.compareTo(max) == 0) return 1;
		else if(distinctValues > distinctValuesLimit) return -1;
		else return distinctValues;
	}

	public NumberBucket(BigDecimal singleValueLabel) {
		super();
		minBoundary = singleValueLabel;
		maxBoundary = null;
	}

	public NumberBucket(BigDecimal min, BigDecimal max) {
		super();
		minBoundary = min;
		maxBoundary = max;
	}

	@Override
	public int belongs(Object object) {
		BigDecimal entry = new BigDecimal(object.toString());
		if(maxBoundary != null) {
			if(entry.compareTo(minBoundary) == -1) return -1;
			else if(entry.compareTo(maxBoundary) == 1 || entry.compareTo(maxBoundary) == 0) return 1;
			else return 0;
		} else {
			return entry.compareTo(minBoundary);
		}
	}

	public void setCount(BigInteger count) {
		this.count = count;
	}

	@Override
	public String getLabel() {
		return generateRawLabel(minBoundary, maxBoundary);
	}

	@Override
	public int compareTo(AbstractBucket o) {
		NumberBucket otherBucket = (NumberBucket)o;
		if(maxBoundary != null) {
			if(this.minBoundary.compareTo(otherBucket.minBoundary) == -1) return -1;
			else if(this.maxBoundary.compareTo(otherBucket.maxBoundary) == 1) return 1;
			else return 0;
		} else {
			return this.minBoundary.compareTo(otherBucket.minBoundary);
		}
	}

	public static String trimRawLabel(String rawLabel, int cutoffLength) {
		BigDecimal[] minAndMax = parseLabel(rawLabel);
		if(minAndMax[1] == null) {
			return trimLabel(minAndMax[0], cutoffLength);
		} else {
			return "[" + trimLabel(minAndMax[0], cutoffLength) + "," + trimLabel(minAndMax[1], cutoffLength) + ")";
		}
	}

	private static String trimLabel(BigDecimal unRangedValue, int cutoffLength) {
		BigInteger whole = unRangedValue.toBigInteger();
		String wholeString = whole.toString();
		BigDecimal dec = unRangedValue.remainder(BigDecimal.ONE).stripTrailingZeros();
		String decString = dec.toPlainString();
		decString = (decString.contains(".")) ? decString.substring(decString.indexOf(".") + 1) : decString;
		StringBuilder sb = new StringBuilder();
		if(wholeString.length() >= cutoffLength) {
			sb.append(wholeString.substring(0,cutoffLength));
			for(int i = 0; i < wholeString.length()-cutoffLength; i++) {
				sb.append("0");
			}
		} else { // TODO error with length here
			sb.append(wholeString);
			int remainingLength = cutoffLength-wholeString.length();
			
			remainingLength = (remainingLength > decString.length()-1) ? decString.length() : remainingLength;
			if(remainingLength > 0) {
				sb.append("." + decString.substring(0, remainingLength));
			}
		}
		return sb.toString();
	}

	private static String generateRawLabel(BigDecimal min, BigDecimal max) {
		if(max == null) {
			return min.toPlainString();
		} else {
			return "[" + min.toPlainString() + "," + max.toPlainString() + ")";
		}
	}

}
