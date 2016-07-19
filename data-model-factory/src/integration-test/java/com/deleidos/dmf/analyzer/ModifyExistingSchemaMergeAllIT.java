package com.deleidos.dmf.analyzer;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.deleidos.dmf.analyzer.workflows.AbstractAnalyzerTestWorkflow;
import com.deleidos.dmf.exception.AnalyticsUndetectableTypeException;
import com.deleidos.dmf.exception.AnalyticsUnsupportedParserException;
import com.deleidos.dmf.exception.AnalyzerException;
import com.deleidos.dmf.integration.DataModelFactoryIntegrationEnvironment;
import com.deleidos.dmf.workflows.ModifyExistingSchemaWorkflow;
import com.deleidos.dp.beans.DataSampleMetaData;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.exceptions.DataAccessException;

public class ModifyExistingSchemaMergeAllIT extends DataModelFactoryIntegrationEnvironment {
	private static final Logger logger = Logger.getLogger(ModifyExistingSchemaMergeAllIT.class);
	public static AbstractAnalyzerTestWorkflow aat1;
	public static AbstractAnalyzerTestWorkflow aat2;
	
	@BeforeClass
	public static void runAnalysisWorkflow() throws AnalyticsUndetectableTypeException, AnalyticsUnsupportedParserException, IOException, AnalyzerException, DataAccessException {
		aat1 = AbstractAnalyzerTestWorkflow.addOrGetStaticWorkflow(
				new ModifyExistingSchemaWorkflow.ThreeSimpleCSVFilesMergedFieldsWorkflow());
		aat1.setOutput(true);
		aat1.runAnalysis();
		
		aat2 = AbstractAnalyzerTestWorkflow.addOrGetStaticWorkflow(
				new ModifyExistingSchemaWorkflow.OneSimpleCSVFileWorkflow(aat1.getGeneratedSchemaGuid()));
		aat2.setOutput(true);
		aat2.runAnalysis();
	}
	
	@Test
	public void testDomainIsSetInSchema() {
		Schema schema = SerializationUtility.deserialize(aat2.getSchemaAnalysis().toString(), Schema.class); 
		assertTrue(schema.getsDomainName() != null); 
	}
	
	@Test
	public void testValuesAccumulatedCorrectly() {
		NumberDetail numberDetailAAfterSecondSchema = 
				Profile.getNumberDetail(SerializationUtility.deserialize(
						aat2.getSchemaAnalysis().toString(), Schema.class).getsProfile().get("a"));
		NumberDetail d2 = new NumberDetail();
		d2.setWalkingCount(BigDecimal.valueOf(400));
		d2.setAverage(BigDecimal.valueOf(2.5));
		d2.setStdDev(1.118);
		boolean a1 = (d2.getWalkingCount().equals(numberDetailAAfterSecondSchema.getWalkingCount()));
		boolean a2 = (d2.getAverage().equals(numberDetailAAfterSecondSchema.getAverage()));
		boolean a3 = (Math.abs(d2.getStdDev() - numberDetailAAfterSecondSchema.getStdDev()) < .1);
		boolean a4 = numberDetailAAfterSecondSchema.getHistogramOptional().get().getLabels().size() == 4;
		logger.info("Walking count: " + a1);
		logger.info(d2.getWalkingCount() + " -> " + numberDetailAAfterSecondSchema.getWalkingCount());
		logger.info("Average: " + a2);
		logger.info(d2.getAverage() + " -> " + numberDetailAAfterSecondSchema.getAverage());
		logger.info("Std dev: " + a3);
		logger.info(d2.getStdDev() + " -> " + numberDetailAAfterSecondSchema.getStdDev());
		logger.info("Bucket size is 4: " + a4);
		assertTrue(a1 && a2 && a3 && a4);
	}
	
	@Test
	public void testAllSampleGuidsPresentInFinalSchema() {
		List<String> expectedGuids = new ArrayList<String>();
		for(String g : aat1.getGeneratedSampleGuids()) {
			expectedGuids.add(g);
		}
		for(String g : aat2.getGeneratedSampleGuids()) {
			expectedGuids.add(g);
		}
		boolean containsAll = true;
		Schema schema = SerializationUtility.deserialize(aat2.getSchemaAnalysis().toString(), Schema.class); 
		for(DataSampleMetaData dsmd : schema.getsDataSamples()) {
			if(containsAll) {
				if(!expectedGuids.contains(dsmd.getDsGuid())) {
					logger.error("Schema does not contain guid: " + dsmd.getDsGuid() +".");
					containsAll = false;
				}
			}
		}
		assertTrue(containsAll);
	}
}
