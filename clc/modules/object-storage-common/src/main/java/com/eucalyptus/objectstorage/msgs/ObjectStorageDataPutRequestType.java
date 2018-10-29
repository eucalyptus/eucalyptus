/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.objectstorage.msgs;

import com.eucalyptus.util.ChannelBufferStreamingInputStream;

/**
 *
 */
public abstract class ObjectStorageDataPutRequestType extends ObjectStorageDataRequestType {
  private Boolean isCompressed;
  private ChannelBufferStreamingInputStream data;
  private boolean isChunked;
  private boolean expectHeader;

  public ObjectStorageDataPutRequestType( ) {
  }

  public ObjectStorageDataPutRequestType( final String bucket, final String key ) {
    super( bucket, key );
  }

  public Long contentLength( ) {
    Long contentLength = null;
    String contentLengthText = getContentLength( );
    if ( contentLengthText != null ) try {
      contentLength = Long.valueOf( contentLengthText );
    } catch ( NumberFormatException ignore ) {
    }
    return contentLength;
  }

  public abstract String getContentLength( );

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
