/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.axis2.transport.http.server;

import org.apache.axis2.transport.OutTransportInfo;
import org.apache.http.*;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;

import java.io.IOException;
import java.io.OutputStream;

public class AxisHttpResponseImpl implements AxisHttpResponse, OutTransportInfo {

    private final HttpResponse response;
    private final AxisHttpConnection conn;
    private final HttpProcessor httpproc;
    private final HttpContext context;

    private AutoCommitOutputStream outstream;
    private String contentType;

    private volatile boolean commited;

    public AxisHttpResponseImpl(
            final AxisHttpConnection conn,
            final HttpResponse response,
            final HttpProcessor httpproc,
            final HttpContext context) {
        super();
        if (response == null) {
            throw new IllegalArgumentException("HTTP response may not be null");
        }
        if (conn == null) {
            throw new IllegalArgumentException("HTTP connection may not be null");
        }
        if (httpproc == null) {
            throw new IllegalArgumentException("HTTP processor may not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("HTTP context may not be null");
        }
        this.response = response;
        this.conn = conn;
        this.httpproc = httpproc;
        this.context = context;
    }

    private void assertNotCommitted() {
        if (this.commited) {
            throw new IllegalStateException("Response already committed");
        }
    }

    public boolean isCommitted() {
        return this.commited;
    }

    public void commit() throws IOException, HttpException {
        if (this.commited) {
            return;
        }
        this.commited = true;

        this.context.setAttribute(ExecutionContext.HTTP_CONNECTION, this.conn);
        this.context.setAttribute(ExecutionContext.HTTP_RESPONSE, this.response);

        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setChunked(true);
        entity.setContentType(this.contentType);

        Header header = response.getFirstHeader(HTTP.CONTENT_LEN);
        if(header != null) {
            //this is to trick BasicHttpProcessor into not barfing
            String contentLengthAsString = header.getValue();
            entity.setContentLength(Long.parseLong(contentLengthAsString));
            entity.setChunked(false);
            response.removeHeader(header);
        }
        this.response.setEntity(entity);

        this.httpproc.process(this.response, this.context);
        this.conn.sendResponse(this.response);
    }

    public OutputStream getOutputStream() {
        if (this.outstream == null) {
            this.outstream = new AutoCommitOutputStream();
        }
        return this.outstream;
    }

    public void sendError(int sc, final String msg) {
        assertNotCommitted();
        ProtocolVersion ver = this.response.getProtocolVersion();
        this.response.setStatusLine(ver, sc, msg);
    }

    public void sendError(int sc) {
        assertNotCommitted();
        this.response.setStatusCode(sc);
    }

    public void setStatus(int sc) {
        assertNotCommitted();
        this.response.setStatusCode(sc);
    }

    public void setContentType(final String contentType) {
        assertNotCommitted();
        this.contentType = contentType;
    }

    public ProtocolVersion getProtocolVersion() {
        return this.response.getProtocolVersion();
    }

    public void addHeader(final Header header) {
        assertNotCommitted();
        this.response.addHeader(header);
    }

    public void addHeader(final String name, final String value) {
        assertNotCommitted();
        this.response.addHeader(name, value);
    }

    public boolean containsHeader(final String name) {
        return this.response.containsHeader(name);
    }

    public Header[] getAllHeaders() {
        return this.response.getAllHeaders();
    }

    public Header getFirstHeader(final String name) {
        return this.response.getFirstHeader(name);
    }

    public Header[] getHeaders(String name) {
        return this.response.getHeaders(name);
    }

    public Header getLastHeader(final String name) {
        return this.response.getLastHeader(name);
    }

    public HeaderIterator headerIterator() {
        return this.response.headerIterator();
    }

    public HeaderIterator headerIterator(String name) {
        return this.response.headerIterator(name);
    }

    public void removeHeader(final Header header) {
        assertNotCommitted();
        this.response.removeHeader(header);
    }

    public void removeHeaders(final String name) {
        assertNotCommitted();
        this.response.removeHeaders(name);
    }

    public void setHeader(final Header header) {
        assertNotCommitted();
        this.response.setHeader(header);
    }

    public void setHeader(final String name, final String value) {
        assertNotCommitted();
        this.response.setHeader(name, value);
    }

    public void setHeaders(Header[] headers) {
        assertNotCommitted();
        this.response.setHeaders(headers);
    }

    public HttpParams getParams() {
        return this.response.getParams();
    }

    public void setParams(final HttpParams params) {
        this.response.setParams(params);
    }

    class AutoCommitOutputStream extends OutputStream {

        private OutputStream out;

        public AutoCommitOutputStream() {
            super();
        }

        private void ensureCommitted() throws IOException {
            try {
                commit();
            } catch (HttpException ex) {
                throw (IOException) new IOException().initCause(ex);
            }
            if (this.out == null) {
                this.out = conn.getOutputStream();
            }
        }

        public void close() throws IOException {
            ensureCommitted();
            this.out.close();
        }

        public void write(final byte[] b, int off, int len) throws IOException {
            ensureCommitted();
            this.out.write(b, off, len);
        }

        public void write(final byte[] b) throws IOException {
            ensureCommitted();
            this.out.write(b);
        }

        public void write(int b) throws IOException {
            ensureCommitted();
            this.out.write(b);
        }

        public void flush() throws IOException {
            ensureCommitted();
            this.out.flush();
        }

    }

}
