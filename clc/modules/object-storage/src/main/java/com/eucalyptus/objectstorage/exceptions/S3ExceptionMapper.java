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
package com.eucalyptus.objectstorage.exceptions;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.amazonaws.AmazonServiceException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;

public class S3ExceptionMapper {

  public static S3Exception fromAWSJavaSDK( AmazonServiceException e ) {
    S3Exception s3Ex = new S3Exception( );
    s3Ex.setCode( e.getErrorCode( ) );
    s3Ex.setMessage( e.getMessage( ) );
    s3Ex.setStatus( HttpResponseStatus.valueOf( e.getStatusCode( ) ) );
    return s3Ex;
  }

  public static AmazonServiceException toAWSJavaSDKService( S3Exception e ) {
    AmazonServiceException ex = new AmazonServiceException( e.getMessage( ) );
    ex.setStatusCode( e.getStatus( ).getCode( ) );
    ex.setErrorCode( e.getCode( ) );
    ex.setErrorType( AmazonServiceException.ErrorType.Client );
    return ex;
  }

}
