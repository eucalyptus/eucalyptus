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

import com.eucalyptus.util.ChannelBufferStreamingInputStream;

public class ObjectStorageDataRequestType extends ObjectStorageRequestType {

  private Boolean isCompressed;
  private ChannelBufferStreamingInputStream data;
  private boolean isChunked;
  private boolean expectHeader;

  public ObjectStorageDataRequestType( ) {
  }

  public ObjectStorageDataRequestType( String bucket, String key ) {
    super( bucket, key );
  }

  public Boolean getIsCompressed( ) {
    return isCompressed;
  }

  public void setIsCompressed( Boolean isCompressed ) {
    this.isCompressed = isCompressed;
  }

  public ChannelBufferStreamingInputStream getData( ) {
    return data;
  }

  public void setData( ChannelBufferStreamingInputStream data ) {
    this.data = data;
  }

  public boolean getIsChunked( ) {
    return isChunked;
  }

  public boolean isIsChunked( ) {
    return isChunked;
  }

  public void setIsChunked( boolean isChunked ) {
    this.isChunked = isChunked;
  }

  public boolean getExpectHeader( ) {
    return expectHeader;
  }

  public boolean isExpectHeader( ) {
    return expectHeader;
  }

  public void setExpectHeader( boolean expectHeader ) {
    this.expectHeader = expectHeader;
  }
}
