package com.deleidos.dmf.analyzer.workflows;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.deleidos.dmf.analyzer.TikaAnalyzer;
import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.exception.AnalyticsUndetectableTypeException;
import com.deleidos.dmf.exception.AnalyticsUnsupportedParserException;
import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

/**
 * Abstract class defining most functionality of a "workflow."  A workflow is a mocked up series of front end interactions built for 
 * integration tests.  Subclasses of AbstractAnalyzerTestWorkflow must implement three methods:<br> 
 * <i>addNecessaryFiles()<br></i>
 * <i>performMockVerificationStep() <br></i>
 * <i>performMockMergeSamplesStep() <br></i>
 * <i>performMockSchemaInlineEdittingStep() <br></i>
 * See methods for more details.<br>
 * <br>
 * When subclasses of AbstractAnalyzerTestWorkflow are instantiated, the new instance should be passed into the AbstractAnalyzerTestWorkflow.addOrGetWorkflow()
 * method.  This method ensures that every workflow only runs one time, even if the <i>runAnalysis()</i> method is called.  If the new 
 * instance is not passed to this method, identical workflows could run multiple times.  The intention of this structure is to run the entire
 * workflow only once, but run multiple tests based on the results of the workflow. 
 * 
 * @author leegc
 *
 */
public abstract class AbstractAnalyzerTestWorkflow implements AnalyzerTestWorkFlow {
	public static final Logger logger = Logger.getLogger(AbstractAnalyzerTestWorkflow.class);
	private File uploadDirFile;
	private String uploadDir;
	private String sessionId;
	private TikaAnalyzer analyzer;
	private List<String> resourceNames;
	private List<String> fileNames;
	private List<String> generatedSampleGuids;
	private List<DefinedTestResource> definedTestResources;
	private String generatedSchemaGuid;
	private String domainName = "transportation";
	private String tolerance = "strict";
	private List<JSONObject> singleSourceAnalysis;
	private JSONArray retrieveSourceAnalysisResult;
	private JSONObject schemaAnalysis;
	private boolean testComplete;
	private boolean output = false;
	public static final String testSessionId = "test-sesssion";

	/**
	 * Static method to add a workflow to the testing suite.  Workflows should be passed into this method when instantiated.  This will
	 * ensure that they are only run once.
	 * @param workflow The instance of the workflow.
	 * @return Either the same instance, or the instance of the same class that has already been instantiated.
	 */
	public static AbstractAnalyzerTestWorkflow addOrGetStaticWorkflow(AbstractAnalyzerTestWorkflow workflow) {
		return AnalyzerTestWorkFlow.addOrGetWorkflow(workflow);
	}

	public void init() {
		testComplete = false;
		uploadDirFile = new File("./target", "test-uploads-dir");
		uploadDir = uploadDirFile.getPath();
		logger.info("Mock upload directory: " + uploadDir);	
		analyzer = new TikaAnalyzer();
		TikaAnalyzer.setUploadFileDir(uploadDir);
		sessionId = testSessionId;
		resourceNames = new ArrayList<String>();
		fileNames = new ArrayList<String>();
		generatedSampleGuids = new ArrayList<String>();
		singleSourceAnalysis = new ArrayList<JSONObject>();
		definedTestResources = new ArrayList<DefinedTestResource>();
		addNecessaryTestFiles();
	}

	protected AbstractAnalyzerTestWorkflow() {

	}

	/**
	 * Add desired files to the workflow.  This step is identical to selecting files for upload on the frontend.  Use <i>addResourcesTestFile</i>
	 * to add resources included in the classpath or <i>addLocalTestFile</i> to add resources from the local file system.
	 */
	public abstract void addNecessaryTestFiles();

	protected void addResourceTestFile(String resourceName) {
		resourceNames.add(resourceName);
	}

	protected void addLocalTestFile(String fileName) {
		fileNames.add(fileName);
	}

