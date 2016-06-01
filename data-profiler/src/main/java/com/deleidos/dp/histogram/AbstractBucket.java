package com.deleidos.dp.histogram;

import java.io.Serializable;
import java.math.BigInteger;

import org.apache.log4j.Logger;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Abstraction of buckets.  Initialize and increment a counter.
 * @author leegc
 * @param <T>
 *
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class AbstractBucket implements Bucket, Comparable<AbstractBucket> {
	private static Logger logger = Logger.getLogger(AbstractBucket.class);
	protected BigInteger count;
	protected String label;
	
	protected AbstractBucket() {
		count = BigInteger.ZERO;
	}
	
	public AbstractBucket(String definition, BigInteger count) {
		this.label = definition;
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
