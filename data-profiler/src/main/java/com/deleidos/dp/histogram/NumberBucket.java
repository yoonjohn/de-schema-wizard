package com.deleidos.dp.histogram;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * Bucket that contains exclusively numerical values.
 * @author leegc
 *
 */
public class NumberBucket extends AbstractBucket {
	private static Logger logger = Logger.getLogger(NumberBucket.class);
	@JsonIgnore
	public BigDecimal minBoundary;
	@JsonIgnore
	public BigDecimal maxBoundary;

	private NumberBucket() {
		super();
	}

	public NumberBucket(String definition, BigInteger count) {
		super(definition, count);
		String[] splits = definition.split(",", 2);
		String minString = null;
		String maxString = null;
		if(splits.length == 2) {
			minString = splits[0].substring(1).trim();
			maxString = splits[1].substring(0, splits[1].length()-1).trim();
		} else {
			minString = splits[0].trim();
			maxString = minString;
		}
		minBoundary = new BigDecimal(minString);
		maxBoundary = new BigDecimal(maxString);
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

	/*public BigDecimal getMinBoundary() {
		return minBoundary;
	}
	public void setMinBoundary(BigDecimal minBoundary) {
		this.minBoundary = minBoundary;
	}

	public BigDecimal getMaxBoundary() {
		return maxBoundary;
	}
	public void setMaxBoundary(BigDecimal maxBoundary) {
		this.maxBoundary = maxBoundary;
	}*/

	@Override
	public String getLabel() {
		if(maxBoundary == null || minBoundary.compareTo(maxBoundary) == 0) return minBoundary.toString();
		String s = "[" + minBoundary.toString() + "," + maxBoundary.toEngineeringString() + ")";
		return s;
	}

	/**
	 * Bucket that contains numerical values that should be expressed in scientific notation.
	 * @author leegc
	 *
	 */
	/*public class ScientificBucket extends NumberBucket {

		public ScientificBucket(BigDecimal min, BigDecimal max) {
			super(min, max);
		}

		public ScientificBucket(String definition, BigInteger count) {
			super(definition, count);
			String[] splits = definition.split(",", 2);
			String minString = null;
			String maxString = null;
			if(splits.length == 2) {
				minString = splits[0].substring(1).trim();
				maxString = splits[1].substring(0, splits[1].length()-1).trim();
			} else {
				minString = splits[0].trim();
				maxString = minString;
			}
			minBoundary = new BigDecimal(minString);
			maxBoundary = new BigDecimal(maxString);
			this.setCount(count);
		}

	}*/

	/**
	 * Bucket the contains discrete numeric values.
	 * @author leegc
	 *
	 */
	/*public class IntegerBucket extends NumberBucket {

		public IntegerBucket(BigDecimal min, BigDecimal max) {
			super(min, max);
		}

		public IntegerBucket(String definition, BigInteger count) {
			super(definition, count);
			String[] splits = definition.split("-", 2);
			String minString = null;
			String maxString = null;
			if(splits.length == 2) {
				minString = splits[0].substring(1).trim();
				maxString = splits[1].substring(0, splits[1].length()-1).trim();
			} else {
				minString = splits[0].trim();
				maxString = minString;
			}
			minBoundary = new BigDecimal(minString);
			maxBoundary = new BigDecimal(maxString);
			this.setCount(count);
		}

		@Override
		public int belongs(Object object) {
			BigDecimal entry = new BigDecimal(object.toString());
			if(entry.compareTo(minBoundary) == -1) return -1;
			else if(entry.compareTo(maxBoundary) == 1) return 1;
			else return 0;
		}

		@Override
		public String getLabel() {
			if(minBoundary.compareTo(maxBoundary) == 0) return minBoundary.toString(); 
			String s = minBoundary.toString() + "-" + maxBoundary.toString();
			return s;
		}
	}
	 */
	@Override
	public int compareTo(AbstractBucket o) {
		NumberBucket otherBucket = (NumberBucket)o;
		if(maxBoundary !=null) {
			if(this.minBoundary.compareTo(otherBucket.minBoundary) == -1) return -1;
			else if(this.maxBoundary.compareTo(otherBucket.maxBoundary) == 1) return 1;
			else return 0;
		} else {
			return this.minBoundary.compareTo(otherBucket.minBoundary);
		}
	}

	/*@Override
	public NumberBucket coalesce(AbstractBucket nextBucket) {
		NumberBucket otherBucket = (NumberBucket) nextBucket;
		NumberBucket newBucket;
		if(maxBoundary != null) {
			if(minBoundary.compareTo(maxBoundary) == 0) {
				newBucket = new NumberBucket(minBoundary, otherBucket.minBoundary);
				BigInteger newCount = this.count.add(otherBucket.count);
				newBucket.setCount(newCount);
			} else {
				newBucket = new NumberBucket(minBoundary, otherBucket.maxBoundary);
				BigInteger newCount = this.count.add(otherBucket.count);
				newBucket.setCount(newCount);
			}
			return newBucket;
		} else {
			newBucket = new NumberBucket(minBoundary, otherBucket.minBoundary);
			BigInteger newCount = this.count.add(otherBucket.count);
			newBucket.setCount(newCount);
			return newBucket;
		}
	}*/
}
