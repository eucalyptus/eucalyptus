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
package com.eucalyptus.walrus.msgs;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.storage.msgs.BucketLogData;
import com.eucalyptus.walrus.WalrusBackend;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

@ComponentMessage( WalrusBackend.class )
public class WalrusResponseType extends BaseMessage {

  private BucketLogData logData;
  private HttpResponseStatus status;
  private String statusMessage;

  public BucketLogData getLogData( ) {
    return logData;
  }

  public void setLogData( BucketLogData logData ) {
    this.logData = logData;
  }

  public HttpResponseStatus getStatus( ) {
    return status;
  }

  public void setStatus( HttpResponseStatus status ) {
    this.status = status;
  }

  public String getStatusMessage( ) {
    return statusMessage;
  }

  public void setStatusMessage( String statusMessage ) {
    this.statusMessage = statusMessage;
  }
}
