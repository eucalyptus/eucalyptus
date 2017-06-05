/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
@GroovyAddClassUUID
package com.eucalyptus.cluster.common.msgs

import com.eucalyptus.binding.HttpEmbedded
import com.eucalyptus.cluster.common.ClusterController
import com.eucalyptus.component.Component
import com.eucalyptus.component.Faults
import com.eucalyptus.component.annotation.ComponentMessage
import com.eucalyptus.compute.common.internal.vm.VmInstance
import com.eucalyptus.compute.common.internal.vm.VmStandardVolumeAttachment
import com.google.common.base.Function
import com.google.common.base.Joiner
import com.google.common.collect.Lists
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

@ComponentMessage(ClusterController)
public class CloudClusterMessage extends EucalyptusMessage {

  public CloudClusterMessage( ) {
    super( );
  }

  public CloudClusterMessage( EucalyptusMessage msg ) {
    super( msg );
  }

  public CloudClusterMessage( String userId ) {
    super( userId );
  }
}

public class ClusterTerminateInstancesType extends CloudClusterMessage {

  ArrayList<String> instancesSet = new ArrayList<String>();

  def ClusterTerminateInstancesType() {
  }

  def ClusterTerminateInstancesType(String instanceId) {
    this.instancesSet.add(instanceId);
  }
}

public class ClusterTerminateInstancesResponseType extends CloudClusterMessage {
  boolean terminated
}

public class ClusterRebootInstancesType extends CloudClusterMessage {

  ArrayList<String> instancesSet = new ArrayList<String>();

  def ClusterRebootInstancesType() {
  }
  def ClusterRebootInstancesType(String instanceId) {
    this.instancesSet.add(instanceId);
  }
}

public class ClusterRebootInstancesResponseType extends CloudClusterMessage {
}


/*
 * Start/StopInstance are internal operation (CC-NC) to shutdown and reboot the VM;
 * The operations are required for CreateImage with noReboot option
 */
public class ClusterStartInstanceType extends CloudClusterMessage {
  String instanceId;
}

public class ClusterStartInstanceResponseType extends CloudClusterMessage {
}

public class ClusterStopInstanceType extends CloudClusterMessage {
  String instanceId;
}

public class ClusterStopInstanceResponseType extends CloudClusterMessage {
}

public class ClusterGetConsoleOutputResponseType extends CloudClusterMessage {
  String instanceId;
  Date timestamp;
  String output;
}

public class ClusterGetConsoleOutputType extends CloudClusterMessage {
  String instanceId;

  ClusterGetConsoleOutputType( ) {
  }

  ClusterGetConsoleOutputType( final String instanceId ) {
    this.instanceId = instanceId
  }
}

public class VirtualBootRecord extends EucalyptusData implements Cloneable {
  String id = "none";
  String resourceLocation = "none";
  String type;
  String guestDeviceName = "none";
  Long size = -1l;
  String format = "none";

  def VirtualBootRecord() {
  }

  def VirtualBootRecord(String id, String resourceLocation, String type, String guestDeviceName, Long sizeBytes, String format) {
    this.id = id;
    this.resourceLocation = resourceLocation;
    this.type = type;
    this.guestDeviceName = guestDeviceName;
    this.size = sizeBytes;
    this.format = format;
  }

  public boolean hasValidId() {
    return !"none".equals(this.id);
  }

  public VirtualBootRecord clone() {
    return (VirtualBootRecord) super.clone();
  }
}


public class VmTypeInfo extends EucalyptusData implements Cloneable {
  String name;
  Integer memory;
  Integer disk;
  Integer cores;
  String rootDeviceName;
  @HttpEmbedded(multiple = true)
  ArrayList<VirtualBootRecord> virtualBootRecord = new ArrayList<VirtualBootRecord>();

  def VmTypeInfo() {
  }

  def VmTypeInfo(String name, Integer memory, Integer disk, Integer cores, String rootDevice) {
    this.name = name;
    this.memory = memory;
    this.disk = disk;
    this.cores = cores;
    this.rootDeviceName = rootDevice;
  }

