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

public class NoSuchUploadException extends S3Exception {

  public NoSuchUploadException( ) {
    super( S3ErrorCodeStrings.NoSuchUpload, "The specified multipart upload does not exist. The upload ID might be invalid, or the multipart upload might have been aborted or completed.", HttpResponseStatus.NOT_FOUND );
  }

  public NoSuchUploadException( String resource ) {
    this( );
    this.setResource( resource );
  }
}
