package com.deleidos.dp.histogram;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class CharacterBucketList extends AbstractBucketList {
	public static final CharacterBucketList EMPTY = new CharacterBucketList();
	Map<Integer, CharacterBucket> bucketMap;
	
	public CharacterBucketList() {
		bucketMap = new HashMap<Integer, CharacterBucket>();
	}

	public boolean putValue(Object object) {
		boolean allAdded = true;
		char[] characterArray = object.toString().toCharArray();
		for(int i = 0; i < characterArray.length; i++) {
			if(!putCharacterValue(characterArray[i])) allAdded = false;
		}
		return allAdded;
	}
	
	public boolean putCharacterValue(char characterValue) {
		if(!simplePutCharacterValue(characterValue)) {
			bucketMap.put(Integer.valueOf(characterValue), new CharacterBucket(characterValue, BigInteger.valueOf(1)));
		}
		return true;
	}
	
	public boolean simplePutCharacterValue(char character) {
		CharacterBucket b = bucketMap.get(Integer.valueOf(character));
		if(b == null) {
			return false;
		} else {
			b.incrementCount();
			bucketMap.put(Integer.valueOf(character), b);
			return true;
		}
	}
	
	@Override
	public String toString() {
		JSONArray jArr = new JSONArray();
		for(Integer i : bucketMap.keySet()) {
			JSONObject obj = new JSONObject();
			//obj.put("definition", bucketList.get(i).getLabel());
			//obj.put("count", bucketList.get(i).getCount());
			obj.put(bucketMap.get(i).getLabel(), bucketMap.get(i).getCount());
			jArr.put(obj);
		}
		return jArr.toString();
	}

	@JsonIgnore
	public Map<Integer, CharacterBucket> getBucketMap() {
		return bucketMap;
	}

	public void setBucketList(Map<Integer, CharacterBucket> bucketMap) {
		this.bucketMap = bucketMap;
	}

	@Override
	public List<AbstractBucket> getOrderedBuckets() {
		LinkedList<AbstractBucket> buckets = new LinkedList<AbstractBucket>();
		for(Integer i : bucketMap.keySet()) {
			buckets.add(bucketMap.get(i));
		}
		Collections.sort(buckets);
		return buckets;
	}
}
