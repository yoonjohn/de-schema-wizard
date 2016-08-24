package com.deleidos.dp.accumulator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.BinaryDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.histogram.AbstractBucket;
import com.deleidos.dp.histogram.ByteBucketList;

public class BinaryProfileAccumulator extends AbstractProfileAccumulator {
	private static final Logger logger = Logger.getLogger(BinaryProfileAccumulator.class);
	private MessageDigest messageDigest;
	protected ByteBucketList byteHistogram;

	protected BinaryProfileAccumulator(String key) {
		super(key, MainType.BINARY);
	}
	
	@Override
	protected void initializeDetailFields(String knownDetailType, Stage resultingStage) {
		setBinaryDetail(new BinaryDetail());
		getBinaryDetail().setWalkingCount(BigDecimal.ONE);
		if(knownDetailType != null) {
			getBinaryDetail().setDetailType(knownDetailType);
		}
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			logger.error(e);
		}
		getBinaryDetail().setLength(BigInteger.ZERO);
	}

	private void accumulateLength(ByteBuffer bytes) {
		getBinaryDetail().setLength(getBinaryDetail().getLength().add(BigInteger.valueOf(bytes.array().length)));
	}

	private void accumulateHash(ByteBuffer bytes) {
		messageDigest.update(bytes.array());
	}

	private void accumulateByteHistogram(ByteBuffer bytes) {
		byteHistogram.putValue(bytes);
	}
	
	/*private void accumulateDistinctBytes(ByteBuffer bytes) {
		for(int i = 0; i < bytes.array().length; i++) {
			Object b = bytes.get(i);
			if(!distinctValues.contains(b)) {
				distinctValues.add(b);
			}
		}
	}*/

	@Override
	public Profile getState() {
		return profile;
	}

	protected void setBinaryDetail(BinaryDetail detail) {
		profile.setDetail(detail);
	}

	public BinaryDetail getBinaryDetail() {
		return Profile.getBinaryDetail(profile);
	}

	@Override
	protected AbstractProfileAccumulator initializeForSecondPassAccumulation(Profile profile) {
		this.setBinaryDetail(Profile.getBinaryDetail(profile));
		byteHistogram = new ByteBucketList();
		return this;
	}


	protected AbstractProfileAccumulator initializeFirstValue(Stage stage, Object value) throws MainTypeException {
		if(!(value instanceof ByteBuffer)) {
			throw new MainTypeException("Incorrect binary data type.");
		}
		if(stage.equals(Stage.SCHEMA_AWAITING_FIRST_VALUE)) {
			byteHistogram.putValue(value);
		}
		return this;
	}

	@Override
	protected AbstractProfileAccumulator initializeForSchemaAccumulation(Profile schemaProfile, int recordsInSchema, List<Profile> sampleProfiles) throws MainTypeException {
		byteHistogram = new ByteBucketList();
		return this;
	}

	@Override
	protected Profile accumulate(Stage accumulationStage, Object value) throws MainTypeException {
		switch(accumulationStage) {
		case UNITIALIZED: throw new MainTypeException("Accumulator called but has not been initialized.");
		case SAMPLE_AWAITING_FIRST_VALUE: { 
			break;
		}
		case SCHEMA_AWAITING_FIRST_VALUE: {
			break;
		}
		case SAMPLE_FIRST_PASS: {
			ByteBuffer bytes = (ByteBuffer)value;
			accumulateHash(bytes);
			accumulateLength(bytes);
			break;
		}
		case SAMPLE_SECOND_PASS: {
			ByteBuffer bytes = (ByteBuffer)value;
			accumulateByteHistogram(bytes);
			break;
		}
		case SCHEMA_PASS: {
			ByteBuffer bytes = (ByteBuffer)value;
			accumulateHash(bytes);
			accumulateLength(bytes);
			accumulateByteHistogram(bytes);
			break;
		}
		default: throw new MainTypeException("Accumulator stuck in unknown stage.");
		}
		return profile;
	}

	@Override
	protected Profile finish(Stage accumulationStage) {
		if(this.getState().getPresence() < 0) {
			return profile;
		}

		if(accumulationStage.equals(Stage.SAMPLE_FIRST_PASS) 
				|| accumulationStage.equals(Stage.SCHEMA_PASS)) {
			getBinaryDetail().setHash(String.valueOf(messageDigest.digest()));
		} 
		if(accumulationStage.equals(Stage.SAMPLE_SECOND_PASS) 
				|| accumulationStage.equals(Stage.SCHEMA_PASS)) {
			double entropy = 0;
			List<AbstractBucket> buckets = byteHistogram.getOrderedBuckets();
			for(AbstractBucket bucket : buckets) {
				// all bucket widths are 1, so no need to divide by width
				double p = bucket.getCount().doubleValue()/getBinaryDetail().getLength().doubleValue();
				if(Double.doubleToRawLongBits(p) > 0) {
					entropy += -p*(Math.log(p)/Math.log(2));
				}
			}			
			profile.getDetail().setNumDistinctValues(String.valueOf(byteHistogram.getBucketList().size()));
			getBinaryDetail().setByteHistogram(byteHistogram.asBean());
			getBinaryDetail().setEntropy(entropy);
		}
		return profile;
	}

}
