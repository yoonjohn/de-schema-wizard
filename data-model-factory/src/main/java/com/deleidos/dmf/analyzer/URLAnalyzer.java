package com.deleidos.dmf.analyzer;

import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dmf.exception.AnalyzerException;
import com.deleidos.dp.profiler.DefaultProfilerRecord;

/**
 * Example implementation of Profilable interface
 * @author leegc
 *
 */
public class URLAnalyzer implements Analyzer<URLAnalzerParams, URLAnalzerParams> {

	/**
	 * A subclass must be passed appropriate parameters to generate a sample bean.  Here the parameters contain the sample profiler
	 * and a boolean that tells whether or not it is a sample pass.
	 */
	@Override
	public URLAnalzerParams sampleAnalysis(URLAnalzerParams sampleProfilableParams) throws AnalyzerException {
		URLAnalyzerRecord urlRecord = new URLAnalyzerRecord();
		urlRecord.add("http://www.google.com");
		urlRecord.add("http://www.leidos.com");
		sampleProfilableParams.getSampleProfiler().load(urlRecord);
		
		DefaultProfilerRecord record = new DefaultProfilerRecord();
		record.put("URL", "http://www.google.com");

		DefaultProfilerRecord record2 = new DefaultProfilerRecord();
		record2.put("URL", "http://www.leidos.com");
		
		sampleProfilableParams.getSampleProfiler().load(record);
		sampleProfilableParams.getSampleProfiler().load(record2);
		
		return sampleProfilableParams;
	}

	@Override
	public URLAnalzerParams schemaAnalysis(URLAnalzerParams schemaProfilableParams) throws AnalyzerException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Example of how encapsulated the profiling becomes with the interfaces.
	 * @param a
	 * @throws AnalyzerException
	 */
	public static void main(String[] a) throws AnalyzerException {
		System.out.println(SerializationUtility.serialize(new URLAnalyzer().sampleAnalysis(new URLAnalzerParams(true)).getProfilerBean()));
	}
}
