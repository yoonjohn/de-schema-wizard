package com.deleidos.dp.histogram;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.Histogram;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.exceptions.MainTypeRuntimeException;

public class DefinedRangesNumberBucketList extends AbstractNumberBucketList {
	private static final Logger logger = Logger.getLogger(DefinedRangesNumberBucketList.class);
	public static final MathContext HISTOGRAM_CONTEXT = MathContext.DECIMAL32;
	protected BigDecimal currentColumnWidth;

	protected DefinedRangesNumberBucketList(Histogram histogram) {
		super(false);
		bucketList = new LinkedList<NumberBucket>();
		List<String> shortLabels = histogram.getLabels();
		List<String> labels = histogram.getLongLabels();
		List<Integer> counts = histogram.getData();
		for(int i = 0; i < histogram.getLongLabels().size(); i++) {
			NumberBucket bucket = new NumberBucket(labels.get(i), shortLabels.get(i), BigInteger.valueOf(counts.get(i)));
			bucketList.add(bucket);
		}
		currentColumnWidth = bucketList.getFirst().maxBoundary.subtract(bucketList.getFirst().minBoundary);
	}

	protected DefinedRangesNumberBucketList(BigDecimal min, BigDecimal max) {
		super(false);
		bucketList = determineHistogram(min, max);
		currentColumnWidth = bucketList.getFirst().maxBoundary.subtract(bucketList.getFirst().minBoundary);
	}

	private DefinedRangesNumberBucketList(LinkedList<NumberBucket> buckets, BigDecimal width) {
		super(false);
		bucketList = buckets;
		currentColumnWidth = width;
	}

	private boolean valueTooLow(BigDecimal value) {
		return bucketList.getFirst().belongs(value) < 0;
	}

	private boolean valueTooHigh(BigDecimal value) {
		return bucketList.getLast().belongs(value) > 0;
	}

	private static DefinedRangesNumberBucketList addBucketsLowEnd(
			LinkedList<NumberBucket> list, int low, int high, BigDecimal width, BigDecimal value) {
		final NumberBucket first = list.getFirst();
		final NumberBucket last = list.getLast();
		double numNecessaryBuckets = first.minBoundary.subtract(value)
				.divide(width, MetricsCalculationsFacade.DEFAULT_CONTEXT).doubleValue();
		if(numNecessaryBuckets > bucketExpansionLimit) {
			throw new ArithmeticException("Magnitude of number too big for dynamic histogram.");
		}
		numNecessaryBuckets = Math.ceil(numNecessaryBuckets);
		int numTotalBuckets = (int)numNecessaryBuckets + list.size();
		if(numTotalBuckets >= high) {		
			int numDoublesNecessary = (int)(Math.log((double)numTotalBuckets/low)/Math.log(2)) + 1;
			BigDecimal doubleCoefficient = BigDecimal.valueOf(Math.pow(2, numDoublesNecessary));
			BigDecimal newWidth = width.multiply(doubleCoefficient);
			BigDecimal tempIncludedMinimum = last.minBoundary.subtract(BigDecimal.valueOf(low-1).multiply(newWidth));		
			DefinedRangesNumberBucketList newNumberBucketList = new DefinedRangesNumberBucketList(tempIncludedMinimum, last.minBoundary);
			for(int j = 0; j < list.size(); j++) {
				NumberBucket bucketToAddTo = newNumberBucketList.getBucketList().get(j/doubleCoefficient.intValue());
				BigInteger count = list.get(j).count;
				for(int i = 0; i< count.intValue(); i++) { //possible loss of precision
					bucketToAddTo.incrementCount();
				}
			}
			BigDecimal currentColumnWidth = newNumberBucketList.getCurrentColumnWidth();
			Collections.sort(list);
			numNecessaryBuckets = first.minBoundary.subtract(value)
					.divide(currentColumnWidth, MetricsCalculationsFacade.DEFAULT_CONTEXT).doubleValue();
			numNecessaryBuckets = Math.ceil(numNecessaryBuckets);
			return newNumberBucketList;
		} else {
			for(int i = 0; i < numNecessaryBuckets; i++) {
				BigDecimal newMin = list.getFirst().minBoundary.subtract(width);
				NumberBucket nb = new NumberBucket(newMin, newMin.add(width));
				list.addFirst(nb);
			}
			return new DefinedRangesNumberBucketList(list, width);
		}
	}

