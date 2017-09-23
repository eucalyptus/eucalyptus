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

public class SetLoadBalancerPoliciesForBackendServerResponseType extends LoadBalancingMessage {

  private SetLoadBalancerPoliciesForBackendServerResult setLoadBalancerPoliciesForBackendServerResult = new SetLoadBalancerPoliciesForBackendServerResult( );
  private ResponseMetadata responseMetadata = new ResponseMetadata( );

  public SetLoadBalancerPoliciesForBackendServerResponseType( ) {
  }

  public SetLoadBalancerPoliciesForBackendServerResult getSetLoadBalancerPoliciesForBackendServerResult( ) {
    return setLoadBalancerPoliciesForBackendServerResult;
  }

  public void setSetLoadBalancerPoliciesForBackendServerResult( SetLoadBalancerPoliciesForBackendServerResult setLoadBalancerPoliciesForBackendServerResult ) {
    this.setLoadBalancerPoliciesForBackendServerResult = setLoadBalancerPoliciesForBackendServerResult;
  }

  public ResponseMetadata getResponseMetadata( ) {
    return responseMetadata;
  }

  public void setResponseMetadata( ResponseMetadata responseMetadata ) {
    this.responseMetadata = responseMetadata;
  }
}
