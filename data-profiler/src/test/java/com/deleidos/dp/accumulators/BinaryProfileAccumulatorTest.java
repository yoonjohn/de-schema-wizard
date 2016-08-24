package com.deleidos.dp.accumulators;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.deleidos.dp.accumulator.BundleProfileAccumulator;
import com.deleidos.dp.beans.BinaryDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.interpretation.IEConfig;
import com.deleidos.dp.interpretation.InterpretationEngine;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.deleidos.dp.profiler.SampleProfiler;

public class BinaryProfileAccumulatorTest {

	@Test
	public void testBinaryAccumulation() throws IOException, MainTypeException, DataAccessException {
		InterpretationEngineFacade.setInstance(IEConfig.BUILTIN_CONFIG);
		String imageResource = "/Hopstarter-Soft-Scraps-Image-JPEG.ico";
		String imageName = "Hopstarter-Soft-Scraps-Image-JPEG";
		InputStream inputStream = getClass().getResourceAsStream(imageResource);
		List<ByteBuffer> bufferedExampleValues = new ArrayList<ByteBuffer>();
		byte[] bytes = new byte[1024];
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		bufferedExampleValues.add(byteBuffer);
		int numRead = 0;
		numRead = inputStream.read(bytes);
		while((numRead = inputStream.read(bytes)) > -1) {
			bufferedExampleValues.add(byteBuffer);
		}
		BinaryDetail binaryDetail = Profile.getBinaryDetail(BundleProfileAccumulator.generateBinaryProfile(imageName, DetailType.IMAGE, bufferedExampleValues));
		assertTrue(MetricsCalculationsFacade.stripNumDistinctValuesChars(binaryDetail.getNumDistinctValues()) > 0);
		assertTrue(binaryDetail.getByteHistogram().getLabels().size() > 0);
	}
}
