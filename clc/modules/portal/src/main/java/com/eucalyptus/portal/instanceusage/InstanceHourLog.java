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
package com.eucalyptus.portal.instanceusage;

import com.eucalyptus.portal.workflow.InstanceTag;
import java.util.Date;
import java.util.List;

// decorate instancelog with hours for aggregation
public interface InstanceHourLog {
  String getInstanceId();
  String getInstanceType();
  String getPlatform();
  String getAvailabilityZone();
  String getRegion();
  List<InstanceTag> getTags();
  void setLogTime(Date date);
  Date getLogTime();
  Long getHours();
  void setHours(long  hours);
}