  public VmTypeInfo child() {
    VmTypeInfo child = new VmTypeInfo(this.name, this.memory, this.disk, this.cores, this.rootDeviceName);
    child.virtualBootRecord.addAll(this.virtualBootRecord.collect { VirtualBootRecord it -> it.clone() });
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

  public void setEbsRoot(String imageId, String iqn, Long sizeBytes) {
    this.virtualBootRecord.add(new VirtualBootRecord(id: imageId, size: sizeBytes, resourceLocation: "${iqn}", guestDeviceName: this.rootDeviceName, type: "ebs"));
//TODO:GRZE: folow up on the iqn://
  }

  public void setRoot(String imageId, String location, Long sizeBytes) {
    this.virtualBootRecord.add(new VirtualBootRecord(id: imageId, size: sizeBytes, resourceLocation: location, guestDeviceName: this.rootDeviceName, type: "machine"));
  }

  public void setKernel(String imageId, String location, Long sizeBytes) {
    this.virtualBootRecord.add(new VirtualBootRecord(id: imageId, size: sizeBytes, resourceLocation: location, type: "kernel"));
  }

  public void setRamdisk(String imageId, String location, Long sizeBytes) {
    this.virtualBootRecord.add(new VirtualBootRecord(id: imageId, size: sizeBytes, resourceLocation: location, type: "ramdisk"));
  }

  protected void setSwap(String deviceName, Long sizeBytes, String formatName) {
    this.virtualBootRecord.add(new VirtualBootRecord(guestDeviceName: deviceName, size: sizeBytes, type: "swap", format: formatName));
  }

  public void setEphemeral(Integer index, String deviceName, Long sizeBytes, String formatName) {
    this.virtualBootRecord.add(new VirtualBootRecord(guestDeviceName: deviceName, size: sizeBytes, type: "ephemeral", format: formatName));
  }

  public VirtualBootRecord lookupRoot() throws NoSuchElementException {
    VirtualBootRecord ret;
    if ((ret = this.virtualBootRecord.find { VirtualBootRecord vbr -> vbr.type == "machine" }) == null) {
      ret = this.virtualBootRecord.find { VirtualBootRecord vbr -> vbr.type == "ebs" && (vbr.guestDeviceName == this.rootDeviceName || vbr.guestDeviceName == "xvda") };
    }
    if (ret != null) {
      return ret;
    } else {
      throw new NoSuchElementException("Failed to find virtual boot record of type machine among: " + this.virtualBootRecord.collect {
        it.dump()
      }.toString());
    }
  }

  public VirtualBootRecord lookupKernel() throws NoSuchElementException {
    VirtualBootRecord ret;
    if ((ret = this.virtualBootRecord.find { VirtualBootRecord vbr -> vbr.type == "kernel" }) == null) {
      throw new NoSuchElementException("Failed to find virtual boot record of type kernel among: " + this.virtualBootRecord.collect {
        it.dump()
      }.toString());
    } else {
      return ret;
    }
  }

  public VirtualBootRecord lookupRamdisk() throws NoSuchElementException {
    VirtualBootRecord ret;
    if ((ret = this.virtualBootRecord.find { VirtualBootRecord vbr -> vbr.type == "ramdisk" }) == null) {
      throw new NoSuchElementException("Failed to find virtual boot record of type ramdisk among: " + this.virtualBootRecord.collect {
        it.dump()
      }.toString());
    } else {
      return ret;
    }
  }
}

public class ClusterMigrateInstancesType extends CloudClusterMessage {
    String sourceHost
    String instanceId
    //Array of strings of the format: e[kmr]i-xxxx=http://presignedurl_for_download_manifest
    ArrayList<String> resourceLocations = new ArrayList<>()
    ArrayList<String> destinationHosts = new ArrayList<String>( )
    Boolean allowHosts = false

    public void addResourceLocations(List<VmTypeInfo> resources) {
        resources.each { VmTypeInfo vmTypeInfo ->
            ArrayList<VirtualBootRecord> images = Lists.newArrayList(vmTypeInfo.lookupKernel(), vmTypeInfo.lookupRamdisk(), vmTypeInfo.lookupRoot())
            images.each { VirtualBootRecord image ->
                if(image != null) {
                    this.resourceLocations.add(image.getId() + '=' +
                                               image.getResourceLocation())
                }
            }

        }
        resourceLocations = Lists.newArrayList(resourceLocations.unique())
    }
}

public class ClusterMigrateInstancesResponseType extends CloudClusterMessage {}

class BroadcastNetworkInfoType extends CloudClusterMessage {
  String version
  String appliedVersion
  String networkInfo
}

class BroadcastNetworkInfoResponseType extends CloudClusterMessage {

}

public class AttachedVolume extends EucalyptusData implements Comparable<AttachedVolume> {
  String volumeId;
  String instanceId;
  String device;
  String remoteDevice;
  String status;
  Date attachTime = new Date();

  def AttachedVolume(final String volumeId, final String instanceId, final String device, final String remoteDevice) {
    this.volumeId = volumeId;
    this.instanceId = instanceId;
    this.device = device;
    this.remoteDevice = remoteDevice;
    this.status = "attaching";
  }

  public AttachedVolume( String volumeId ) {
    this.volumeId = volumeId;
  }

  public AttachedVolume() {
  }

  public boolean equals(final Object o) {
    if ( this.is(o) ) return true;
    if ( o == null || !getClass().equals( o.class ) ) return false;
    AttachedVolume that = (AttachedVolume) o;
    if ( volumeId ? !volumeId.equals(that.volumeId) : that.volumeId != null ) return false;
    return true;
  }

