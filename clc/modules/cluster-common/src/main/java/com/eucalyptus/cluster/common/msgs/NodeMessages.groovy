/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
@GroovyAddClassUUID
package com.eucalyptus.cluster.common.msgs

import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID


class CloudNodeMessage extends EucalyptusMessage {
  String nodeName
}

class NcAssignAddressResponseType extends CloudNodeMessage {
}

class VolumeType extends EucalyptusData {
  String volumeId
  String remoteDev
  String localDev
  String state
}

class NcDescribeSensorsType extends CloudNodeMessage {
  Integer historySize
  Integer collectionIntervalTimeMs
  ArrayList<String> instanceIds = new ArrayList<String>()
  ArrayList<String> sensorIds = new ArrayList<String>()
}

class NcTerminateInstanceResponseType extends CloudNodeMessage {
  String instanceId
  String shutdownState
  String previousState
}

class NcStopInstanceType extends CloudNodeMessage {
  String instanceId
}

class NcStartNetworkType extends CloudNodeMessage {
  Integer remoteHostPort
  Integer vlan
  String uuid
  ArrayList<String> remoteHosts = new ArrayList<String>()
}

class NcStartInstanceResponseType extends CloudNodeMessage {
}

class NcBundleInstanceResponseType extends CloudNodeMessage {
}

class NcTerminateInstanceType extends CloudNodeMessage {
  String instanceId
  Boolean force
}

class NcMigrateInstancesResponseType extends CloudNodeMessage {
}

class NcCancelBundleTaskResponseType extends CloudNodeMessage {
}

class NcGetConsoleOutputResponseType extends CloudNodeMessage {
  String consoleOutput
}

class NcAttachVolumeType extends CloudNodeMessage {
  String instanceId
  String volumeId
  String remoteDev
  String localDev
}

class NcBroadcastNetworkInfoResponseType extends CloudNodeMessage {
}

class NcDescribeInstancesResponseType extends CloudNodeMessage {
  ArrayList<InstanceType> instances = new ArrayList<InstanceType>()
}

class NcCreateImageType extends CloudNodeMessage {
  String instanceId
  String volumeId
  String remoteDev
}

class NcRebootInstanceType extends CloudNodeMessage {
  String instanceId
}

class NcStartNetworkResponseType extends CloudNodeMessage {
  String networkStatus
}

class NcBundleInstanceType extends CloudNodeMessage {
  String instanceId
  String bucketName
  String filePrefix
  String objectStorageURL
  String userPublicKey
  String cloudPublicKey
  String s3Policy
  String s3PolicySig
  String architecture
}

class NcDetachVolumeResponseType extends CloudNodeMessage {
}

class NcBroadcastNetworkInfoType extends CloudNodeMessage {
  String networkInfo
}

class VirtualMachineType extends EucalyptusData {
  String name
  Integer memory
  Integer cores
  Integer disk
  ArrayList<VirtualBootRecordType> virtualBootRecord = new ArrayList<VirtualBootRecordType>()
}

class NcStopInstanceResponseType extends CloudNodeMessage {
}

class NcMigrateInstancesType extends CloudNodeMessage {
  String action
  String credentials
  ArrayList<InstanceType> instances = new ArrayList<InstanceType>()
  ArrayList<String> resourceLocation = new ArrayList<String>()
}

class NcBundleRestartInstanceResponseType extends CloudNodeMessage {
}

class VirtualBootRecordType extends EucalyptusData {
  String resourceLocation
  String guestDeviceName
  Long size
  String format
  String id
  String type
}

class NcDescribeSensorsResponseType extends CloudNodeMessage {
  ArrayList<SensorsResourceType> sensorsResources = new ArrayList<SensorsResourceType>()
}

class NcPowerDownType extends CloudNodeMessage {
}

