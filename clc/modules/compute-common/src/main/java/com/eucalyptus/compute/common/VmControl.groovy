/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.compute.common;

import com.eucalyptus.binding.HttpEmbedded
import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.binding.HttpValue
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID
import edu.ucsb.eucalyptus.msgs.HasTags
import groovy.transform.TupleConstructor

import javax.annotation.Nonnull
import javax.annotation.Nullable

import static edu.ucsb.eucalyptus.msgs.ComputeMessageValidation.FieldRegex
import static edu.ucsb.eucalyptus.msgs.ComputeMessageValidation.FieldRegexValue

public class VmControlMessage extends ComputeMessage {
  
  public VmControlMessage( ) {
    super( );
  }
  
  public VmControlMessage( ComputeMessage msg ) {
    super( msg );
  }
  
  public VmControlMessage( String userId ) {
    super( userId );
  }
}
public class VmPlacementMessage extends ComputeMessage {
  
  public VmPlacementMessage( ) {
    super( );
  }
  
  public VmPlacementMessage( ComputeMessage msg ) {
    super( msg );
  }
  
  public VmPlacementMessage( String userId ) {
    super( userId );
  }
}
/** *******************************************************************************/
public class TerminateInstancesResponseType extends VmControlMessage {
  ArrayList<TerminateInstancesItemType> instancesSet = new ArrayList<TerminateInstancesItemType>();
}
public class TerminateInstancesType extends VmControlMessage {
  
  @HttpParameterMapping (parameter = "InstanceId")
  ArrayList<String> instancesSet = new ArrayList<String>();
  
  def TerminateInstancesType() {
  }
  
  def TerminateInstancesType(String instanceId) {
    this.instancesSet.add(instanceId);
  }
}
/** *******************************************************************************/

public class DescribeInstancesType extends VmControlMessage {
  
  @HttpParameterMapping (parameter = "InstanceId")
  ArrayList<String> instancesSet = new ArrayList<String>();
  Integer maxResults
  String nextToken
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}
public class DescribeInstancesResponseType extends VmControlMessage {
  
  ArrayList<ReservationInfoType> reservationSet = new ArrayList<ReservationInfoType>();
}

/** *******************************************************************************/

public class DescribeInstanceStatusType extends VmControlMessage {
  @HttpParameterMapping (parameter = "InstanceId")
  ArrayList<String> instancesSet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  String nextToken;
  Integer maxResults;
  Boolean includeAllInstances;
  public DescribeInstanceStatusType() {  }
}

public class InstanceStatusEventType extends EucalyptusData {
  String code;
  String description;
  Date notBefore;
  Date notAfter;
  public InstanceStatusEventType() {  }
}

public class InstanceStatusEventsSetType extends EucalyptusData {
  public InstanceStatusEventsSetType() {  }
  ArrayList<InstanceStatusEventType> item = new ArrayList<InstanceStatusEventType>();
}

public class InstanceStatusDetailsSetItemType extends EucalyptusData {
  String name;
  String status;
  Date impairedSince;
  public InstanceStatusDetailsSetItemType() {  }
}

public class InstanceStatusDetailsSetType extends EucalyptusData {
  public InstanceStatusDetailsSetType() {  }
  ArrayList<InstanceStatusDetailsSetItemType> item = new ArrayList<InstanceStatusDetailsSetItemType>();
}

public class InstanceStatusType extends EucalyptusData {
  String status;
  InstanceStatusDetailsSetType details;
  public InstanceStatusType() {  }
}

public class InstanceStatusItemType extends EucalyptusData {
  String instanceId;
  String availabilityZone;
  InstanceStatusEventsSetType eventsSet;
  InstanceStateType instanceState;
  InstanceStatusType systemStatus;
  InstanceStatusType instanceStatus;
  public InstanceStatusItemType() {  }
}

public class InstanceStatusSetType extends EucalyptusData {
  public InstanceStatusSetType() {  }
  ArrayList<InstanceStatusItemType> item = new ArrayList<InstanceStatusItemType>();
}

public class DescribeInstanceStatusResponseType extends VmControlMessage {
  InstanceStatusSetType instanceStatusSet = new InstanceStatusSetType();
  String nextToken;
  public DescribeInstanceStatusResponseType() {  }
}

