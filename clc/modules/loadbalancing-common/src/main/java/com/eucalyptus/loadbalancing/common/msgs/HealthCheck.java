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
package com.eucalyptus.loadbalancing.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class HealthCheck extends EucalyptusData {

  private static final long serialVersionUID = 1L;
  private String target;
  private Integer interval;
  private Integer timeout;
  private Integer unhealthyThreshold;
  private Integer healthyThreshold;

  public HealthCheck( ) {
  }

  public String getTarget( ) {
    return target;
  }

  public void setTarget( String target ) {
    this.target = target;
  }

  public Integer getInterval( ) {
    return interval;
  }

  public void setInterval( Integer interval ) {
    this.interval = interval;
  }

  public Integer getTimeout( ) {
    return timeout;
  }

  public void setTimeout( Integer timeout ) {
    this.timeout = timeout;
  }

  public Integer getUnhealthyThreshold( ) {
    return unhealthyThreshold;
  }

  public void setUnhealthyThreshold( Integer unhealthyThreshold ) {
    this.unhealthyThreshold = unhealthyThreshold;
  }

  public Integer getHealthyThreshold( ) {
    return healthyThreshold;
  }

  public void setHealthyThreshold( Integer healthyThreshold ) {
    this.healthyThreshold = healthyThreshold;
  }
}
