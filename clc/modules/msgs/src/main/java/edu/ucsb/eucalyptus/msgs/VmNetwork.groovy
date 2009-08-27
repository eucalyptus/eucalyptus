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
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.msgs

public class StartNetworkType extends EucalyptusMessage {

  int vlan;
  String netName;
  String nameserver;
  ArrayList<String> clusterControllers;

  def StartNetworkType(final msg, final vlan, final netName) {
    super(msg);
    this.vlan = vlan;
    this.netName = netName;
  }



  def StartNetworkType(final EucalyptusMessage msg, final Integer vlan, final String netName)
  {
    super(msg);
    this.vlan = vlan;
    this.netName = netName;
  }

  def StartNetworkType(){}

  def StartNetworkType(final Integer vlan, final String netName)
  {
    this.vlan = vlan;
    this.netName = netName;
  }
}

public class StartNetworkResponseType extends EucalyptusMessage {
}
public class StopNetworkType extends EucalyptusMessage {
  Integer vlan;
  String netName;

  def StopNetworkType(){}

  def StopNetworkType(final String userId, final String netName, final Integer vlan)
  {
    super( userId );
    this.vlan = vlan;
    this.netName = netName;
  }

  def StopNetworkType(final StartNetworkType msg)
  {
    super(msg);
    this.vlan = msg.vlan;
    this.netName = msg.netName;
  }

}

public class StopNetworkResponseType extends EucalyptusMessage {
  boolean _return;
}

//TODO: new message type
public class DescribeNetworksType extends EucalyptusMessage {
  String nameserver;
  ArrayList<String> clusterControllers;
}
//TODO: new message type
public class DescribeNetworksResponeType extends EucalyptusMessage {
  Integer mode;
  Integer addrsPerNetwork;
  ArrayList<NetworkInfoType> activeNetworks = new ArrayList<NetworkInfoType>();
}
//TODO: new message type
public class NetworkInfoType extends EucalyptusData {
  Integer vlan;
  String networkName;
  String userName;
  ArrayList<String> allocatedAddresses = new ArrayList<String>();
}


public class AssignAddressType extends EucalyptusMessage {

  String source;
  String destination;
  def AssignAddressType(final source, final destination)
  {
    this.source = source;
    this.destination = destination;
  }

  def AssignAddressType(final msg, final source, final destination)
  {
    super(msg);
    this.source = source;
    this.destination = destination;
  }

  def AssignAddressType()
  {
  }



}
public class AssignAddressResponseType extends EucalyptusMessage {
}
public class UnassignAddressType extends EucalyptusMessage {

  String source;
  String destination;

  def UnassignAddressType(final msg, final source, final destination)
  {
    super(msg);
    this.source = source;
    this.destination = destination;
  }
  def UnassignAddressType(final source, final destination)
  {

    this.source = source;
    this.destination = destination;
  }


  def UnassignAddressType()
  {
  }

}
public class UnassignAddressResponseType extends EucalyptusMessage {
}
public class DescribePublicAddressesType extends EucalyptusMessage {
}
public class DescribePublicAddressesResponseType extends EucalyptusMessage {
  ArrayList<String> addresses;
  ArrayList<String> mapping;
}

public class ConfigureNetworkType extends EucalyptusMessage {

  ArrayList<PacketFilterRule> rules = new ArrayList<PacketFilterRule>();

  def ConfigureNetworkType(final EucalyptusMessage msg, final ArrayList<PacketFilterRule> rules)
  {
    super(msg);
    this.rules = rules;
  }

  def ConfigureNetworkType(final ArrayList<PacketFilterRule> rules)
  {
    this.rules = rules;
  }

  def ConfigureNetworkType(){}
}
public class ConfigureNetworkResponseType extends EucalyptusMessage {
}
