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

@GroovyAddClassUUID
package edu.ucsb.eucalyptus.msgs;

import org.jboss.netty.handler.codec.http.HttpResponseStatus

import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.component.ComponentId
import com.eucalyptus.component.ServiceConfiguration
import com.eucalyptus.component.ServiceConfigurations
import com.eucalyptus.component.annotation.ComponentMessage
import com.eucalyptus.component.id.ComponentService
import com.eucalyptus.component.id.Eucalyptus
import com.eucalyptus.system.Threads
import com.eucalyptus.util.Exceptions

import edu.ucsb.eucalyptus.cloud.VirtualBootRecord
import com.eucalyptus.binding.HttpEmbedded


public class ComponentType extends EucalyptusData {
  String component;
  String name;
  String uri;
  public ComponentType( String component, String name, String uri ) {
    this.component = component;
    this.name = name;
    this.uri = uri;
  }
  public ComponentType( ) {
  }
  public ServiceConfiguration toConfiguration() {
    URI realUri = URI.create( this.getUri( ) );
    final ComponentId c = ComponentId.lookup( component );
    return ServiceConfigurations.createEphemeral( name, c, realUri );
  }
}
public class ComponentProperty extends EucalyptusData {
  private String type;
  private String displayName;
  private String value;
  private String qualifiedName;
 
  public ComponentProperty() {}
 
  public ComponentProperty(String type, String displayName, String value, String qualifiedName) {
    this.type = type;
    this.displayName = displayName;
    this.value = value;
    this.qualifiedName = qualifiedName;
  }
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }
  public String getQualifiedName() {
    return qualifiedName;
  }
  public void setQualifiedName(String qualifiedName) {
    this.qualifiedName = qualifiedName;
  }
  public String getDisplayName() {
    return displayName;
  }
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }
  public String getValue() {
    return value;
  }
  public void setValue(String value) {
    this.value = value;
  }
}
public class StorageStateType extends BaseMessage{
  private String name;
  def StorageStateType() {
  }
  
  def StorageStateType(final name) {
    this.name = name;
  }
}

public class WalrusStateType extends BaseMessage{
  private String name;
  
  def WalrusStateType() {
  }
  
  def WalrusStateType(final name) {
    this.name = name;
  }
}

/**
 * GRZE:WARN: anything inheriting from this is (and /should be/) treated as in the 'ec2' vendor namespace as far as the IAM implementation is concerned. 
 * There is no reason to annotate /any/ message which inherits from this class:
 * - to get vendor namespace use ComponentId.getVendorName()
 * - to get action use 
 */
@ComponentMessage(Eucalyptus.class)
public class EucalyptusMessage extends BaseMessage implements Cloneable, Serializable {
  
  public EucalyptusMessage() {
    super();
  }
  
  public EucalyptusMessage( EucalyptusMessage msg ) {
    this();
    regarding(msg);
    regardingUserRequest(msg);
    this.userId = msg.userId;
    this.effectiveUserId = msg.effectiveUserId;
    this.correlationId = msg.correlationId;
  }
  
  public  EucalyptusMessage(final String userId) {
    this();
    this.userId = userId;
    this.effectiveUserId = userId;
  }
  
  public MetaClass getMetaClass() {
    return metaClass;
  }
}

