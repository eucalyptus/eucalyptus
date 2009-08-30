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

import org.jibx.runtime.BindingDirectory
import org.jibx.runtime.IBindingFactory
import org.jibx.runtime.IMarshallingContext

public class INTERNAL extends EucalyptusMessage {
  
  def INTERNAL() {
    super();
    this.userId = "eucalyptus";
    this.effectiveUserId = "eucalyptus";
  }
}

public class HeartbeatType extends EucalyptusMessage {
  ArrayList<HeartbeatComponentType> components = new ArrayList<HeartbeatComponentType>();
}
public class HeartbeatResponseType extends EucalyptusMessage {}
public class HeartbeatComponentType extends EucalyptusData {
  String component;
  String name;
  public HeartbeatComponentType( String component, String name ) {
    super( );
    this.component = component;
    this.name = name;
  }
}

public class StorageStateType extends EucalyptusMessage{
  private String name;
  private String volumesPath;
  private Integer maxVolumeSizeInGB;
  private Integer totalVolumesSizeInGB;
  private String storageInterface;
  private Boolean zeroFillVolumes;
  
  def StorageStateType() {
  }
  
  def StorageStateType(final name, final volumesPath, final maxVolumeSizeInGB,
  final totalVolumesSizeInGB, final storageInterface, final zeroFillVolumes) {
    this.name = name;
    this.volumesPath = volumesPath;
    this.maxVolumeSizeInGB = maxVolumeSizeInGB;
    this.totalVolumesSizeInGB = totalVolumesSizeInGB;
    this.storageInterface = storageInterface;
    this.zeroFillVolumes = zeroFillVolumes;
  }
}

public class WalrusStateType extends EucalyptusMessage{
  private String name;
  private String bucketsRootDirectory;
  private Integer maxBucketsPerUser;
  private Integer maxBucketSizeInMB;
  private Integer maxCacheSizeInMB;
  private Integer snapshotsTotalInGB;
  
  def WalrusStateType() {
  }
  
  def StorageStateType(final name, final bucketsRootDirectory, final maxBucketsPerUser,
  final maxBucketSizeInMB, final maxCacheSizeInMB, final snapshotsTotalInGB) {
    this.name = name;
    this.bucketsRootDirectory = bucketsRootDirectory;
    this.maxBucketsPerUser = maxBucketsPerUser;
    this.maxBucketSizeInMB = maxBucketSizeInMB;
    this.maxCacheSizeInMB = maxCacheSizeInMB;
    this.snapshotsTotalInGB = snapshotsTotalInGB;
  }
}


public class EucalyptusMessage implements Cloneable, Serializable {
  
  String correlationId;
  String userId;
  String effectiveUserId;
  boolean _return;
  String statusMessage;
  
  public EucalyptusMessage() {
    this.correlationId = UUID.randomUUID();
  }
  
  public EucalyptusMessage( EucalyptusMessage msg ) {
    this();
    this.userId = msg.userId;
    this.effectiveUserId = msg.effectiveUserId;
    this.correlationId = msg.correlationId;
  }
  
  def EucalyptusMessage(final String userId) {
    this();
    this.userId = userId;
    this.effectiveUserId = userId;
  }
  
  
  
  
  public MetaClass getMetaClass() {
    return metaClass;
  }
  
  public String getEffectiveUserId() {
    if ( isAdministrator() ) return "eucalyptus";
    return effectiveUserId;
  }
  
  public boolean isAdministrator() {
    return "eucalyptus".equals(this.effectiveUserId);
  }
  
  public String toString() {
    ByteArrayOutputStream temp = new ByteArrayOutputStream();
    Class targetClass = this.getClass();
    while ( !targetClass.getSimpleName().endsWith("Type") ) targetClass = targetClass.getSuperclass();
    IBindingFactory bindingFactory = BindingDirectory.getFactory("msgs_eucalyptus_ucsb_edu", targetClass);
    IMarshallingContext mctx = bindingFactory.createMarshallingContext();
    mctx.setIndent(2);
    mctx.marshalDocument(this, "UTF-8", null, temp);
    return temp.toString();
  }
  
