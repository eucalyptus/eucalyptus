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
package edu.ucsb.eucalyptus.cloud


import edu.ucsb.eucalyptus.msgs.*
import com.eucalyptus.records.EventType;
import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentSkipListSet
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.HasName;
import com.eucalyptus.records.*;
import com.google.common.collect.*;

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
public class VmAllocationInfo extends EucalyptusMessage {
  
  RunInstancesType request;
  RunInstancesResponseType reply;
  byte[] userData;
  Long reservationIndex;
  String reservationId;
  VmImageInfo imageInfo;
  VmKeyInfo keyInfo;
  VmTypeInfo vmTypeInfo;
  
  List<Network> networks = new ArrayList<Network>();
  
  List<ResourceToken> allocationTokens = new ArrayList<ResourceToken>();
  List<String> addresses = new ArrayList<String>();
  ArrayList<Integer> networkIndexList = new ArrayList<Integer>();
  
  def VmAllocationInfo() {
  }
  
  def VmAllocationInfo(final RunInstancesType request) {
    this.request = request;
    this.reply = request.getReply();
    this.setCorrelationId( request.getCorrelationId() )
  }
  
  public EucalyptusMessage getRequestMessage() {
    return this.getRequest();
  }
}

public class VmDescribeType extends EucalyptusMessage {
  
  ArrayList<String> instancesSet = new ArrayList<String>();
}
public class VmDescribeResponseType extends EucalyptusMessage {
  
  String originCluster;
  ArrayList<VmInfo> vms = new ArrayList<VmInfo>();
}

public class VmRunResponseType extends EucalyptusMessage {
  
  ArrayList<VmInfo> vms = new ArrayList<VmInfo>();
}

public class VmInfo extends EucalyptusData {
  
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
  ArrayList<String> groupNames = new ArrayList<String>();
  ArrayList<AttachedVolume> volumes = new ArrayList<AttachedVolume>();
  String placement;
    
  ArrayList<String> productCodes = new ArrayList<String>();
  
  @Override
  public String toString( ) {
    return String.format(
    "VmInfo [groupNames=%s, imageId=%s, instanceId=%s, instanceType=%s, kernelId=%s, keyValue=%s, launchIndex=%s, launchTime=%s, netParams=%s, ownerId=%s, placement=%s, productCodes=%s, ramdiskId=%s, reservationId=%s, serviceTag=%s, stateName=%s, userData=%s, volumes=%s]",
    this.groupNames, this.imageId, this.instanceId, this.instanceType, this.kernelId,
    this.keyValue, this.launchIndex, this.launchTime, this.netParams, 
    this.ownerId, this.placement, this.productCodes, this.ramdiskId, this.reservationId,
    this.serviceTag, this.stateName, this.userData, this.volumes );
  }
  
  
}

public class VmRunType extends EucalyptusMessage {
  
  /** these are for more convenient binding later on but really should be done differently... sigh    **/
  
  String reservationId, userData;
  int min, max, vlan, launchIndex;
  
  VmImageInfo imageInfo;
  VmTypeInfo vmTypeInfo;
  VmKeyInfo keyInfo;
  
  List<String> instanceIds = new ArrayList<String>();
  List<String> macAddresses = new ArrayList<String>();
  List<String> networkNames = new ArrayList<String>();
  ArrayList<String> networkIndexList = new ArrayList<String>();
  
  def VmRunType() {
  }
  
  def VmRunType(final String reservationId, final String userData, final int amount,
  final VmImageInfo imageInfo, final VmTypeInfo vmTypeInfo, final VmKeyInfo keyInfo,
  final List<String> instanceIds, final List<String> macAddresses,
  final int vlan, final List<String> networkNames, final List<String> networkIndexList ) {
    this.reservationId = reservationId;
    this.userData = userData;
    this.min = amount;
    this.max = amount;
    this.vlan = vlan;
    this.imageInfo = imageInfo;
    this.vmTypeInfo = vmTypeInfo;
    this.keyInfo = keyInfo;
    this.instanceIds = instanceIds;
    this.macAddresses = macAddresses;
    this.networkNames = networkNames;
    this.networkIndexList = networkIndexList;
  }
  
  def VmRunType(RunInstancesType request) {
    this.effectiveUserId = request.effectiveUserId;
    this.correlationId = request.correlationId;
    this.userId = request.userId;
  }
  
