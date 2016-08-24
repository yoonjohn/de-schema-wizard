package com.deleidos.dmf.analyzer;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.deleidos.dmf.accessor.ServiceLayerAccessor;
import com.deleidos.dmf.analyzer.workflows.AbstractAnalyzerTestWorkflow;
import com.deleidos.dmf.exception.AnalyticsUndetectableTypeException;
import com.deleidos.dmf.exception.AnalyticsUnsupportedParserException;
import com.deleidos.dmf.exception.AnalyzerException;
import com.deleidos.dmf.integration.DataModelFactoryIntegrationEnvironment;
import com.deleidos.dmf.workflows.ModifyExistingSchemaWorkflow;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.h2.H2DataAccessObject;

public class ModifyingExistingSchemaAndDeleteFieldsIT extends DataModelFactoryIntegrationEnvironment {
	private static final Logger logger = Logger.getLogger(ModifyExistingSchemaMergeAllIT.class);
	public static AbstractAnalyzerTestWorkflow aat1;
	public static AbstractAnalyzerTestWorkflow aat2;
	
	@BeforeClass
	public static void runAnalysisWorkflow() throws AnalyticsUndetectableTypeException, AnalyticsUnsupportedParserException, IOException, AnalyzerException, DataAccessException {
		aat1 = AbstractAnalyzerTestWorkflow.addOrGetStaticWorkflow(
				new ModifyExistingSchemaWorkflow.ThreeSimpleCSVFilesMergedFieldsWorkflow());
		aat1.setOutput(true);
		aat1.runAnalysis();
		
		Schema existingSchema = H2DataAccessObject.getInstance().getSchemaByGuid(aat1.getGeneratedSchemaGuid(), true);
		aat2 = AbstractAnalyzerTestWorkflow.addOrGetStaticWorkflow(
				new ModifyExistingSchemaWorkflow.OneSimpleCSVFileWithFieldDeleteWorkflow(existingSchema, "a"));
		aat2.setOutput(true);
		aat2.runAnalysis();
	}
	
	@Test
	public void testFieldADeletedFromSchema() {
		ServiceLayerAccessor sla = new ServiceLayerAccessor();
		Response response2 = sla.getSchemaByGuid(aat2.getGeneratedSchemaGuid());
		JSONObject schemaJson2 = new JSONObject(response2.getEntity().toString());
		assertTrue(!schemaJson2.getJSONObject("sProfile").has("a"));
		logger.info("Field \'a\' successfully taken out of schema.");
	}
}