  public String toString(String namespace) {
    ByteArrayOutputStream temp = new ByteArrayOutputStream();
    Class targetClass = this.getClass();
    while ( !targetClass.getSimpleName().endsWith("Type") ) targetClass = targetClass.getSuperclass();
    IBindingFactory bindingFactory = BindingDirectory.getFactory(namespace, targetClass);
    IMarshallingContext mctx = bindingFactory.createMarshallingContext();
    mctx.setIndent(2);
    mctx.marshalDocument(this, "UTF-8", null, temp);
    return temp;
  }
  
  public Object clone() {
    return super.clone();
  }
  
  public EucalyptusMessage getReply() {
    Class msgClass = this.getClass();
    if ( !this.getClass().getSimpleName().endsWith("Type") )
      msgClass = msgClass.getSuperclass();
    Class responseClass = Class.forName(msgClass.getName().replaceAll("Type", "") + "ResponseType");
    EucalyptusMessage reply = (EucalyptusMessage) responseClass.newInstance();
    reply.setCorrelationId(this.getCorrelationId());
    reply.setUserId(this.getUserId());
    reply.setEffectiveUserId(this.getEffectiveUserId());
    return reply;
  }
  
  public Class getReplyType() {
    Class msgClass = this.getClass();
    if ( !this.getClass().getSimpleName().endsWith("Type") )
      msgClass = msgClass.getSuperclass();
    Class responseClass = Class.forName(msgClass.getName().replaceAll("Type", "") + "ResponseType");
    return responseClass;
  }
  
  
}
public class EucalyptusErrorMessageType extends EucalyptusMessage {
  
  String source;
  String message;
  String requestType = "not available";
  Throwable exception;
  
  public EucalyptusErrorMessageType() {
  }
  
  public EucalyptusErrorMessageType(String source, String message) {
    this.source = source;
    this.message = message;
  }
  
  public EucalyptusErrorMessageType(String source, EucalyptusMessage msg, String message) {
    this(source, message);
    this.correlationId = msg.getCorrelationId();
    this.userId = msg.getUserId();
    this.requestType = msg != null ? msg.getClass().getSimpleName() : this.requestType;
  }
  
  public String toString() {
    return String.format("SERVICE: %s PROBLEM: %s MSG-TYPE: %s", this.source, this.message, this.requestType);
  }
  
}

public class EucalyptusData implements Cloneable, Serializable {
  public MetaClass getMetaClass() {
    return metaClass;
  }
  
  public String toString() {
    return this.getProperties().toMapString();
  }
  
  public Object clone(){
    return super.clone();
  }
}
/** *******************************************************************************/
public class DescribeResourcesType extends EucalyptusMessage {
  
  ArrayList<VmTypeInfo> instanceTypes = new ArrayList<VmTypeInfo>();
}
public class DescribeResourcesResponseType extends EucalyptusMessage {
  
  ArrayList<ResourceType> resources = new ArrayList<ResourceType>();
  ArrayList<String> serviceTags = new ArrayList<String>();
}

public class VmTypeInfo extends EucalyptusData {
  
  String name;
  Integer memory;
  Integer disk;
  Integer cores;
  
  def VmTypeInfo(){
  }
  
  def VmTypeInfo(final name, final memory, final disk, final cores) {
    this.name = name;
    this.memory = memory;
    this.disk = disk;
    this.cores = cores;
  }
  
  @Override
  public String toString() {
    return "VmTypeInfo{" +
    "name='" + name + '\'' +
    ", memory=" + memory +
    ", disk=" + disk +
    ", cores=" + cores +
    '}';
  }
  
}
public class ResourceType extends EucalyptusData {
  
  VmTypeInfo instanceType;
  int maxInstances;
  int availableInstances;
}
public class NetworkConfigType extends EucalyptusData {
  String macAddress;
  String ignoredMacAddress;
  String ipAddress;
  String ignoredPublicIp;
  int vlan;
  
