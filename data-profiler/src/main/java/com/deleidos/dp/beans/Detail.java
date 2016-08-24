package com.deleidos.dp.beans;

import java.math.BigDecimal;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.deleidos.dp.histogram.AbstractBucketList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author leegc
 * @author yoonj1
 *
 */
public abstract class Detail {
	private static final Logger logger = Logger.getLogger(Detail.class);
	protected String detailType;
	protected String numDistinctValues;
	protected BigDecimal walkingCount;
	protected BigDecimal walkingSum;
	protected BigDecimal walkingSquareSum;

	public Detail() { }

	@JsonIgnore
	public Optional<Histogram> getHistogramOptional() {
		if(this instanceof NumberDetail) {
			if(this instanceof NumberDetail) {
				NumberDetail nDetail = (NumberDetail)this;
				return Optional.of(nDetail.getFreqHistogram());
			} else {
				return Optional.empty();
			}
		} else if(this instanceof StringDetail) {
			if(this instanceof StringDetail) {
				StringDetail sDetail = (StringDetail)this;
				return Optional.of(sDetail.getTermFreqHistogram());
			} else {
				return Optional.empty();
			}
		} else if(this instanceof BinaryDetail) {
			if(this instanceof BinaryDetail) {
				BinaryDetail bDetail = (BinaryDetail)this;
				return Optional.of(bDetail.getByteHistogram());
			} else {
				return Optional.empty();
			}
		} else {
			logger.error("Not a number, string, or binary detail type!!");
			return null;
		}
	}

	@JsonIgnore
	public void nullifyBucketList() {
		if(this instanceof NumberDetail) {
			NumberDetail nDetail = (NumberDetail)this;
			nDetail.setFreqHistogram(null);
		} else if(this instanceof StringDetail) {
			StringDetail sDetail = (StringDetail)this;
			sDetail.setTermFreqHistogram(null);
		} else if(this instanceof BinaryDetail) {
			BinaryDetail bDetail = (BinaryDetail)this;
			bDetail.setByteHistogram(null);
		}
	}

	@JsonIgnore
	public void setRegionDataIfApplicable(RegionData regionData) {
		getHistogramOptional().ifPresent(x->x.setRegionData(regionData));
	}

	@JsonProperty("detail-type")
	public String getDetailType() {
		return detailType;
	}

	@JsonProperty("detail-type")
	public void setDetailType(String detailType) {
		this.detailType = detailType;
	}

	@JsonProperty("num-distinct-values")
	public String getNumDistinctValues() {
		return numDistinctValues;
	}

	@JsonProperty("num-distinct-values")
	public void setNumDistinctValues(String numDistinctValues) {
		this.numDistinctValues = numDistinctValues;
	}

	public BigDecimal getWalkingCount() {
		return walkingCount;
	}

	public void setWalkingCount(BigDecimal walkingCount) {
		this.walkingCount = walkingCount;
	}

	public BigDecimal getWalkingSum() {
		return walkingSum;
	}

	public void setWalkingSum(BigDecimal walkingSum) {
		this.walkingSum = walkingSum;
	}

	public BigDecimal getWalkingSquareSum() {
		return walkingSquareSum;
	}

	public void setWalkingSquareSum(BigDecimal walkingSquareSum) {
		this.walkingSquareSum = walkingSquareSum;
	}

	@JsonIgnore
	public boolean isNumberDetail() {
		if(this instanceof NumberDetail) {
			return true;
		}
		return false;
	}

	@JsonIgnore
	public boolean isStringDetail() {
		if(this instanceof StringDetail) {
			return true;
		}
		return false;
	}

	@JsonIgnore
	public boolean isBinaryDetail() {
		if(this instanceof BinaryDetail) {
			return true;
		}
		return false;
	}
	
	@JsonIgnore
	public static boolean isNumberDetail(Detail detail) {
		return detail instanceof NumberDetail;
	}

	@JsonIgnore
	public static boolean isStringDetail(Detail detail) {
		return detail instanceof StringDetail;
	}

	@JsonIgnore
	public static boolean isBinaryDetail(Detail detail) {
		return detail instanceof BinaryDetail;
	}
}
