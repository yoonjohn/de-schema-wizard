package com.deleidos.dp.accumulators;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.junit.Test;

import com.deleidos.dp.accumulator.BinaryProfileAccumulator;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.exceptions.MainTypeException;

public class BinaryProfileAccumulatorTest {

	@Test
	public void testBinaryAccumulation() throws IOException, MainTypeException {
		String imageResource = "/Hopstarter-Soft-Scraps-Image-JPEG.ico";
		BinaryProfileAccumulator bpa = new BinaryProfileAccumulator(imageResource, "image/ico");
		InputStream inputStream = getClass().getResourceAsStream(imageResource);
		byte[] bytes = new byte[1024];
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		int numRead = 0;
		numRead = inputStream.read(bytes);
		bpa.initFirstValue(byteBuffer);
		while((numRead = inputStream.read(bytes)) > -1) {
			bpa.accumulate(byteBuffer);
		}
		bpa.setMediaType("image/ico");
		bpa.finish();
		assertTrue(bpa.getState().getDetail().getHistogramOptional().get().getLabels().size() > 0);
	}
}
