/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
 ************************************************************************/
@GroovyAddClassUUID
package edu.ucsb.eucalyptus.msgs

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
  String sourceHost;
  String instanceId;
  ArrayList<String> destinationHosts = new ArrayList<String>( );
  Boolean allowHosts = false;
}

public class ClusterMigrateInstancesResponseType extends CloudClusterMessage {}

public class AssignAddressType extends CloudClusterMessage {
  String uuid;
  String instanceId;
  String source;
  String destination;
  def AssignAddressType(final String uuid, final String source, final String destination, final String instanceId ) {
    this.uuid = uuid;
    this.source = source;
    this.destination = destination;
    this.instanceId = instanceId;
  }

  def AssignAddressType(final EucalyptusMessage msg, final String uuid, final String source, final String destination, final String instanceId) {
    super(msg);
    this.uuid = uuid;
    this.source = source;
    this.destination = destination;
    this.instanceId = instanceId;
  }

  def AssignAddressType() {
  }

  @Override
  public String toString( ) {
    return "AssignAddress ${this.source}=>${this.destination} ${this.instanceId} ${this.uuid}";
  }

  @Override
  public String toSimpleString( ) {
    return "${super.toSimpleString( )} ${this.source}=>${this.destination} ${this.instanceId} ${this.uuid}";
  }
}

public class AssignAddressResponseType extends CloudClusterMessage {
}

public class UnassignAddressType extends CloudClusterMessage {

  String source;
  String destination;

  def UnassignAddressType(final EucalyptusMessage msg, final String source, final String destination) {
    super(msg);
    this.source = source;
    this.destination = destination;
  }
  def UnassignAddressType(final source, final destination) {

    this.source = source;
    this.destination = destination;
  }


  def UnassignAddressType() {
  }

  @Override
  public String toString( ) {
    return "UnassignAddress ${this.source}=>${this.destination}";
  }

  @Override
  public String toSimpleString( ) {
    return "${super.toSimpleString( )} ${this.source}=>${this.destination}";
  }
}

public class UnassignAddressResponseType extends CloudClusterMessage {
}

class BroadcastNetworkInfoType extends CloudClusterMessage {
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



