package com.deleidos.dmf.analyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.json.JSONArray;

import com.deleidos.dmf.exception.AnalyzerException;
import com.deleidos.dmf.progressbar.ProgressBarManager;
import com.deleidos.dmf.progressbar.ProgressState;
import com.deleidos.dmf.progressbar.ProgressState.STAGE;
import com.deleidos.dmf.progressbar.SimpleProgressUpdater;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.h2.H2DataAccessObject;
import com.deleidos.hd.h2.H2Database;

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

	default JSONArray matchAnalyzedFields(String sessionId, String schemaGuid, String[] sampleGuids, 
			ProgressBarManager existingProgressBar) throws DataAccessException {
		List<DataSample> samples = H2DataAccessObject.getInstance().getSamplesByGuids(sampleGuids); 
		List<String> names = new ArrayList<String>();
		samples.forEach(x->names.add(x.getDsName()));
		Set<String> failedGuids = H2Database.getFailedAnalysisMapping().keySet();
		Schema schema = (schemaGuid == null) ? null : H2DataAccessObject.getInstance().getSchemaByGuid(schemaGuid, true);
		
		int totalFields = 0;
		for(DataSample ds : samples) {
			if(!failedGuids.contains(ds.getDsGuid())) {
				totalFields += ds.getDsProfile().size();
			}
		}
		if(schema != null) {
			totalFields += schema.getsProfile().size();
		}
		ProgressBarManager progressBar = null;
		if(existingProgressBar == null) {
			progressBar = ProgressBarManager.matchingProgressBar(names);	
		} else {
			progressBar = existingProgressBar;
		}
		
		progressBar.jumpToNthIndexStage(0, STAGE.MATCHING);
		SimpleProgressUpdater progressUpdater = new SimpleProgressUpdater(sessionId, progressBar, totalFields);
		
		samples = MetricsCalculationsFacade.matchFieldsAcrossSamplesAndSchema(
				schema, samples, failedGuids, progressUpdater);
		return new JSONArray(SerializationUtility.serialize(samples));
	}
	/**
	 * Retrieve the analysis of a group of samples.
	 * @param sampleGuids The list of sample guids that should be analyzed.
	 * @return A JSON representation of the automatically detected (but not finalized) schema.  Includes "merged-into" and "used-in-schema" flags.
	 * @throws DataAccessException thrown if the backend has an error accessing necessary data, or a null sample guid is passed
	 */
	default JSONArray matchAnalyzedFields(String sessionId, String schemaGuid, String[] sampleGuids) 
			throws DataAccessException {
		return matchAnalyzedFields(sessionId, schemaGuid, sampleGuids, null);
	}

	/**
	 * Generate a guid for the entity being analyzed.
	 * @return the new guid
	 */
	public static String generateUUID() {
		return UUID.randomUUID().toString();
	}
}
