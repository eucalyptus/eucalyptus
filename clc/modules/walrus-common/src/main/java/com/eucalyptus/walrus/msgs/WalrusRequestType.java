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

  public Date getTimestamp( ) {
    return this.timeStamp;
  }

  public void setTimestamp( Date stamp ) {
    this.timeStamp = stamp;
  }
  public BucketLogData getLogData( ) {
    return logData;
  }

  public void setLogData( BucketLogData logData ) {
    this.logData = logData;
  }
}
