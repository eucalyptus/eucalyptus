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
