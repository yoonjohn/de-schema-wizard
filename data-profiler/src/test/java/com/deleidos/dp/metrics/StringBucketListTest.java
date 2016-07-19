package com.deleidos.dp.metrics;

import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.histogram.AbstractBucket;
import com.deleidos.dp.histogram.CharacterBucketList;
import com.deleidos.dp.histogram.TermBucketList;

public class StringBucketListTest {
	private Logger logger = Logger.getLogger(StringBucketListTest.class);
	@Test
	public void testCharacterBucketAdd() {
		CharacterBucketList cbl = new CharacterBucketList();
		cbl.putValue("åyroihljnflkjdnbljklksjnhfbiunselkjbnslekjrnblkjenbrlkbjnelkjrb");
		logger.debug(cbl);
	}
	
	@Test
	public void testTermBucketAdd() {
		TermBucketList tbl = new TermBucketList();
		for(int i = 0; i < 201; i++) {
			tbl.putValue("user" + String.valueOf(i));
			int totalCount = 0;
			for(AbstractBucket ab : tbl.getBucketList()) {
				totalCount += ab.getCount().intValue();
			}
			boolean a1 = totalCount == i+1;
			if(!a1) {
				logger.error("Incorrect total count: "+ totalCount + " at " + i);
			}
			assertTrue(a1);
		}
		int size = tbl.getBucketList().size();
		logger.info("Term bucket size: " + size);
		logger.debug(tbl);
		int totalCount = 0;
		for(AbstractBucket ab : tbl.getBucketList()) {
			totalCount += ab.getCount().intValue();
		}
		boolean a1 = totalCount == 201;
		if(!a1) {
			logger.error("Incorrect total count: "+ totalCount);
		}
		assertTrue(a1);
	}
}
