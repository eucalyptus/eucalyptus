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

public class StackSummary extends EucalyptusData {

  private Date creationTime;
  private Date deletionTime;
  private Date lastUpdatedTime;
  private String stackId;
  private String stackName;
  private String stackStatus;
  private String stackStatusReason;
  private String templateDescription;

  public Date getCreationTime( ) {
    return creationTime;
  }

  public void setCreationTime( Date creationTime ) {
    this.creationTime = creationTime;
  }

  public Date getDeletionTime( ) {
    return deletionTime;
  }

  public void setDeletionTime( Date deletionTime ) {
    this.deletionTime = deletionTime;
  }

  public Date getLastUpdatedTime( ) {
    return lastUpdatedTime;
  }

  public void setLastUpdatedTime( Date lastUpdatedTime ) {
    this.lastUpdatedTime = lastUpdatedTime;
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

  public String getTemplateDescription( ) {
    return templateDescription;
  }

  public void setTemplateDescription( String templateDescription ) {
    this.templateDescription = templateDescription;
  }
}