public class ReportInstanceStatusType extends VmControlMessage {
  ArrayList<String> instanceId
  String status
  Date startTime
  Date endTime
  ArrayList<String> reasonCode
  String description
}

public class ReportInstanceStatusResponseType extends VmControlMessage { }

/** *******************************************************************************/

public class RebootInstancesType extends VmControlMessage {
  
  @HttpParameterMapping (parameter = "InstanceId")
  ArrayList<String> instancesSet = new ArrayList<String>();
  
  def RebootInstancesType() {
  }
  def RebootInstancesType(String instanceId) {
    this.instancesSet.add(instanceId);
  }
}
public class RebootInstancesResponseType extends VmControlMessage {
}
/** *******************************************************************************/
public class RunInstancesResponseType extends VmControlMessage {
  
  ReservationInfoType rsvInfo;
}


public class RunInstancesType extends VmControlMessage implements HasTags {
  
  String imageId;
  String reservationId;
  int minCount;
  int maxCount;
  String keyName;
  ArrayList<String> instanceIds = Lists.newArrayList();
  @HttpParameterMapping (parameter = "SecurityGroup")
  ArrayList<String> groupSet = Lists.newArrayList() // Query binding
  @HttpParameterMapping (parameter = "SecurityGroupId")
  ArrayList<String> groupIdSet = Lists.newArrayList()  // Query binding and also SOAP binding before 2011-01-01
  ArrayList<GroupItemType> securityGroups = Lists.newArrayList() // Used in SOAP binding since 2011-01-01
  String additionalInfo;
  String userData;
  String version;
  String encoding;
  String addressingType;
  String instanceType = "m1.small";
  String kernelId; //** added 2008-02-01  **/
  String ramdiskId; //** added 2008-02-01  **/
  @HttpParameterMapping (parameter = "Placement.AvailabilityZone")
  String availabilityZone; //** added 2008-02-01  **/
  @HttpParameterMapping (parameter = "Placement.GroupName")
  String placementGroup = "default"; //** added 2010-02-01  **/
  @HttpParameterMapping (parameter = "Placement.Tenancy")
  String placementTenancy = "default"
  @HttpEmbedded (multiple = true)
  ArrayList<BlockDeviceMappingItemType> blockDeviceMapping = Lists.newArrayList(); //** added 2008-02-01  **/
  @HttpParameterMapping (parameter = "Monitoring.Enabled")
  Boolean monitoring = false;
  String subnetId;
  @HttpParameterMapping (parameter = "DisableApiTermination")
  Boolean disableTerminate;
  @HttpParameterMapping (parameter = "InstanceInitiatedShutdownBehavior")
  String shutdownAction = "stop"; //or "terminate"
  /** InstanceLicenseRequest license; **/
  String privateIpAddress;
  String clientToken;
  @HttpEmbedded
  InstanceNetworkInterfaceSetRequestType networkInterfaceSet;
  @HttpParameterMapping(parameter = "IamInstanceProfile.Arn")
  String iamInstanceProfileArn
  @HttpParameterMapping(parameter = "IamInstanceProfile.Name")
  String iamInstanceProfileName
  ArrayList<Integer> networkIndexList = Lists.newArrayList();
  String privateMacBase;
  String publicMacBase;
  int macLimit;
  int vlan;
  Boolean ebsOptimized = Boolean.FALSE
  @HttpEmbedded( multiple = true )
  ArrayList<ResourceTagSpecification> tagSpecification = new ArrayList<ResourceTagSpecification>()

  Set<String> securityGroupNames() {
    Set<String> names = Sets.newLinkedHashSet()
    names.addAll( groupSet )
    names.addAll( groupIdSet.findAll{ String id -> !id.startsWith( "sg-" ) } ) // ID was historically the name
    names.addAll( securityGroups.findAll{ GroupItemType group -> group.groupName != null }.collect{ GroupItemType group -> group.groupName } )
    names.addAll( securityGroups.findAll{ GroupItemType group -> group.groupId != null && !group.groupId.startsWith( "sg-" ) }.collect{ GroupItemType group -> group.groupId } )
    names
  }

