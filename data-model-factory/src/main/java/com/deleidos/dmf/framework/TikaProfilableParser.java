package com.deleidos.dmf.framework;

import java.io.InputStream;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.Parser;
import org.xml.sax.ContentHandler;

import com.deleidos.dmf.analyzer.Analyzer;
import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dp.profiler.api.ProfilerRecord;

public interface TikaProfilableParser extends Parser, Analyzer<TikaSampleProfilableParameters, TikaSchemaProfilableParameters> {

	public ProfilerRecord getNextProfilerRecord(InputStream stream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws AnalyticsTikaProfilingException;

}
