package com.deleidos.dp.beans;

import java.math.BigDecimal;

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
	private Histogram freqHistogram;

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
	public Histogram getFreqHistogram() {
		return freqHistogram;
	}

	@JsonProperty("freq-histogram")
	public void setFreqHistogram(Histogram freqHistogram) {
		this.freqHistogram = freqHistogram;
	}
}