  def NetworkConfigType() {
  }
  
  @Override
  public String toString() {
    return "NetworkConfigType{" +
    "macAddress='" + macAddress + '\'' +
    ", ipAddress='" + ipAddress + '\'' +
    ", publicIp='" + ignoredPublicIp + '\'' +
    ", vlan=" + vlan +
    '}';
  }
  
  
}

public class NetworkParameters extends EucalyptusData {
  String privateMacAddress;
  String publicMacAddress;
  int macLimit;
  int vlan;
}

public class PacketFilterRule extends EucalyptusData {
  public static String ACCEPT = "firewall-open";
  public static String DENY = "firewall-close";
  
  public static PacketFilterRule revoke( PacketFilterRule  existingRule ) {
    PacketFilterRule pf = new PacketFilterRule();
    pf.destUserName = existingRule.getDestUserName();
    pf.destNetworkName = existingRule.getDestNetworkName();
    pf.policy = DENY;
    pf.portMin = existingRule.getPortMin();
    pf.portMax = existingRule.getPortMax();
    pf.protocol = existingRule.getProtocol();
    pf.setPeers( existingRule.getPeers() );
    pf.setSourceCidrs( existingRule.getSourceCidrs() );
    pf.setSourceNetworkNames( existingRule.getSourceNetworkNames() );
    pf.setSourceUserNames( existingRule.getSourceUserNames() );
    return pf;
  }
  String destUserName;
  String destNetworkName;
  String policy = "firewall-open";
  String protocol;
  int portMin;
  int portMax;
  ArrayList<String> sourceCidrs = new ArrayList<String>();
  ArrayList<VmNetworkPeer> peers = new ArrayList<VmNetworkPeer>();
  ArrayList<String> sourceNetworkNames = new ArrayList<String>();
  ArrayList<String> sourceUserNames = new ArrayList<String>();
  
  def PacketFilterRule(final destUserName, final destNetworkName, final protocol, final portMin, final portMax) {
    this.destUserName = destUserName;
    this.destNetworkName = destNetworkName;
    this.protocol = protocol;
    this.portMin = portMin;
    this.portMax = portMax;
  }
  
  def PacketFilterRule(){
  }
  
  @Override
  public String toString() {
    return "PacketFilterRule{" +
    "destUserName='" + destUserName + '\'' +
    "destNetworkName='" + destNetworkName + '\'' +
    ", policy='" + policy + '\'' +
    ", protocol='" + protocol + '\'' +
    ", portMin=" + portMin +
    ", portMax=" + portMax +
    ", sourceCidrs=" + sourceCidrs +
    ", peers=" + peers +
    ", sourceNetworkNames=" + sourceNetworkNames +
    ", sourceUserNames=" + sourceUserNames +
    '}';
  }
  
  
  public void addPeer( String queryKey, String groupName ) {
    VmNetworkPeer peer = new VmNetworkPeer( queryKey, groupName );
    this.peers.add(peer);
    this.sourceNetworkNames.add( peer.getSourceNetworkName() );
    this.sourceUserNames.add(peer.userName);
  }
  
}

public class VmNetworkPeer  extends EucalyptusData {
  
  String userName;
  String sourceNetworkName;
  
  def VmNetworkPeer() {
  }
  
  def VmNetworkPeer(final userName, final sourceNetworkName) {
    this.userName = userName;
    this.sourceNetworkName = sourceNetworkName;
  }
  
  
  
  boolean equals(final Object o) {
    if ( this.is(o) ) return true;
    
    if ( !o || getClass() != o.class ) return false;
    
    VmNetworkPeer that = (VmNetworkPeer) o;
    
    if ( sourceNetworkName ? !sourceNetworkName.equals(that.sourceNetworkName) : that.sourceNetworkName != null ) return false;
    if ( userName ? !userName.equals(that.userName) : that.userName != null ) return false;
    
    return true;
  }
  
