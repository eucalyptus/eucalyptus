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
package com.eucalyptus.autoscaling.common.msgs;

import java.util.Date;
import com.eucalyptus.util.CompatFunction;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class Activity extends EucalyptusData {

  private String activityId;
  private String autoScalingGroupName;
  private String description;
  private String cause;
  private Date startTime;
  private Date endTime;
  private String statusCode;
  private String statusMessage;
  private Integer progress;
  private String details;

  public static CompatFunction<Activity, Date> startTime() {
    return Activity::getStartTime;
  }

  public String getActivityId( ) {
    return activityId;
  }

  public void setActivityId( String activityId ) {
    this.activityId = activityId;
  }

  public String getAutoScalingGroupName( ) {
    return autoScalingGroupName;
  }

  public void setAutoScalingGroupName( String autoScalingGroupName ) {
    this.autoScalingGroupName = autoScalingGroupName;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public String getCause( ) {
    return cause;
  }

  public void setCause( String cause ) {
    this.cause = cause;
  }

  public Date getStartTime( ) {
    return startTime;
  }

  public void setStartTime( Date startTime ) {
    this.startTime = startTime;
  }

  public Date getEndTime( ) {
    return endTime;
  }

  public void setEndTime( Date endTime ) {
    this.endTime = endTime;
  }

  public String getStatusCode( ) {
    return statusCode;
  }

  public void setStatusCode( String statusCode ) {
    this.statusCode = statusCode;
  }

  public String getStatusMessage( ) {
    return statusMessage;
  }

  public void setStatusMessage( String statusMessage ) {
    this.statusMessage = statusMessage;
  }

  public Integer getProgress( ) {
    return progress;
  }

  public void setProgress( Integer progress ) {
    this.progress = progress;
  }

  public String getDetails( ) {
    return details;
  }

  public void setDetails( String details ) {
    this.details = details;
  }
}
