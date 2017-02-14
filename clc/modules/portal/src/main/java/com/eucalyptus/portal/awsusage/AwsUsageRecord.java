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
import java.util.Date;

public interface AwsUsageRecord {
  void setOwnerAccountNumber(final String accountNumber);
  String getOwnerAccountNumber();

  void setService(final String service);
  String getService();

  void setOperation(final String operation);
  String getOperation();

  void setUsageType(final String usageType);
  String getUsageType();

  void setResource(final String resource);
  String getResource();

  void setStartTime(final Date startTime);
  Date getStartTime();

  void setEndTime(final Date endTime);
  Date getEndTime();

  void setUsageValue(final String usageValue);
  String getUsageValue();
}