public class ExceptionResponseType extends BaseMessage {
  String source = "not available";
  String message = "not available";
  String requestType = "not available";
  Throwable exception;
  String error = "not available";
  HttpResponseStatus httpStatus = HttpResponseStatus.BAD_REQUEST;
  public ExceptionResponseType() {
  }
  public ExceptionResponseType( BaseMessage msg, String message, Throwable exception ) {
    this( msg, message, HttpResponseStatus.BAD_REQUEST, exception );
  }
  public ExceptionResponseType( BaseMessage msg, String message, HttpResponseStatus httpStatus, Throwable exception ) {
    this( msg, msg?.getClass()?.getSimpleName(), message, httpStatus, exception )
  }
  public ExceptionResponseType( BaseMessage msg, String requestType, String message, HttpResponseStatus httpStatus, Throwable exception ) {
    super( msg );
    this.httpStatus = httpStatus;
    this.source = exception.getClass( ).getCanonicalName( );
    this.message = message?:exception.getMessage()?:exception.getClass()
    this.requestType = requestType
    this.exception = exception;
    if( this.exception != null ) {
      this.error = Exceptions.string( exception );
    } else {
      this.error = Threads.currentStackString( );
    }
    this.set_return(false);
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
  
  public EucalyptusErrorMessageType(String source, BaseMessage msg, String message) {
    this(source, message);
    regardingUserRequest( msg );
    this.requestType = msg != null ? msg.getClass().getSimpleName() : this.requestType;
  }
  
  public String toString() {
    return String.format("SERVICE: %s PROBLEM: %s MSG-TYPE: %s", this.source, this.message, this.requestType);
  }
}

public class EucalyptusData implements BaseData {
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

public class DescribeSensorsType extends CloudClusterMessage {
    Integer historySize;
    Integer collectionIntervalTimeMs;
    @HttpParameterMapping (parameter = "sensorId")
    ArrayList<String> sensorIds = new ArrayList<String>();
    @HttpParameterMapping (parameter = "instanceId")
    ArrayList<String> instanceIds = new ArrayList<String>();

    DescribeSensorsType(){}
    
    DescribeSensorsType (Integer historySize, Integer collectionIntervalTimeMs, ArrayList<String> instanceIds ) {
      this.historySize = historySize;
      this.collectionIntervalTimeMs = collectionIntervalTimeMs;
      this.instanceIds = instanceIds;
    }
}

public class ModifyNode extends CloudClusterMessage {
    String stateName;
    String nodeName;
    ModifyNode(){}
   
    ModifyNode (String nodeName, String stateName ) {
      this.nodeName = nodeName;
      this.stateName = stateName;
    }
}

public class ModifyNodeResponse extends CloudClusterMessage {
    ModifyNodeResponse(){}
      
}

public class DescribeSensorsResponse extends CloudClusterMessage {
    
    ArrayList<SensorsResourceType> sensorsResources = new ArrayList<SensorsResourceType>();
    
}

public class SensorsResourceType extends EucalyptusData {
    String resourceName;
    String resourceType;
    String resourceUuid;
    
    ArrayList<MetricsResourceType> metrics = new ArrayList<MetricsResourceType>();
    
}

public class MetricsResourceType extends EucalyptusData {
    String metricName;
    ArrayList<MetricCounterType> counters = new ArrayList<MetricCounterType>();
}

public class MetricCounterType extends EucalyptusData {
    String type;
    Long collectionIntervalMs;
    ArrayList<MetricDimensionsType> dimensions = new ArrayList<MetricDimensionsType>();
}

public class MetricDimensionsType extends EucalyptusData {
    String dimensionName;
    Long sequenceNum;
    ArrayList<MetricDimensionsValuesType> values = new ArrayList<MetricDimensionsValuesType>();
}

public class MetricDimensionsValuesType extends EucalyptusData {
    Date timestamp;
    Double value;
}


public class DescribeResourcesType extends CloudClusterMessage {
  
  ArrayList<VmTypeInfo> instanceTypes = new ArrayList<VmTypeInfo>();
}
public class NodeType extends EucalyptusData {
  String serviceTag;
  String iqn;
  public String toString() {
    return "NodeType ${URI.create(serviceTag).getHost()} ${iqn}";
  }
}
public class DescribeResourcesResponseType extends CloudClusterMessage {
  ArrayList<ResourceType> resources = new ArrayList<ResourceType>();
  ArrayList<NodeType> nodes = new ArrayList<NodeType>();
  ArrayList<String> serviceTags = new ArrayList<String>();
  
  public String toString() {
    String out = "";
    resources.each{ out += "${this.getClass().getSimpleName()}: ${it.toString()}\n" };
    nodes.each{ out += "${this.getClass().getSimpleName()}: ${it.toString()}\n" };
    return out;
  }
}

public class VmTypeInfo extends EucalyptusData implements Cloneable {
  
  String name;
  Integer memory;
  Integer disk;
  Integer cores;
  String rootDeviceName;
  @HttpEmbedded(multiple=true)
  ArrayList<BlockDeviceMappingItemType> deviceMappings = new ArrayList<BlockDeviceMappingItemType>();
  @HttpEmbedded(multiple=true)
  ArrayList<VirtualBootRecord> virtualBootRecord = new ArrayList<VirtualBootRecord>();
  def VmTypeInfo(){
  }
  
