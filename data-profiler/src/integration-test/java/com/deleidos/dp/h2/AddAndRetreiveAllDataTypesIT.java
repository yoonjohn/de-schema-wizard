package com.deleidos.dp.h2;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.integration.DataProfilerIntegrationEnvironment;
import com.deleidos.dp.profiler.BinaryProfilerRecord;
import com.deleidos.dp.profiler.DefaultProfilerRecord;
import com.deleidos.dp.profiler.SampleProfiler;
import com.deleidos.dp.profiler.SchemaProfiler;
import com.deleidos.dp.profiler.api.ProfilerRecord;

public class AddAndRetreiveAllDataTypesIT extends DataProfilerIntegrationEnvironment {
	private static final Logger logger = Logger.getLogger(AddAndRetreiveAllDataTypesIT.class);
	private DataSample sample;
	private Schema schema;
	private String imageResource = "/Hopstarter-Soft-Scraps-Image-JPEG.ico";
	private static String guid1;
	private static String guid2;

	@Before
	public void allTypesTest() throws IOException, MainTypeException, DataAccessException {
		guid1 = UUID.randomUUID().toString();
		guid2 = UUID.randomUUID().toString();
		SampleProfiler sampleProfiler = new SampleProfiler(Tolerance.STRICT);
		
		List<ProfilerRecord> records = new ArrayList<ProfilerRecord>();
		DefaultProfilerRecord defaultRecord1 = new DefaultProfilerRecord();
		defaultRecord1.put("a", Arrays.asList(1,2,3));
		defaultRecord1.put("b", Arrays.asList("hi","hello","hey"));

		InputStream inputStream = getClass().getResourceAsStream(imageResource);
		records.add(defaultRecord1);
		
		byte[] bytes = new byte[1024];
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		int numRead = 0;
		while((numRead = inputStream.read(bytes)) > -1) {
			BinaryProfilerRecord binaryRecord = new BinaryProfilerRecord(imageResource.substring(1), DetailType.IMAGE, byteBuffer);
			records.add(binaryRecord);
		}

		sample = SampleProfiler.generateDataSampleFromProfilerRecords("Transportation", Tolerance.STRICT, records);

		for(String key : sample.getDsProfile().keySet()) {
			sample.getDsProfile().get(key).setUsedInSchema(true);
		}

		Map<String, List<ProfilerRecord>> sampleToRecordsMapping = new HashMap<String, List<ProfilerRecord>>();
		sampleToRecordsMapping.put(sample.getDsGuid(), records);
		schema = SchemaProfiler.generateSchema(Arrays.asList(sample), sampleToRecordsMapping);
		

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
		assertTrue(Profile.getBinaryDetail(sample.getDsProfile().get(imageResource.substring(1))).getByteHistogram().getLabels().size() == 256);

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