  Set<String> securityGroupsIds() {
    Set<String> names = Sets.newLinkedHashSet()
    names.addAll( groupIdSet.findAll{ String id -> id.startsWith( "sg-" ) } ) // ID was historically the name
    names.addAll( securityGroups.findAll{ GroupItemType group -> group.groupId != null && group.groupId.startsWith( "sg-" ) }.collect{ GroupItemType group -> group.groupId } )
    names
  }

  public Object clone() {
    RunInstancesType c = (RunInstancesType) super.clone();
    c.instanceIds = (ArrayList<String>)this.instanceIds.clone()
    c.groupSet = (ArrayList<String>)this.groupSet.clone()
    c.groupIdSet = (ArrayList<String>)this.groupIdSet.clone()
    c.securityGroups = Lists.newArrayList()
    if ( this.securityGroups != null )
      for ( GroupItemType groupItemType: this.securityGroups )
        c.securityGroups.add((GroupItemType) groupItemType.clone());
    c.blockDeviceMapping = Lists.newArrayList()
    if ( this.blockDeviceMapping != null )
      for ( BlockDeviceMappingItemType b: this.blockDeviceMapping )
        c.blockDeviceMapping.add((BlockDeviceMappingItemType) b.clone());
    return c;
  }

  void setInstanceProfileNameOrArn ( String nameOrArn ) {
    if ( nameOrArn.startsWith( "arn:" ) ) {
        this.iamInstanceProfileArn = nameOrArn;
    } else {
        this.iamInstanceProfileName = nameOrArn;
    }
  }

  InstanceNetworkInterfaceSetItemRequestType primaryNetworkInterface( boolean create ) {
    if ( networkInterfaceSet == null ) {
      networkInterfaceSet = new InstanceNetworkInterfaceSetRequestType( )
    }
    InstanceNetworkInterfaceSetItemRequestType primary = networkInterfaceSet.item.find{
      InstanceNetworkInterfaceSetItemRequestType networkInterface ->
        0 == networkInterface.deviceIndex
    }
    if ( primary == null && create ) {
      primary = new InstanceNetworkInterfaceSetItemRequestType( deviceIndex: 0 )
      networkInterfaceSet.item << primary
    }
    primary
  }

  @Override
  Set<String> getTagKeys( @Nullable String resourceType, @Nullable String resourceId ) {
    getTagKeys( tagSpecification, resourceType, resourceId )
  }

  @Override
  String getTagValue( @Nullable String resourceType, @Nullable String resourceId, @Nonnull String tagKey ) {
    getTagValue( tagSpecification, resourceType, resourceId, tagKey )
  }
}
/** *******************************************************************************/
public class GetConsoleOutputResponseType extends VmControlMessage {
  
  String instanceId;
  Date timestamp;
  String output;
}

public class GetConsoleOutputType extends VmControlMessage {
  @HttpParameterMapping (parameter = ["InstanceId", "InstanceId.1"])
  String instanceId;

  GetConsoleOutputType( ) {
  }

  GetConsoleOutputType( final String instanceId ) {
    this.instanceId = instanceId
  }
}

public class GetPasswordDataType extends VmControlMessage {
  String instanceId;
}
public class GetPasswordDataResponseType extends VmControlMessage {
  
  String instanceId;
  Date timestamp;
  String output;
}

class GetConsoleScreenshotType extends VmControlMessage {
  String instanceId
  Boolean wakeUp
}

class GetConsoleScreenshotResponseType extends VmControlMessage {
  String instanceId
  String imageData = ''
}

/** *******************************************************************************/
public class ReservationInfoType extends EucalyptusData {
  String reservationId;
  String ownerId;
  ArrayList<GroupItemType> groupSet = Lists.newArrayList()
  ArrayList<RunningInstancesItemType> instancesSet = Lists.newArrayList()

  def ReservationInfoType( String reservationId, String ownerId, Collection<GroupItemType> groupIdsToNames ) {
      this.reservationId = reservationId
      this.ownerId = ownerId
      this.groupSet.addAll( groupIdsToNames )
      Collections.sort( this.groupSet )
  }
  
  def ReservationInfoType() {
  }
}

public class GroupItemType extends EucalyptusData implements Comparable<GroupItemType> {
  String groupId;
  String groupName;

