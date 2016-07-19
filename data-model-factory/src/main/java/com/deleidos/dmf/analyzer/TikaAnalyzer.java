package com.deleidos.dmf.analyzer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.deleidos.dmf.analyzer.workflows.AbstractAnalyzerTestWorkflow;
import com.deleidos.dmf.exception.AnalyticsInitializationRuntimeException;
import com.deleidos.dmf.exception.AnalyticsUndetectableTypeException;
import com.deleidos.dmf.exception.AnalyticsUnsupportedParserException;
import com.deleidos.dmf.exception.AnalyzerException;
import com.deleidos.dmf.framework.AnalyticsDefaultDetector;
import com.deleidos.dmf.framework.AnalyticsDefaultParser;
import com.deleidos.dmf.framework.TikaSampleAnalyzerParameters;
import com.deleidos.dmf.framework.TikaSchemaAnalyzerParameters;
import com.deleidos.dmf.handler.AnalyticsProgressTrackingContentHandler;
import com.deleidos.dmf.progressbar.ProgressBar;
import com.deleidos.dmf.progressbar.ProgressState;
import com.deleidos.dmf.progressbar.SampleAnalysisProgressUpdater;
import com.deleidos.dmf.progressbar.SampleSecondPassProgressUpdater;
import com.deleidos.dmf.progressbar.SchemaAnalysisProgressUpdater;
import com.deleidos.dmf.web.SchemaWizardWebSocketUtility;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.h2.H2DataAccessObject;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.deleidos.dp.profiler.SampleProfiler;
import com.deleidos.dp.profiler.SampleSecondPassProfiler;
import com.deleidos.dp.profiler.SchemaProfiler;
import com.deleidos.dp.profiler.api.Profiler;
import com.deleidos.hd.h2.H2Database;

public class TikaAnalyzer implements FileAnalyzer {
	private static String uploadFileDir = null;
	private static final Logger logger = Logger.getLogger(TikaAnalyzer.class);

	public TikaAnalyzer() {
		SchemaWizardWebSocketUtility.getInstance();
		// need to call web socket singleton at initialization time so it is
		// registered before any methods are called
	}

	public static TikaSampleAnalyzerParameters generateSampleParameters(String sampleFilePath, String domainName, String tolerance,
			String sessionId, int sampleNumber, int totalNumberSamples) throws FileNotFoundException {
		String guid = Analyzer.generateUUID();

		File uploadFile = new File(sampleFilePath);
		if(uploadFileDir == null && uploadFile.isFile()) {
			uploadFileDir = uploadFile.getParent();
		}

		InputStream is = null;
		if(uploadFile.exists()) {
			is = new FileInputStream(uploadFile);
		} else {
			is = TikaAnalyzer.class.getResourceAsStream(sampleFilePath);
			if(is == null) {
				throw new AnalyticsInitializationRuntimeException("Upload file not found on classpath.");
			}
		}
		ProgressBar progressBar = 
				new ProgressBar(uploadFile.getName(), sampleNumber, totalNumberSamples, ProgressState.detectStage);

		SampleProfiler sampleProfiler = new SampleProfiler(domainName, Tolerance.fromString(tolerance));

		TikaSampleAnalyzerParameters tikaProfilerParams = new TikaSampleAnalyzerParameters(sampleProfiler
				, new SampleAnalysisProgressUpdater(), uploadFileDir, guid, is, new AnalyticsProgressTrackingContentHandler(), new Metadata());
		tikaProfilerParams.setPersistInH2(true);
		tikaProfilerParams.setDoReverseGeocode(true);
		tikaProfilerParams.setUploadFileDir(uploadFileDir);
		tikaProfilerParams.setProgress(progressBar);
		tikaProfilerParams.setDomainName(domainName);
		tikaProfilerParams.setNumSamplesUploading(totalNumberSamples);
		tikaProfilerParams.setSampleFilePath(sampleFilePath);
		tikaProfilerParams.setSampleNumber(sampleNumber);
		tikaProfilerParams.setTolerance(tolerance);
		tikaProfilerParams.setStream(is);
		tikaProfilerParams.setSessionId(sessionId);
		tikaProfilerParams.setStreamLength(uploadFile.length());
		tikaProfilerParams.set(File.class, uploadFile);
		return tikaProfilerParams;
	}