	private void giveSources() throws IOException, AnalyticsUndetectableTypeException, AnalyticsUnsupportedParserException {
		for(int i = 0; i < resourceNames.size(); i++) {
			String file;
			InputStream stream;
			file = resourceNames.get(i);
			stream = getClass().getResourceAsStream(file);

			File resourceCopy = new File(uploadDirFile, file);
			FileUtils.copyInputStreamToFile(stream, resourceCopy);

			definedTestResources.add(new DefinedTestResource(resourceCopy.getPath(), null, null, new FileInputStream(resourceCopy), true, true));
		}
		for(int i = 0; i < fileNames.size(); i++) {
			String file;
			file = fileNames.get(i);
			File existingFile = new File(file);
			File fileCopy = new File(uploadDirFile, existingFile.getName());

			FileUtils.copyFile(existingFile, fileCopy);

			definedTestResources.add(new DefinedTestResource(fileCopy.getPath(), null, null, new FileInputStream(fileCopy), true, true));
		}

		if(output) {
			logger.info("Added " + definedTestResources.size() + " test files.");
		}
		
		for(int i = 0; i < definedTestResources.size(); i++) {
			DefinedTestResource dtr = definedTestResources.get(i);
			String guid = analyzer.giveSource(dtr.getFilePath(), domainName, tolerance, getSessionId(), i, definedTestResources.size());
			generatedSampleGuids.add(guid);
		}
	}

	private void retrieveSingleSourceAnalysis() {
		for(int i = 0; i < generatedSampleGuids.size(); i++) {
			JSONObject singleAnalysis = analyzer.retrieveSourceAnalysis(generatedSampleGuids.get(i));
			singleSourceAnalysis.add(singleAnalysis);
		}
		if(output) {
			logger.info("Retrieved each source analysis individually.");
		}
	}

	/**
	 * This step represents the sample verification step of the schema wizard workflow.  The string array returned will be the sample guids
	 * analyzed.
	 * @param generatedSampleGuids The list of all sample guids from the preceding "upload"
	 * @return the editted String array, or null if this step is unimportant for the workflow
	 */
	public abstract String[] performMockVerificationStep(String[] generatedSampleGuids);

	private void retrieveMultipleSourceAnalysis() {
		String[] sampleGuids = performMockVerificationStep(generatedSampleGuids.toArray(new String[generatedSampleGuids.size()]).clone());
		if(sampleGuids == null) {
			testComplete = true;
			return;
		} else {
			retrieveSourceAnalysisResult = analyzer.retrieveSourceAnalysis(sampleGuids);
			if(output) {
				logger.info("Retrieved source group analysis.");
			}
		}
	}	

	/**
	 * This step represents the user interaction that defines merges, seeds, and dropped values.  JSON objects must be manually manipulated
	 * and returned as a JSONArray.
	 * @param retrieveSourceAnalysisResult The array of Sample JSONObjects that is returned from TikaAnalyzer.retrieveSourceAnalysis()
	 * @return the editted DataSample JSONArray, or null if this step is unimportant for the workflow
	 */
	public abstract JSONArray performMockMergeSamplesStep(JSONArray retrieveSourceAnalysisResult);

	private void retrieveSchemaAnalysis() throws AnalyticsUndetectableTypeException, AnalyticsUnsupportedParserException, AnalyticsTikaProfilingException {
		JSONArray mockedUpFrontendAdjustedSourceAnalysis = performMockMergeSamplesStep(new JSONArray(retrieveSourceAnalysisResult.toString()));
		if(mockedUpFrontendAdjustedSourceAnalysis == null) {
			testComplete = true;
			return;
		} else { 
			schemaAnalysis = analyzer.retrieveSchemaAnalysis(mockedUpFrontendAdjustedSourceAnalysis, getSessionId());
			if(output) {
				logger.info("Retrieved schema analysis.");
			}
		}
	}

	/**
	 * This step represents the schema editted step of the schema wizard workflow.  The Schema object must be manually editted and returned 
	 * in subclasses that implement this method.
	 * @param schemaAnalysis The schema analysis that is returned from TikaAnalyzer.retrieveSchemaAnalysis() 
	 * @return the editted Schema JSONObject, or null if this step is unimportant for the workflow
	 */
	public abstract JSONObject performMockSchemaInlineEdittingStep(JSONObject schemaAnalysis);