  public VmRunType(RunInstancesType request, String reservationId, String userData,
  int amount, VmImageInfo imageInfo, VmTypeInfo vmTypeInfo, VmKeyInfo keyInfo,
  ArrayList<String> instanceIds, List<String> macAddresses, int vlan,
  List<String> networkNames, ArrayList<String> networkIndexList) {
    this.correlationId = request.correlationId;
    this.userId = request.userId;
    this.effectiveUserId = request.effectiveUserId;
    this.reservationId = reservationId;
    this.userData = userData;
    this.min = amount;
    this.max = amount;
    this.vlan = vlan;
    this.imageInfo = imageInfo;
    this.vmTypeInfo = vmTypeInfo;
    this.keyInfo = keyInfo;
    this.instanceIds = instanceIds;
    this.macAddresses = macAddresses;
    this.networkNames = networkNames;
    this.networkIndexList = networkIndexList;
  }
  
  @Override
  public String toString( ) {
    return String.format(
    "VmRunType [imageInfo=%s, instanceIds=%s, keyInfo=%s, launchIndex=%s, macAddresses=%s, max=%s, min=%s, networkIndexList=%s, networkNames=%s, reservationId=%s, userData=%s, vlan=%s, vmTypeInfo=%s]",
    this.imageInfo, this.instanceIds, this.keyInfo, this.launchIndex, this.macAddresses,
    this.max, this.min, this.networkIndexList, this.networkNames, this.reservationId,
    this.userData, this.vlan, this.vmTypeInfo );
  }  
  
  
}

public class VmImageInfo {
  
  String imageId;
  String kernelId;
  String ramdiskId;
  String imageLocation;
  String kernelLocation;
  String ramdiskLocation;
  ArrayList<String> productCodes = new ArrayList<String>();
  ArrayList<String> ancestorIds = new ArrayList<String>();
  Long size = 0l;
  
  def VmImageInfo(final imageId, final kernelId, final ramdiskId, final imageLocation, final kernelLocation, final ramdiskLocation, final productCodes) {
    this.imageId = imageId;
    this.kernelId = kernelId;
    this.ramdiskId = ramdiskId;
    this.imageLocation = imageLocation;
    this.kernelLocation = kernelLocation;
    this.ramdiskLocation = ramdiskLocation;
    this.productCodes = productCodes;
  }
  
  def VmImageInfo() {
  }
  
  @Override
  public String toString( ) {
    return String.format(
    "VmImageInfo [ancestorIds=%s, imageId=%s, imageLocation=%s, kernelId=%s, kernelLocation=%s, productCodes=%s, ramdiskId=%s, ramdiskLocation=%s, size=%s]",
    this.ancestorIds, this.imageId, this.imageLocation, this.kernelId, this.kernelLocation,
    this.productCodes, this.ramdiskId, this.ramdiskLocation, this.size );
  }
  
  
}

public class VmKeyInfo {
  
  String name = "";
  String value = "";
  String fingerprint = "";
  
  def VmKeyInfo(final name, final value, final fingerprint) {
    this.name = name;
    this.value = value;
    this.fingerprint = fingerprint;
  }
  
  def VmKeyInfo() {
  }
  
  @Override
  public String toString( ) {
    return String.format( "VmKeyInfo [fingerprint=%s, name=%s, value=%s]", this.fingerprint, this.name, this.value );
  }
  
  
}

public class Network implements HasName<Network> {
  private static Logger LOG = Logger.getLogger( Network.class );
  String name;
  String networkName;
  String userName;
  Integer max;
  ArrayList<PacketFilterRule> rules = new ArrayList<PacketFilterRule>();
  private ConcurrentMap<String, NetworkToken> clusterTokens = new ConcurrentHashMap<String,NetworkToken>();
  private NavigableSet<Integer> availableNetworkIndexes = new ConcurrentSkipListSet<Integer>();
  private NavigableSet<Integer> assignedNetworkIndexes = new ConcurrentSkipListSet<Integer>();
  AtomicInteger vlan;
  
  def Network() {
  }
  
  def Network(final String userName, final String networkName) {
    this.userName = userName;
    this.networkName = networkName;
    this.name = this.userName + "-" + this.networkName;
    try {
      Network me = Networks.getInstance().lookup( this.networkName );
      this.max = me.max;
    } catch (Throwable t) {
      this.max = 2048;
    }
    for( int i = 2; i < max; i++ ) {//FIXME: potentially a network can be more than a /24. update w/ real constraints at runtime.
      this.availableNetworkIndexes.add( i );
    }
    this.vlan = new AtomicInteger(Integer.valueOf(0));
  }
  
