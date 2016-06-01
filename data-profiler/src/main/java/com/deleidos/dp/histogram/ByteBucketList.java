package com.deleidos.dp.histogram;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ByteBucketList extends AbstractBucketList {
	public static final ByteBucketList EMPTY = new ByteBucketList();
	List<ByteBucket> bucketList;
	//Map<Integer, ByteBucket> bucketMap;
	
	public ByteBucketList() {
		//bucketMap = new HashMap<Integer, ByteBucket>();
		bucketList = new ArrayList<ByteBucket>(256);
		for(int i = 0; i < 256; i++) {
			bucketList.add(getUnsignedIntValue((byte)i), new ByteBucket((byte)i, BigInteger.valueOf(1)));
		}
	}

	public boolean putValue(Object object) {
		ByteBuffer bytes = (ByteBuffer)object;
		boolean allAdded = true;
		byte[] byteArray = bytes.array();
		for(int i = 0; i < byteArray.length; i++) {
			if(!putByteValue(byteArray[i])) allAdded = false;
		}
		return allAdded;
	}
	
	public boolean putByteValue(byte byteValue) {
		simplePutByteValue(byteValue);
		return true;
	}
	
	private int getUnsignedIntValue(byte byteValue) {
		return Byte.toUnsignedInt(byteValue);
	}
	
	public boolean simplePutByteValue(byte byteValue) {
		ByteBucket b = bucketList.get(getUnsignedIntValue(byteValue));
		if(b == null) {
			return false;
		} else {
			b.incrementCount();
			bucketList.set(getUnsignedIntValue(byteValue), b);
			return true;
		}
	}
	
	@Override
	public String toString() {
		JSONArray jArr = new JSONArray();
		for(ByteBucket bucket : bucketList) {
			JSONObject obj = new JSONObject();
			//obj.put("definition", bucketList.get(i).getLabel());
			//obj.put("count", bucketList.get(i).getCount());
			obj.put(bucket.getLabel(), bucket.getCount());
			jArr.put(obj);
		}
		return jArr.toString();
	}

	@Override
	public List<AbstractBucket> getOrderedBuckets() {
		LinkedList<AbstractBucket> buckets = new LinkedList<AbstractBucket>();
		for(ByteBucket bucket : bucketList) {
			buckets.add(bucket);
		}
		Collections.sort(buckets);
		return buckets;
	}

	public List<ByteBucket> getBucketList() {
		return bucketList;
	}

	public void setBucketList(List<ByteBucket> bucketList) {
		this.bucketList = bucketList;
	}
}
