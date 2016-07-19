package com.deleidos.dp.histogram;

import java.math.BigInteger;

import com.deleidos.dp.beans.Histogram;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Abstraction of buckets.  Initialize and increment a counter.
 * @author leegc
 * @param <T>
 *
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class AbstractBucket implements Bucket, Comparable<AbstractBucket> {
	public static final String EMPTY_STRING_INDICATOR = "(Blank Value)";
	protected BigInteger count;
	
	protected AbstractBucket() {
		count = BigInteger.ZERO;
	}
	
	public AbstractBucket(BigInteger count) {
		this.count = count;
	}

	public BigInteger getCount() {
		return count;
	}
	
	@Override
	public void incrementCount() {
		count = count.add(BigInteger.ONE);
	}
	
}
