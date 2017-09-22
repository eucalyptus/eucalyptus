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

import java.util.Date;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.storage.msgs.BucketLogData;
import com.eucalyptus.walrus.WalrusBackend;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

@ComponentMessage( WalrusBackend.class )
public class WalrusRequestType extends BaseMessage {

  protected String accessKeyID;
  protected Date timeStamp;
  protected String signature;
  protected String credential;
  protected String bucket;
  protected String key;
  private BucketLogData logData;

  public WalrusRequestType( ) { }

  public WalrusRequestType( String bucket, String key ) {
    this.bucket = bucket;
    this.key = key;
  }

  public WalrusRequestType( String accessKeyID, Date timeStamp, String signature, String credential ) {
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

  public BucketLogData getLogData( ) {
    return logData;
  }

  public void setLogData( BucketLogData logData ) {
    this.logData = logData;
  }
}
