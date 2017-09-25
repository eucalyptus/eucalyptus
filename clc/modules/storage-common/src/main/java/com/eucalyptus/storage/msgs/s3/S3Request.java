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

import java.util.Date;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * Base type for all S3 requests
 *
 * @author zhill
 */
public class S3Request extends BaseMessage {

  protected String accessKeyID;
  protected Date timeStamp;
  protected String signature;
  protected String credential;
  protected String bucket;
  protected String key;

  public S3Request( ) {
  }

  public S3Request( String bucket, String key ) {
    this.bucket = bucket;
    this.key = key;
  }

  public S3Request( String accessKeyID, Date timeStamp, String signature, String credential ) {
    this.accessKeyID = accessKeyID;
    this.timeStamp = timeStamp;
    this.signature = signature;
    this.credential = credential;
  }

  public String getAccessKeyID( ) {
    return accessKeyID;
  }

  public void setAccessKeyID( String accessKeyID ) {
    this.accessKeyID = accessKeyID;
  }

  public String getCredential( ) {
    return credential;
  }

  public void setCredential( String credential ) {
    this.credential = credential;
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
}
