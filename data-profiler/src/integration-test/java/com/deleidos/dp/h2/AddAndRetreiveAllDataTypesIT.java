package com.deleidos.dp.h2;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.deleidos.dp.accumulator.BinaryProfileAccumulator;
import com.deleidos.dp.accumulator.BundleProfileAccumulator;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.DataSampleMetaData;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.integration.DataProfilerIntegrationEnvironment;
import com.deleidos.dp.profiler.BinaryProfilerRecord;
import com.deleidos.dp.profiler.DefaultProfilerRecord;
import com.deleidos.dp.profiler.SampleProfiler;
import com.deleidos.dp.profiler.SchemaProfiler;

public class AddAndRetreiveAllDataTypesIT extends DataProfilerIntegrationEnvironment {
	private static final Logger logger = Logger.getLogger(AddAndRetreiveAllDataTypesIT.class);
	private DataSample sample;
	private Schema schema;
	private String imageResource = "/Hopstarter-Soft-Scraps-Image-JPEG.ico";
	private static String guid1;
	private static String guid2;

	@Before
	public void allTypesTest() throws IOException {
		guid1 = UUID.randomUUID().toString();
		guid2 = UUID.randomUUID().toString();
		SampleProfiler sampleProfiler = new SampleProfiler("Transportation", Tolerance.STRICT);

		DefaultProfilerRecord defaultRecord1 = new DefaultProfilerRecord();
		defaultRecord1.put("a", Arrays.asList(1,2,3));
		defaultRecord1.put("b", Arrays.asList("hi","hello","hey"));

		sampleProfiler.load(defaultRecord1);

		// copy-pasting binary into eclipse doesn't really go too well..

		InputStream inputStream = getClass().getResourceAsStream(imageResource);

		byte[] bytes = new byte[1024];
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		int numRead = 0;
		while((numRead = inputStream.read(bytes)) > -1) {
			BinaryProfilerRecord binaryRecord = new BinaryProfilerRecord(imageResource.substring(1), byteBuffer);
			sampleProfiler.load(binaryRecord);
		}
		((BinaryProfileAccumulator)sampleProfiler
				.getMetricsBundle(imageResource.substring(1)).getState()
				.get(BundleProfileAccumulator.BINARY_METRICS_INDEX)).setMediaType("image/ico");

		sample = sampleProfiler.asBean();
		sample.setDsDescription("test");
		sample.setDsExtractedContentDir(null);
		sample.setDsName("testname");
		sample.setDsFileName("testfilename");
		sample.setDsFileType("testfiletype");
		sample.setDsGuid(guid1);
		sample.setDsVersion("1.0");
		sample.setDsLastUpdate(Timestamp.from(Instant.now()));

		for(String key : sample.getDsProfile().keySet()) {
			sample.getDsProfile().get(key).setUsedInSchema(true);
		}

		SchemaProfiler schemaProfiler = new SchemaProfiler();
		schemaProfiler.setCurrentDataSample(sample);
		schemaProfiler.load(defaultRecord1);

		inputStream = getClass().getResourceAsStream(imageResource);

		bytes = new byte[1024];
		byteBuffer = ByteBuffer.wrap(bytes);
		while((numRead = inputStream.read(bytes)) > -1) {
			BinaryProfilerRecord binaryRecord = new BinaryProfilerRecord(imageResource.substring(1), byteBuffer);
			schemaProfiler.load(binaryRecord);
		}
		schema = schemaProfiler.asBean();
		schema.setsGuid(guid2);
		schema.setsName("test-name");
		schema.setsVersion("1.0");
		schema.setsLastUpdate(Timestamp.from(Instant.now()));
		schema.setsDescription("test description");
		DataSampleMetaData dsmd = new DataSampleMetaData();
		dsmd.setDataSampleId(sample.getDataSampleId());
		dsmd.setDsDescription(sample.getDsDescription());
		dsmd.setDsExtractedContentDir(sample.getDsExtractedContentDir());
		dsmd.setDsVersion(sample.getDsVersion());
		dsmd.setDsName(sample.getDsName());
		dsmd.setDsLastUpdate(sample.getDsLastUpdate());
		dsmd.setDsGuid(sample.getDsGuid());
		dsmd.setDsFileName(sample.getDsFileName());
		dsmd.setDsFileType(sample.getDsFileType());
		schema.setsDataSamples(Arrays.asList(dsmd));

	}

	@Test
	public void testSerialization() {
		try {
			String s = SerializationUtility.serialize(sample);
			DataSample newSample = SerializationUtility.deserialize(s, DataSample.class);
			String sc = SerializationUtility.serialize(schema);
			Schema schema = SerializationUtility.deserialize(sc, Schema.class);
		} catch(Exception e) {
			logger.error(e);
			logger.error("Serialization/Deserialization failed.");
			assertTrue(false);
		}
		logger.info("Sample deserialization successful.");
	}

	@Test
	public void testAddAndGetSampleH2() throws IOException, DataAccessException {
		final int expectedFieldCount = 3;
		String guid = H2DataAccessObject.getInstance().addSample(sample);
		DataSample sample = H2DataAccessObject.getInstance().getSampleByGuid(guid);
		int size = sample.getDsProfile().get(imageResource.substring(1)).getDetail().getHistogramOptional().get().getLabels().size();
		try {
			assertTrue(size == 256);
		} catch (AssertionError e) {
			logger.error("Size was " + size + " when it should have been 256.");
			assertTrue(false);
		}

		boolean sampleProfileSizeAssertion = schema.getsProfile().keySet().size() == expectedFieldCount;
		if(sampleProfileSizeAssertion) {
			logger.info("Sample has correct number of fields.");
		} else {
			logger.error("Sample has " + sample.getDsProfile().keySet().size() + " fields instead of "+expectedFieldCount+".");
		}
		schema.getsDataSamples().get(0).setDataSampleId(sample.getDataSampleId());

		String schemaGuid = H2DataAccessObject.getInstance().addSchema(schema);
		Schema schema = H2DataAccessObject.getInstance().getSchemaByGuid(schemaGuid, true);
		assertTrue(Profile.getBinaryDetail(schema.getsProfile().get(imageResource.substring(1))).getByteHistogram().getLabels().size() == 256);


		String sch = SerializationUtility.serialize(schema);

		boolean schemaProfileSizeAssertion =schema.getsProfile().keySet().size() == 3;
		if(schemaProfileSizeAssertion) {
			logger.info("Schema has correct number of fields.");
		} else {
			logger.error("Schema has " + schema.getsProfile().keySet().size() + " fields instead of "+expectedFieldCount+".");
		}


	}
	
}