	private void giveSchema() {
		JSONObject mockedUpSchemaJSON = performMockSchemaInlineEdittingStep(new JSONObject(schemaAnalysis.toString()));
		if(mockedUpSchemaJSON == null) {
			testComplete = true;
			return;
		} else {
			generatedSchemaGuid = analyzer.giveSchema(mockedUpSchemaJSON);
			if(output) {
				logger.info("Gave schema.");
			}
		}
	}

	public void runAnalysis() {
		try {
			if(testComplete) {
				return;
			} else {
				init();
			}
			try {
				giveSources();
				generatedSampleGuids.forEach(x->logger.debug(x));
			} catch (IOException e) {
				logger.error(e);
				Arrays.asList(e.getStackTrace()).forEach(x->logger.error(x));
				logger.error("IOException giving sources.");
			}

			retrieveSingleSourceAnalysis();
			singleSourceAnalysis.forEach(x->logger.debug(x));
			retrieveMultipleSourceAnalysis();
			if(retrieveSourceAnalysisResult != null) {
				for(int i = 0; i < getRetrieveSourceAnalysisResult().length(); i++) {
					logger.debug(getRetrieveSourceAnalysisResult().get(i));
				}
			}


			if(testComplete) {
				if(output) {
					logger.warn("Stopping " + getClass().getName() + " at retrieveMultipleSourceAnalysis() (null parameter passed during test).");
				}
				return;
			} else {
				retrieveSchemaAnalysis();
				logger.debug(schemaAnalysis);
			}


			if(testComplete) {
				if(output) {
					logger.warn("Stopping " + getClass().getName() + " at retrieveSchemaAnalysis() (null parameter passed during test).");
				}
				return;
			} else {
				giveSchema();
			}



			if(testComplete) {
				if(output) {
					logger.warn("Stopping " + getClass().getName() + " at giveSchema() (null parameter passed during test).");
				}
				return;
			} 
			logger.info("File to schema process finished.");

			logger.info(this.getClass().getName() + " workflow finished.");

			testComplete = true;
		} catch (Exception e) {
			logger.error(e);
			logger.error("Exception in workflow.  Presumably not the fault of the test.");
		}
	}

	public JSONObject simulateMerge(JSONObject sampleObject1, String nonMergedKey, String mergedFieldKey) {
		JSONObject dsProfile = sampleObject1.getJSONObject("dsProfile");
		JSONObject oldProfile = new JSONObject(dsProfile.getJSONObject(nonMergedKey).toString());
		if(nonMergedKey.equals(mergedFieldKey)) {
			oldProfile.put("merged-into-schema", true);
		} else {
			oldProfile.put("original-name", nonMergedKey);
			oldProfile.put("merged-into-schema", true);
		}
		dsProfile.put(mergedFieldKey, oldProfile);
		Object rKey = dsProfile.remove(nonMergedKey);
		if(rKey == null) {
			logger.error("Error removing key.");
		}
		sampleObject1.put("dsProfile", dsProfile);
		return sampleObject1;
	}

	public List<String> getGeneratedSampleGuids() {
		return generatedSampleGuids;
	}

	public void setGeneratedSampleGuids(List<String> generatedSampleGuids) {
		this.generatedSampleGuids = generatedSampleGuids;
	}

	public String getGeneratedSchemaGuid() {
		return generatedSchemaGuid;
	}

	public void setGeneratedSchemaGuid(String generatedSchemaGuid) {
		this.generatedSchemaGuid = generatedSchemaGuid;
	}

	public List<JSONObject> getSingleSourceAnalysis() {
		return singleSourceAnalysis;
	}

	public void setSingleSourceAnalysis(List<JSONObject> singleSourceAnalysis) {
		this.singleSourceAnalysis = singleSourceAnalysis;
	}

	public JSONArray getRetrieveSourceAnalysisResult() {
		return retrieveSourceAnalysisResult;
	}

	public void setRetrieveSourceAnalysisResult(JSONArray retrieveSourceAnalysisResult) {
		this.retrieveSourceAnalysisResult = retrieveSourceAnalysisResult;
	}

	public JSONObject getSchemaAnalysis() {
		return schemaAnalysis;
	}

	public void setSchemaAnalysis(JSONObject schemaAnalysis) {
		this.schemaAnalysis = schemaAnalysis;
	}

	public boolean isOutput() {
		return output;
	}

	public void setOutput(boolean output) {
		this.output = output;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

}
