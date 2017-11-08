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
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;

public class ElasticLoadBalancingHealthCheckType {

  @Property
  private Integer healthyThreshold;

  @Property
  private Integer interval;

  @Property
  private String target;

  @Property
  private Integer timeout;

  @Property
  private Integer unhealthyThreshold;

  public Integer getHealthyThreshold( ) {
    return healthyThreshold;
  }

  public void setHealthyThreshold( Integer healthyThreshold ) {
    this.healthyThreshold = healthyThreshold;
  }

  public Integer getInterval( ) {
    return interval;
  }

  public void setInterval( Integer interval ) {
    this.interval = interval;
  }

  public String getTarget( ) {
    return target;
  }

  public void setTarget( String target ) {
    this.target = target;
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

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "healthyThreshold", healthyThreshold )
        .add( "interval", interval )
        .add( "target", target )
        .add( "timeout", timeout )
        .add( "unhealthyThreshold", unhealthyThreshold )
        .toString( );
  }
}
