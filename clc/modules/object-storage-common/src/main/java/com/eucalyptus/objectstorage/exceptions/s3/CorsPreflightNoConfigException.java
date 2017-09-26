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
 * On a CORS preflight OPTIONS request, when no CORS configuration exists,
 * to match AWS's response, return 403 Forbidden with this error code
 * and message.
 */
public class CorsPreflightNoConfigException extends S3Exception {

  public CorsPreflightNoConfigException( String requestMethod, String resourceType ) {
    super( S3ErrorCodeStrings.AccessForbidden, "CORSResponse: CORS is not enabled for this bucket.", HttpResponseStatus.FORBIDDEN, requestMethod );
    this.setResourceType( resourceType );
  }
}
