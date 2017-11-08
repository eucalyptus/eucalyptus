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
package com.eucalyptus.portal.monthlyreport;

import com.eucalyptus.portal.workflow.AwsUsageRecord;
import java.util.Optional;

/* Monthly report is mostly a one-to-one mapping from AWS usage reports.
   AWS aggregates all data-transfers (across services) under a distinct product code (AWSDataTransfer).
   Due the limited availablity of data transfer metrics, EUCA will keep data transfer under separate services and operation
 */
public enum MonthlyReportEntryType {
  INSTANCE_HOURS("AmazonEC2", "RunInstances", "BoxUsage") {
    @Override
    public Double getQuantity(String usageValue) {
      return identityQuantity(usageValue);
    }
  },
  INSTANCE_DATA_TRANSFER_IN("AmazonEC2", "RunInstances", "DataTransfer-In-Bytes") {
    @Override
    public Double getQuantity(String usageValue) {
      return toGigabytes(usageValue);
    }
  },
  INSTANCE_DATA_TRANSFER_OUT("AmazonEC2", "RunInstances", "DataTransfer-Out-Bytes") {
    @Override
    public Double getQuantity(String usageValue) {
      return toGigabytes(usageValue);
    }
  },
  ELASTICIP_HOURS("AmazonEC2", null, "ElasticIP:IdleAddress") {
    @Override
    public Double getQuantity(String usageValue) {
      return identityQuantity(usageValue);
    }
  },
  ELB_HOURS("AmazonEC2", "LoadBalancing", "LoadBalancerUsage") {
    @Override
    public Double getQuantity(String usageValue) {
      return identityQuantity(usageValue);
    }
  },
  ELB_DATA_PROCESSED("AmazonEC2", "LoadBalancing", "DataTransfer") {
    @Override
    public Double getQuantity(String usageValue) {
      return toGigabytes(usageValue);
    }
  },
  EBS_VOLUME_STORED("AmazonEC2", null, "EBS:VolumeUsage") {
    @Override
    public Double getQuantity(String usageValue) {
      return toGigabytes(usageValue);
    }
  },
  EBS_SNAPSHOT_STORED("AmazonEC2", null, "EBS:SnapshotUsage") {
    @Override
    public Double getQuantity(String usageValue) {
      return toGigabytes(usageValue);
    }
  },
  CLOUDWATCH_ALARMS("AmazonEC2", null, "CW:AlarmMonitorUsage") {
    @Override
    public Double getQuantity(String usageValue) {
      return identityQuantity(usageValue);
    }
  },
  CLOUDWATCH_REQUESTS("AmazonEC2", null, "CW:Requests") {
    @Override
    public Double getQuantity(String usageValue) {
      return toThousands(usageValue);
    }
  },
  S3_REQUESTS("AmazonS3", null, "Requests") {
    @Override
    public Double getQuantity(String usageValue) {
      return toThousands(usageValue);
    }
  },
  S3_STORAGE_USED("AmazonS3", null, "TimedStorage-ByteHrs") {
    @Override
    public Double getQuantity(String usageValue) {
      return toGigabytes(usageValue);
    }
  };
  private String service;
  private String operation;
  private String usageType;

  MonthlyReportEntryType(final String service, final String operation, final String usageType) {
    this.service = service;
    this.operation = operation;
    this.usageType = usageType;
  }

  // given usageValue in AWS usage report, convert it to monthly usage quantity in Double type
  public abstract Double getQuantity(final String usageValue);

  public Double identityQuantity(final String usageValue) {
    return Double.parseDouble(usageValue);
  }

  public Double toGigabytes(final String usageValue) {
    return (double) (Long.parseLong(usageValue) / 1073741824);
  }

  public Double toThousands(final String usageValue) {
    return (double) (Long.parseLong(usageValue) / 1000);
  }

  public String getService() { return this.service; }
  public Optional<String> getOperation() { return this.operation != null ? Optional.of(this.operation) : Optional.empty(); }
  public String getUsageType() { return this.usageType; }

  public static Optional<MonthlyReportEntryType> getType(final AwsUsageRecord record) {
    for ( final MonthlyReportEntryType type : MonthlyReportEntryType.values() ) {
      if (type.getService().equals(record.getService()) &&
              record.getUsageType()!=null && record.getUsageType().startsWith(type.getUsageType())) {
        return Optional.of(type);
      }
    }
    return Optional.empty();
  }
}
