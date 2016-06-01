package com.deleidos.dmf.analyzer;

import java.util.HashMap;

import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.profiler.SampleProfiler;
import com.deleidos.dp.profiler.SchemaProfiler;

/**
 * Example implementation of ProfilableParams interface
 * @author leegc
 *
 */
public class URLAnalzerParams extends HashMap<String, Object> implements AnalyzerParameters {
	SampleProfiler sampleProfiler;
	SchemaProfiler schemaProfiler;
	boolean isSample;
	
	public URLAnalzerParams(boolean isSample) {
		this.isSample = isSample;
		if(isSample) {
			sampleProfiler = new SampleProfiler("transportation", Tolerance.STRICT);
		} else {
			schemaProfiler = new SchemaProfiler();
		}
	}
	
	public SampleProfiler getSampleProfiler() {
		return sampleProfiler;
	}

	public void setSampleProfiler(SampleProfiler sampleProfiler) {
		this.sampleProfiler = sampleProfiler;
	}

	public SchemaProfiler getSchemaProfiler() {
		return schemaProfiler;
	}

	public void setSchemaProfiler(SchemaProfiler schemaProfiler) {
		this.schemaProfiler = schemaProfiler;
	}

	public boolean isSample() {
		return isSample;
	}

	public void setSample(boolean isSample) {
		this.isSample = isSample;
	}

	@Override
	public Object getProfilerBean() {
		if(isSample) {
			return sampleProfiler.asBean();
		} else {
			return schemaProfiler.asBean();
		}
	}

}
