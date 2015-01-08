/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
@GroovyAddClassUUID
package com.eucalyptus.balancing.common.config

import com.eucalyptus.config.DeregisterComponentResponseType;
import com.eucalyptus.config.DeregisterComponentType;
import com.eucalyptus.config.DescribeComponentsResponseType;
import com.eucalyptus.config.DescribeComponentsType;
import com.eucalyptus.config.ModifyComponentAttributeResponseType;
import com.eucalyptus.config.ModifyComponentAttributeType;
import com.eucalyptus.config.RegisterComponentResponseType;
import com.eucalyptus.config.RegisterComponentType
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID;

public class RegisterLoadBalancingType extends RegisterComponentType {}
public class RegisterLoadBalancingResponseType extends RegisterComponentResponseType {}
public class DeregisterLoadBalancingType extends DeregisterComponentType {}
public class DeregisterLoadBalancingResponseType extends DeregisterComponentResponseType {}
public class ModifyLoadBalancingAttributeType extends ModifyComponentAttributeType{}
public class ModifyLoadBalancingAttributeResponseType extends ModifyComponentAttributeResponseType {}
public class DescribeLoadBalancingType extends DescribeComponentsType {}
public class DescribeLoadBalancingResponseType extends DescribeComponentsResponseType {}