	public static TikaSchemaAnalyzerParameters generateSchemaParameters(String existingSchemaGuid, 
			String domainName, JSONArray edittedSourceAnalysis, String sessionId) throws DataAccessException {
		logger.info("Existing schema guid is " + existingSchemaGuid);

		List<DataSample> sampleList = new ArrayList<DataSample>();
		for(int i = 0; i < edittedSourceAnalysis.length(); i++) {
			try {
				DataSample sample = SerializationUtility.deserialize(edittedSourceAnalysis.get(i).toString(), DataSample.class);
				sampleList.add(sample);
			} catch (JSONException e) {
				logger.error(e);
				throw new AnalyticsInitializationRuntimeException("Error deserializing into DataSample bean.");
			}
		}

		String guid;
		Schema existingSchema = null;
		if(existingSchemaGuid == null) {
			guid = Analyzer.generateUUID();
		} else {
			existingSchema = H2DataAccessObject.getInstance().getSchemaByGuid(existingSchemaGuid, true);
			if(existingSchema == null) {
				guid = Analyzer.generateUUID();
			} else {
				// TODO need to use existing once update methods are ready
				guid = Analyzer.generateUUID();
				existingSchema.setsGuid(guid);
				//guid = existingSchema.getsGuid();
			}
		}

		SchemaProfiler schemaProfiler = new SchemaProfiler();
		schemaProfiler.initExistingSchema(existingSchema);

		TikaSchemaAnalyzerParameters schemaParameters = new TikaSchemaAnalyzerParameters(
				schemaProfiler, new SchemaAnalysisProgressUpdater(), uploadFileDir, guid, domainName, sampleList);
		schemaParameters.setSessionId(sessionId);
		schemaParameters.setExistingSchema(existingSchema);

		return schemaParameters;
	}
	
	public static TikaSchemaAnalyzerParameters generateSchemaParameters(JSONObject schemaAnalysisData, 
			String domainName, String sessionId) throws DataAccessException {
		Schema schemaFromFrontEnd = null;
		if(!schemaAnalysisData.isNull("existing-schema")) {
			schemaFromFrontEnd = 
					SerializationUtility.deserialize(schemaAnalysisData.getJSONObject("existing-schema"), Schema.class);
			logger.info("Existing schema guid is " + schemaFromFrontEnd.getsGuid());
		}
		
		JSONArray edittedSourceAnalysis = schemaAnalysisData.getJSONArray("data-samples");
		List<DataSample> sampleList = new ArrayList<DataSample>();
		for(int i = 0; i < edittedSourceAnalysis.length(); i++) {
			try {
				DataSample sample = SerializationUtility.deserialize(edittedSourceAnalysis.get(i).toString(), DataSample.class);
				sampleList.add(sample);
			} catch (JSONException e) {
				logger.error(e);
				throw new AnalyticsInitializationRuntimeException("Error deserializing into DataSample bean.");
			}
		}

		String guid = Analyzer.generateUUID();
		
		/*else {
			schemaFromFrontEnd = H2DataAccessObject.getInstance().getSchemaByGuid(schemaFromFrontEnd.getsGuid(), true);
			if(schemaFromFrontEnd == null) {
				guid = Analyzer.generateUUID();
			} else {
				// TODO need to use existing once update methods are ready
				guid = Analyzer.generateUUID();
				schemaFromFrontEnd.setsGuid(guid);
				//guid = existingSchema.getsGuid();
			}
		}*/

		SchemaProfiler schemaProfiler = new SchemaProfiler();
		schemaProfiler.initExistingSchema(schemaFromFrontEnd);

		TikaSchemaAnalyzerParameters schemaParameters = new TikaSchemaAnalyzerParameters(
				schemaProfiler, new SchemaAnalysisProgressUpdater(), uploadFileDir, guid, domainName, sampleList);
		schemaParameters.setSessionId(sessionId);
		schemaParameters.setExistingSchema(schemaFromFrontEnd);

		return schemaParameters;
	}

