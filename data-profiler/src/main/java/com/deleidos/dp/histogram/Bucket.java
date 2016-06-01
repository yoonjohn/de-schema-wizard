package com.deleidos.dp.histogram;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Interface for buckets to be used in a BucketList (histogram).
 * @author leegc
 *
 */
public interface Bucket {
	/**
	 * Determine where the object belongs.
	 * @param object The provided object
	 * @return -1 if the object should go before the bucket.
	 * 1 if the object should go after the bucket.
	 * 0 if the object belongs in the bucket.
	 */
	public int belongs(Object object);
	/**
	 * Increment the count of the bucket.
	 */
	public void incrementCount();
	/**
	 * Get the label that defines the column of the histogram.
	 * @return A string representation of the label.
	 */
	public String getLabel();
}
