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
