package com.deleidos.dp.metrics;

import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetEncoder;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.histogram.ByteBucketList;

public class ByteBucketListTest {
	private Logger logger = Logger.getLogger(ByteBucketListTest.class);
	
	@Test
	public void testAccumulateBucketList() {
		ByteBucketList byteBucketList = new ByteBucketList();
		String someBinary = "herearea;alkhgoiubnrlkjbnowi"
				+ "uhergpbu9y0948yhtljwknb9p8vyxshupb"
				+ "oih398yhopiwjnbp9w8eu5-h98p452ohinouhcdsoag";
		ByteBuffer byteBuffer = ByteBuffer.wrap(someBinary.getBytes());
		byteBucketList.putValue(byteBuffer);
		assertTrue(byteBucketList.getBucketList().size() > 0);
	}

}
