/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.objectstorage;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

public class OSGMessageResponse {
  private HttpResponseStatus httpResponseStatus;
  private String httpResponseBody;

  public String getHttpResponseBody() {
    return httpResponseBody;
  }

  public void setHttpResponseBody(String httpResponseBody) {
    this.httpResponseBody = httpResponseBody;
  }

  public HttpResponseStatus getHttpResponseStatus() {
    return httpResponseStatus;
  }

  public void setHttpResponseStatus(HttpResponseStatus httpResponseStatus) {
    this.httpResponseStatus = httpResponseStatus;
  }

  public OSGMessageResponse(HttpResponseStatus httpResponseStatus) {
    this.httpResponseStatus = httpResponseStatus;
    // no response body
  }

  public OSGMessageResponse(HttpResponseStatus httpResponseStatus, String httpResponseBody) {
    this.httpResponseStatus = httpResponseStatus;
    this.httpResponseBody = httpResponseBody;
  }

  public static final OSGMessageResponse Continue = new OSGMessageResponse(HttpResponseStatus.CONTINUE);
  public static final OSGMessageResponse Whitespace = new OSGMessageResponse(null, "");
}
