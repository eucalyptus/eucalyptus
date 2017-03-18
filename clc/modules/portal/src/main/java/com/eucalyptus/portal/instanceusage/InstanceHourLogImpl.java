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

import com.eucalyptus.portal.workflow.InstanceLog;
import com.eucalyptus.portal.workflow.InstanceTag;

import java.util.Date;
import java.util.List;

public class InstanceHourLogImpl implements InstanceHourLog {
  private InstanceLog log = null;
  private Long hours = 0L;
  public InstanceHourLogImpl(final InstanceLog log, final long hours) {
    this.log = log;
    this.hours = hours;
  }

  @Override
  public String getInstanceId() {
    return log.getInstanceId();
  }

  @Override
  public String getInstanceType() {
    return log.getInstanceType();
  }

  @Override
  public String getPlatform() {
    return log.getPlatform();
  }

  @Override
  public String getRegion() {
    return log.getRegion();
  }

  @Override
  public String getAvailabilityZone() {
    return log.getAvailabilityZone();
  }

  @Override
  public List<InstanceTag> getTags() {
    return log.getTags();
  }

  @Override
  public Date getLogTime() {
    return log.getLogTime();
  }

  @Override
  public void setLogTime(final Date date) {
    log.setLogTime(date);
  }

  @Override
  public Long getHours() {
    return this.hours;
  }

  @Override
  public void setHours(long hours) {
    this.hours = hours;
  }
}
