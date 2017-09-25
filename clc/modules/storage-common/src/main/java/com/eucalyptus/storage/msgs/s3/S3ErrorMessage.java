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
