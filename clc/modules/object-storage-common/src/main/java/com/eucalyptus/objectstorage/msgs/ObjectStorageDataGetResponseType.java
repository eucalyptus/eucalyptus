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
package com.eucalyptus.objectstorage.msgs;

import java.io.InputStream;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

public class ObjectStorageDataGetResponseType extends ObjectStorageDataResponseType {

  private HttpResponseStatus status;
  private InputStream dataInputStream;
  private Long byteRangeStart;
  private Long byteRangeEnd;

  public HttpResponseStatus getStatus( ) {
    return status;
  }

  public void setStatus( HttpResponseStatus status ) {
    this.status = status;
  }

  public InputStream getDataInputStream( ) {
    return dataInputStream;
  }

  public void setDataInputStream( InputStream dataInputStream ) {
    this.dataInputStream = dataInputStream;
  }

  public Long getByteRangeStart( ) {
    return byteRangeStart;
  }

  public void setByteRangeStart( Long byteRangeStart ) {
    this.byteRangeStart = byteRangeStart;
  }

  public Long getByteRangeEnd( ) {
    return byteRangeEnd;
  }

  public void setByteRangeEnd( Long byteRangeEnd ) {
    this.byteRangeEnd = byteRangeEnd;
  }
}
