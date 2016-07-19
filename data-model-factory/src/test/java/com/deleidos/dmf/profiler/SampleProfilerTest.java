package com.deleidos.dmf.profiler;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.deleidos.dmf.analyzer.TikaAnalyzer;
import com.deleidos.dmf.framework.TikaSampleAnalyzerParameters;
import com.deleidos.dmf.handler.AnalyticsProgressTrackingContentHandler;
import com.deleidos.dmf.loader.ResourceLoader;
import com.deleidos.dmf.progressbar.ProgressBar;
import com.deleidos.dmf.progressbar.ProgressState;
import com.deleidos.dmf.progressbar.SampleAnalysisProgressUpdater;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.h2.H2DataAccessObject;
import com.deleidos.dp.interpretation.IEConfig;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.deleidos.dp.profiler.SampleProfiler;
import com.deleidos.hd.h2.H2TestDatabase;

public class SampleProfilerTest extends ResourceLoader {
	private Logger logger = Logger.getLogger(SampleProfilerTest.class);
	
	@Test
	public void testSampleProfile() throws Exception {
		TikaAnalyzer analyzer = new TikaAnalyzer();
			String file = "/TeamsHalf.csv";
			InputStream inputStream = getClass().getResourceAsStream(file);
			TikaSampleAnalyzerParameters params = new TikaSampleAnalyzerParameters(new SampleProfiler("Default", Tolerance.STRICT),
					new SampleAnalysisProgressUpdater(), new File(file).getParent(), "test-guid", inputStream, new AnalyticsProgressTrackingContentHandler(), new Metadata());
			params.setDoReverseGeocode(false);
			params.setPersistInH2(false);
			params.setProgress(new ProgressBar(file, 0, 1, ProgressState.detectStage));
			params.setStream(inputStream);
			params.setSampleFilePath(file);
			params.setDomainName("transportation");
			params.setTolerance("strict");
			params.setNumSamplesUploading(1);
			params.setSampleNumber(1);
			DataSample sampleFile = 
					analyzer.runSampleAnalysis(params).getProfilerBean();
			Profile fieldGProfile = sampleFile.getDsProfile().get("G"); 
			NumberDetail nm = (NumberDetail)fieldGProfile.getDetail();
			NumberDetail expectedGMetrics = new NumberDetail();
			expectedGMetrics.setAverage(BigDecimal.valueOf(53.42308));
			expectedGMetrics.setDetailType(DetailType.INTEGER.toString());
			expectedGMetrics.setMin(BigDecimal.valueOf(48));
			expectedGMetrics.setMax(BigDecimal.valueOf(60));
			expectedGMetrics.setStdDev(5.281814);
			expectedGMetrics.setNumDistinctValues("13");
			assertTrue(assertBasicNumberMetricEquality(nm, expectedGMetrics));
			logger.info("Basic number metric equality confirmed.");

			inputStream.close();
		
	}

	public boolean assertBasicNumberMetricEquality(NumberDetail nm1, NumberDetail nm2) {
		double acceptableRoundingError = .02;
		boolean isBatchMatch = true;
		if(Math.abs((nm1.getAverage().subtract(nm2.getAverage())).doubleValue()) > acceptableRoundingError) return false;
		if(Math.abs((nm1.getMin().subtract(nm2.getMin()).doubleValue())) > acceptableRoundingError) return false;
		if(Math.abs((nm1.getMax().subtract(nm2.getMax())).doubleValue()) > acceptableRoundingError) return false;
		if(!nm1.getDetailType().equals(nm2.getDetailType())) return false;
		if(!nm1.getNumDistinctValues().equals(nm2.getNumDistinctValues())) return false;
		return isBatchMatch;
	}
}
