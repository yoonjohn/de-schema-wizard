package com.deleidos.dmf.analyzer;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.json.JSONArray;

import com.deleidos.dmf.exception.AnalyzerException;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.h2.H2DataAccessObject;

/**
 * Analyzer interface to add new analyzing functionality to Schema Wizard.
 * @author leegc
 *
 * @param <SampleParameters> the parameter type that needs to be passed to complete a sample analysis
 * @param <SchemaParameters> the parameter type that needs to be passed to complete a schema analysis
 */
public interface Analyzer<SampleParameters extends AnalyzerParameters, SchemaParameters extends AnalyzerParameters> {

	/**
	 * Analyzes the given parameters in the context of the sample analysis process.  
	 * @param sampleProfilableParams The parameters that this class needs to complete the sample analysis.
	 * @return An instance of AnalyzerParameters whose getProfilerBean() method will return a DataSample bean.
	 * @throws AnalyzerException thrown if an exception occurs anywhere in the analyzing process.
	 * @throws DataAccessException thrown if the backend has an error accessing necessary data
	 */
	public SampleParameters runSampleAnalysis(SampleParameters sampleProfilableParams) 
			throws IOException, AnalyzerException, DataAccessException;

	/**
	 * Analyzes the given parameters in the context of a schema analysis pass.
	 * @param schemaProfilableParams The parameters that this class needs to complete the schema analysis.
	 * @return An instance of AnalyzerParameters whose getProfilerBean() method will return a Schema bean.
	 * @throws AnalyzerException thrown if an exception occurs anywhere in the analyzing process.
	 * @throws DataAccessException thrown if the backend has an error accessing necessary data
	 */
	public SchemaParameters runSchemaAnalysis(SchemaParameters schemaProfilableParams) 
			throws IOException, AnalyzerException, DataAccessException;


	/**
	 * Retrieve the analysis of a group of samples.
	 * @param sampleGuids The list of sample guids that should be analyzed.
	 * @return A JSON representation of the automatically detected (but not finalized) schema.  Includes "merged-into" and "used-in-schema" flags.
	 * @throws DataAccessException thrown if the backend has an error accessing necessary data, or a null sample guid is passed
	 */
	default JSONArray matchAnalyzedFields(String schemaGuid, String[] sampleGuids) throws DataAccessException {
		List<DataSample> samples = H2DataAccessObject.getInstance().getSamplesByGuids(sampleGuids); 
		Set<String> failedGuids = H2DataAccessObject.getInstance().getH2Database().getFailedAnalysisMapping().keySet();
		Schema schema = (schemaGuid == null) ? null : H2DataAccessObject.getInstance().getSchemaByGuid(schemaGuid, true);
		samples = MetricsCalculationsFacade.matchFieldsAcrossSamplesAndSchema(schema, samples, failedGuids);
		return new JSONArray(SerializationUtility.serialize(samples));
	}

	/**
	 * Generate a guid for the entity being analyzed.
	 * @return the new guid
	 */
	public static String generateUUID() {
		return UUID.randomUUID().toString();
	}
}