	public TikaSampleAnalyzerParameters runSampleAnalysis(TikaSampleAnalyzerParameters params)
			throws IOException, AnalyzerException, DataAccessException {
		AnalyticsDefaultDetector detector = new AnalyticsDefaultDetector();
		detector.enableProgressUpdates(params.getSessionId(), params.getProgress());
		AnalyticsDefaultParser parser = new AnalyticsDefaultParser(detector, params);

		Profiler profiler = params.get(Profiler.class);
		SampleProfiler sampleProfiler = null;
		if (profiler instanceof SampleProfiler) {
			sampleProfiler = (SampleProfiler) profiler;
		} else {
			throw new AnalyticsInitializationRuntimeException(
					"Expected a sample profiler, got a " + params.get(Profiler.class).getClass() + ".");
		}

		DataSample dataSampleBean = null;
		params.getMetadata().set(Metadata.CONTENT_LENGTH, String.valueOf(params.getStreamLength()));

		params.getProgress().setCurrentState(ProgressState.detectStage);
		SchemaWizardWebSocketUtility.getInstance().updateProgress(params.getProgress(), params.getSessionId());

		parser.runSampleAnalysis(params);
		params.setRecordsInSample(sampleProfiler.getRecordsParsed());
		logger.info("Setting content as extracted.");

		params.getProgress().setCurrentState(ProgressState.geocodingStage);
		SchemaWizardWebSocketUtility.getInstance().updateProgress(params.getProgress(), params.getSessionId());

		dataSampleBean = sampleProfiler.asBean();
		dataSampleBean.setDsFileType(params.getMetadata().get(Metadata.CONTENT_TYPE));

		if (params.isDoReverseGeocode()) {
			if (sampleProfiler.getNumGeoSpatialQueries() > 0) {
				params.setReverseGeocodingPass(true);
				// second pass will eventually be "conversions" as well as
				// reverse geocoding

				boolean isInterpretationEngineLive = true;
				try {
					isInterpretationEngineLive = InterpretationEngineFacade.getInstance().isLive();
				} catch (DataAccessException e) {
					logger.error(e);
					isInterpretationEngineLive = false;
				}
				if (!isInterpretationEngineLive) {
					logger.error("Interpretation Engine is not connected.  Skipping reverse geocoding step.");
				} else {
					try {
						InputStream secondPassStream = new FileInputStream(params.getSampleFilePath());
						params.setReverseGeocodingCallsEstimate(sampleProfiler.getNumGeoSpatialQueries());

						SampleSecondPassProfiler sampleGeocoder = new SampleSecondPassProfiler();
						SampleSecondPassProgressUpdater sampleGeocodingUpdater = new SampleSecondPassProgressUpdater();

						sampleGeocoder.initializeWithSampleBean(dataSampleBean);

						params.set(Profiler.class, sampleGeocoder);
						params.set(AnalyzerProgressUpdater.class, sampleGeocodingUpdater);
						params.setStream(secondPassStream);
						params.setHandler(new AnalyticsProgressTrackingContentHandler());
						parser.getExtractor().setAreContentsExtracted(false);

						dataSampleBean = parser.runSampleAnalysis(params).getProfilerBean();
					} catch (IOException e) {
						logger.error(e);
						logger.error(
								"There was an error retrieving the file for a second pass.  Returning results from first pass.");
					}
				}
			} else {
				// when there is no reverse geocoding, show a fake smooth progress bar
				params.getProgress().setCurrentState(ProgressState.geocodingStage);
				final long fakeUpdateDelay = 100;
				final int fakeUpdates = 5;
				final float fakeUpdateProgressInterval = (float) params.getProgress().getCurrentState().rangeLength()
						/ (float) fakeUpdates;
				for (int i = 0; i < fakeUpdates; i++) {
					params.getProgress()
					.updateCurrentSampleNumerator(params.getProgress().getCurrentState().getStartValue()
							+ (int) (fakeUpdateProgressInterval * i));
					SchemaWizardWebSocketUtility.getInstance().updateProgress(params.getProgress(),
							params.getSessionId());
					try {
						Thread.sleep(fakeUpdateDelay);
					} catch (InterruptedException e) {
						logger.error("Mock geocoding progress Bar thread interrupted.");
					}
				}
			}
		}

		logger.debug("Sample " + params.getSampleFilePath() + " complete.");
		params.getProgress().setCurrentState(ProgressState.complete);
		SchemaWizardWebSocketUtility.getInstance().updateProgress(params.getProgress(), params.getSessionId());

		for (String key : dataSampleBean.getDsProfile().keySet()) {
			Interpretation interpretation = dataSampleBean.getDsProfile().get(key).getInterpretation();
			if (interpretation != null && !(Interpretation.isUnknown(interpretation))) {
				logger.info("Field \"" + key + "\" interpretted to be " + interpretation.getInterpretation() + ".");
			}
		}

		if (params.isPersistInH2()) {
			String guid = H2DataAccessObject.getInstance().addSample(params.getProfilerBean());

			if (guid == null) {
				if (params.isPersistInH2()) {
					logger.error(
							params.getSampleFilePath() + " profile not successfully persisted in backend database.");
				}
			}
		} else {
			logger.info("Profile set by parameters to not be persisted.");
		}
		return params;
	}

