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
