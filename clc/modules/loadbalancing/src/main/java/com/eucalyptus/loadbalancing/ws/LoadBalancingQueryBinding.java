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
package com.eucalyptus.loadbalancing.ws;

import com.eucalyptus.ws.protocol.BaseQueryBinding;
import com.eucalyptus.ws.protocol.OperationParameter;

/**
 * @author Chris Grzegorczyk <grze@eucalyptus.com>
 */
public class LoadBalancingQueryBinding extends BaseQueryBinding<OperationParameter> {

  static final String LOADBALANCING_NAMESPACE_PATTERN = "http://elasticloadbalancing.amazonaws.com/doc/2011-04-05/";  //TODO:GEN2OOLS: replace version with pattern : %s  
  static final String LOADBALANCING_DEFAULT_VERSION = "2011-04-05"; //"2012-06-01";              //TODO:GEN2OOLS: replace with correct default API version
  static final String LOADBALANCING_DEFAULT_NAMESPACE = String.format( LOADBALANCING_NAMESPACE_PATTERN, LOADBALANCING_DEFAULT_VERSION );

  public LoadBalancingQueryBinding() {
    super( LOADBALANCING_NAMESPACE_PATTERN, LOADBALANCING_DEFAULT_VERSION, OperationParameter.Action );
  }
}