  def VmTypeInfo(final name, final memory, final disk, final cores, final rootDevice ) {
    this.name = name;
    this.memory = memory;
    this.disk = disk;
    this.cores = cores;
    this.rootDeviceName = rootDevice;
  }
  
  public VmTypeInfo child( ) {
    VmTypeInfo child = new VmTypeInfo( this.name, this.memory, this.disk, this.cores, this.rootDeviceName );
    child.deviceMappings.addAll( this.deviceMappings );
    child.virtualBootRecord.addAll( this.virtualBootRecord.collect{ (VirtualBootRecord) it.clone() } );
    return child;
  }
  
  @Override
  public String toString() {
    return "VmTypeInfo ${name} mem=${memory} disk=${disk} cores=${cores}";
  }
  
  public String dump() {
   StringBuilder sb = new StringBuilder();
   sb.append("VmTypeInfo ${name} mem=${memory} disk=${disk} cores=${cores} rootDeviceName=${rootDeviceName} ");
   for (VirtualBootRecord vbr : this.virtualBootRecord) {
    sb.append("{VirtualBootRecord deviceName=").append(vbr.getGuestDeviceName())
	  .append(" resourceLocation=").append(vbr.resourceLocation)
	  .append(" size=").append(vbr.size).append("} ");
	}
	return sb.toString();
  }
  
  public void setEbsRoot( String imageId, String iqn, Long sizeBytes ) {
    this.virtualBootRecord.add( new VirtualBootRecord( id : imageId, size : sizeBytes, resourceLocation : "${iqn}", guestDeviceName : this.rootDeviceName, type : "ebs" ) );//TODO:GRZE: folow up on the iqn://
  }
  
  public void setRoot( String imageId, String location, Long sizeBytes ) {
    this.virtualBootRecord.add( new VirtualBootRecord( id : imageId, size : sizeBytes, resourceLocation : "walrus://${location}", guestDeviceName : this.rootDeviceName, type : "machine" ) );
  }
  
  public void setKernel( String imageId, String location ) {
    this.virtualBootRecord.add( new VirtualBootRecord( id : imageId, resourceLocation : "walrus://${location}", type : "kernel" ) );
  }
  
  public void setRamdisk( String imageId, String location ) {
    this.virtualBootRecord.add( new VirtualBootRecord( id : imageId, resourceLocation : "walrus://${location}", type : "ramdisk" ) );
  }
  
  protected void setSwap( String deviceName, Long sizeBytes ) {
    this.virtualBootRecord.add( new VirtualBootRecord( guestDeviceName : deviceName, size : sizeBytes, type : "swap", format : "swap" ) );
  }
  
  public void setEphemeral( Integer index, String deviceName, Long sizeBytes, String formatName ) {
    this.virtualBootRecord.add( new VirtualBootRecord( guestDeviceName : deviceName, size : sizeBytes, type : "ephemeral", format : formatName ) );
  }
  
  public VirtualBootRecord lookupRoot( ) throws NoSuchElementException {
    VirtualBootRecord ret;
    if (( ret = this.virtualBootRecord.find{ VirtualBootRecord vbr -> vbr.type == "machine" })==null ) {
      ret = this.virtualBootRecord.find{ VirtualBootRecord vbr -> vbr.type == "ebs" && ( vbr.guestDeviceName == this.rootDeviceName || vbr.guestDeviceName == "xvda" ) };
    }
    if( ret != null ) {
      return ret;
    } else {
      throw new NoSuchElementException( "Failed to find virtual boot record of type machine among: " + this.virtualBootRecord.collect{it.dump()}.toString() );
    }
  }
  public VirtualBootRecord lookupKernel( ) throws NoSuchElementException {
    VirtualBootRecord ret;
    if (( ret = this.virtualBootRecord.find{ VirtualBootRecord vbr -> vbr.type == "kernel" })==null ) {
      throw new NoSuchElementException( "Failed to find virtual boot record of type kernel among: " + this.virtualBootRecord.collect{it.dump()}.toString() );
    } else {
      return ret;
    }
  }
  public VirtualBootRecord lookupRamdisk( ) throws NoSuchElementException {
    VirtualBootRecord ret;
    if (( ret = this.virtualBootRecord.find{ VirtualBootRecord vbr -> vbr.type == "ramdisk" })==null ) {
      throw new NoSuchElementException( "Failed to find virtual boot record of type ramdisk among: " + this.virtualBootRecord.collect{it.dump()}.toString() );
    } else {
      return ret;
    }
  }
}
public class ResourceType extends EucalyptusData {
  