  GroupItemType() {}

  def GroupItemType( String groupId, String groupName ) {
    this.groupId = groupId;
    this.groupName = groupName;
  }

  @Override
  int compareTo(final GroupItemType o) {
    return (groupId?:'').compareTo( o.groupId?:'' )
  }
}
class InstanceNetworkInterfaceSetRequestType extends EucalyptusData {
  InstanceNetworkInterfaceSetRequestType() {  }
  @HttpParameterMapping(parameter = "NetworkInterface")
  @HttpEmbedded(multiple = true)
  ArrayList<InstanceNetworkInterfaceSetItemRequestType> item = new ArrayList<InstanceNetworkInterfaceSetItemRequestType>();
}
class InstanceNetworkInterfaceSetItemRequestType extends EucalyptusData {
  String networkInterfaceId;
  Integer deviceIndex;
  String subnetId;
  String description;
  String privateIpAddress;
  @HttpEmbedded
  SecurityGroupIdSetType groupSet;
  Boolean deleteOnTermination;
  @HttpEmbedded
  PrivateIpAddressesSetRequestType privateIpAddressesSet;
  Integer secondaryPrivateIpAddressCount;
  Boolean associatePublicIpAddress;
  InstanceNetworkInterfaceSetItemRequestType() {  }

  void securityGroups( Iterable<String> groupIds ) {
    groupSet = new SecurityGroupIdSetType(
        item: groupIds.collect{ String id -> new SecurityGroupIdSetItemType( groupId: id ) } as ArrayList<SecurityGroupIdSetItemType>
    )
  }
}
class GroupIdSetType extends EucalyptusData {
  GroupIdSetType() {  }
  @HttpParameterMapping(parameter = "GroupId")
  @HttpEmbedded(multiple = true)
  ArrayList<SecurityGroupIdSetItemType> item = new ArrayList<SecurityGroupIdSetItemType>();
}
class SecurityGroupIdSetType extends EucalyptusData {
  SecurityGroupIdSetType() {  }
  @HttpParameterMapping(parameter = "SecurityGroupId")
  @HttpEmbedded(multiple = true)
  ArrayList<SecurityGroupIdSetItemType> item = new ArrayList<SecurityGroupIdSetItemType>();
  ArrayList<String> groupIds( ) {
    (item?.collect{ SecurityGroupIdSetItemType item -> item.groupId } ?: [ ]) as ArrayList<String>
  }
}
class SecurityGroupIdSetItemType extends EucalyptusData {
  @HttpValue
  String groupId;
  SecurityGroupIdSetItemType() {  }
}
class PrivateIpAddressesSetRequestType extends EucalyptusData {
  PrivateIpAddressesSetRequestType() {  }
  @HttpParameterMapping(parameter = "PrivateIpAddresses")
  @HttpEmbedded(multiple = true)
  ArrayList<PrivateIpAddressesSetItemRequestType> item = new ArrayList<PrivateIpAddressesSetItemRequestType>();
}
class PrivateIpAddressesSetItemRequestType extends EucalyptusData {
  @FieldRegex( FieldRegexValue.IP_ADDRESS )
  String privateIpAddress;
  Boolean primary;
  PrivateIpAddressesSetItemRequestType() {  }
}
public class InstanceNetworkInterfaceSetType extends EucalyptusData {
  InstanceNetworkInterfaceSetType() {  }
  ArrayList<InstanceNetworkInterfaceSetItemType> item = new ArrayList<InstanceNetworkInterfaceSetItemType>();
}
class InstanceNetworkInterfaceSetItemType extends EucalyptusData {
  String networkInterfaceId;
  String subnetId;
  String vpcId;
  String description;
  String ownerId;
  String status;
  String macAddress;
  String privateIpAddress;
  String privateDnsName;
  Boolean sourceDestCheck;
  GroupSetType groupSet;
  InstanceNetworkInterfaceAttachmentType attachment = new InstanceNetworkInterfaceAttachmentType( )
  InstanceNetworkInterfaceAssociationType association;
  InstancePrivateIpAddressesSetType privateIpAddressesSet;

  InstanceNetworkInterfaceSetItemType() {  }

