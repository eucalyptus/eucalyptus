package edu.ucsb.eucalyptus.msgs
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import com.eucalyptus.component.ComponentId
import com.eucalyptus.component.ComponentMessage;
import com.eucalyptus.component.id.*;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.config.EphemeralConfiguration;
import com.eucalyptus.empyrean.Empyrean;
import edu.ucsb.eucalyptus.cloud.VirtualBootRecord;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

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
 *******************************************************************************
 * @author: chris grzegorczyk <grze@eucalyptus.com>
 */


public class HeartbeatType extends EucalyptusMessage {
  ArrayList<HeartbeatComponentType> components = new ArrayList<HeartbeatComponentType>();
  ArrayList<ComponentType> started = new ArrayList<ComponentType>();
  ArrayList<ComponentType> stopped = new ArrayList<ComponentType>();
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

public class ComponentType extends EucalyptusData {
  String component;
  String name;
  String uri;
  public ComponentType( String component, String name, String uri ) {
    this.component = component;
    this.name = name;
    this.uri = uri;
  }
  public ComponentType( ) {}  
  public ServiceConfiguration toConfiguration() {
    URI realUri = URI.create( this.getUri( ) );
    final ComponentId c = ComponentId.lookup( component );
    return new EphemeralConfiguration( name, c, realUri );
  }
}
public class ComponentProperty extends EucalyptusData {
  private String type;
  private String displayName;
  private String value;
  private String qualifiedName;
		
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
public class StorageStateType extends EucalyptusMessage{
  private String name;
  def StorageStateType() {
  }
  
  def StorageStateType(final name) {
    this.name = name;
  }
}

public class WalrusStateType extends EucalyptusMessage{
  private String name;
  
  def WalrusStateType() {
  }
  
  def WalrusStateType(final name) {
    this.name = name;
  }
}


@ComponentMessage(Eucalyptus.class)
public class EucalyptusMessage extends BaseMessage implements Cloneable, Serializable {
    
  public EucalyptusMessage() {
    super();
  }
  
  public EucalyptusMessage( EucalyptusMessage msg ) {
    this();
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
  String source;
  String message;
  String requestType = "not available";
  Throwable exception;
  public ExceptionResponseType() {}
  public ExceptionResponseType( BaseMessage msg, String message, HttpResponseStatus status, Throwable exception ) {
    this.source = source;
    this.message = message;
    this.correlationId = msg.getCorrelationId();
    this.userId = msg.getUserId();
    this.requestType = msg != null ? msg.getClass().getSimpleName() : this.requestType;
    this.exception = exception;
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
    this.correlationId = msg.getCorrelationId();
    this.userId = msg.getUserId();
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
public class DescribeResourcesType extends EucalyptusMessage {
  
  ArrayList<VmTypeInfo> instanceTypes = new ArrayList<VmTypeInfo>();
}
public class NodeType extends EucalyptusData {
  String serviceTag;
  String iqn;
  public String toString() {
    return "NodeType ${URI.create(serviceTag).getHost()} ${iqn}";
  }
}
public class DescribeResourcesResponseType extends EucalyptusMessage {
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

public class VmTypeInfo extends EucalyptusData {
  
  String name;
  Integer memory;
  Integer disk;
  Integer cores;
  String rootDeviceName = "sda1";
  
  ArrayList<BlockDeviceMappingItemType> deviceMappings = new ArrayList<BlockDeviceMappingItemType>();
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
  
  @Override
  public String toString() {
    return "VmTypeInfo ${name} mem=${memory} disk=${disk} cores=${cores}";
  }
  
  public void setRoot( String imageId, String location, Long sizeKb ) {
    this.virtualBootRecord.add( new VirtualBootRecord( id : imageId, size : sizeKb, resourceLocation : "walrus://${location}", guestDeviceName : this.rootDeviceName, type : "machine" ) );
  }
  
  public void setKernel( String imageId, String location ) {
    this.virtualBootRecord.add( new VirtualBootRecord( id : imageId, resourceLocation : "walrus://${location}", type : "kernel" ) );
  }

  public void setRamdisk( String imageId, String location ) {
    this.virtualBootRecord.add( new VirtualBootRecord( id : imageId, resourceLocation : "walrus://${location}", type : "ramdisk" ) );
  }

  public void setSwap( String deviceName, Long sizeKb ) {
    this.virtualBootRecord.add( new VirtualBootRecord( guestDeviceName : deviceName, size : sizeKb, , type : "swap", format : "swap" ) );
  }

  public void setEphemeral( Integer index, String deviceName, Long sizeKb ) {
    this.virtualBootRecord.add( new VirtualBootRecord( type : "ephemeral" + index, guestDeviceName : deviceName, size : sizeKb ) );
  }

  public VirtualBootRecord lookupRoot( ) throws NoSuchElementException {
    VirtualBootRecord ret;
    if (( ret = this.virtualBootRecord.find{ VirtualBootRecord vbr -> vbr.type == "machine" })==null ) {
      throw new NoSuchElementException( "Failed to find virtual boot record of type machine among: " + this.virtualBootRecord.collect{it.dump()}.toString() );
    } else {
      return ret;
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
  int vlan;
  Integer networkIndex;
  
  def NetworkConfigType() {
  }
  
  public void updateDns( String domain ) {
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

public class BundleInstanceType extends VmBundleMessage {
  String instanceId;
  @HttpParameterMapping(parameter="Storage.S3.Bucket")
  String bucket;
  @HttpParameterMapping(parameter="Storage.S3.Prefix")
  String prefix;
  @HttpParameterMapping(parameter="Storage.S3.AWSAccessKeyId")
  String awsAccessKeyId;
  @HttpParameterMapping(parameter="Storage.S3.UploadPolicy")
  String uploadPolicy;  
  @HttpParameterMapping(parameter="Storage.S3.UploadPolicySignature")
  String uploadPolicySignature;  
  String url;
  String userKey;
}
public class BundleInstanceResponseType extends VmBundleMessage {
  BundleTask task;
}
public class CancelBundleTaskType extends VmBundleMessage {
  String bundleId;
  String instanceId;
}
public class CancelBundleTaskResponseType extends VmBundleMessage {
  BundleTask task;
}
public class BundleTaskState extends EucalyptusData {
  String instanceId;
  String state;
}
public class BundleTask extends EucalyptusData {
  String instanceId;
  String bundleId;
  String state;
  Date startTime;
  Date updateTime;
  String progress;
  String bucket;
  String prefix;
  String errorMessage;
  String errorCode;
  public BundleTask() {}
  public BundleTask( String bundleId, String instanceId, String bucket, String prefix ) {
    this.bundleId = bundleId;
    this.instanceId = instanceId;
    this.bucket = bucket;
    this.prefix = prefix;
    this.state = "pending";
    this.startTime = new Date();
    this.updateTime = new Date();
    this.progress = "0%";
  }
}
public class DescribeBundleTasksType extends VmBundleMessage {
  @HttpParameterMapping (parameter = "BundleId")
  ArrayList<String> bundleIds = new ArrayList<String>();
}
public class DescribeBundleTasksResponseType extends VmBundleMessage {
  ArrayList<BundleTask> bundleTasks = new ArrayList<BundleTask>();
  ArrayList<BundleTaskState> bundleTaskStates = new ArrayList<BundleTaskState>();
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

@ComponentMessage(ComponentService.class)
public class ComponentMessageType extends BaseMessage {
  String component;
  String host;
  
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
