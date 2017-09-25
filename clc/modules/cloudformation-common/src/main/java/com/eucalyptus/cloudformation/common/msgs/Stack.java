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
package com.eucalyptus.cloudformation.common.msgs;

import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class Stack extends EucalyptusData {

  private ResourceList capabilities;
  private Date creationTime;
  private String description;
  private Boolean disableRollback;
  private Date lastUpdatedTime;
  private ResourceList notificationARNs;
  private Outputs outputs;
  private Parameters parameters;
  private String stackId;
  private String stackName;
  private String stackStatus;
  private String stackStatusReason;
  private Tags tags;
  private Integer timeoutInMinutes;

  public ResourceList getCapabilities( ) {
    return capabilities;
  }

  public void setCapabilities( ResourceList capabilities ) {
    this.capabilities = capabilities;
  }

  public Date getCreationTime( ) {
    return creationTime;
  }

  public void setCreationTime( Date creationTime ) {
    this.creationTime = creationTime;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public Boolean getDisableRollback( ) {
    return disableRollback;
  }

  public void setDisableRollback( Boolean disableRollback ) {
    this.disableRollback = disableRollback;
  }

  public Date getLastUpdatedTime( ) {
    return lastUpdatedTime;
  }

  public void setLastUpdatedTime( Date lastUpdatedTime ) {
    this.lastUpdatedTime = lastUpdatedTime;
  }

  public ResourceList getNotificationARNs( ) {
    return notificationARNs;
  }

  public void setNotificationARNs( ResourceList notificationARNs ) {
    this.notificationARNs = notificationARNs;
  }

  public Outputs getOutputs( ) {
    return outputs;
  }

  public void setOutputs( Outputs outputs ) {
    this.outputs = outputs;
  }

  public Parameters getParameters( ) {
    return parameters;
  }

  public void setParameters( Parameters parameters ) {
    this.parameters = parameters;
  }

  public String getStackId( ) {
    return stackId;
  }

  public void setStackId( String stackId ) {
    this.stackId = stackId;
  }

  public String getStackName( ) {
    return stackName;
  }

  public void setStackName( String stackName ) {
    this.stackName = stackName;
  }

  public String getStackStatus( ) {
    return stackStatus;
  }

  public void setStackStatus( String stackStatus ) {
    this.stackStatus = stackStatus;
  }

  public String getStackStatusReason( ) {
    return stackStatusReason;
  }

  public void setStackStatusReason( String stackStatusReason ) {
    this.stackStatusReason = stackStatusReason;
  }

  public Tags getTags( ) {
    return tags;
  }

  public void setTags( Tags tags ) {
    this.tags = tags;
  }

  public Integer getTimeoutInMinutes( ) {
    return timeoutInMinutes;
  }

  public void setTimeoutInMinutes( Integer timeoutInMinutes ) {
    this.timeoutInMinutes = timeoutInMinutes;
  }
}