	private static DefinedRangesNumberBucketList addBucketsHighEnd(
			LinkedList<NumberBucket> list, int low, int high, BigDecimal width, BigDecimal value) {
		final NumberBucket first = list.getFirst();
		final NumberBucket last = list.getLast();
		// determine number of buckets necessary to adjust the histogram to hold the new value
		double numNecessaryBuckets = value.subtract(last.maxBoundary)
				.divide(width, MetricsCalculationsFacade.DEFAULT_CONTEXT).doubleValue();
		if(numNecessaryBuckets > bucketExpansionLimit) {
			// if it's too enormous throw the exception 
			throw new ArithmeticException("Number is too drastic of an outlier for dynamic histogram.");
		}
		// because range is exclusive on the upper bound, need to add an additional bucket if the value being added falls exactly on the upper end of a boundary
		// otherwise, it will be included in the final added bucket, so ceiling the number required
		numNecessaryBuckets = (numNecessaryBuckets == (int)numNecessaryBuckets) ? numNecessaryBuckets + 1 : Math.ceil(numNecessaryBuckets);
		int numTotalBuckets = (int)numNecessaryBuckets + list.size();
		if(numTotalBuckets >= high) {
			int numDoublesNecessary = (int)(Math.log((double)numTotalBuckets/low)/Math.log(2));
			BigDecimal doubleCoefficient = BigDecimal.valueOf(Math.pow(2, numDoublesNecessary));
			BigDecimal newWidth = width.multiply(doubleCoefficient);
			BigDecimal tempIncludedMaxValue = first.minBoundary.add(BigDecimal.valueOf(low-1).multiply(newWidth));
			DefinedRangesNumberBucketList newNumberBucketList = new DefinedRangesNumberBucketList(first.minBoundary, tempIncludedMaxValue);
			for(int j = 0; j < list.size(); j++) {
				NumberBucket bucketToAddTo = newNumberBucketList.getBucketList().get(j/doubleCoefficient.intValue());
				BigInteger count = list.get(j).count;
				for(int i = 0; i < count.intValue(); i++) { //possible loss of precision
					bucketToAddTo.incrementCount();
				}
			}
			BigDecimal currentColumnWidth = newNumberBucketList.getCurrentColumnWidth();
			list = newNumberBucketList.getBucketList();
			Collections.sort(list);
			// re-asses number of necessary buckets with new histogram
			numNecessaryBuckets = value.subtract(list.getLast().maxBoundary)
					.divide(currentColumnWidth, MetricsCalculationsFacade.DEFAULT_CONTEXT).doubleValue();
			numNecessaryBuckets = (numNecessaryBuckets == (int)numNecessaryBuckets) ? numNecessaryBuckets + 1 : Math.ceil(numNecessaryBuckets);

			for(int i = 0; i < numNecessaryBuckets; i++) {
				BigDecimal newMax = list.getLast().maxBoundary.add(currentColumnWidth);
				NumberBucket nb = new NumberBucket(newMax.subtract(currentColumnWidth), newMax);
				list.addLast(nb);
			}
			return newNumberBucketList;
		} else {
			// create leftover buckets
			for(int i = 0; i < numNecessaryBuckets; i++) {
				BigDecimal newMax = list.getLast().maxBoundary.add(width);
				NumberBucket nb = new NumberBucket(newMax.subtract(width), newMax);
				list.addLast(nb);
			}
			return new DefinedRangesNumberBucketList(list, width);
		}
	}

	private boolean dynamicallyResizeHistogramAndAddValue(BigDecimal value) throws ArithmeticException {
		if(valueTooHigh(value)) {
			DefinedRangesNumberBucketList newNumberBucketList =
					addBucketsHighEnd(getBucketList(), getNumBucketsLow(), getNumBucketsHigh(), getCurrentColumnWidth(), value);
			this.bucketList = newNumberBucketList.getBucketList();
			this.currentColumnWidth = newNumberBucketList.getCurrentColumnWidth();
		} else if(valueTooLow(value)) { 
			DefinedRangesNumberBucketList newNumberBucketList =
					addBucketsLowEnd(getBucketList(), getNumBucketsLow(), getNumBucketsHigh(), getCurrentColumnWidth(), value);
			this.bucketList = newNumberBucketList.getBucketList();
			this.currentColumnWidth = newNumberBucketList.getCurrentColumnWidth();
		} 
		return binarySearchAdd(value);
	}

