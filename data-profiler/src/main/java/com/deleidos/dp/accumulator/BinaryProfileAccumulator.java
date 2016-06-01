package com.deleidos.dp.accumulator;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.BinaryDetail;
import com.deleidos.dp.beans.Detail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.histogram.AbstractBucket;
import com.deleidos.dp.histogram.ByteBucketList;

public class BinaryProfileAccumulator extends AbstractProfileAccumulator {
	private static final Logger logger = Logger.getLogger(BinaryProfileAccumulator.class);
	protected BinaryDetail binaryDetail;
	private MessageDigest messageDigest;
	private BigInteger length;

	public BinaryProfileAccumulator(String key, Object value) {
		super(key, value);
		profile.setMainType(MainType.BINARY.toString());
	}
	
	public BinaryProfileAccumulator(String key) {
		super(key);
		profile.setMainType(MainType.BINARY.toString());
	}
	
	public void setMediaType(String mediaType) {
		getBinaryDetail().setMimeType(mediaType);
		getDetail().setDetailType(mediaType);
		getDetailTypeTracker()[this.determineDetailTypeByMediaType(mediaType).getIndex()]++;
	}
	
	private void initMessageDigest() {
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			logger.error(e);
		}
	}
	
	private DetailType determineDetailTypeByMediaType(String mediaType) {
		if(mediaType.startsWith("image")) {
			return DetailType.IMAGE;
		} else if(mediaType.startsWith("video")) {
			return DetailType.VIDEO_FRAME;
		} else if(mediaType.startsWith("audio")) {
			return DetailType.AUDIO_SEGMENT;
		} else {
			logger.error("Unsupported binary type.");
			return null;
		}
	}

	@Override
	public boolean accumulate(Object value) {
		ByteBuffer bytes = (ByteBuffer)value;
		accumulateHash(bytes);
		accumulateLength(bytes);
		return true;
	}

	private void accumulateLength(ByteBuffer bytes) {
		length = length.add(BigInteger.valueOf(bytes.array().length));
	}

	private void accumulateHash(ByteBuffer bytes) {
		messageDigest.update(bytes.array());
	}

	@Override
	public Profile getState() {
		return profile;
	}

	@Override
	public void setDetail(Detail metrics) {
		this.binaryDetail = (BinaryDetail)metrics;
	}

	@Override
	public boolean initFirstValue(Object value) {
		if(!(value instanceof ByteBuffer)) {
			return false;
		}
		binaryDetail = new BinaryDetail();
		initMessageDigest();
		length = BigInteger.ZERO;
		binaryDetail.setByteHistogram(new ByteBucketList());
		binaryDetail.getByteHistogram().putValue(value);
		return accumulate(value);
	}

	@Override
	public Detail getDetail() {
		return binaryDetail;
	}
	
	public BinaryDetail getBinaryDetail() {
		return binaryDetail;
	}
	
	@Override
	public void finish() {
		super.finish();
		binaryDetail.setLength(length);
		binaryDetail.setHash(String.valueOf(messageDigest.digest()));
		
		double entropy = 0;
		List<AbstractBucket> buckets = binaryDetail.getByteHistogram().getOrderedBuckets();
		for(AbstractBucket bucket : buckets) {
			// all bucket widths are 1, so no need to divide by width
			double p = bucket.getCount().doubleValue()/length.doubleValue();
			if(Double.doubleToRawLongBits(p) > 0) {
				entropy += -p*(Math.log(p)/Math.log(2));
			}
		}
		binaryDetail.setEntropy(entropy);
	}

}
