/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.storage.msgs.s3;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class S3ErrorMessage extends BaseMessage {

  protected String message;
  protected String code;
  protected HttpResponseStatus status;
  protected String resourceType;
  protected String resource;
  protected String requestId;
  protected String hostId;
  protected String errorSource;

  public S3ErrorMessage( ) {
  }

  public S3ErrorMessage( String message, String code, HttpResponseStatus status, String resourceType, String resource, String requestId, String hostId ) {
    this.message = message;
    this.code = code;
    this.status = status;
    this.resourceType = resourceType;
    this.resource = resource;
    this.requestId = requestId;
    this.hostId = hostId;
  }

  public HttpResponseStatus getStatus( ) {
    return status;
  }

  public String getCode( ) {
    return code;
  }

  protected void setCode( final String code ) {
    this.code = code;
  }

  public String getMessage( ) {
    return message;
  }

  public String getResourceType( ) {
    return resourceType;
  }

  public String getResource( ) {
    return resource;
  }
}
