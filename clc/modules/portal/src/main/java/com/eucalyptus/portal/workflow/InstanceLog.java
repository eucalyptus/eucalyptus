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
package com.eucalyptus.portal.workflow;

import java.util.Date;
import java.util.List;

public interface InstanceLog {
  String getAccountId();
  void setAccountId(final String accountId);

  String getInstanceId();
  void setInstanceId(final String instanceId);

  String getInstanceType();
  void setInstanceType(final String instanceType);

  String getPlatform();
  void setPlatform(final String platform);

  String getRegion();
  void setRegion(final String region);

  String getAvailabilityZone();
  void setAvailabilityZone(final String az);

  List<InstanceTag> getTags();
  void addTag(final InstanceTag tag);

  Date getLogTime();
  void setLogTime(final Date date);
}
