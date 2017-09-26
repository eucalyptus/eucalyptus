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

public class InvalidPolicyDocumentException extends S3Exception {

  public InvalidPolicyDocumentException( ) {
    super( S3ErrorCodeStrings.InvalidPolicyDocument, "The content of the form does not meet the conditions specified in the policy document.", HttpResponseStatus.BAD_REQUEST );
  }

  public InvalidPolicyDocumentException( String resource ) {
    this( );
    this.setResource( resource );
  }

  public InvalidPolicyDocumentException( String resource, String detailMessage ) {
    this( );
    this.setResource( resource );
    this.setMessage( this.getMessage( ) + " Detail: " + detailMessage );
  }
}
