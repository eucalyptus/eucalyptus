/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.cloudwatch.domain.cpu;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_cloudwatch")
@Table(name="cpu_utilization")
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class CPUUtilizationEntity extends AbstractPersistent {

  public CPUUtilizationEntity() {
  }
  @Column(name = "instance_id", nullable = false)
  private String instanceId;
  @Column(name = "timestamp", nullable = false)
  private Date timestamp;
  @Column(name = "machine_usage_milliseconds", nullable = false)
  private Long machineUsageMilliseconds;
  public String getInstanceId() {
    return instanceId;
  }
  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }
  public Date getTimestamp() {
    return timestamp;
  }
  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }
  public Long getMachineUsageMilliseconds() {
    return machineUsageMilliseconds;
  }
  public void setMachineUsageMilliseconds(Long machineUsageMilliseconds) {
    this.machineUsageMilliseconds = machineUsageMilliseconds;
  }
}