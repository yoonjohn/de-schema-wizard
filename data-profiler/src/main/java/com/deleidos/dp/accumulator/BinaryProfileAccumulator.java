package com.deleidos.dp.accumulator;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.BinaryDetail;
import com.deleidos.dp.beans.Detail;
import com.deleidos.dp.beans.Histogram;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.histogram.AbstractBucket;
import com.deleidos.dp.histogram.ByteBucketList;

public class BinaryProfileAccumulator extends AbstractProfileAccumulator {
	private static final Logger logger = Logger.getLogger(BinaryProfileAccumulator.class);
	protected BinaryDetail binaryDetail;
	private MessageDigest messageDigest;
	private BigInteger length;
	protected ByteBucketList byteHistogram;

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
	public void accumulate(Object value, boolean accumulatePresence) throws MainTypeException {
		super.accumulate(value, accumulatePresence);
		if(value == null) {
			return;
		}
		ByteBuffer bytes = (ByteBuffer)value;
		accumulateHash(bytes);
		accumulateLength(bytes);
		accumulateByteHistogram(bytes);
	}

	private void accumulateLength(ByteBuffer bytes) {
		length = length.add(BigInteger.valueOf(bytes.array().length));
	}

	private void accumulateHash(ByteBuffer bytes) {
		messageDigest.update(bytes.array());
	}

	private void accumulateByteHistogram(ByteBuffer bytes) {
		byteHistogram.putValue(bytes);
	}

	@Override
	public Profile getState() {
		return profile;
	}

	private void setBinaryDetail(Detail detail) {
		this.binaryDetail = (BinaryDetail)detail;
	}

	@Override
	public void initializeFromExistingProfile(Profile profile) {
		super.initializeFromExistingProfile(profile);
		this.setBinaryDetail(Profile.getBinaryDetail(profile));
		profile.getDetail().getHistogramOptional().ifPresent(x->this.byteHistogram = Histogram.toByteBucketList(x));
	}

	@Override
	public boolean initFirstValue(Object value) throws MainTypeException {
		try {
			if(!(value instanceof ByteBuffer)) {
				return false;
			}
			binaryDetail = new BinaryDetail();
			initMessageDigest();
			length = BigInteger.ZERO;
			byteHistogram = new ByteBucketList();
			byteHistogram.putValue(value);
			accumulate(value, true);
		} catch (MainTypeException e) {
			logger.error("Failed to initialized first value.", e);
			return false;
		}
		return true;
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
		if(this.getState().getPresence() < 0) {
			return;
		}
		binaryDetail.setLength(length);
		binaryDetail.setHash(String.valueOf(messageDigest.digest()));

		double entropy = 0;
		binaryDetail.setEntropy(entropy);
		
		binaryDetail.setByteHistogram(byteHistogram.asBean());
	}

}
