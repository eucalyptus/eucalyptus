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
package com.eucalyptus.compute.common;

import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class BundleTask extends EucalyptusData {

  private String instanceId;
  private String bundleId;
  private String state;
  private Date startTime;
  private Date updateTime;
  private String progress;
  private String bucket;
  private String prefix;
  private String errorMessage;
  private String errorCode;

  public BundleTask( ) {
  }

  public BundleTask( String bundleId, String instanceId, String bucket, String prefix ) {
    this.bundleId = bundleId;
    this.instanceId = instanceId;
    this.bucket = bucket;
    this.prefix = prefix;
    this.state = "pending";
    this.startTime = new Date( );
    this.updateTime = new Date( );
    this.progress = "0%";
  }

  public BundleTask( String instanceId, String bundleId, String state, Date startTime, Date updateTime, String progress, String bucket, String prefix, String errorMessage, String errorCode ) {
    super( );
    this.instanceId = instanceId;
    this.bundleId = bundleId;
    this.state = state;
    this.startTime = startTime;
    this.updateTime = updateTime;
    this.progress = progress;
    this.bucket = bucket;
    this.prefix = prefix;
    this.errorMessage = errorMessage;
    this.errorCode = errorCode;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getBundleId( ) {
    return bundleId;
  }

  public void setBundleId( String bundleId ) {
    this.bundleId = bundleId;
  }

  public String getState( ) {
    return state;
  }

  public void setState( String state ) {
    this.state = state;
  }

  public Date getStartTime( ) {
    return startTime;
  }

  public void setStartTime( Date startTime ) {
    this.startTime = startTime;
  }

  public Date getUpdateTime( ) {
    return updateTime;
  }

  public void setUpdateTime( Date updateTime ) {
    this.updateTime = updateTime;
  }

  public String getProgress( ) {
    return progress;
  }

  public void setProgress( String progress ) {
    this.progress = progress;
  }

  public String getBucket( ) {
    return bucket;
  }

  public void setBucket( String bucket ) {
    this.bucket = bucket;
  }

  public String getPrefix( ) {
    return prefix;
  }

  public void setPrefix( String prefix ) {
    this.prefix = prefix;
  }

  public String getErrorMessage( ) {
    return errorMessage;
  }

  public void setErrorMessage( String errorMessage ) {
    this.errorMessage = errorMessage;
  }

  public String getErrorCode( ) {
    return errorCode;
  }

  public void setErrorCode( String errorCode ) {
    this.errorCode = errorCode;
  }
}