  InstanceNetworkInterfaceSetItemType(
      final String networkInterfaceId,
      final String subnetId,
      final String vpcId,
      final String description,
      final String ownerId,
      final String status,
      final String macAddress,
      final String privateIpAddress,
      final String privateDnsName,
      final Boolean sourceDestCheck,
      final GroupSetType groupSet,
      final InstanceNetworkInterfaceAttachmentType attachment,
      final InstanceNetworkInterfaceAssociationType association,
      final InstancePrivateIpAddressesSetType privateIpAddressesSet) {
    this.networkInterfaceId = networkInterfaceId
    this.subnetId = subnetId
    this.vpcId = vpcId
    this.description = description
    this.ownerId = ownerId
    this.status = status
    this.macAddress = macAddress
    this.privateIpAddress = privateIpAddress
    this.privateDnsName = privateDnsName
    this.sourceDestCheck = sourceDestCheck
    this.groupSet = groupSet
    this.attachment = attachment
    this.association = association
    this.privateIpAddressesSet = privateIpAddressesSet
  }
}
class InstanceNetworkInterfaceAttachmentType extends EucalyptusData {
  String attachmentId;
  Integer deviceIndex;
  String status;
  Date attachTime;
  Boolean deleteOnTermination;
  InstanceNetworkInterfaceAttachmentType( ) {  }

  InstanceNetworkInterfaceAttachmentType(
      final String attachmentId,
      final Integer deviceIndex,
      final String status,
      final Date attachTime,
      final Boolean deleteOnTermination) {
    this.attachmentId = attachmentId
    this.deviceIndex = deviceIndex
    this.status = status
    this.attachTime = attachTime
    this.deleteOnTermination = deleteOnTermination
  }
}
class InstanceNetworkInterfaceAssociationType extends EucalyptusData {
  String publicIp;
  String publicDnsName;
  String ipOwnerId;
  InstanceNetworkInterfaceAssociationType( ) {  }

  InstanceNetworkInterfaceAssociationType( final String publicIp,
                                           final String publicDnsName,
                                           final String ipOwnerId ) {
    this.publicIp = publicIp
    this.publicDnsName = publicDnsName
    this.ipOwnerId = ipOwnerId
  }
}
class InstancePrivateIpAddressesSetType extends EucalyptusData {
  InstancePrivateIpAddressesSetType( ) {  }

  InstancePrivateIpAddressesSetType( final Collection<InstancePrivateIpAddressesSetItemType> item ) {
    this.item = Lists.newArrayList( item )
  }
  ArrayList<InstancePrivateIpAddressesSetItemType> item = new ArrayList<InstancePrivateIpAddressesSetItemType>();
}
class InstancePrivateIpAddressesSetItemType extends EucalyptusData {
  String privateIpAddress;
  String privateDnsName;
  Boolean primary;
  InstanceNetworkInterfaceAssociationType association;
  InstancePrivateIpAddressesSetItemType( ) {  }

  InstancePrivateIpAddressesSetItemType(
      final String privateIpAddress,
      final String privateDnsName,
      final Boolean primary,
      final InstanceNetworkInterfaceAssociationType association) {
    this.privateIpAddress = privateIpAddress
    this.privateDnsName = privateDnsName
    this.primary = primary
    this.association = association
  }
}
public class RunningInstancesItemType extends EucalyptusData implements Comparable<RunningInstancesItemType> {
  String instanceId;
  String imageId;
  String stateCode;
  String stateName;
  String privateDnsName;
  String dnsName;
  String reason;
  String keyName;
  String amiLaunchIndex;
  ArrayList<String> productCodes = new ArrayList<String>();
  String instanceType;
  Date launchTime;
  String placement;
  String kernel;
  String ramdisk;
  String platform;
  String architecture;
  String monitoring;
  Boolean disableApiTermination = false;
  Boolean instanceInitiatedShutdownBehavior = "stop"; //or "terminate"
  String ipAddress;
  String privateIpAddress;
  String rootDeviceType = "instance-store";
  String rootDeviceName = "/dev/sda1";
  ArrayList<InstanceBlockDeviceMapping> blockDevices = new ArrayList<InstanceBlockDeviceMapping>();
  String virtualizationType;
  String clientToken;
  IamInstanceProfile iamInstanceProfile = new IamInstanceProfile();
  ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>();
  ArrayList<GroupItemType> groupSet = Lists.newArrayList()
  String subnetId;
  String vpcId;
  Boolean sourceDestCheck;
  InstanceNetworkInterfaceSetType networkInterfaceSet;

