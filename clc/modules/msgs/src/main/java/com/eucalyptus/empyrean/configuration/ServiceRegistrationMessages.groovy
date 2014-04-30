@GroovyAddClassUUID
package com.eucalyptus.empyrean.configuration

import com.eucalyptus.empyrean.EmpyreanMessage
import com.eucalyptus.empyrean.ServiceId
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/
/**
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
public class ServiceRegistrationMessage extends EmpyreanMessage {
  ArrayList<String> statusMessages = new ArrayList<String>();
}

public class RegisterServiceType extends ServiceRegistrationMessage {
  String type;
  String partition;
  String name;
  String host;
  Integer port;
}

public class RegisterServiceResponseType extends ServiceRegistrationMessage {
  ArrayList<ServiceId> registeredServices = new ArrayList<ServiceId>();
}

public class DeregisterServiceType extends ServiceRegistrationMessage {
  String type;
  String name;
}

public class DeregisterServiceResponseType extends ServiceRegistrationMessage {
  ArrayList<ServiceId> deregisteredServices = new ArrayList<ServiceId>();
}

public class DescribeServiceAttributesType extends ServiceRegistrationMessage {
  Boolean verbose = Boolean.FALSE;
  Boolean reset = Boolean.FALSE;
  String type;
  String partition;
  String name;
}

public class DescribeServiceAttributesResponseType extends ServiceRegistrationMessage {
  ArrayList<ServiceAttribute> attributes = new ArrayList<ServiceAttribute>();
}

public class ServiceAttribute extends EucalyptusData {
  String name;
  String value;
  String description;
  Boolean readOnly;
  Boolean required;
  String defaultValue;
  String scope;//e.g., cloud-wide, partition-wide, service-specific
  //ArrayList<String> authorizedRoles = new ArrayList<String>();
}

public class ModifyServiceAttributeType extends ServiceRegistrationMessage {
  String name;
  String partition;
  String attribute;
  String value;
}

public class ModifyServiceAttributeResponseType extends ServiceRegistrationMessage {}

public class DescribeAvailableServiceTypesType extends ServiceRegistrationMessage {
  Boolean verbose = Boolean.FALSE;
}

public class DescribeAvailableServiceTypesResponseType extends ServiceRegistrationMessage {
  ArrayList<AvailableComponentInfo> available = new ArrayList<AvailableComponentInfo>();
}

public class AvailableComponentInfo extends EucalyptusData {
  /**
   * Info about component
   */
  //The name of the component
  String componentName;
  //The name of the component
  String componentCapitalizedName;
  //A human readable description of what the service is/does/whatever.
  String description;
  //Whether the component has a specific certificate
  Boolean hasCredentials;

  /**
   * Info about its registration requirements
   */
  //means that the placement of services can be controlled by the administrator using this registration service.
  Boolean registerable;
  //if false then the user cannot control the name used by the system when reporting registered services of this component type.
  Boolean requiresName;
  //means that the service must: must have a unique name and its registration requires a registered partition's name.
  Boolean partitioned;
  //means the component delivers service which requires direct user access.
  Boolean publicApiService;

  /**
   * Info about service groups
   */
  //service groups for this component.
  //service instances will be created when such service groups are registered.
  ArrayList<String> serviceGroups = new ArrayList<String>();
  //components which are members of a service group.
  //registering or deregistering the service group creates a service instance of each listed component.
  ArrayList<String> serviceGroupMembers = new ArrayList<String>();
  /**
   * Other
   */
  //means the component information reported is potentially specific to the responding host.
  //  Boolean local;
}

