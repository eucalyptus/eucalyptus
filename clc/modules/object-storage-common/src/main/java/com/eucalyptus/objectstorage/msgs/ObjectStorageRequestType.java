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

import java.util.Date;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.storage.msgs.BucketLogData;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

@ComponentMessage( ObjectStorage.class )
public class ObjectStorageRequestType extends BaseMessage {

  protected Date timeStamp;
  protected String bucket;
  protected String key;
  protected String origin;
  protected String httpMethod;
  protected String versionId;
  private BucketLogData logData;

  public ObjectStorageRequestType( ) {
  }

  public ObjectStorageRequestType( String bucket, String key ) {
    this.bucket = bucket;
    this.key = key;
  }

  public ObjectStorageRequestType( Date timeStamp ) {
    this.timeStamp = timeStamp;
  }

  public String getBucket( ) {
    return bucket;
  }

  public void setBucket( String bucket ) {
    this.bucket = bucket;
  }

  public String getKey( ) {
    return key;
  }

  public void setKey( String key ) {
    this.key = key;
  }

  public Date getTimestamp( ) {
    return this.timeStamp;
  }

  public void setTimestamp( Date stamp ) {
    this.timeStamp = stamp;
  }

  public String getOrigin( ) {
    return origin;
  }

  public void setOrigin( String origin ) {
    this.origin = origin;
  }

  public String getHttpMethod( ) {
    return httpMethod;
  }

  public void setHttpMethod( String httpMethod ) {
    this.httpMethod = httpMethod;
  }

  public String getVersionId( ) {
    return this.versionId;
  }

  public void setVersionId( String versionId ) {
    this.versionId = versionId;
  }

  public String getFullResource( ) {
    return this.bucket + "/" + this.key;
  }

  public BucketLogData getLogData( ) {
    return logData;
  }

  public void setLogData( BucketLogData logData ) {
    this.logData = logData;
  }
}
