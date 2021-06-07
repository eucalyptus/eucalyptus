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
package com.eucalyptus.objectstorage.msgs;

public interface ObjectStorageCommonResponseType {

  public abstract String getOrigin( );

  public abstract void setOrigin( String origin );

  public abstract String getHttpMethod( );

  public abstract void setHttpMethod( String httpMethod );

  public abstract String getBucketName( );

  public abstract void setBucketName( String bucketName );

  public abstract String getBucketUuid( );

  public abstract void setBucketUuid( String bucketUuid );

  public abstract String getAllowedOrigin( );

  public abstract void setAllowedOrigin( String allowedOrigin );

  public abstract String getAllowedMethods( );

  public abstract void setAllowedMethods( String allowedMethods );

  public abstract String getExposeHeaders( );

  public abstract void setExposeHeaders( String exposeHeaders );

  public abstract String getMaxAgeSeconds( );

  public abstract void setMaxAgeSeconds( String maxAgeSeconds );

  public abstract String getAllowCredentials( );

  public abstract void setAllowCredentials( String allowCredentials );

  public abstract String getVary( );

  public abstract void setVary( String vary );
}