  public int hashCode() {
    return (volumeId ? volumeId.hashCode() : 0);
  }

  public int compareTo( AttachedVolume that ) {
    return this.volumeId.compareTo( that.volumeId );
  }

  public String toString() {
    return "AttachedVolume ${volumeId} ${instanceId} ${status} ${device} ${remoteDevice} ${attachTime}"
  }

  public static Function<AttachedVolume, VmStandardVolumeAttachment> toStandardVolumeAttachment(final VmInstance vm ) {
    return new Function<AttachedVolume, VmStandardVolumeAttachment>( ) {
      @Override
      public VmStandardVolumeAttachment apply( AttachedVolume vol ) {
        return new VmStandardVolumeAttachment( vm, vol.getVolumeId( ), vol.getDevice( ), vol.getRemoteDevice( ), vol.getStatus( ), vol.getAttachTime( ), false, Boolean.FALSE );
      }
    };
  }
}

public class ClusterAttachVolumeType extends CloudClusterMessage {

  String volumeId;
  String instanceId;
  String device;
  String remoteDevice;
  public ClusterAttachVolumeType( ) {
    super( );
  }
  public ClusterAttachVolumeType( String volumeId, String instanceId, String device, String remoteDevice ) {
    super( );
    this.volumeId = volumeId;
    this.instanceId = instanceId;
    this.device = device;
    this.remoteDevice = remoteDevice;
  }

}
public class ClusterAttachVolumeResponseType extends CloudClusterMessage {

  AttachedVolume attachedVolume = new AttachedVolume();
}

public class ClusterDetachVolumeType extends CloudClusterMessage {

  String volumeId;
  String instanceId;
  String device;
  String remoteDevice;
  Boolean force = false;
}

public class ClusterDetachVolumeResponseType extends CloudClusterMessage {

  AttachedVolume detachedVolume = new AttachedVolume();
}

public class DescribeSensorsType extends CloudClusterMessage {
  Integer historySize;
  Integer collectionIntervalTimeMs;
  ArrayList<String> sensorIds = new ArrayList<String>();
  ArrayList<String> instanceIds = new ArrayList<String>();

  DescribeSensorsType(){}

  DescribeSensorsType (Integer historySize, Integer collectionIntervalTimeMs, ArrayList<String> instanceIds ) {
    this.historySize = historySize;
    this.collectionIntervalTimeMs = collectionIntervalTimeMs;
    this.instanceIds = instanceIds;
  }
}

public class ModifyNodeType extends CloudClusterMessage {
  String stateName
  String nodeName

  ModifyNodeType(){}

  ModifyNodeType(String nodeName, String stateName ) {
    this.nodeName = nodeName;
    this.stateName = stateName;
  }
}

public class ModifyNodeResponseType extends CloudClusterMessage {
}

public class DescribeSensorsResponseType extends CloudClusterMessage {

  ArrayList<SensorsResourceType> sensorsResources = new ArrayList<SensorsResourceType>();

}

public class DescribeResourcesType extends CloudClusterMessage {

