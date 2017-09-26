/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.objectstorage.exceptions.s3;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageException;

public class S3Exception extends ObjectStorageException {

  private String requestMethod;

  public S3Exception( ) {
  }

  public S3Exception( String errorCode, String description, HttpResponseStatus statusCode ) {
    super( );
    this.setCode( errorCode );
    this.setMessage( description );
    this.setStatus( statusCode );
  }

  public S3Exception( String errorCode, String description, HttpResponseStatus statusCode, String requestMethod ) {
    super( );
    this.setCode( errorCode );
    this.setMessage( description );
    this.setStatus( statusCode );
    this.requestMethod = requestMethod;
  }

  public String getRequestMethod( ) {
    return requestMethod;
  }

  public void setRequestMethod( String requestMethod ) {
    this.requestMethod = requestMethod;
  }
}
