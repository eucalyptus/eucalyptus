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

public class EnableAvailabilityZonesForLoadBalancerResponseType extends LoadBalancingMessage {

  private EnableAvailabilityZonesForLoadBalancerResult enableAvailabilityZonesForLoadBalancerResult = new EnableAvailabilityZonesForLoadBalancerResult( );
  private ResponseMetadata responseMetadata = new ResponseMetadata( );

  public EnableAvailabilityZonesForLoadBalancerResponseType( ) {
  }

  public EnableAvailabilityZonesForLoadBalancerResult getEnableAvailabilityZonesForLoadBalancerResult( ) {
    return enableAvailabilityZonesForLoadBalancerResult;
  }

  public void setEnableAvailabilityZonesForLoadBalancerResult( EnableAvailabilityZonesForLoadBalancerResult enableAvailabilityZonesForLoadBalancerResult ) {
    this.enableAvailabilityZonesForLoadBalancerResult = enableAvailabilityZonesForLoadBalancerResult;
  }

  public ResponseMetadata getResponseMetadata( ) {
    return responseMetadata;
  }

  public void setResponseMetadata( ResponseMetadata responseMetadata ) {
    this.responseMetadata = responseMetadata;
  }
}