	private BigDecimal getCurrentColumnWidth() {
		return currentColumnWidth;
	}

	private static LinkedList<NumberBucket> determineHistogram(BigDecimal min, BigDecimal max) {
		int scale = Math.max(min.scale(), max.scale());
		LinkedList<NumberBucket> list = new LinkedList<NumberBucket>();
		BigDecimal roundedMax = null;
		BigDecimal roundedMin = null;
		BigDecimal proposedRange = null;
		BigDecimal range = max.subtract(min);
		if(range.compareTo(BigDecimal.valueOf(50)) >= 0 && range.compareTo(BigDecimal.valueOf(100)) <= 0) {
			roundedMax = roundToNearest0or5(max, true).setScale(scale, BigDecimal.ROUND_CEILING);
			roundedMax = roundToNearest0or5(roundedMax.toBigInteger().add(BigInteger.ONE), true);
			roundedMin = roundToNearest0or5(min, true).setScale(scale, BigDecimal.ROUND_FLOOR);
			roundedMin = roundToNearest0or5(roundedMin.toBigInteger(), true);
			proposedRange = roundToNearest0or5(
					roundedMax.subtract(roundedMin).divide(BigDecimal.valueOf(50)).toBigInteger(),
					false, 1);
		} else if(range.compareTo(BigDecimal.valueOf(100)) > 0) {
			roundedMax = roundToNearest0or5(max, true).setScale(scale, BigDecimal.ROUND_CEILING);
			roundedMax = roundToNearest0or5(roundedMax.toBigInteger().add(BigInteger.ONE), true);
			roundedMin = roundToNearest0or5(min, true).setScale(scale, BigDecimal.ROUND_FLOOR);
			roundedMin = roundToNearest0or5(roundedMin.toBigInteger(), true);
			proposedRange = roundToNearest0or5(
					roundedMax.subtract(roundedMin).divide(BigDecimal.valueOf(50)).toBigInteger(),
					false, 5);
		} else if(range.compareTo(BigDecimal.ONE) <= 0) {
			roundedMax = max.setScale(0, RoundingMode.CEILING);
			roundedMin = min.setScale(0, RoundingMode.FLOOR);
			proposedRange = BigDecimal.valueOf(1/20D); 
		} else if(range.setScale(0, RoundingMode.HALF_UP).compareTo(BigDecimal.TEN) == 0) {
			roundedMax = max.setScale(0, RoundingMode.CEILING);
			roundedMin = min.setScale(0, RoundingMode.FLOOR);
			proposedRange = BigDecimal.valueOf(1);
		} else {
			roundedMax = roundToNearest0or5(max.setScale(scale > 5 ? 5 : scale, BigDecimal.ROUND_CEILING), true);
			roundedMin = roundToNearest0or5(min.setScale(scale > 5 ? 5 : scale, BigDecimal.ROUND_FLOOR), true);
			proposedRange = roundedMax.subtract(roundedMin).divide(BigDecimal.valueOf(50)).setScale(roundedMin.scale(), RoundingMode.HALF_UP);
			proposedRange = roundToNearest0or5(proposedRange, false, proposedRange.doubleValue());
		}

		if(proposedRange.compareTo(BigDecimal.ZERO) == 0) {
			throw new MainTypeRuntimeException("Proposed range zero for number bucket.");
		}

		for(BigDecimal current = roundedMin; current.compareTo(roundedMax) <= 0 ; current = current.add(proposedRange)) {
			list.add(new NumberBucket(current, current.add(proposedRange)));
		}

		return list;
	}

	private static BigDecimal roundToNearest0or5(BigInteger num, boolean canBeZero) {
		return roundToNearest0or5(num, canBeZero, 1);
	}

	private static BigDecimal roundToNearest0or5(BigDecimal num, boolean canBeZero) {
		return roundToNearest0or5(num, canBeZero, 1);
	}