  @Override
  public int compareTo( RunningInstancesItemType that ) {
    return this.instanceId.compareTo( that.instanceId );
  }
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.instanceId == null )
        ? 0
        : this.instanceId.hashCode( ) );
    return result;
  }
  @Override
  public boolean equals( Object obj ) {
    if ( this.is( obj ) ) {
      return true;
    }
    if ( obj == null ) {
      return false;
    }
    if ( !getClass( ).equals( obj.getClass( ) ) ) {
      return false;
    }
    RunningInstancesItemType other = ( RunningInstancesItemType ) obj;
    if ( this.instanceId == null ) {
      if ( other.instanceId != null ) {
        return false;
      }
    } else if ( !this.instanceId.equals( other.instanceId ) ) {
      return false;
    }
    return true;
  }
  
}

public class InstanceBlockDeviceMapping extends EucalyptusData {
  String deviceName;
  EbsInstanceBlockDeviceMapping ebs;
  public InstanceBlockDeviceMapping() {}
  public InstanceBlockDeviceMapping( String deviceName ) {
    this.deviceName = deviceName;
  }
  public InstanceBlockDeviceMapping( String deviceName, String volumeId, String status, Date attachTime, Boolean deleteOnTerminate ) {
    this.deviceName = deviceName;
    this.ebs = new EbsInstanceBlockDeviceMapping( volumeId, status, attachTime, deleteOnTerminate );
  }
}

public class EbsInstanceBlockDeviceMapping extends EucalyptusData {
  String volumeId;
  String status;
  Date attachTime;
  Boolean deleteOnTermination;
  public EbsInstanceBlockDeviceMapping() {}
  public EbsInstanceBlockDeviceMapping( String volumeId, String status, Date attachTime, Boolean deleteOnTerminate ) {
    this.volumeId = volumeId;
    this.status = status;
    this.attachTime = attachTime;
    this.deleteOnTermination = deleteOnTerminate;
  }
}
public class EbsDeviceMapping extends EucalyptusData {  //** added 2008-02-01  **/
  String virtualName; // ephemeralN, root, ami, swap
  String snapshotId;
  Integer volumeSize = null;
  Boolean encrypted
  Boolean deleteOnTermination = Boolean.TRUE;
  String volumeType = "standard"
  Integer iops
}

public class BlockDeviceMappingItemType extends EucalyptusData {  //** added 2008-02-01  **/
  String virtualName; // ephemeralN, root, ami, swap
  String deviceName;
  Integer size; // in megabytes //TODO:GRZE: maybe remove
  String format; // optional, defaults to none (none, ext3, ntfs, swap) //TODO:GRZE: maybe remove
  Boolean noDevice; // suppress mapping, added 2013-03. This should be a string, any value means there is no device (!= null)
  @HttpEmbedded (multiple = true)
  EbsDeviceMapping ebs;
  def BlockDeviceMappingItemType(final virtualName, final deviceName) {
    this.virtualName = virtualName;
    this.deviceName = deviceName;
  }
  
  def BlockDeviceMappingItemType() {
  }

  // Adding hashCode() and equals() to support set operations on BlockDeviceMappingItemType objects
  
  @Override
  public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((deviceName == null) ? 0 : deviceName.hashCode());
	return result;
  }

  @Override
  public boolean equals(Object obj) {
	if ( this.is( obj ) )
		return true;
	if (obj == null)
		return false;
	if (getClass() != obj.getClass())
		return false;
	BlockDeviceMappingItemType other = (BlockDeviceMappingItemType) obj;
	if (deviceName == null) {
		if (other.deviceName != null)
			return false;
	} else if (!deviceName.equals(other.deviceName))
		return false;
	return true;
  }
}

public class InstanceStateType extends EucalyptusData {
  int code;
  String name;
}
public class TerminateInstancesItemType extends EucalyptusData {
  String instanceId;
  String previousStateCode;
  String previousStateName;
  String shutdownStateCode;
  String shutdownStateName;
  