class NcDescribeResourceResponseType extends CloudNodeMessage {
  String nodeStatus
  Boolean migrationCapable
  String iqn
  Integer memorySizeMax
  Integer memorySizeAvailable
  Integer diskSizeMax
  Integer diskSizeAvailable
  Integer numberOfCoresMax
  Integer numberOfCoresAvailable
  String publicSubnets
  String hypervisor
}

class NcAttachNetworkInterfaceType extends CloudNodeMessage {
  String instanceId
  NetConfigType netConfig
}

class NcRunInstanceType extends CloudNodeMessage {
  String imageId
  String kernelId
  String ramdiskId
  String imageURL
  String kernelURL
  String ramdiskURL
  String ownerId
  String accountId
  String reservationId
  String instanceId
  String uuid
  VirtualMachineType instanceType
  String keyName
  NetConfigType netParams
  String userData
  String credential
  String launchIndex
  String platform
  Date expiryTime
  String rootDirective
  ArrayList<String> groupNames = new ArrayList<String>()
  ArrayList<String> groupIds = new ArrayList<String>()
  ArrayList<NetConfigType> secondaryNetConfig = new ArrayList<NetConfigType>()
}

class NcModifyNodeType extends CloudNodeMessage {
  String stateName
}

class NcStartInstanceType extends CloudNodeMessage {
  String instanceId
}

class NcDetachNetworkInterfaceType extends CloudNodeMessage {
  String instanceId
  String attachmentId
  Boolean force
}

class NcPowerDownResponseType extends CloudNodeMessage {
}

class BundleTaskType extends EucalyptusData {
  String instanceId
  String state
  String manifest
  Integer progress
}

class NcCancelBundleTaskType extends CloudNodeMessage {
  String instanceId
}

class NcAttachVolumeResponseType extends CloudNodeMessage {
}

class NcDescribeResourceType extends CloudNodeMessage {
  String resourceType
}

class NcModifyNodeResponseType extends CloudNodeMessage {
}

class NcBundleRestartInstanceType extends CloudNodeMessage {
  String instanceId
}

class NcAttachNetworkInterfaceResponseType extends CloudNodeMessage {
}

class NcCreateImageResponseType extends CloudNodeMessage {
}

class NetConfigType extends EucalyptusData {
  String interfaceId
  Integer device
  String privateMacAddress
  String privateIp
  String publicIp
  Integer vlan
  Integer networkIndex
  String attachmentId
}

class NcAssignAddressType extends CloudNodeMessage {
  String instanceId
  String publicIp
}

class NcDescribeInstancesType extends CloudNodeMessage {
  ArrayList<String> instanceIds = new ArrayList<String>()
}

class NcDetachVolumeType extends CloudNodeMessage {
  String instanceId
  String volumeId
  String remoteDev
  String localDev
  Boolean force
}

class NcRebootInstanceResponseType extends CloudNodeMessage {
  Boolean status
}

class NcDetachNetworkInterfaceResponseType extends CloudNodeMessage {
}

class NcGetConsoleOutputType extends CloudNodeMessage {
  String instanceId
}

class NcRunInstanceResponseType extends CloudNodeMessage {
  InstanceType instance
}

class InstanceType extends EucalyptusData {
  String uuid
  String reservationId
  String instanceId
  String imageId
  String kernelId
  String ramdiskId
  String userId
  String ownerId
  String accountId
  String keyName
  VirtualMachineType instanceType
  NetConfigType netParams
  String stateName
  String bundleTaskStateName
  Double bundleTaskProgress
  String createImageStateName
  Date launchTime
  Date expiryTime
  Integer blkbytes
  Integer netbytes
  String guestStateName
  String migrationStateName
  String migrationSource
  String migrationDestination
  String userData
  String launchIndex
  String platform
  String serviceTag
  Integer hasFloopy
  ArrayList<String> groupNames = new ArrayList<String>()
  ArrayList<String> groupIds = new ArrayList<String>()
  ArrayList<VolumeType> volumes = new ArrayList<VolumeType>()
  ArrayList<NetConfigType> secondaryNetConfig = new ArrayList<NetConfigType>()
}
