package com.deleidos.dmf.analyzer;

import com.deleidos.dmf.exception.AnalyzerException;

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
	 */
	public SampleParameters sampleAnalysis(SampleParameters sampleProfilableParams) throws AnalyzerException;
	
	/**
	 * Analyzes the given parameters in the context of a schema analysis pass.
	 * @param schemaProfilableParams The parameters that this class needs to complete the schema analysis.
	 * @return An instance of AnalyzerParameters whose getProfilerBean() method will return a Schema bean.
	 * @throws AnalyzerException thrown if an exception occurs anywhere in the analyzing process.
	 */
	public SchemaParameters schemaAnalysis(SchemaParameters schemaProfilableParams) throws AnalyzerException;
	
}
