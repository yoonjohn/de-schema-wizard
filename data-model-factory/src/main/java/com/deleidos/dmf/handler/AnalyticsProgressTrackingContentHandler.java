package com.deleidos.dmf.handler;

import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.WriteOutContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class AnalyticsProgressTrackingContentHandler extends BodyContentHandler {
	private static final Logger logger = Logger.getLogger(AnalyticsProgressTrackingContentHandler.class);
	private final List<String> imageSources = new ArrayList<String>();
	private boolean write = true;
	private int numExpectedCharacters = 0;
	private int numWrittenCharacters = 0;
	
    /**
     * Creates a content handler that passes all XHTML body events to the
     * given underlying content handler.
     *
     * @param handler content handler
     */
    public AnalyticsProgressTrackingContentHandler(ContentHandler handler) {
        super(handler);
        this.write = true;
    }

    /**
     * Creates a content handler that writes XHTML body character events to
     * the given writer.
     *
     * @param writer writer
     */
    public AnalyticsProgressTrackingContentHandler(Writer writer) {
        this(new WriteOutContentHandler(writer));
    }

    /**
     * Creates a content handler that writes XHTML body character events to
     * the given output stream using the default encoding.
     *
     * @param stream output stream
     */
    public AnalyticsProgressTrackingContentHandler(OutputStream stream) {
        this(new WriteOutContentHandler(stream));
    }

    /**
     * Creates a content handler that writes XHTML body character events to
     * an internal string buffer. The contents of the buffer can be retrieved
     * using the {@link #toString()} method.
     * <p>
     * The internal string buffer is bounded at the given number of characters.
     * If this write limit is reached, then a {@link SAXException} is thrown.
     *
     * @since Apache Tika 0.7
     * @param writeLimit maximum number of characters to include in the string,
     *                   or -1 to disable the write limit
     */
    public AnalyticsProgressTrackingContentHandler(int writeLimit) {
        this(new WriteOutContentHandler(writeLimit));
    }

    /**
     * Creates a content handler that writes XHTML body character events to
     * an internal string buffer. The contents of the buffer can be retrieved
     * using the {@link #toString()} method.
     * <p>
     * The internal string buffer is bounded at 100k characters. If this write
     * limit is reached, then a {@link SAXException} is thrown.
     */
    public AnalyticsProgressTrackingContentHandler() {
        this(new WriteOutContentHandler());
    }
    
    public AnalyticsProgressTrackingContentHandler(boolean shouldWrite) {
    	this();
    	setWrite(shouldWrite);
    }
    
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
    	numWrittenCharacters += ch.length;
    	if(write) {
    		super.characters(ch, start, length);
    	}
    }

	public boolean isWrite() {
		return write;
	}

	public void setWrite(boolean write) {
		this.write = write;
	}

	public int getNumExpectedCharacters() {
		return numExpectedCharacters;
	}

	public void setNumExpectedCharacters(int numExpectedCharacters) {
		this.numExpectedCharacters = numExpectedCharacters;
	}

	public int getNumWrittenCharacters() {
		return numWrittenCharacters;
	}

	public void setNumWrittenCharacters(int numWrittenCharacters) {
		this.numWrittenCharacters = numWrittenCharacters;
	}
	
}