  ArrayList<VmTypeInfo> instanceTypes = new ArrayList<VmTypeInfo>();
}
public class NodeType extends EucalyptusData {
  String serviceTag;
  String iqn;
  String hypervisor;
  public String toString() {
    return "NodeType ${URI.create(serviceTag).getHost()} ${iqn} ${hypervisor}";
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

public class ResourceType extends EucalyptusData {

  VmTypeInfo instanceType;
  int maxInstances;
  int availableInstances;
  public String toString() {
    return "ResourceType ${instanceType} ${availableInstances} / ${maxInstances}";
  }
}
public class NetworkConfigType extends EucalyptusData {
  String interfaceId; // for use in vpc mode only
  Integer device = 0;
  String macAddress;
  String ipAddress;
  String ignoredPublicIp = "0.0.0.0";
  String privateDnsName;
  String publicDnsName;
  Integer vlan = -1;
  Long networkIndex = -1l;
  String attachmentId; // for use in vpc mode only

  def NetworkConfigType() {
  }
  
  def NetworkConfigType(final String interfaceId, final Integer device) {
    this.interfaceId = interfaceId;
    this.device = device;
  }

  public void updateDns( String domain ) {
    this.ipAddress = ( this.ipAddress == null ? "0.0.0.0" : this.ipAddress )
    this.ignoredPublicIp = ( this.ignoredPublicIp == null ? "0.0.0.0" : this.ignoredPublicIp )
    this.publicDnsName = "euca-${this.ignoredPublicIp.replaceAll( '\\.', '-' )}.eucalyptus.${domain}";
    this.privateDnsName = "euca-${this.ipAddress.replaceAll( '\\.', '-' )}.eucalyptus.internal";
  }

  @Override
  public String toString() {
    return "NetworkConfig interfaceId=${interfaceId}, device=${device}, macAddress=${macAddress}, ipAddress=${ipAddress}, " +
    "ignoredPublicIp=${ignoredPublicIp}, privateDnsName=${privateDnsName}, publicDnsName=${publicDnsName}, vlan=${vlan}, networkIndex=${networkIndex}";
  }
}

public class NetworkParameters extends EucalyptusData {
  String privateMacAddress;
  String publicMacAddress;
  int macLimit;
  int vlan;
}

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

public class VmDescribeType extends CloudClusterMessage {

  ArrayList<String> instancesSet = new ArrayList<String>();
}
public class VmDescribeResponseType extends CloudClusterMessage {

  String originCluster;
  ArrayList<VmInfo> vms = new ArrayList<VmInfo>();
  public String toString() {
    return "${this.getClass().getSimpleName()} " + Joiner.on("\n${this.getClass().getSimpleName()} " as String).join(vms.iterator());
  }
}

public class VmRunResponseType extends CloudClusterMessage {

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
  String accountId;
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
  Double bundleTaskProgress;
  String createImageStateName;
  String guestStateName;
  //TODO:GRZE: these are to be cleaned up into a separate object rather than being munged in and possibly null.
  String migrationStateName;
  String migrationSource;
  String migrationDestination;
  List<NetworkConfigType> secondaryNetConfigList = new ArrayList<NetworkConfigType>();

  ArrayList<String> productCodes = new ArrayList<String>();

  @Override
  public String toString( ) {
    return "VmInfo ${reservationId} ${instanceId} ${ownerId} ${stateName} ${instanceType} ${imageId} ${kernelId} ${ramdiskId} ${launchIndex} ${serviceTag} ${netParams} ${volumes} ${migrationStateName} ${secondaryNetConfigList}";
  }
}



public class VmKeyInfo extends EucalyptusData {

  String name = "";
  String value = "";
  String fingerprint = "";

  def VmKeyInfo() {
  }

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
  String partition;
  Hypervisor hypervisor = Hypervisor.Unknown;
  Boolean hasClusterCert = false;
  Boolean hasNodeCert = false;
  Component.State lastState;
  Faults.CheckException lastException;
  String lastMessage;
  Date lastSeen;
  NodeCertInfo certs = new NodeCertInfo();
  NodeLogInfo logs = new NodeLogInfo();

  def NodeInfo() {
  }

  def NodeInfo(final String serviceTag ) {
    this.serviceTag = serviceTag;
    this.name = (new URI(this.serviceTag)).getHost();
    this.lastSeen = new Date();
    this.certs.setServiceTag(this.serviceTag);
    this.logs.setServiceTag(this.serviceTag);
  }

  def NodeInfo(final String partition, final NodeType nodeType) {
    this.partition = partition;
    this.serviceTag = nodeType.getServiceTag( );
    this.iqn = nodeType.getIqn( );
    this.hypervisor = Hypervisor.fromString(nodeType.getHypervisor());
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


  public void touch(Component.State lastState, String lastMessage, Faults.CheckException lastEx ) {
    this.lastSeen = new Date();
    this.lastException = lastEx;
    this.lastState = lastState;
    this.lastMessage = lastMessage;
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
    return "NodeInfo name=${name} lastSeen=${lastSeen} serviceTag=${serviceTag} iqn=${iqn} hypervisor=${hypervisor}";
  }

  public enum Hypervisor {
    KVM(true), ESXI(false), Unknown(false);

    private final boolean supportsEkiEri;

    public Hypervisor(boolean supportsEkiEri) {
      this.supportsEkiEri = supportsEkiEri;
    }

    public static Hypervisor fromString(String hypervisor) {
      try {
        return Hypervisor.valueOf(hypervisor);
      } catch (IllegalArgumentException ex) {
        return Unknown;
      }
    }

    public boolean supportEkiEri() {
      return supportsEkiEri;
    }
  }
}


public class ClusterBundleInstanceType extends CloudClusterMessage {
  String architecture
  String awsAccessKeyId
  String bucket
  String instanceId
  String prefix
  String uploadPolicy
  String uploadPolicySignature
  String url
  String userKey
}

public class ClusterBundleInstanceResponseType extends CloudClusterMessage {
}

public class ClusterBundleRestartInstanceType extends CloudClusterMessage {
  String instanceId;
}

public class ClusterBundleRestartInstanceResponseType extends CloudClusterMessage {
}

public class ClusterCancelBundleTaskType extends CloudClusterMessage {
  String bundleId;
  String instanceId;
}

public class ClusterCancelBundleTaskResponseType extends CloudClusterMessage {
}