  def TerminateInstancesItemType(final instanceId, final previousStateCode, final previousStateName, final shutdownStateCode, final shutdownStateName) {
    this.instanceId = instanceId;
    this.previousStateCode = previousStateCode;
    this.previousStateName = previousStateName;
    this.shutdownStateCode = shutdownStateCode;
    this.shutdownStateName = shutdownStateName;
  }
  
  def TerminateInstancesItemType() {
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.instanceId == null ) ? 0 : this.instanceId.hashCode( ) );
    return result;
  }

  @Override
  public boolean equals( Object obj ) {
    if ( this.is( obj ) ) {
      return true;
    }
    if ( obj == null ) {
      return false;
    }
    if ( !getClass( ).is( obj.getClass( ) ) ) {
      return false;
    }
    TerminateInstancesItemType other = ( TerminateInstancesItemType ) obj;
    if ( this.instanceId == null ) {
      if ( other.instanceId != null ) {
        return false;
      }
    } else if ( !this.instanceId.equals( other.instanceId ) ) {
      return false;
    }
    return true;
  }
  
}

public class StopInstancesResponseType extends VmControlMessage{
  ArrayList<TerminateInstancesItemType> instancesSet = new ArrayList<TerminateInstancesItemType>();
  public StopInstancesResponseType() {  }
}

public class StopInstancesType extends VmControlMessage{
  @HttpParameterMapping( parameter = "InstanceId" )
  ArrayList<String> instancesSet = new ArrayList<String>();
  Boolean force;
  public StopInstancesType() {  }
}
public class StartInstancesResponseType extends VmControlMessage{
  ArrayList<TerminateInstancesItemType> instancesSet = new ArrayList<TerminateInstancesItemType>();
  public StartInstancesResponseType() {  }
}

public class StartInstancesType extends VmControlMessage{
  @HttpParameterMapping( parameter = "InstanceId" )
  ArrayList<String> instancesSet = new ArrayList<String>();
  String additionalInfo
  public StartInstancesType() {  }
}

public class InstanceEbsBlockDeviceType extends EucalyptusData {
  String volumeId
  Boolean deleteOnTermination = true
}

public class InstanceBlockDeviceMappingItemType extends EucalyptusData {
  String deviceName
  InstanceEbsBlockDeviceType ebs = new InstanceEbsBlockDeviceType( )
}

public class InstanceBlockDeviceMappingSetType extends EucalyptusData {
  @HttpParameterMapping(parameter = "BlockDeviceMapping")
  @HttpEmbedded(multiple = true)
  ArrayList<InstanceBlockDeviceMappingItemType> item = Lists.newArrayList( )
}

public class ModifyInstanceAttributeType extends VmControlMessage {
  String instanceId;
  AttributeValueType instanceType;
  AttributeValueType kernel;
  AttributeValueType ramdisk;
  AttributeValueType userData;
  AttributeBooleanValueType disableApiTermination
  AttributeValueType instanceInitiatedShutdownBehavior
  @HttpEmbedded
  InstanceBlockDeviceMappingSetType blockDeviceMappingSet
  AttributeBooleanValueType sourceDestCheck
  @HttpEmbedded
  GroupIdSetType groupIdSet
  @HttpParameterMapping(parameter = ["EbsOptimized", "EbsOptimized.Value"])
  AttributeBooleanFlatValueType ebsOptimized
  AttributeBooleanValueType sriovNetSupport
}

public class ModifyInstanceAttributeResponseType extends VmControlMessage {
  public ModifyInstanceAttributeResponseType() {  }
}

public class ResetInstanceAttributeType extends VmControlMessage {
  String instanceId;
  String attribute;
  public ResetInstanceAttributeType() {  }
}
public class ResetInstanceAttributeResponseType extends VmControlMessage {
  public ResetInstanceAttributeResponseType() {  }
}

