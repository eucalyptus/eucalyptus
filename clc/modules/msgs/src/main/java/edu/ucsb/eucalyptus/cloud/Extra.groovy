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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.cloud


import java.util.ArrayList
import java.util.List
import com.eucalyptus.records.*
import com.google.common.collect.*
import edu.ucsb.eucalyptus.msgs.*

public class Pair {
  
  public static List<Pair> getPaired(List one, List two) {
    List<Pair> newList = new ArrayList<Pair>();
    for ( int idx = 0; idx < one.size(); idx++ )
      newList.add(new Pair(one[ idx ], two[ idx ]));
    return newList;
  }
  
  def String left, right;
  
  def Pair(final left, final right) {
    this.left = left;
    this.right = right;
  }
  
}

public class VmDescribeType extends EucalyptusMessage {
  
  ArrayList<String> instancesSet = new ArrayList<String>();
}
public class VmDescribeResponseType extends EucalyptusMessage {
  
  String originCluster;
  ArrayList<VmInfo> vms = new ArrayList<VmInfo>();
  public String toString() {
    return "${this.getClass().getSimpleName()} " + vms*.toString().join("\n${this.getClass().getSimpleName()} ");
  }
}

public class VmRunResponseType extends EucalyptusMessage {
  
  ArrayList<VmInfo> vms = new ArrayList<VmInfo>();
}

public class VmInfo extends EucalyptusData {
  String uuid;
  String imageId;
  String kernelId;
  String ramdiskId;
  String instanceId;
  VmTypeInfo instanceType = new VmTypeInfo();
  String keyValue;
  Date launchTime;
  String stateName;
  NetworkConfigType netParams = new NetworkConfigType();
  String ownerId;
  String reservationId;
  String serviceTag;
  String userData;
  String launchIndex;
  Long networkBytes = 0l;
  Long blockBytes = 0l;
  ArrayList<String> groupNames = new ArrayList<String>();
  ArrayList<AttachedVolume> volumes = new ArrayList<AttachedVolume>();
  String placement;
  String platform;
  String bundleTaskStateName;  
  String createImageStateName;
  
  ArrayList<String> productCodes = new ArrayList<String>();
  
  @Override
  public String toString( ) {
    return "VmInfo ${reservationId} ${instanceId} ${ownerId} ${stateName} ${instanceType} ${imageId} ${kernelId} ${ramdiskId} ${launchIndex} ${serviceTag} ${netParams} ${volumes}";
  }
  
  
}

public class VmRunType extends EucalyptusMessage {
  
  /** these are for more convenient binding later on but really should be done differently... sigh    **/
  
  String reservationId, userData, platform;
  int min, max, vlan, launchIndex;
  
  VmTypeInfo vmTypeInfo;
  VmKeyInfo keyInfo;
  
  List<String> instanceIds = new ArrayList<String>();
  List<String> uuids = new ArrayList<String>();
  List<String> macAddresses = new ArrayList<String>();
  List<String> networkNames = new ArrayList<String>();
  ArrayList<String> networkIndexList = new ArrayList<String>();
  
  def VmRunType() {
  }
  
  def VmRunType(final String reservationId, final String userData, final int amount,
  final VmTypeInfo vmTypeInfo, final VmKeyInfo keyInfo, final String platform,
  final List<String> instanceIds, final List<String> macAddresses,
  final int vlan, final List<String> networkNames, final List<String> networkIndexList, final List<String> uuids ) {
    this.reservationId = reservationId;
    this.userData = userData;
    this.min = amount;
    this.max = amount;
    this.vlan = vlan;
    this.vmTypeInfo = vmTypeInfo;
    this.keyInfo = keyInfo;
    this.instanceIds = instanceIds;
    this.macAddresses = macAddresses;
    this.networkNames = networkNames;
    this.networkIndexList = networkIndexList;
    this.uuids = uuids;
    this.platform = platform;
  }
  
  def VmRunType(RunInstancesType request) {
    this.setUserId( msg.getUserId( ) );
    this.setEffectiveUserId( msg.getEffectiveUserId( ) );
    this.setCorrelationId( msg.getCorrelationId( ) );
  }
  