	@Override
	public TikaSchemaAnalyzerParameters runSchemaAnalysis(TikaSchemaAnalyzerParameters params)
			throws IOException, AnalyzerException, DataAccessException {

		AnalyticsDefaultDetector detector = new AnalyticsDefaultDetector();
		detector.enableProgressUpdates(params.getSessionId(), params.getProgress());
		AnalyticsDefaultParser parser = new AnalyticsDefaultParser(detector, params);

		List<DataSample> userModifiedSampleList = params.getUserModifiedSampleList();
		String uploadFileDir = params.getUploadFileDir();

		if(uploadFileDir == null) {
			throw new AnalyticsInitializationRuntimeException("Upload directory is null!");
		}

		Profiler profiler = params.get(Profiler.class);
		SchemaProfiler schemaProfiler = null;
		if (profiler instanceof SchemaProfiler) {
			schemaProfiler = (SchemaProfiler) profiler;
		} else {
			throw new AnalyticsInitializationRuntimeException(
					"Expected a schema profiler, got a " + params.get(Profiler.class).getClass() + ".");
		}
		SchemaAnalysisProgressUpdater schemaProgress = new SchemaAnalysisProgressUpdater();

		int i = 0;
		for (DataSample sample : userModifiedSampleList) {

			Metadata metadata = new Metadata();
			File file = new File(uploadFileDir, sample.getDsFileName());

			if (file != null) {
				params.set(File.class, file);
			}

			schemaProfiler.setCurrentDataSample(sample);

			FileInputStream fis = new FileInputStream(file);
			params.setHandler(new AnalyticsProgressTrackingContentHandler());
			params.setMetadata(metadata);
			params.setStream(fis);
			if(sample.getDsExtractedContentDir() != null) {
				params.setExtractedContentDir(sample.getDsExtractedContentDir());
			}
			params.set(AnalyzerProgressUpdater.class, schemaProgress);
			params.setProgress(new ProgressBar(sample.getDsFileName(), i, userModifiedSampleList.size(), ProgressState.schemaProgress));
			params.getProgress().setCurrentState(ProgressState.schemaProgress);
			SchemaWizardWebSocketUtility.getInstance().updateProgress(params.getProgress(), params.getSessionId());
			params.set(Profiler.class, schemaProfiler);

			parser.runSchemaAnalysis(params);

			params.getProgress().setCurrentState(ProgressState.complete);
			SchemaWizardWebSocketUtility.getInstance().updateProgress(params.getProgress(), params.getSessionId());

			logger.info("Schema analysis successfully completed for " + sample.getDsFileName() + ".");
			i++;
		}
		return params;
	}

