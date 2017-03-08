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
