
package com.eucalyptus.ws.handlers;

import org.apache.commons.fileupload.RequestContext;

import java.io.InputStream;
import java.io.IOException;

public class POSTRequestContext implements RequestContext {
    private InputStream inputStream;
    private String contentType;
    int contentLength;

    public POSTRequestContext(InputStream inputStream, String contentType, int contentLength) {
        this.inputStream = inputStream;
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    public String getCharacterEncoding() {
        return null;
    }

    public String getContentType() {
        return contentType;
    }

    public int getContentLength() {
        return contentLength;
    }

    public InputStream getInputStream() throws IOException {
        return inputStream;
    }
}