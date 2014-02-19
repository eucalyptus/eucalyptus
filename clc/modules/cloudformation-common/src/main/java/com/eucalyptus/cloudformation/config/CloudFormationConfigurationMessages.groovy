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
package com.eucalyptus.cloudformation.config

import com.eucalyptus.config.DeregisterComponentResponseType;
import com.eucalyptus.config.DeregisterComponentType;
import com.eucalyptus.config.DescribeComponentsResponseType;
import com.eucalyptus.config.DescribeComponentsType;
import com.eucalyptus.config.ModifyComponentAttributeResponseType;
import com.eucalyptus.config.ModifyComponentAttributeType;
import com.eucalyptus.config.RegisterComponentResponseType;
import com.eucalyptus.config.RegisterComponentType;

public class RegisterCloudFormationType extends RegisterComponentType {}
public class RegisterCloudFormationResponseType extends RegisterComponentResponseType {}
public class DeregisterCloudFormationType extends DeregisterComponentType {}
public class DeregisterCloudFormationResponseType extends DeregisterComponentResponseType {}
public class ModifyCloudFormationAttributeType extends ModifyComponentAttributeType{}
public class ModifyCloudFormationAttributeResponseType extends ModifyComponentAttributeResponseType {}
public class DescribeCloudFormationType extends DescribeComponentsType {}
public class DescribeCloudFormationResponseType extends DescribeComponentsResponseType {}
