/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.config;

import java.util.ArrayList
import com.eucalyptus.component.ComponentId.ComponentMessage
import com.eucalyptus.util.HasSideEffect
import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.EucalyptusData

@ComponentMessage(ConfigurationService.class)
public class ConfigurationMessage extends BaseMessage {}
public class ComponentInfoType extends EucalyptusData {
  String type;
  String partition;
  String name;
  String hostName;
  String fullName;
  String state;//really an enum
  String detail;
  public ComponentInfoType(){}
}

public class NodeComponentInfoType extends EucalyptusData {
  String name;
  String clusterName;  
  ArrayList<String> instances = new ArrayList<String>();
  public NodeComponentInfoType(){}
  public NodeComponentInfoType(String name, String clusterName){
    this.name = name; 
    this.clusterName = clusterName;
  }
}

public class RegisterComponentType extends ConfigurationMessage {
  String partition;
  String name;
  String host;
  Integer port;
  def RegisterComponentType() {}
  def RegisterComponentType(final String partition, final String name, final String host, final Integer port) {
    this.partition = partition;
    this.name = name;
    this.host = host;
    this.port = port;
  }
}
public class RegisterComponentResponseType extends ConfigurationMessage {}
public class DeregisterComponentType extends ConfigurationMessage {
  String name;
  String partition;
}
public class DeregisterComponentResponseType extends ConfigurationMessage {}

public class DescribeComponentsType extends ConfigurationMessage {
  Boolean verbose = Boolean.FALSE;
}
public class DescribeComponentsResponseType extends ConfigurationMessage {
  ArrayList<ComponentInfoType> registered = new ArrayList<ComponentInfoType>();
}
public class ModifyComponentAttributeType extends ConfigurationMessage {
  String partition;
  String name;
  String attribute; //{partition,state}
  String value;
}
public class ModifyComponentAttributeResponseType extends ConfigurationMessage {}

public class GetComponentLogsType extends DescribeComponentsType {}
public class GetComponentLogsResponseType extends DescribeComponentsResponseType {}
