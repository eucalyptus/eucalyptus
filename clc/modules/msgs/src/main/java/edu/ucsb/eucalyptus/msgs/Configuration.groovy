/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.msgs;

public class ComponentInfoType extends EucalyptusData {
  String name;
  String detail;
  public ComponentInfoType(){}
  public ComponentInfoType(String name, String detail){this.name = name; this.detail = detail;}
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
public class ConfigurationMessage extends EucalyptusMessage {
  String getComponentName(){
    String className = this.getClass().getSimpleName();
    return className.replaceAll("Describe","").replaceAll("Deregister","").replaceAll("Register","").substring(0,6);
  }
}
public class RegisterComponentType extends ConfigurationMessage {
  String name;
  String host;
  Integer port;
  def RegisterComponentType() {}
  def RegisterComponentType(final String name, final String host, final Integer port) {
    this.name = name;
    this.host = host;
    this.port = port;
  }
}
public class RegisterComponentResponseType extends ConfigurationMessage {}
public class DeregisterComponentType extends ConfigurationMessage {
  String name;
}
public class DeregisterComponentResponseType extends ConfigurationMessage {}

public class DescribeComponentsType extends ConfigurationMessage {}
public class DescribeComponentsResponseType extends ConfigurationMessage {
  ArrayList<ComponentInfoType> registered = new ArrayList<ComponentInfoType>();
}

public class RegisterClusterType extends RegisterComponentType {
  public RegisterClusterType( ) {}
  public RegisterClusterType( String name, String host, Integer port ) {
    super( name, host, port );
  }  
}

public class RegisterClusterResponseType extends RegisterComponentResponseType {}
public class DeregisterClusterType extends DeregisterComponentType {}
public class DeregisterClusterResponseType extends DeregisterComponentResponseType {}
public class DescribeClustersType extends DescribeComponentsType {}
public class DescribeClustersResponseType extends DescribeComponentsResponseType {}
public class DescribeNodesType extends ConfigurationMessage  {}
public class DescribeNodesResponseType extends ConfigurationMessage {
  ArrayList<NodeComponentInfoType> registered = new ArrayList<NodeComponentInfoType>();
}

public class RegisterStorageControllerType extends RegisterComponentType {}
public class RegisterStorageControllerResponseType extends RegisterComponentResponseType {}
public class DeregisterStorageControllerType extends DeregisterComponentType {}
public class DeregisterStorageControllerResponseType extends DeregisterComponentResponseType {}
public class DescribeStorageControllersType extends DescribeComponentsType {}
public class DescribeStorageControllersResponseType extends DescribeComponentsResponseType {}

public class RegisterWalrusType extends RegisterComponentType {}
public class RegisterWalrusResponseType extends RegisterComponentResponseType {}
public class DeregisterWalrusType extends DeregisterComponentType {}
public class DeregisterWalrusResponseType extends DeregisterComponentResponseType {}
public class DescribeWalrusesType extends DescribeComponentsType {}
public class DescribeWalrusesResponseType extends DescribeComponentsResponseType {}

public class GetComponentLogsType extends DescribeComponentsType {}
public class GetComponentLogsResponseType extends DescribeComponentsResponseType {}















