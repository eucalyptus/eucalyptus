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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
package edu.ucsb.eucalyptus.msgs;

public class ComponentInfoType extends EucalyptusData {
  String name;
  String detail;
  public ComponentInfoType(){}
  public ComponentInfoType(String name, String detail){this.name = name; this.detail = detail;}
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


















