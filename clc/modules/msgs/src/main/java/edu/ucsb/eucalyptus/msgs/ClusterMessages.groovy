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
package edu.ucsb.eucalyptus.msgs

import com.google.common.base.Joiner
import com.google.common.collect.Lists
import edu.ucsb.eucalyptus.cloud.VirtualBootRecord
import groovy.transform.Canonical

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
public class StartInstanceType extends CloudClusterMessage {
  String instanceId;
}

public class StartInstanceResponseType extends CloudClusterMessage {
}

public class StopInstanceType extends CloudClusterMessage {
  String instanceId;
}

public class StopInstanceResponseType extends CloudClusterMessage {
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