  public void setVlan( Integer i ) {
    this.setVlanIfZero( i );
  }
  public boolean initVlan( Integer i ) {
    return this.vlan.compareAndSet(Integer.valueOf(0),i); 
  }
  public Integer getVlan() {
    return this.vlan.get();
  }
  public void extantNetworkIndex( String cluster, Integer index ) {
    if( index < 2 ) {
      this.availableNetworkIndexes.remove( index );
    } else {
      if( !this.assignedNetworkIndexes.contains( index ) && this.availableNetworkIndexes.remove( index ) ) {
        EventRecord.caller( this.getClass( ), EventType.TOKEN_ALLOCATED, "network=${this.name}","cluster=${cluster}","networkIndex=${index}").debug( );
        this.assignedNetworkIndexes.add( index );
        NetworkToken token = this.getClusterToken( cluster );
        token.indexes.add( index );
      }
    }
  }
  
  public NetworkToken getClusterToken( String cluster ) {
    NetworkToken token = this.clusterTokens.putIfAbsent( cluster, new NetworkToken( cluster, this.userName, this.networkName, this.vlan.get() ) );
    if( token == null ) token = this.clusterTokens.get( cluster );
    return token;
  }
  
  public NetworkToken createNetworkToken( String cluster ) {
    getClusterToken( cluster );
    return new NetworkToken( cluster, this.userName, this.networkName, this.vlan.get() );
  }
  
  public void trim( Integer max ) {
    this.max = max;
    this.availableNetworkIndexes.tailSet( max-1, true ).clear( );
    this.availableNetworkIndexes.headSet( 2 ).clear();
  }
  
  public void extantNetworkIndexes( List<Integer> indexes ) {
    indexes.each{ this.extantNetworkIndex( Integer.parseInt( it ) ); };
  }
  
  public Integer allocateNetworkIndex( String cluster ) {
    Integer nextIndex = this.availableNetworkIndexes.pollFirst( );
    if( nextIndex == null ) { 
      EventRecord.caller( this.getClass( ), EventType.TOKEN_RESERVED, "network=${this.name}","cluster=${cluster}","networkIndex=${nextIndex}").debug( );
    } else {
      this.assignedNetworkIndexes.add( nextIndex );
      this.getClusterToken( cluster )?.getIndexes().add( nextIndex );
      EventRecord.caller( this.getClass( ), EventType.TOKEN_RESERVED, "network=${this.name}","cluster=${cluster}","networkIndex=${nextIndex}").debug( );
    }
    return nextIndex;
  }
  
  public void returnNetworkIndexes( Collection<Integer> indexes ) {
    indexes.each{ this.returnNetworkIndex( it ); };
    LOG.debug( this.toString( ) );
  }
  
  public void returnNetworkIndex( Integer index ) {
    EventRecord.caller( this.getClass( ), EventType.TOKEN_RETURNED, "network=${this.name}","networkIndex=${index}").debug( );
    this.assignedNetworkIndexes.remove( index );
    this.clusterTokens.values().each { 
      it.getIndexes().remove( index );
    }
    if( index < 2 ) return;
    this.availableNetworkIndexes.add( index );
  }
  
  public NetworkToken addTokenIfAbsent(NetworkToken token) {
    if( this.vlan.get() == 0 ) {
      this.vlan.compareAndSet(0,token.getVlan( ));
    }
    NetworkToken clusterToken = this.clusterTokens.putIfAbsent( token.getCluster(), token );
    if( clusterToken == null ) clusterToken = this.clusterTokens.get( token.getCluster() );
    return clusterToken;
  }
  
  public boolean hasToken(String cluster) {
    return this.clusterTokens.containsKey(cluster);
  }
  
  public boolean hasTokens() {
    this.clusterTokens.values().each { NetworkToken it ->
      if(it.getIndexes().isEmpty()) {
        this.removeToken(it.getCluster());
      }
    }    
    return !this.clusterTokens.values( ).isEmpty( );
  }
  
  public void removeToken(String cluster) {
    this.clusterTokens.remove(cluster);
  }
  
  public boolean isPeer( String peerName, String peerNetworkName ) {
    return (Boolean) this.rules.collect{ pf -> pf.peers.contains( new VmNetworkPeer( peerName, peerNetworkName )) }.max();
  }
  
  public String getName() {
    return this.name;
  }

  public int compareTo(final Network that) {
    return this.getName().compareTo(that.getName());
  }
  