  VmTypeInfo instanceType;
  int maxInstances;
  int availableInstances;
  public String toString() {
    return "ResourceType ${instanceType} ${availableInstances} / ${maxInstances}";
  }
}
public class NetworkConfigType extends EucalyptusData {
  String macAddress;
  String ipAddress;
  String ignoredPublicIp;
  String privateDnsName;
  String publicDnsName;
  Integer vlan;
  Long networkIndex;
  
  def NetworkConfigType() {
  }
  
  public void updateDns( String domain ) {
    this.ipAddress = ( this.ipAddress == null ? "0.0.0.0" : this.ipAddress )
    this.ignoredPublicIp = ( this.ignoredPublicIp == null ? "0.0.0.0" : this.ignoredPublicIp )
    this.publicDnsName = "euca-${this.ignoredPublicIp.replaceAll( '\\.', '-' )}.eucalyptus.${domain}";
    this.privateDnsName = "euca-${this.ipAddress.replaceAll( '\\.', '-' )}.eucalyptus.internal";
  }
  
  @Override
  public String toString() {
    return "NetworkConfig ${vlan} ${networkIndex} ${ipAddress} ${ignoredPublicIp} ${privateDnsName} ${publicDnsName}";
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
    return "PacketFilterRule ${destUserName} ${destNetworkName} ${policy} ${protocol} ${portMin}-${portMax} " +
    ((!sourceCidrs.isEmpty())?"":" source ${sourceCidrs}") +
    ((!peers.isEmpty())?"":" peers ${peers}") +
    ((!sourceNetworkNames.isEmpty())?"":" sourceNetworks ${sourceNetworkNames}") +
    ((!sourceUserNames.isEmpty())?"":" sourceUsers ${sourceUserNames}");
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


public class CloudGatherLogMessage extends EucalyptusMessage {

  public CloudGatherLogMessage( ) {
    super( );
  }

  public CloudGatherLogMessage( EucalyptusMessage msg ) {
    super( msg );
  }

  public CloudGatherLogMessage( String userId ) {
    super( userId );
  }
}

public class GetLogsType extends CloudGatherLogMessage implements Comparable {
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
public class GetLogsResponseType extends CloudGatherLogMessage {
  NodeLogInfo logs = new NodeLogInfo();
}
public class GetKeysType extends CloudGatherLogMessage implements Comparable {
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
public class GetKeysResponseType extends CloudGatherLogMessage {
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
    return "NodeCertInfo [" +
    "serviceTag='" + serviceTag.replaceAll("services/EucalyptusNC","") + '\'' +
    ", ccCert='" + ccCert + '\'' +
    ", ncCert='" + ncCert + '\'' +
    ']';
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

public class StatEventRecord extends BaseMessage {
  
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

@ComponentMessage(ComponentService.class)
public class ComponentMessageType extends BaseMessage {
  String component;
  String host;
  String name;
  
  def ComponentMessageType() {
  }
  
  def ComponentMessageType(String component) {
    this.component = component;
  }
}

public class ComponentMessageResponseType extends BaseMessage {
  def ComponentMessageResponseType() {
  }
}

public class Filter extends EucalyptusData {
  String name;
  @HttpParameterMapping (parameter = "Value")
  ArrayList<String> valueSet = new ArrayList<String>( );
}

public class ErrorDetail extends EucalyptusData {
  String type
  Integer code
  String message
  String stackTrace
  public ErrorDetail() {  }
}

public class ErrorResponse extends BaseMessage {
  String requestId
  public ErrorResponse() {
  }
  ArrayList<ErrorDetail> error = new ArrayList<ErrorDetail>()
}

