package com.deleidos.dp.beans;

import java.math.BigDecimal;

import com.deleidos.dp.histogram.CharacterBucketList;
import com.deleidos.dp.histogram.TermBucketList;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author leegc
 * @author yoonj1
 *
 */
public class StringDetail extends Detail {
	private int minLength;
	private int maxLength;
	private double averageLength;
	private double stdDevLength;
	private CharacterBucketList charFreqHistogram;
	private TermBucketList termFreqHistogram;

	@JsonProperty("min-length")
	public int getMinLength() {
		return minLength;
	}

	@JsonProperty("min-length")
	public void setMinLength(int minLength) {
		this.minLength = minLength;
	}

	@JsonProperty("max-length")
	public int getMaxLength() {
		return maxLength;
	}

	@JsonProperty("max-length")
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	@JsonProperty("average-length")
	public double getAverageLength() {
		return averageLength;
	}

	@JsonProperty("average-length")
	public void setAverageLength(double averageLength) {
		this.averageLength = averageLength;
	}

	@JsonProperty("std-dev-length")
	public double getStdDevLength() {
		return stdDevLength;
	}

	@JsonProperty("std-dev-length")
	public void setStdDevLength(double stdDevLength) {
		this.stdDevLength = stdDevLength;
	}

	@JsonProperty("char-freq-histogram")
	public CharacterBucketList getCharFreqHistogram() {
		return charFreqHistogram;
	}

	@JsonProperty("char-freq-histogram")
	public void setCharFreqHistogram(CharacterBucketList charFreqHistogram) {
		this.charFreqHistogram = charFreqHistogram;
	}

	@JsonProperty("freq-histogram")
	public TermBucketList getTermFreqHistogram() {
		return termFreqHistogram;
	}

	@JsonProperty("freq-histogram")
	public void setTermFreqHistogram(TermBucketList termFreqHistogram) {
		this.termFreqHistogram = termFreqHistogram;
	}

}