  @Override
  public String toString( ) {
    return String.format("Network [available=%s, assigned=%s, name=%s, networkName=%s, clusterTokens=%s, rules=%s, userName=%s]",
                         LogUtil.rangedIntegerList( this.availableNetworkIndexes ), LogUtil.rangedIntegerList( this.assignedNetworkIndexes ), 
                         this.name, this.networkName, this.clusterTokens, this.rules, this.userName );
  }
  
  
}

public class NetworkToken implements Comparable {
  
  String networkName;
  String cluster;
  Integer vlan;
  NavigableSet<Integer> indexes = new ConcurrentSkipListSet<Integer>( );
  String userName;
  String name;
  
  def NetworkToken(final String cluster, final String userName, final String networkName, final int vlan ) {
    this.networkName = networkName;
    this.cluster = cluster;
    this.vlan = vlan;
    this.userName = userName;
    this.name = this.userName + "-" + this.networkName;
  }
  
  @Override
  boolean equals(final Object o) {
    if ( this.is( o ) ) return true;
    if ( !(o instanceof NetworkToken) ) return false;
    NetworkToken that = (NetworkToken) o;
    
    if ( !cluster.equals(that.cluster) ) return false;
    if ( !networkName.equals(that.networkName) ) return false;
    if ( !userName.equals(that.userName) ) return false;
    
    return true;
  }
  
  @Override
  int hashCode() {
    int result;
    
    result = networkName.hashCode();
    result = 31 * result + cluster.hashCode();
    result = 31 * result + userName.hashCode();
    return result;
  }
  
  
  
  @Override
  public int compareTo(Object o) {
    NetworkToken that = (NetworkToken) o;
    return (!this.cluster.equals(that.cluster) && (this.getVlan().equals( that.getVlan() ) ) ) ? this.getVlan() - that.getVlan() : this.cluster.compareTo(that.cluster);
  }
  
  @Override
  public String toString( ) {
    return "NetworkToken ${cluster} ${name} ${vlan} ${indexes}";
  }
}

public class ResourceToken implements Comparable {
  
  String cluster;
  String correlationId;
  String userName;
  ArrayList<String> instanceIds = new ArrayList<String>();
  ArrayList<String> addresses = new ArrayList<String>();
  ArrayList<NetworkToken> networkTokens = new ArrayList<NetworkToken>();
  Integer amount;
  String vmType;
  Date creationTime;
  Integer sequenceNumber;
  
  public ResourceToken(final String cluster, final String correlationId, final String userName, final int amount, final int sequenceNumber, final String vmType) {
    this.cluster = cluster;
    this.correlationId = correlationId;
    this.userName = userName;
    this.amount = amount;
    this.sequenceNumber = sequenceNumber;
    this.creationTime = Calendar.getInstance().getTime();
    this.vmType = vmType;
  }
  
  public NetworkToken getPrimaryNetwork() {
    return this.networkTokens.size()>0?this.networkTokens.get(0):null;
  }
  
  @Override
  public boolean equals(final Object o) {
    if ( this.is( o ) ) return true;
    if ( !(o instanceof ResourceToken) ) return false;
    
    ResourceToken that = (ResourceToken) o;
    
    if ( !amount.equals( that.amount ) ) return false;
    if ( !cluster.equals(that.cluster) ) return false;
    if ( !correlationId.equals(that.correlationId) ) return false;
    if ( !creationTime.equals(that.creationTime) ) return false;
    
    return true;
  }
  
  @Override
  public int hashCode() {
    int result = cluster.hashCode();
    result = 31 * result + correlationId.hashCode();
    result = 31 * result + amount;
    result = 31 * result + creationTime.hashCode();
    return result;
  }
  
  @Override
  public int compareTo(final Object o) {
    ResourceToken that = (ResourceToken) o;
    return this.getSequenceNumber() - that.getSequenceNumber();
  }
  
  @Override
  public String toString( ) {
    return String.format(
    "ResourceToken [addresses=%s, amount=%s, cluster=%s, correlationId=%s, creationTime=%s, instanceIds=%s, networkTokens=%s, sequenceNumber=%s, userName=%s, vmType=%s]",
    this.addresses, this.amount, this.cluster, this.correlationId, this.creationTime,
    this.instanceIds, this.networkTokens, this.sequenceNumber, this.userName, this.vmType );
  }
  
}

public class NodeInfo implements Comparable {
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
    return "NodeInfo name=${name} lastSeen=${lastSeen} serviceTag=${serviceTag}";
  }
}