	@Override
	public String analyzeSample(String sampleFilePath, String domainName, String tolerance,
			String sessionId, int sampleNumber, int totalNumberSamples) 
					throws IOException, AnalyzerException, DataAccessException {
		TikaSampleAnalyzerParameters params = generateSampleParameters(sampleFilePath, domainName, 
				tolerance, sessionId, sampleNumber, totalNumberSamples);
		try {
			DataSample sampleBean = runSampleAnalysis(params).getProfilerBean();
			if(sampleNumber == totalNumberSamples - 1) {
				ProgressBar matchingProgressBar = new ProgressBar("Sample Upload Completed", 0, 1, ProgressState.matching);
				SchemaWizardWebSocketUtility.getInstance().updateProgress(matchingProgressBar, sessionId);
			}
			
			return sampleBean.getDsGuid();
		} catch (IOException e) {
			logger.error(e);
			return H2Database.IO_ERROR_GUID;
		} catch (DataAccessException e) {
			logger.error(e);
			return H2Database.DATA_ERROR_GUID;
		} catch (AnalyticsUndetectableTypeException e) {
			logger.error(e);
			return H2Database.UNDETECTABLE_SAMPLE_GUID;
		} catch (AnalyticsUnsupportedParserException e) {
			logger.error(e);
			return H2Database.UNSUPPORTED_PARSER_GUID;
		} catch (Exception e) {
			logger.error(e);
			return H2Database.UNDETERMINED_ERROR_GUID;
		}
	}

	@Override
	public JSONObject analyzeSchema(String existingSchemaGuid, String domainName, JSONArray edittedSourceAnalysis, String sessionId) 
			throws IOException, AnalyzerException, DataAccessException {
		TikaSchemaAnalyzerParameters params = 
				generateSchemaParameters(existingSchemaGuid, domainName, edittedSourceAnalysis, sessionId);
		Schema schemaBean = runSchemaAnalysis(params).getProfilerBean();
		String currentVersion = schemaBean.getsVersion();
		if (currentVersion != null) {
			double newVersion = Double.parseDouble(currentVersion);
			newVersion += 0.01;
			String newStringVersion = new DecimalFormat("#0.00").format(newVersion);
			schemaBean.setsVersion(newStringVersion);
		} else {
			schemaBean.setsVersion("1.00");
		}
		return new JSONObject(SerializationUtility.serialize(schemaBean));
	}
	
	@Override
	public JSONObject analyzeSchema(JSONObject schemaAnalysisData, String domainName, String sessionId)
			throws IOException, AnalyzerException, DataAccessException {
		TikaSchemaAnalyzerParameters params = 
				generateSchemaParameters(schemaAnalysisData, domainName, sessionId);
		Schema schemaBean = runSchemaAnalysis(params).getProfilerBean();
		String currentVersion = schemaBean.getsVersion();
		if (currentVersion != null) {
			double newVersion = Double.parseDouble(currentVersion);
			newVersion += 0.01;
			String newStringVersion = new DecimalFormat("#0.00").format(newVersion);
			schemaBean.setsVersion(newStringVersion);
		} else {
			schemaBean.setsVersion("1.00");
		}
		return new JSONObject(SerializationUtility.serialize(schemaBean));
	}

	public static void setUploadFileDir(String uploadFileDir) {
		TikaAnalyzer.uploadFileDir = uploadFileDir;
	}

	public static String getUploadFileDir() {
		return TikaAnalyzer.uploadFileDir;
	}

