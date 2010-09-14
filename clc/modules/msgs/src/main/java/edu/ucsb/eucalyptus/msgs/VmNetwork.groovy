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
package edu.ucsb.eucalyptus.msgs

public class StartNetworkType extends EucalyptusMessage {

  int vlan;
  String netName;
  String nameserver;
  ArrayList<String> clusterControllers = new ArrayList<String>();


  def StartNetworkType(){}

  def StartNetworkType(final String userId, final Integer vlan, final String netName)
  {
    super( userId );
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
}

public class DescribeNetworksType extends EucalyptusMessage {
  String nameserver;
  ArrayList<String> clusterControllers = new ArrayList<String>();
}

public class DescribeNetworksResponseType extends EucalyptusMessage {
  Integer mode;
  Integer addrsPerNetwork;
  ArrayList<NetworkInfoType> activeNetworks = new ArrayList<NetworkInfoType>();
  
  public String toString() {
    return "${this.getClass().getSimpleName()} mode=${mode} addrsPerNet=${addrsPerNetwork} " \
      + "\n${this.getClass().getSimpleName()} " + this.activeNetworks*.toString().join( "\n${this.getClass().getSimpleName()} " ); 
  }
}

public class NetworkInfoType extends EucalyptusData {
  Integer vlan;
  String networkName;
  String userName;
  ArrayList<String> allocatedAddresses = new ArrayList<String>();
  public String toString( ) {
    return "NetworkInfoType ${userName} ${networkName} ${vlan} ${allocatedAddresses}";
  }
}


public class AssignAddressType extends EucalyptusMessage {
  String instanceId;
  String source;
  String destination;
  def AssignAddressType(final source, final destination, final instanceId )
  {
    this.source = source;
    this.destination = destination;
    this.instanceId = instanceId;
  }

  def AssignAddressType(final msg, final source, final destination, final instanceId)
  {
    super(msg);
    this.source = source;
    this.destination = destination;
    this.instanceId = instanceId;
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
  ArrayList<String> addresses = new ArrayList<String>();
  ArrayList<String> mapping = new ArrayList<String>();
  public String toString() {
    return "${this.getClass().getSimpleName()} " + addresses.eachWithIndex{ it, i -> "${it} ${mapping[i]}" }.join("\n${this.getClass().getSimpleName()} ");
  }
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
