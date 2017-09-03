/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.euare;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.Role;

public class EuareException extends EucalyptusWebServiceException {

  private static final long serialVersionUID = 1L;

  public static final String ENTITY_ALREADY_EXISTS = "EntityAlreadyExists";
  public static final String LIMIT_EXCEEDED = "LimitExceeded";
  public static final String NO_SUCH_ENTITY = "NoSuchEntity";
  public static final String INTERNAL_FAILURE = "InternalFailure";
  public static final String NOT_AUTHORIZED = "NotAuthorized";
  public static final String ACCESS_DENIED = "AccessDenied";
  public static final String DELETE_CONFLICT = "DeleteConflict";
  public static final String NOT_IMPLEMENTED = "NotImplemented";
  public static final String MALFORMED_POLICY_DOCUMENT = "MalformedPolicyDocument";
  public static final String DUPLICATE_CERTIFICATE = "DuplicateCertificate";
  public static final String INVALID_CERTIFICATE = "InvalidCertificate";
  public static final String MALFORMED_CERTIFICATE = "MalformedCertificate";
  public static final String INVALID_NAME = "InvalidName";
  public static final String INVALID_ID = "InvalidId";
  public static final String INVALID_VALUE = "InvalidValue";
  public static final String VALIDATION_ERROR = "ValidationError";

  private HttpResponseStatus status;

  public EuareException( HttpResponseStatus status, String error ) {
    this( status, error, "Internal error" );
  }

  public EuareException( HttpResponseStatus status, String error, String message, Throwable cause ) {
    super( error, statusAsRole( status ), message );
    initCause( cause );
    this.status = status;
  }

  public EuareException( HttpResponseStatus status, String error, String message ) {
    super( error, statusAsRole( status ), message );
    this.status = status;
  }

  public HttpResponseStatus getStatus( ) {
    return this.status;
  }

  public String getError( ) {
    return getCode( );
  }

  private static Role statusAsRole( final HttpResponseStatus status ) {
    return status.getCode( ) >= 400 && status.getCode( ) < 500 ?
        Role.Sender :
        Role.Receiver;
  }
}