public class DescribeInstanceAttributeType extends VmControlMessage {
  String instanceId;
  String attribute;
  public DescribeInstanceAttributeType() {  }
}
public class DescribeInstanceAttributeResponseType extends VmControlMessage {
  String instanceId
  ArrayList<InstanceBlockDeviceMapping> blockDeviceMapping = Lists.newArrayList( )
  Boolean disableApiTermination
  Boolean ebsOptimized
  ArrayList<GroupItemType> groupSet = Lists.newArrayList( )
  String instanceInitiatedShutdownBehavior
  String instanceType
  String kernel
  Boolean productCodes // not supported, would be a list of codes
  String ramdisk
  String rootDeviceName
  Boolean sourceDestCheck
  Boolean sriovNetSupport
  String userData

  boolean hasDisableApiTermination( ) {
    this.disableApiTermination != null
  }
  boolean hasEbsOptimized( ) {
    this.ebsOptimized != null
  }
  boolean hasInstanceType() {
    this.instanceType != null
  }
  boolean hasInstanceInitiatedShutdownBehavior( ) {
    this.instanceInitiatedShutdownBehavior != null
  }
  boolean hasKernel() {
    this.kernel != null
  }
  boolean hasProductCodes() {
    this.productCodes != null
  }
  boolean hasRamdisk() {
    this.ramdisk != null
  }
  boolean hasRootDeviceName() {
    this.rootDeviceName != null
  }
  boolean hasUserData() {
    this.userData != null
  }
  boolean hasNonEmptyUserData() {
    this.userData != null && !this.userData.empty
  }
  boolean hasBlockDeviceMapping() {
    !this.blockDeviceMapping.isEmpty( )
  }
  boolean hasGroupSet( ) {
    !this.groupSet.isEmpty( )
  }
  boolean hasSourceDestCheck( ) {
    this.sourceDestCheck != null
  }
  boolean hasSriovNetSupport( ) {
    return this.sriovNetSupport != null
  }
}
public class MonitorInstanceState extends EucalyptusData {
  String instanceId;
  String monitoringState;
  public MonitorInstanceState() {}
}

public class IamInstanceProfile extends EucalyptusData {
  String arn;
  String id;
  public IamInstanceProfile() {}

    def IamInstanceProfile(String arn, String id) {
        this.arn = arn;
        this.id = id;
    }
}

public class MonitorInstancesResponseType extends VmControlMessage {
  ArrayList<MonitorInstanceState> instancesSet = new ArrayList<MonitorInstanceState>();
  public MonitorInstancesResponseType() {  }
}

public class MonitorInstancesType extends VmControlMessage {
  @HttpParameterMapping(parameter="InstanceId")
  ArrayList<String> instancesSet = new ArrayList<String>();
  public MonitorInstancesType() {  }
}
public class UnmonitorInstancesResponseType extends VmControlMessage {
  ArrayList<MonitorInstanceState> instancesSet = new ArrayList<MonitorInstanceState>();
  public UnmonitorInstancesResponseType() {  }
}

public class UnmonitorInstancesType extends VmControlMessage {
  @HttpParameterMapping(parameter="InstanceId")
  ArrayList<String> instancesSet = new ArrayList<String>();
  public MonitorInstancesType() {  }
}

class IamInstanceProfileComputeMessage extends VmControlMessage { } // subclass useful for binding generation

class  IamInstanceProfileSpecification extends EucalyptusData {
  String arn
  String name
}

class AssociateIamInstanceProfileType extends IamInstanceProfileComputeMessage {
  IamInstanceProfileSpecification iamInstanceProfile
  String instanceId
}

class AssociateIamInstanceProfileResponseType extends IamInstanceProfileComputeMessage {
}

class DescribeIamInstanceProfileAssociationsType extends IamInstanceProfileComputeMessage {
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  ArrayList<String> associationId = Lists.newArrayList( )
  Integer maxResults
  String nextToken
}

class DescribeIamInstanceProfileAssociationsResponseType extends IamInstanceProfileComputeMessage {
}

class DisassociateIamInstanceProfileType extends IamInstanceProfileComputeMessage {
  String associationId
}

class DisassociateIamInstanceProfileResponseType extends IamInstanceProfileComputeMessage {
}

class ReplaceIamInstanceProfileAssociationType extends IamInstanceProfileComputeMessage {
  String associationId
  IamInstanceProfileSpecification iamInstanceProfile
}

class ReplaceIamInstanceProfileAssociationResponseType extends IamInstanceProfileComputeMessage {
}



