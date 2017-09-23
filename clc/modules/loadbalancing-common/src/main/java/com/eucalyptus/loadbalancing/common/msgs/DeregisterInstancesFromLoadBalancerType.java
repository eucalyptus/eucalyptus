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

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class DeregisterInstancesFromLoadBalancerType extends LoadBalancingMessage {

  private String loadBalancerName;
  private Instances instances;

  public DeregisterInstancesFromLoadBalancerType( ) {
  }

  public DeregisterInstancesFromLoadBalancerType( String loadBalancerName, Collection<String> instanceIds ) {
    this.loadBalancerName = loadBalancerName;
    this.instances = new Instances( );
    this.instances.setMember( instanceIds.stream( )
        .map( Instance.instance( ) )
        .collect( Collectors.toCollection( ArrayList::new ) ) );
  }

  public String getLoadBalancerName( ) {
    return loadBalancerName;
  }

  public void setLoadBalancerName( String loadBalancerName ) {
    this.loadBalancerName = loadBalancerName;
  }

  public Instances getInstances( ) {
    return instances;
  }

  public void setInstances( Instances instances ) {
    this.instances = instances;
  }
}