	public static void main(String[] args)
			throws IOException, AnalyzerException, DataAccessException {
		Scanner scanner = new Scanner(System.in);
		List<String> argList = new ArrayList<String>(args.length);
		List<String> files = new ArrayList<String>();
		for (String a : args) {
			argList.add(a);
		}
		String tolerance = "strict";
		String domainName = "Transportation";
		String interpretationEngine = "";
		try {
			int index = -1;
			if ((index = argList.indexOf("-i")) > -1) {
				interpretationEngine = argList.get(index + 1);
				argList.remove(index + 1);
				argList.remove(index);
			}
			if ((index = argList.indexOf("-d")) > -1) {
				domainName = argList.get(index + 1);
				argList.remove(index + 1);
				argList.remove(index);
			}
			if ((index = argList.indexOf("-t")) > -1) {
				tolerance = argList.get(index + 1);
				argList.remove(index + 1);
				argList.remove(index);
			}
			if (args.length == 0) {
				logger.error("Provide a path(s) to the file(s) that you want to analyze.");
				scanner.close();
				return;
			}
		} catch (Exception e) {
			System.err.println(e);
			return;
		}

		do {
			AbstractAnalyzerTestWorkflow mainWorkflow = new AbstractAnalyzerTestWorkflow() {

				@Override
				public void addNecessaryTestFiles() {
					for (String file : files) {
						addLocalTestFile(file);
					}
				}

				@Override
				public String[] performMockVerificationStep(String[] generatedSampleGuids) {
					List<String> guids = new ArrayList<String>(Arrays.asList(generatedSampleGuids));
					boolean cont;
					do {
						for (int i = 0; i < generatedSampleGuids.length; i++) {
							System.out.println("Sample with guid: " + generatedSampleGuids[i] + " at index: " + i);
						}
						System.out.print(
								"Enter the index of the sample you would like to drop or \"Enter\" to continue: ");
						String s = scanner.nextLine();
						try {
							if (s.isEmpty()) {
								cont = false;
							} else {
								int sampleIndex = Integer.valueOf(s);
								cont = true;
								if (sampleIndex > guids.size()) {
									System.out.println("Index out of bounds.");
									continue;
								}
								guids.remove(sampleIndex);
							}
						} catch (NumberFormatException e) {
							cont = false;
						}
					} while (cont);
					String[] modifiedSampleGuids = new String[guids.size()];
					for (int i = 0; i < guids.size(); i++) {
						modifiedSampleGuids[i] = guids.get(i);
						System.out.println(modifiedSampleGuids[i]);
					}
					return modifiedSampleGuids;
				}

				@Override
				public JSONArray performMockMergeSamplesStep(Schema existingSchema,
						JSONArray retrieveSourceAnalysisResult) {
					System.out.println(retrieveSourceAnalysisResult);
					boolean cont;
					do {
						for (int i = 0; i < retrieveSourceAnalysisResult.length(); i++) {
							System.out.println(
									"Fields of " + retrieveSourceAnalysisResult.getJSONObject(i).getString("dsName")
									+ " at index " + i);
							JSONObject dsProfile = retrieveSourceAnalysisResult.getJSONObject(i)
									.getJSONObject("dsProfile");
							for (String key : dsProfile.keySet()) {
								System.out.print(key + "\t");
							}
							System.out.println();
						}
						System.out.print(
								"Enter the index of the sample in which you would like to perform merges, or press \"Enter\" to continue: ");
						String s = scanner.nextLine();
						try {
							if (s.isEmpty()) {
								cont = false;
							} else {
								int sampleIndex = Integer.valueOf(s);
								cont = true;
								if (sampleIndex > retrieveSourceAnalysisResult.length()) {
									System.out.println("Index out of bounds.");
									continue;
								}

								JSONObject dataSample = retrieveSourceAnalysisResult.getJSONObject(sampleIndex);
								String nonMergedKey;
								boolean exists;
								String mergedFieldKey;
								do {
									System.out.print("Enter the name of the unmerged field: ");
									nonMergedKey = scanner.nextLine();
									exists = (dataSample.getJSONObject("dsProfile").keySet().contains(nonMergedKey))
											? true : false;
								} while (!exists);

								System.out.print("Enter the merged name of field \"" + nonMergedKey + "\": ");
								mergedFieldKey = scanner.nextLine();

								simulateMerge(dataSample, nonMergedKey, mergedFieldKey);
								retrieveSourceAnalysisResult.put(sampleIndex, dataSample);

							}
						} catch (NumberFormatException e) {
							cont = false;
						}
					} while (cont);
					return retrieveSourceAnalysisResult;
				}

				@Override
				public JSONObject performMockSchemaInlineEdittingStep(JSONObject schemaAnalysis) {
					return schemaAnalysis;
				}
			};
			mainWorkflow.setOutput(true);
			mainWorkflow.setTolerance(tolerance);
			mainWorkflow.setDomainName(domainName);
			mainWorkflow.runAnalysis();
			logger.info(mainWorkflow.getSchemaAnalysis());
			System.out.println("Would you like to run another analysis? (y/n)");
			if (scanner.nextLine().equals("y")) {
				List<String> f = new ArrayList<String>();
				do {
					System.out.println("Enter file name: ");
					f.add(scanner.nextLine());
					System.out.println("Another? (y/n) ");
				} while (scanner.nextLine().equals("y"));
				for (int i = 0; i < f.size(); i++) {
					files.set(i, f.get(i));
				}
			} else {
				break;
			}
		} while (files.size() > 0);
		scanner.close();
	}

}
