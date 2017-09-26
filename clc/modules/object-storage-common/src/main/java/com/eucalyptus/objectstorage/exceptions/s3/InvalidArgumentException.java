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

public class InvalidArgumentException extends S3Exception {

  private String argumentValue;
  private String argumentName;

  public InvalidArgumentException( ) {
    super( S3ErrorCodeStrings.InvalidArgument, "Argument format not recognized", HttpResponseStatus.BAD_REQUEST );
  }

  public InvalidArgumentException( String resource ) {
    this( );
    this.setResource( resource );
  }

  public InvalidArgumentException( String resource, String message ) {
    this( );
    this.setResource( resource );
    this.setMessage( message );
  }

  public InvalidArgumentException withArgumentName( String argumentName ) {
    this.argumentName = argumentName;
    return this;
  }

  public InvalidArgumentException withArgumentValue( String argumentValue ) {
    this.argumentValue = argumentValue;
    return this;
  }

  public String getArgumentValue( ) {
    return argumentValue;
  }

  public void setArgumentValue( String argumentValue ) {
    this.argumentValue = argumentValue;
  }

  public String getArgumentName( ) {
    return argumentName;
  }

  public void setArgumentName( String argumentName ) {
    this.argumentName = argumentName;
  }
}
