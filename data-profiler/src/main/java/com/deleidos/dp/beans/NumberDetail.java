package com.deleidos.dp.beans;

import java.math.BigDecimal;

import com.deleidos.dp.histogram.NumberBucketList;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author leegc
 * @author yoonj1
 *
 */
public class NumberDetail extends Detail {
	private BigDecimal min;
	private BigDecimal max;
	private BigDecimal average;
	private double stdDev;
	private NumberBucketList freqHistogram;

	public BigDecimal getMin() {
		return min;
	}

	public void setMin(BigDecimal min) {
		this.min = min;
	}

	public BigDecimal getMax() {
		return max;
	}

	public void setMax(BigDecimal max) {
		this.max = max;
	}

	public BigDecimal getAverage() {
		return average;
	}

	public void setAverage(BigDecimal average) {
		this.average = average;
	}

	@JsonProperty("std-dev")
	public double getStdDev() {
		return stdDev;
	}

	@JsonProperty("std-dev")
	public void setStdDev(double stdDev) {
		this.stdDev = stdDev;
	}

	@JsonProperty("freq-histogram")
	public NumberBucketList getFreqHistogram() {
		return freqHistogram;
	}

	@JsonProperty("freq-histogram")
	public void setFreqHistogram(NumberBucketList freqHistogram) {
		this.freqHistogram = freqHistogram;
	}
}