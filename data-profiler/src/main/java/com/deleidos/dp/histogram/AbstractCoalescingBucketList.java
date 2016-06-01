package com.deleidos.dp.histogram;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Abstract class to track buckets as a histogram in RAM.  The coalescing histogram model is meant to give a best effort
 * representation without any initial parameters.  This means no minimum, maximum, or number of buckets are defined 
 * when the histogram is created.  The functionality of the histogram, <i>B</i>, depends on a low bound, <i>L</i> and a high bound
 * , <i>H</i> where <i>H</i> = 2*<i>L</i>.  At startup, <i>B</i> will put values into unique buckets until the lower bucket bound
 * is reached.  Once the lower bound for number of buckets is reached, <i>B</i> is redefined in terms of 
 * the values it currently contains.  A set of <i>L</i> buckets should then be created with an equally distributed
 * range for each bucket.  At this point, when values are added, they must be added in a bucket defined by this range.
 * If the value is below the minimum or above the maximum, new buckets of the defined range should be created sequentially until the
 * value "belongs" in the histogram.  This is where <i>H</i> becomes important.  If the size of <i>B</i> reaches <i>H</i>, <i>B</i> 
 * will coalesce its buckets into <i>B'</i> with size <i>L</i>.  Each bucket <i>b'<sub>i</sub></i> in <i>B'</i> is defined by
 * <i>b<sub>2i</sub></i> and <i>b<sub>2i+1</sub></i>, where <i>b<sub>i</sub></i> is the bucket in position <i>i</i>(starting at 0) in <i>B</i>.
 * <i>b'<sub>i</sub></i> is created with the lower bound of <i>b<sub>2i</sub></i> and the upper bound of <i>b<sub>2i+1</sub></i>, 
 * and <i>count(b'<sub>2i</sub>)</i> = <i>count(b<sub>2i</sub>)</i> + <i>count(b<sub>2i+1</sub>)</i>.  
 * <i>B</i> is then redefined as <i>B'</i>.
 * This redefinition will occur any time |<i>B</i>| >= <i>H</i>. 
 * 
 * @author leegc
 *
 */

public abstract class AbstractCoalescingBucketList extends AbstractBucketList {
	
	public AbstractCoalescingBucketList() {	}
	
	/**
	 * Default low boundary for number of buckets, <i>L</i>
	 */
	public static final int DEFAULT_NUM_BUCKETS_LOW = 50;
	@JsonIgnore
	private int numBucketsLow = DEFAULT_NUM_BUCKETS_LOW;
	/**
	 * Default high boundary for number of buckets, <i>H</i>
	 */
	public static final int DEFAULT_NUM_BUCKETS_HIGH = DEFAULT_NUM_BUCKETS_LOW*2;
	@JsonIgnore
	private int numBucketsHigh = DEFAULT_NUM_BUCKETS_HIGH;
	/**
	 * Put a value into the histogram.  This method should handle an addition for whatever "stage" of definition the
	 * histogram is in.
	 * @param object Value to add
	 * @return true if the value is added, false if not
	 */
	public abstract boolean putValue(Object object);
	/**
	 * Coalesce each pair of buckets into one bucket when the size of the histogram is <i>H</i>.  The size of the histogram 
	 * should be <i>L</i> after this transformation.
	 */
	public abstract void coalesce();
	/**
	 * Transform the unique valued histogram into a set of equally distributed ranges.  This method should only be called
	 * once.
	 */
	public abstract void transformToRange();
	@JsonIgnore
	public int getNumBucketsLow() {
		return numBucketsLow;
	}
	/**
	 * Set <i>L</i> and automatically set <i>H</i> = 2*<i>L</i>
	 * @param numBucketsLow
	 */
	public void setNumBucketsLow(int numBucketsLow) {
		this.numBucketsLow = numBucketsLow;
		this.numBucketsHigh = 2*numBucketsLow;
	}
	@JsonIgnore
	public int getNumBucketsHigh() {
		return numBucketsHigh;
	}
}