  @Override
  public String toString( ) {
    return String.format(
    "VmRunType [instanceIds=%s, keyInfo=%s, launchIndex=%s, max=%s, min=%s, networkIndexList=%s, networkNames=%s, reservationId=%s, userData=%s, vlan=%s, vmTypeInfo=%s]",
    this.instanceIds, this.keyInfo, this.launchIndex, 
    this.max, this.min, this.networkIndexList, this.networkNames, this.reservationId,
    this.userData, this.vlan, this.vmTypeInfo );
  }  
  
}

public class VirtualBootRecord extends EucalyptusData implements Cloneable {
  String id = "none";
  String resourceLocation = "none";
  String type;
  String guestDeviceName = "none";
  Long size = -1l;
  String format = "none";

  def VirtualBootRecord() {}
  
  def VirtualBootRecord(final id, final resourceLocation, final type, final guestDeviceName, final size, final format) {
	  this.id = id;
	  this.resourceLocation = resourceLocation;
	  this.type = type;
	  this.guestDeviceName = guestDeviceName;
	  this.size = size;
	  this.format = format;
  }
  
  public boolean isBlockStorage() {
    return "ebs".equals( this.type );
  }
  
  public Object clone( ) {
    return super.clone();
  }
}

public class VmKeyInfo extends EucalyptusData {
  
  String name = "";
  String value = "";
  String fingerprint = "";

  def VmKeyInfo() {}
  
  def VmKeyInfo(final name, final value, final fingerprint) {
    this.name = name;
    this.value = value;
    this.fingerprint = fingerprint;
  }
  
  @Override
  public String toString( ) {
    return String.format( "VmKeyInfo [fingerprint=%s, name=%s, value=%s]", this.fingerprint, this.name, this.value );
  }  
}

public class NodeInfo implements Comparable {
  String iqn;
  String serviceTag;
  String name;
  Boolean hasClusterCert = false;
  Boolean hasNodeCert = false;
  Date lastSeen;
  NodeCertInfo certs = new NodeCertInfo();
  NodeLogInfo logs = new NodeLogInfo();
  
  def NodeInfo() {}
  
  def NodeInfo(final String serviceTag ) {
    this.serviceTag = serviceTag;
    this.name = (new URI(this.serviceTag)).getHost();
    this.lastSeen = new Date();
    this.certs.setServiceTag(this.serviceTag);
    this.logs.setServiceTag(this.serviceTag);
  }

    def NodeInfo(final NodeType nodeType) {
    this.serviceTag = nodeType.getServiceTag( );
    this.iqn = nodeType.getIqn( );
    this.name = (new URI(this.serviceTag)).getHost();
    this.lastSeen = new Date();
    this.certs.setServiceTag(this.serviceTag);
    this.logs.setServiceTag(this.serviceTag);
  }
  
  def NodeInfo(NodeInfo orig, final NodeCertInfo certs) {
    this(orig.serviceTag);
    this.logs = orig.logs;
    this.certs = certs;
  }
  
  def NodeInfo(NodeInfo orig, final NodeLogInfo logs) {
    this(orig.serviceTag);
    this.certs = orig.certs;
    this.logs = logs;
  }
  
  def NodeInfo(final NodeCertInfo certs) {
    this(certs.getServiceTag());
    this.certs = certs;
  }
  
  def NodeInfo(final NodeLogInfo logs) {
    this(logs.getServiceTag());
    this.logs = logs;
  }
  
  
  public void touch() {
    this.lastSeen = new Date();
  }
  
  public int compareTo(Object o) {
    return this.serviceTag.compareTo(((NodeInfo) o).serviceTag);
  }
  
  boolean equals(final o) {
    if ( this.is( o ) ) return true;
    if ( !(o instanceof NodeInfo) ) return false;
    NodeInfo nodeInfo = (NodeInfo) o;
    if ( !serviceTag.equals(nodeInfo.serviceTag) ) return false;
    return true;
  }
  
  int hashCode() {
    return serviceTag.hashCode();
  }
  
  @Override
  public String toString( ) {
    return "NodeInfo name=${name} lastSeen=${lastSeen} serviceTag=${serviceTag} iqn=${iqn}";
  }

}
