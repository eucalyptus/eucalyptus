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

/**
 * When no CORS configuration exists, to match AWS's response, return 404 NotFound with
 * this error code and message.
 */
public class NoSuchCorsConfigurationException extends S3Exception {

  public NoSuchCorsConfigurationException( ) {
    super( S3ErrorCodeStrings.NoSuchCORSConfiguration, "The CORS configuration does not exist.", HttpResponseStatus.NOT_FOUND );
  }

  public NoSuchCorsConfigurationException( String resource ) {
    this( );
    this.setResource( resource );
  }
}
