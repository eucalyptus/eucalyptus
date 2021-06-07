/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.objectstorage.msgs;

import com.eucalyptus.objectstorage.policy.AdminOverrideAllowed;
import com.eucalyptus.objectstorage.policy.RequiresACLPermission;
import com.eucalyptus.objectstorage.policy.RequiresPermission;
import com.eucalyptus.objectstorage.policy.ResourceType;
import com.eucalyptus.objectstorage.policy.S3PolicySpec;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;

@AdminOverrideAllowed
@RequiresPermission( standard = S3PolicySpec.S3_LISTBUCKET )
@ResourceType( S3PolicySpec.S3_RESOURCE_BUCKET )
@RequiresACLPermission( object = {}, bucket = { ObjectStorageProperties.Permission.READ } )
public class ListBucketType extends ObjectStorageRequestType {

  private String prefix;
  private String maxKeys;
  private String delimiter;

  // v1
  private String marker;

  // v2
  private String startAfter;
  private String continuationToken;
  private String fetchOwner;
  private String listType;

  public ListBucketType( ) {
    prefix = "";
    marker = "";
    //delimiter = "";
  }

  public String getPrefix( ) {
    return prefix;
  }

  public void setPrefix( String prefix ) {
    this.prefix = prefix;
  }

  public String getMarker( ) {
    return marker;
  }

  public void setMarker( String marker ) {
    this.marker = marker;
  }

  public String getStartAfter( ) {
    return startAfter;
  }

  public void setStartAfter( final String startAfter ) {
    this.startAfter = startAfter;
  }

  public String getContinuationToken( ) {
    return continuationToken;
  }

  public void setContinuationToken( final String continuationToken ) {
    this.continuationToken = continuationToken;
  }

  public String getFetchOwner( ) {
    return fetchOwner;
  }

  public void setFetchOwner( final String fetchOwner ) {
    this.fetchOwner = fetchOwner;
  }

  public String getListType( ) {
    return listType;
  }

  public void setListType( final String listType ) {
    this.listType = listType;
  }

  public String getMaxKeys( ) {
    return maxKeys;
  }

  public void setMaxKeys( String maxKeys ) {
    this.maxKeys = maxKeys;
  }

  public String getDelimiter( ) {
    return delimiter;
  }

  public void setDelimiter( String delimiter ) {
    this.delimiter = delimiter;
  }
}
