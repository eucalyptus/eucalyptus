/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.portal.awsusage;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.portal.workflow.AwsUsageRecord;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import java.util.Date;

@Entity
@PersistenceContext( name = "eucalyptus_billing" )
@Table( name = "aws_usage_hourly" )
public class AwsUsageRecordEntity extends AbstractPersistent implements AwsUsageRecord {
  public AwsUsageRecordEntity() { }
  public AwsUsageRecordEntity(final String ownerAccountNumber) {
    this.ownerAccountNumber = ownerAccountNumber;
  }

  @Column( name = "account_id" )
  private String ownerAccountNumber = null;

  // Service, Operation, UsageType, Resource, StartTime, EndTime, UsageValue
  @Column( name = "service", nullable=false)
  private String service = null;

  @Column( name = "operation", nullable=true)
  private String operation = null;

  @Column( name = "usage_type", nullable=true)
  private String usageType = null;

  @Column( name = "resource", nullable=true)
  private String resource = null;

  @Column( name = "start_time", nullable=false)
  private Date startTime = null;

  @Column( name = "end_time", nullable=false)
  private Date endTime = null;

  @Column( name = "usage_value", nullable=false)
  private String usageValue = null;


  @Override
  public void setOwnerAccountNumber(final String accountNumber) {
    this.ownerAccountNumber = accountNumber;
  }

  @Override
  public String getOwnerAccountNumber() {
    return this.ownerAccountNumber;
  }

  @Override
  public void setService(final String service) {
    this.service = service;
  }

  @Override
  public String getService() {
    return this.service;
  }

  @Override
  public void setOperation(final String operation) {
    this.operation = operation;
  }

  @Override
  public String getOperation() {
    return this.operation;
  }

  @Override
  public void setUsageType(final String usageType) {
    this.usageType = usageType;
  }

  @Override
  public String getUsageType() {
    return this.usageType;
  }

  @Override
  public void setResource(final String resource) {
    this.resource = resource;
  }

  @Override
  public String getResource() {
    return this.resource;
  }

  @Override
  public void setStartTime(final Date startTime) {
    this.startTime = startTime;
  }

  @Override
  public Date getStartTime() {
    return this.startTime;
  }

  @Override
  public void setEndTime(final Date endTime) {
    this.endTime = endTime;
  }

  @Override
  public Date getEndTime() {
    return this.endTime;
  }

  @Override
  public void setUsageValue(final String usageValue) {
    this.usageValue = usageValue;
  }

  @Override
  public String getUsageValue() {
    return this.usageValue;
  }
}
