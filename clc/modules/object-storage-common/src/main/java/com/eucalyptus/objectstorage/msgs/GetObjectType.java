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
@RequiresPermission( standard = S3PolicySpec.S3_GETOBJECT, version = S3PolicySpec.S3_GETOBJECTVERSION )
@ResourceType( S3PolicySpec.S3_RESOURCE_OBJECT )
@RequiresACLPermission( object = { ObjectStorageProperties.Permission.READ }, bucket = {} )
public class GetObjectType extends ObjectStorageDataGetRequestType {

  private Boolean getMetaData;
  private Boolean inlineData;
  private Boolean deleteAfterGet;
  private Boolean getTorrent;

  public GetObjectType( ) {
  }

  public GetObjectType( final String bucketName, final String key, final Boolean getMetaData, final Boolean inlineData ) {
    super( bucketName, key );
    this.getMetaData = getMetaData;
    this.inlineData = inlineData;
  }

  public Boolean getGetMetaData( ) {
    return getMetaData;
  }

  public void setGetMetaData( Boolean getMetaData ) {
    this.getMetaData = getMetaData;
  }

  public Boolean getInlineData( ) {
    return inlineData;
  }

  public void setInlineData( Boolean inlineData ) {
    this.inlineData = inlineData;
  }

  public Boolean getDeleteAfterGet( ) {
    return deleteAfterGet;
  }

  public void setDeleteAfterGet( Boolean deleteAfterGet ) {
    this.deleteAfterGet = deleteAfterGet;
  }

  public Boolean getGetTorrent( ) {
    return getTorrent;
  }

  public void setGetTorrent( Boolean getTorrent ) {
    this.getTorrent = getTorrent;
  }
}
