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

public class PutScheduledUpdateGroupActionType extends AutoScalingMessage {

  private String autoScalingGroupName;
  private String scheduledActionName;
  private Date time;
  private Date startTime;
  private Date endTime;
  private String recurrence;
  private Integer minSize;
  private Integer maxSize;
  private Integer desiredCapacity;

  public String getAutoScalingGroupName( ) {
    return autoScalingGroupName;
  }

  public void setAutoScalingGroupName( String autoScalingGroupName ) {
    this.autoScalingGroupName = autoScalingGroupName;
  }

  public String getScheduledActionName( ) {
    return scheduledActionName;
  }

  public void setScheduledActionName( String scheduledActionName ) {
    this.scheduledActionName = scheduledActionName;
  }

  public Date getTime( ) {
    return time;
  }

  public void setTime( Date time ) {
    this.time = time;
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

  public String getRecurrence( ) {
    return recurrence;
  }

  public void setRecurrence( String recurrence ) {
    this.recurrence = recurrence;
  }

  public Integer getMinSize( ) {
    return minSize;
  }

  public void setMinSize( Integer minSize ) {
    this.minSize = minSize;
  }

  public Integer getMaxSize( ) {
    return maxSize;
  }

  public void setMaxSize( Integer maxSize ) {
    this.maxSize = maxSize;
  }

  public Integer getDesiredCapacity( ) {
    return desiredCapacity;
  }

  public void setDesiredCapacity( Integer desiredCapacity ) {
    this.desiredCapacity = desiredCapacity;
  }
}
