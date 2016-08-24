package com.deleidos.dmf.parser;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.ContentHandler;

import com.deleidos.dmf.exception.AnalyticsParsingRuntimeException;
import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.framework.AbstractAnalyticsParser;
import com.deleidos.dmf.framework.TikaProfilerParameters;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.profiler.BinaryProfilerRecord;
import com.deleidos.dp.profiler.api.ProfilerRecord;

/**
 * Profilable parser that handles all binary profiling.  Note, this class simply pushes the bytes of the binary to the profiler.
 * @author leegc
 *
 */
public class BinaryParser extends AbstractAnalyticsParser {
	private static final Logger logger = Logger.getLogger(BinaryParser.class);
	public static final int IMAGE_BUFFER_SIZE = 1000000;
	private String mediaType;
	private String name;
	private boolean binaryParsingEnabled = false;
	private final static String ENABLE_BINARY_PARSING = "ENABLE_BINARY_PARSING";
	

	private final static Map<MediaType, DetailType> types = new HashMap<MediaType, DetailType>();

	@Override
	public void preParse(InputStream inputStream, ContentHandler handler, Metadata metadata,
			TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		String enableBinary;
		if((enableBinary = System.getenv(ENABLE_BINARY_PARSING)) != null) {
			binaryParsingEnabled = ("true".equals(enableBinary) || "1".equals(enableBinary)) ? true : false; 
		}
		mediaType = (metadata.get(Metadata.CONTENT_TYPE) != null) ? metadata.get(Metadata.CONTENT_TYPE) : mediaType;
		if(metadata.get(Metadata.RESOURCE_NAME_KEY) != null) {
			name = metadata.get(Metadata.RESOURCE_NAME_KEY);
		} else if(context.get(File.class) != null) {
			name = context.get(File.class).getName();
		} else if(metadata.get(Metadata.CONTENT_TYPE) != null){
			name = metadata.get(Metadata.CONTENT_TYPE);
		} else {
			name = "binary";
		}
	}

	@Override
	public ProfilerRecord getNextProfilerRecord(InputStream stream, ContentHandler handler, Metadata metadata,
			TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		if(!binaryParsingEnabled) {
			return null;
		}

		try {
			byte[] bytes = new byte[2048];
			ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
			int numBytesRead = stream.read(bytes);
			context.setCharsRead(numBytesRead);
			if(numBytesRead > 0) {
				if(!types.containsKey(mediaType)) {
					throw new AnalyticsParsingRuntimeException("Required media type was not set for binary parser.", this);
				}
				BinaryProfilerRecord binaryRecord = new BinaryProfilerRecord(name, types.get(mediaType), byteBuffer);
				return binaryRecord;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new AnalyticsTikaProfilingException("Error profiling binary object.", e);
		}
	}

	@Override
	public void postParse(ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		super.postParse(handler, metadata, context);
	}

	@Override
	public Set<MediaType> getSupportedTypes(ParseContext context) {
		types.put(MediaType.image("jpeg"), DetailType.IMAGE);
		types.put(MediaType.image("png"), DetailType.IMAGE);
		return types.keySet();
	}

	public String getMediaType() {
		return mediaType;
	}

	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
