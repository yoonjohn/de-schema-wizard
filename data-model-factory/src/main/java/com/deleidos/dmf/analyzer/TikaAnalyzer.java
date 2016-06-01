package com.deleidos.dmf.analyzer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.exception.AnalyticsUndetectableTypeException;
import com.deleidos.dmf.exception.AnalyticsUnsupportedParserException;
import com.deleidos.dmf.framework.AnalyticsDefaultDetector;
import com.deleidos.dmf.framework.AnalyticsDefaultParser;
import com.deleidos.dmf.framework.TikaSampleProfilableParameters;
import com.deleidos.dmf.framework.TikaSchemaProfilableParameters;
import com.deleidos.dmf.handler.AnalyticsProgressTrackingContentHandler;
import com.deleidos.dmf.progressbar.ProgressBar;
import com.deleidos.dmf.progressbar.ProgressState;
import com.deleidos.dmf.progressbar.SampleAnalysisProgressUpdater;
import com.deleidos.dmf.progressbar.SampleGeoCodingProgressUpdater;
import com.deleidos.dmf.progressbar.SchemaAnalysisProgressUpdater;
import com.deleidos.dmf.web.SchemaWizardWebSocketUtility;
import com.deleidos.dmf.worker.H2Worker;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dmf.exception.AnalyzerException;
import com.deleidos.dp.profiler.SampleProfiler;
import com.deleidos.dp.profiler.SampleReverseGeocodingProfiler;
import com.deleidos.dp.profiler.SchemaProfiler;
import com.deleidos.dp.profiler.api.Profiler;
import com.deleidos.dp.reversegeocoding.ReverseGeocodingDataAccessObject;

public class TikaAnalyzer implements FileAnalyzer {
	private static String uploadFileDir = null;	
	private static final Logger logger = Logger.getLogger(TikaAnalyzer.class);

	public TikaAnalyzer() {
		SchemaWizardWebSocketUtility.getInstance(); 
		// need to call web socket singleton at initialization time so it is registered before any methods are called
	}