	private static BigDecimal roundToNearest0or5(BigInteger num, boolean canBeZero, double defaultIfZero) {
		BigInteger absNum = num.abs();
		String numString = absNum.toString();
		int n = Character.getNumericValue(numString.charAt(numString.length()-1));
		if(n > 2 && n < 7) {
			numString = numString.substring(0, numString.length()-1) + "5";
		} else if (n < 2) {
			numString = absNum.subtract(BigInteger.valueOf(n)).toString();
		} else {
			numString = absNum.add(BigInteger.valueOf(10-n)).toString();
		}
		BigDecimal bigDecimal = new BigDecimal(numString);
		if(!canBeZero) {
			bigDecimal = (bigDecimal.compareTo(BigDecimal.ZERO) == 0) ? BigDecimal.valueOf(defaultIfZero) : bigDecimal; 
		}
		if(num.compareTo(BigInteger.ZERO) < 0) {
			bigDecimal = bigDecimal.negate();
		}
		return bigDecimal;
	}

	private static BigDecimal roundToNearest0or5(BigDecimal num, boolean canBeZero, double defaultIfZero) {
		if(num.scale() <= 0) {
			return roundToNearest0or5(num.toBigInteger(), canBeZero, 1);
		} else {
			try {
				return roundToNearest0or5(num.toBigIntegerExact(), canBeZero, 1);
			} catch (ArithmeticException ae) {
				String numString = num.toString();
				int n = Character.getNumericValue(numString.charAt(numString.length()-1));
				if(n > 2 && n < 7) {
					numString = numString.substring(0, numString.length()-1) + "5";
				} else if (n <= 2) {
					//numString = num.setScale(num.scale()-1, BigDecimal.ROUND_DOWN).toPlainString();
					numString = numString.substring(0, numString.length()-1) + "0";
				} else {
					BigDecimal withoutLastDigit = new BigDecimal(numString.substring(0, numString.length()-1));
					BigDecimal roundAddition = num.subtract(withoutLastDigit);
					numString = withoutLastDigit.add(roundAddition.setScale(num.scale(), RoundingMode.CEILING)).toPlainString();
				}
				BigDecimal bigDecimal = new BigDecimal(numString);
				if(!canBeZero) {
					bigDecimal = (bigDecimal.compareTo(BigDecimal.ZERO) == 0) ? BigDecimal.valueOf(defaultIfZero) : bigDecimal; 
				}
				return bigDecimal;
			}
		}
	}

	/*@Override
	public boolean putValue(Object object) {
		List<AbstractBucket> bucketList = this.getOrderedBuckets();
		int min = 0;
		int max = bucketList.size() - 1;
		int half = max/2;
		int c = 0;
		while(min <= max) {
			c = bucketList.get(half).belongs(object);
			if(c > 0) {
				min = half + 1;
			} else if(c < 0){
				max = half - 1;
			} else {
				bucketList.get(half).incrementCount();
				return true;
			}
			half = (max - min)/2 + min;
		}
		return false;
	}*/

	@Override
	public List<AbstractBucket> getOrderedBuckets() {
		LinkedList<AbstractBucket> list = new LinkedList<AbstractBucket>();
		for(NumberBucket b : bucketList) {
			list.add(b);
		}
		return list;
	}

	@Override
	public void coalesce() {
		Collections.sort(bucketList);
		DefinedRangesNumberBucketList nbl = new DefinedRangesNumberBucketList(
				bucketList.getFirst().minBoundary, bucketList.getLast().minBoundary);
		nbl.getBucketList().addLast(new NumberBucket(
				nbl.getBucketList().getLast().maxBoundary, nbl.getBucketList().getLast().maxBoundary.add(nbl.currentColumnWidth)));
		//nlogn could maybe do better
		for(int j = 0; j < bucketList.size(); j++) {
			BigInteger count = bucketList.get(j).count;
			BigDecimal value = bucketList.get(j).minBoundary;
			for(int i = 0; i< count.intValue(); i++) { //possible loss of precision
				nbl.binarySearchAdd(value);
			}
		}
		currentColumnWidth = nbl.currentColumnWidth;
		bucketList = nbl.getBucketList();
	}

	@Override
	public boolean putValue(Object object) {
		boolean successfullyAdded = false;
		BigDecimal value = new BigDecimal(object.toString());
		try {
			successfullyAdded = dynamicallyResizeHistogramAndAddValue(value);
		} catch (ArithmeticException e) {
			logger.info(e);
			return false;
		}
		return successfullyAdded;
	}
}