  int hashCode() {
    int result;
    
    result = (userName ? userName.hashCode() : 0);
    result = 31 * result + (sourceNetworkName ? sourceNetworkName.hashCode() : 0);
    return result;
  }
}



public class EventRecord extends EucalyptusMessage {
  
  String host = "cloud";
  String service;
  long timestamp = System.currentTimeMillis();
  String eventUserId;
  String eventCorrelationId;
  String eventId;
  String other;
  
  def EventRecord(final service, final eventUserId, final eventCorrelationId, final eventId, final other) {
    this.service = service;
    this.eventUserId = eventUserId;
    this.eventCorrelationId = eventCorrelationId;
    this.eventId = eventId;
    this.other = other;
  }
  
  public EventRecord() {
  }
  
  public String toString() {
    return String.format("%s/%s:%s:%s:%s:%7.4f:%s", this.host, this.service, this.eventUserId, this.eventCorrelationId, this.eventId, this.timestamp / 1000.0f, this.other != null ? this.other : "");
  }
  
  public static EventRecord create(final service, final eventUserId, final eventCorrelationId, final eventId, final other) {
    return new EventRecord(service, eventUserId, eventCorrelationId, eventId, other);
  }
  
}

public class GetLogsType extends EucalyptusMessage implements Comparable {
  String serviceTag;
  def GetLogsType(){
  }
  def GetLogsType(final serviceTag) {
    this.serviceTag = serviceTag;
  }
  public int compareTo(Object o) {
    return this.serviceTag.compareTo(((GetLogsType)o).serviceTag);
  }
}
public class GetLogsResponseType extends EucalyptusMessage {
  NodeLogInfo logs = new NodeLogInfo();
}
public class GetKeysType extends EucalyptusMessage implements Comparable {
  String serviceTag;
  def GetKeysType(){
  }
  def GetKeysType(final serviceTag) {
    this.serviceTag = serviceTag;
  }
  
  public int compareTo(Object o) {
    return this.serviceTag.compareTo(((GetKeysType)o).serviceTag);
  }
  
}
public class GetKeysResponseType extends EucalyptusMessage {
  NodeCertInfo certs = new NodeCertInfo();
}

public class NodeCertInfo extends EucalyptusData implements Comparable {
  String serviceTag;
  String ccCert = "";
  String ncCert = "";
  
  public int compareTo(Object o) {
    return this.serviceTag.compareTo(((NodeCertInfo)o).serviceTag);
  }
  
  @Override
  public String toString() {
    return "NodeCertInfo{" +
    "serviceTag='" + serviceTag.replaceAll("services/EucalyptusNC","") + '\'' +
    ", ccCert='" + ccCert + '\'' +
    ", ncCert='" + ncCert + '\'' +
    '}';
  }
  
}

public class NodeLogInfo extends EucalyptusData implements Comparable {
  String serviceTag;
  String ccLog = "";
  String ncLog = "";
  String httpdLog = "";
  String axis2Log = "";
  
  public int compareTo(Object o) {
    return this.serviceTag.compareTo(((NodeLogInfo)o).serviceTag);
  }
  
}

public class HeartbeatMessage extends EucalyptusMessage implements Cloneable, Serializable {
  String heartbeatId;
  
  def HeartbeatMessage(final String heartbeatId) {
    this.heartbeatId = heartbeatId;
  }
  
  def HeartbeatMessage() {
  }
  
  
}

public class VmBundleMessage extends EucalyptusMessage {
}

public class DescribeBundleTasksType extends VmBundleMessage {
  ArrayList<String> bundleIds = new ArrayList<String>();
}
public class DescribeBundleTasksResponseType extends VmBundleMessage {
}

public class StatEventRecord extends EucalyptusMessage {
  
  protected String service = "Eucalyptus";
  protected String version = "Unknown";
  
  def StatEventRecord(final String service, final String version) {
    this.service = service;
    this.version = version;
  }
  
  def StatEventRecord() {
  }
  
  public String toString() {
    return String.format("%s", this.service);
  }
}