	public static TikaSampleProfilableParameters getSampleParameters(String sampleFilePath, String domainName, String tolerance,
			String sessionId, int sampleNumber, int totalNumberSamples) throws FileNotFoundException {
		String guid = FileAnalyzer.generateUUID();

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
		//sampleProfiler.setSource(params.getSampleFilePath());
		
		//sampleProfiler.setSourceGuid(guid);

		TikaSampleProfilableParameters tikaProfilerParams = new TikaSampleProfilableParameters(sampleProfiler
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
		return tikaProfilerParams;
	}

	public static TikaSchemaProfilableParameters getSchemaParameters(JSONArray edittedSourceAnalysis, String sessionId) throws AnalyticsTikaProfilingException {

		List<DataSample> sampleList = new ArrayList<DataSample>();
		for(int i = 0; i < edittedSourceAnalysis.length(); i++) {
			try {
				DataSample sample = SerializationUtility.deserialize(edittedSourceAnalysis.get(i).toString(), DataSample.class);
				sampleList.add(sample);
			} catch (JSONException e) {
				logger.error(e);
				throw new AnalyticsTikaProfilingException("Error deserializing into DataSample bean.");
			}
		}

		String guid = FileAnalyzer.generateUUID();

		TikaSchemaProfilableParameters schemaParameters = new TikaSchemaProfilableParameters(
				new SchemaProfiler(), new SchemaAnalysisProgressUpdater(), uploadFileDir, guid, sampleList);
		schemaParameters.setSessionId(sessionId);

		return schemaParameters;
	}

	@Override
	public String giveSource(String sampleFilePath, String domainName, String tolerance,
			String sessionId, int sampleNumber, int totalNumberSamples) 
					throws IOException, AnalyticsUndetectableTypeException, AnalyticsUnsupportedParserException {

		TikaSampleProfilableParameters tikaProfilerParams = getSampleParameters(sampleFilePath, domainName, 
				tolerance, sessionId, sampleNumber, totalNumberSamples);

		try {
			sampleAnalysis(tikaProfilerParams);
		} catch (Exception e) {
			logger.error(e);
			if(e.getCause() instanceof AnalyticsUnsupportedParserException) {
				throw new AnalyticsUnsupportedParserException("Unsupported parser for " + tikaProfilerParams.getSampleFilePath(), e);
			} else if(e.getCause() instanceof IOException) {
				throw new IOException(e);
			} else if(e.getCause() instanceof AnalyticsUndetectableTypeException) {
				throw new AnalyticsUndetectableTypeException("Undetectable type for sample file " +tikaProfilerParams.getSampleFilePath(), e);
			} 
		}

		return tikaProfilerParams.getProfilerBean().getDsGuid();


	}

	@Override
	public JSONObject retrieveSchemaAnalysis(JSONArray edittedSourceAnalysis, String sessionId) throws AnalyticsTikaProfilingException {

		TikaSchemaProfilableParameters schemaParameters = getSchemaParameters(edittedSourceAnalysis, sessionId);

		try {
			Schema schemaBean = schemaAnalysis(schemaParameters).getProfilerBean();
			return new JSONObject(SerializationUtility.serialize(schemaBean));
		} catch (Exception e) {
			logger.error("Error profiling schema for session " + sessionId + ".");
			throw new AnalyticsTikaProfilingException(e);
		}
	}

	public static void setUploadFileDir(String uploadFileDir) {
		TikaAnalyzer.uploadFileDir = uploadFileDir;
	}

	public static String getUploadFileDir() {
		return TikaAnalyzer.uploadFileDir;
	}

	// I would like to call profileSample and profileSchema in the websocket rather than giveSource and retrieveSourceAnalysis
	// directly
	// this means front end must wait for some sort of finished flag (progress bar at 100%) before it calls retrieve analysis
	// this will start a new thread for each analysis - multi client
	// websocket worker will choose the appropriate analyzer based on the parameters from the front end
	@Override
	public TikaSampleProfilableParameters sampleAnalysis(TikaSampleProfilableParameters params) throws AnalyzerException {
		AnalyticsDefaultDetector detector = new AnalyticsDefaultDetector();
		detector.enableProgressUpdates(params.getSessionId(), params.getProgress());
		AnalyticsDefaultParser parser = new AnalyticsDefaultParser(detector, params);

		Profiler profiler = params.get(Profiler.class);
		SampleProfiler sampleProfiler = null;
		if(profiler instanceof SampleProfiler) {
			sampleProfiler = (SampleProfiler) profiler;
		} else {
			throw new AnalyticsInitializationRuntimeException("Expected a sample profiler, got a " + params.get(Profiler.class).getClass() + "." );
		}

		try {
			DataSample dataSampleBean = null;
			params.getMetadata().set(Metadata.CONTENT_LENGTH, String.valueOf(params.getStreamLength()));

			if(params.getSampleFilePath() != null) {
				params.set(File.class, new File(params.getSampleFilePath()));
			}

			params.getProgress().setCurrentState(ProgressState.detectStage);
			SchemaWizardWebSocketUtility.getInstance().updateProgress(params.getProgress(), params.getSessionId());

			parser.sampleAnalysis(params);
			logger.info("Setting content as extracted.");

			params.getProgress().setCurrentState(ProgressState.geocodingStage);
			SchemaWizardWebSocketUtility.getInstance().updateProgress(params.getProgress(), params.getSessionId());

			dataSampleBean = sampleProfiler.asBean();
			dataSampleBean.setDsFileType(params.getMetadata().get(Metadata.CONTENT_TYPE));

			if(params.isDoReverseGeocode()) {
				if(sampleProfiler.getNumGeoSpatialQueries() > 0) { 
					params.setReverseGeocodingPass(true);
					//second pass will eventually be "conversions" as well as reverse geocoding

					if(!ReverseGeocodingDataAccessObject.getInstance().isLive()) {
						logger.warn("Reverse Geocoding service failed to connect.  Skipping reverse geocoding step.");
					} else {

						InputStream secondPassStream = new FileInputStream(params.getSampleFilePath());
						params.setReverseGeocodingCallsEstimate(sampleProfiler.getNumGeoSpatialQueries());

						SampleReverseGeocodingProfiler sampleGeocoder = new SampleReverseGeocodingProfiler();
						SampleGeoCodingProgressUpdater sampleGeocodingUpdater = new SampleGeoCodingProgressUpdater();

						sampleGeocoder.initializeWithSampleBean(dataSampleBean);

						params.set(Profiler.class, sampleGeocoder);
						params.set(AnalyzerProgressUpdater.class, sampleGeocodingUpdater);
						params.setStream(secondPassStream);
						params.setHandler(new AnalyticsProgressTrackingContentHandler());
						parser.getExtractor().setAreContentsExtracted(false);

						dataSampleBean = parser.sampleAnalysis(params).getProfilerBean();
					}
				} else {
					// when there is no reverse geocoding, mock up a fake smooth progress bar
					// what they don't know can't hurt them... :)
					params.getProgress().setCurrentState(ProgressState.geocodingStage);
					final long fakeUpdateDelay = 100;
					final int fakeUpdates = 5;
					final float fakeUpdateProgressInterval = (float)params.getProgress().getCurrentState().rangeLength()/(float)fakeUpdates;
					for(int i = 0; i < fakeUpdates; i++) {
						params.getProgress().updateCurrentSampleNumerator(params.getProgress().getCurrentState().getStartValue() + (int)(fakeUpdateProgressInterval*i));
						SchemaWizardWebSocketUtility.getInstance().updateProgress(params.getProgress(), params.getSessionId());
						try {
							Thread.sleep(fakeUpdateDelay);
						} catch (InterruptedException e) {
							logger.error("Mock geocoding progress Bar thread interrupted.");
						}
					}
				}
			}

			params.getProgress().setCurrentState(ProgressState.complete);
			logger.debug("Sample "+params.getSampleFilePath()+" complete.");
			SchemaWizardWebSocketUtility.getInstance().updateProgress(params.getProgress(), params.getSessionId());

			for(String key : dataSampleBean.getDsProfile().keySet()) {
				Interpretation interpretation = dataSampleBean.getDsProfile().get(key).getInterpretation();
				if(interpretation != null && !(Interpretation.isUnknown(interpretation))) {
					logger.info("Field \"" +key+ "\" interpretted to be " + interpretation.getInterpretation()+".");
				}
			}
		} catch (FileNotFoundException e) {
			logger.error(e);
			logger.error("Wrapping FileNotFoundException in profilable exception.");
			throw new AnalyticsTikaProfilingException(e);
		}

		if(params.isPersistInH2()) {
			String guid = H2Worker.persistDataSample(params.getProfilerBean());

			if(guid == null) {
				if(params.isPersistInH2()) {
					logger.error(params.getSampleFilePath() + " profile not successfully persisted in backend database.");
				}
			}
		} else {
			logger.info("Profile set by parameters to not be persisted.");
		}

		return params;

	}

	@Override
	public TikaSchemaProfilableParameters schemaAnalysis(TikaSchemaProfilableParameters params) throws AnalyzerException {

		AnalyticsDefaultDetector detector = new AnalyticsDefaultDetector();
		detector.enableProgressUpdates(params.getSessionId(), params.getProgress());
		AnalyticsDefaultParser parser = new AnalyticsDefaultParser(detector, params);

		try {
			List<DataSample> userModifiedSampleList = params.getUserModifiedSampleList();
			String uploadFileDir = params.getUploadFileDir(); 

			if(uploadFileDir == null) {
				throw new IOException("Upload directory is null!");
			}

			SchemaProfiler schemaProfiler = new SchemaProfiler();
			SchemaAnalysisProgressUpdater schemaProgress = new SchemaAnalysisProgressUpdater();

			int i = 0;
			for(DataSample sample : userModifiedSampleList) {

				Metadata metadata = new Metadata();
				File file = new File(uploadFileDir, sample.getDsFileName());

				if(file != null) {
					params.set(File.class, file);
				}

				schemaProfiler.setCurrentDataSample(sample);

				FileInputStream fis = new FileInputStream(file);
				try {
					//parser.parse(fis, new AnalyticsContentHandler(), metadata, analyticsContext);
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

					parser.schemaAnalysis(params);

					params.getProgress().setCurrentState(ProgressState.complete);
					SchemaWizardWebSocketUtility.getInstance().updateProgress(params.getProgress(), params.getSessionId());

					logger.info("Schema analysis successfully completed for " + sample.getDsFileName() + ".");
				} catch (RuntimeException e) {
					logger.error(e);
					logger.error("Runtime exception from Tika framework during schema analysis.");
				} finally {
					fis.close();
				}
				i++;
			}
		} catch (IOException e) {
			logger.error(e);
			logger.error("Wrapping FileNotFoundException in profilable exception.");
			throw new AnalyticsTikaProfilingException(e);
		}
		return params;

	}


	// a lot of duplicate functionality here with service layer
	// might as well use service layer for these calls rather than analyzer
	@Override
	public JSONObject retrieveSourceAnalysis(String guid) {
		return H2Worker.getSampleJSON(guid);
	}

	@Override
	public JSONArray retrieveSourceAnalysis(String[] sampleGuids) {
		return H2Worker.analyzeMultipleSamples(sampleGuids);
	}

	@Override
	public String giveSchema(JSONObject schemaJson) {
		return H2Worker.giveSchema(schemaJson);
	}
	
	public static void main(String[] args) throws AnalyticsUndetectableTypeException, AnalyticsUnsupportedParserException {
		Scanner scanner = new Scanner(System.in);
		if(args.length == 0) {
			logger.error("Provide a path(s) to the file(s) that you want to analyze.");
			scanner.close();
			return;
		}
		String[] files = args;

		do {
			AbstractAnalyzerTestWorkflow mainWorkflow = new AbstractAnalyzerTestWorkflow( ){

				@Override
				public void addNecessaryTestFiles() {
					for(String file : files) {
						addLocalTestFile(file);
					}
				}

				@Override
				public String[] performMockVerificationStep(String[] generatedSampleGuids) {
					List<String> guids = new ArrayList<String>(Arrays.asList(generatedSampleGuids));
					boolean cont;
					do {
						for(int i = 0 ; i < generatedSampleGuids.length; i++) {
							System.out.println("Sample with guid: " + generatedSampleGuids[i] + " at index: " + i);
						}
						System.out.print("Enter the index of the sample you would like to drop or \"Enter\" to continue: ");
						String s = scanner.nextLine();
						try {
							if(s.isEmpty()) {
								cont = false;
							} else {
								int sampleIndex = Integer.valueOf(s);
								cont = true;
								if(sampleIndex > guids.size()) {
									System.out.println("Index out of bounds.");
									continue;
								}
								guids.remove(sampleIndex);
							}
						} catch (NumberFormatException e) {
							cont = false;
						}
					} while(cont);
					String[] modifiedSampleGuids = new String[guids.size()];
					for(int i = 0; i < guids.size(); i++) {
						modifiedSampleGuids[i] = guids.get(i);
						System.out.println(modifiedSampleGuids[i]);
					}
					return modifiedSampleGuids;
				}

				@Override
				public JSONArray performMockMergeSamplesStep(JSONArray retrieveSourceAnalysisResult) {
					System.out.println(retrieveSourceAnalysisResult);
					boolean cont;
					do {
						for(int i = 0 ; i < retrieveSourceAnalysisResult.length(); i++) {
							System.out.println("Fields of " + retrieveSourceAnalysisResult.getJSONObject(i).getString("dsName") + " at index " + i);
							JSONObject dsProfile = retrieveSourceAnalysisResult.getJSONObject(i).getJSONObject("dsProfile");
							for(String key : dsProfile.keySet()) {
								System.out.print(key + "\t");
							}
							System.out.println();
						}
						System.out.print("Enter the index of the sample in which you would like to perform merges, or press \"Enter\" to continue: ");
						String s = scanner.nextLine();
						try {
							if(s.isEmpty()) {
								cont = false;
							} else {
								int sampleIndex = Integer.valueOf(s);
								cont = true;
								if(sampleIndex > retrieveSourceAnalysisResult.length()) {
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
									exists = (dataSample.getJSONObject("dsProfile").keySet().contains(nonMergedKey)) ? true : false;
								} while(!exists);

								System.out.print("Enter the merged name of field \"" + nonMergedKey + "\": ");
								mergedFieldKey = scanner.nextLine();

								dataSample = simulateMerge(dataSample, nonMergedKey, mergedFieldKey);
								retrieveSourceAnalysisResult.put(sampleIndex, dataSample);

							}
						} catch (NumberFormatException e) {
							cont = false;
						}
					} while(cont);
					return retrieveSourceAnalysisResult;
				}

				@Override
				public JSONObject performMockSchemaInlineEdittingStep(JSONObject schemaAnalysis) {
					return schemaAnalysis;
				}	
			};
			mainWorkflow.setOutput(true);
			mainWorkflow.runAnalysis();
			logger.info(mainWorkflow.getSchemaAnalysis());
			System.out.println("Would you like to run another analysis? (y/n)");
			if(scanner.nextLine().equals("y")) {
				List<String> f = new ArrayList<String>();
				do {
					System.out.println("Enter file name: ");
					f.add(scanner.nextLine());
					System.out.println("Another? (y/n) "); 
				} while(scanner.nextLine().equals("y"));
				for(int i = 0; i < f.size(); i++) {
					files[i] = f.get(i);
				}
			} else {
				break;
			}
		} while(files.length > 0);
		scanner.close();
	}
}
