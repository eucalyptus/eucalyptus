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
package edu.ucsb.eucalyptus.msgs

import com.eucalyptus.binding.HttpEmbedded
import com.eucalyptus.binding.HttpParameterMapping

public class VmControlMessage extends EucalyptusMessage {
  
  public VmControlMessage( ) {
    super( );
  }
  
  public VmControlMessage( EucalyptusMessage msg ) {
    super( msg );
  }
  
  public VmControlMessage( String userId ) {
    super( userId );
  }
}
public class VmPlacementMessage extends EucalyptusMessage {
  
  public VmPlacementMessage( ) {
    super( );
  }
  
  public VmPlacementMessage( EucalyptusMessage msg ) {
    super( msg );
  }
  
  public VmPlacementMessage( String userId ) {
    super( userId );
  }
}
/** *******************************************************************************/
public class TerminateInstancesResponseType extends VmControlMessage {
  boolean terminated = false;
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


public class RunInstancesType extends VmControlMessage {
  
  String imageId;
  String reservationId;
  int minCount;
  int maxCount;
  String keyName;
  ArrayList<String> instanceIds = [];
  @HttpParameterMapping (parameter = "SecurityGroup")
  ArrayList<String> groupSet = [] // Query binding
  @HttpParameterMapping (parameter = "SecurityGroupId")
  ArrayList<String> groupIdSet = []  // Query binding and also SOAP binding before 2011-01-01
  @HttpEmbedded
  ArrayList<GroupItemType> securityGroups = [] // Used in SOAP binding since 2011-01-01
  String additionalInfo;
  String userData;
  String version;
  String encoding;
  String addressingType;
  String instanceType = "m1.small";
  String kernelId; //** added 2008-02-01  **/
  String ramdiskId; //** added 2008-02-01  **/
  @HttpParameterMapping (parameter = "Placement.AvailabilityZone")
  String availabilityZone = "default"; //** added 2008-02-01  **/
  @HttpParameterMapping (parameter = "Placement.GroupName")
  String placementGroup = "default"; //** added 2010-02-01  **/
  @HttpParameterMapping (parameter = "Placement.Tenancy")
  String placementTenancy = "default"
  @HttpEmbedded (multiple = true)
  ArrayList<BlockDeviceMappingItemType> blockDeviceMapping = []; //** added 2008-02-01  **/
  @HttpParameterMapping (parameter = "Monitoring.Enabled")
  Boolean monitoring = false;
  String subnetId;
  @HttpParameterMapping (parameter = "DisableApiTermination")
  Boolean disableTerminate;
  @HttpParameterMapping (parameter = "InstanceInitiatedShutdownBehavior")
  String shutdownAction = "stop"; //or "terminate"
  /** InstanceLicenseRequest license; **/
  String privateIpAddress = "";
  String clientToken;
  @HttpParameterMapping(parameter = "IamInstanceProfile.Arn")
  String iamInstanceProfileArn
  @HttpParameterMapping(parameter = "IamInstanceProfile.Name")
  String iamInstanceProfileName
  ArrayList<Integer> networkIndexList = [];
  String privateMacBase;
  String publicMacBase;
  int macLimit;
  int vlan;
  Boolean ebsOptimized = Boolean.FALSE
  @HttpEmbedded
  VmTypeInfo vmType = new VmTypeInfo();

  Set<String> securityGroupNames() {
    Set<String> names = []
    names.addAll( groupSet )
    names.addAll( groupIdSet.findAll{ !it.startsWith( "sg-" ) } ) // ID was historically the name
    names.addAll( securityGroups.findAll{ it.groupName != null }.collect{ it.groupName } )
    names.addAll( securityGroups.findAll{ it.groupId != null && !it.groupId.startsWith( "sg-" ) }.collect{ it.groupId } )
    names
  }

  Set<String> securityGroupsIds() {
    Set<String> names = []
    names.addAll( groupIdSet.findAll{ it.startsWith( "sg-" ) } ) // ID was historically the name
    names.addAll( securityGroups.findAll{ it.groupId != null && it.groupId.startsWith( "sg-" ) }.collect{ it.groupId } )
    names
  }

  public Object clone() {
    RunInstancesType c = (RunInstancesType) super.clone();
    c.instanceIds = (ArrayList<String>)this.instanceIds.clone()
    c.groupSet = (ArrayList<String>)this.groupSet.clone()
    c.groupIdSet = (ArrayList<String>)this.groupIdSet.clone()
    c.securityGroups = []
    if ( this.securityGroups != null )
      for ( GroupItemType groupItemType: this.securityGroups )
        c.securityGroups.add((GroupItemType) groupItemType.clone());
    c.blockDeviceMapping = []
    if ( this.blockDeviceMapping != null )
      for ( BlockDeviceMappingItemType b: this.blockDeviceMapping )
        c.blockDeviceMapping.add((BlockDeviceMappingItemType) b.clone());
    if ( this.vmType != null )
      c.vmType = (VmTypeInfo) this.vmType.clone();
    c.networkIndexList = (ArrayList<Integer>)this.networkIndexList.clone( );
    c.vmType = (VmTypeInfo) this.vmType.clone()
    return c;
  }

  void setInstanceProfileNameOrArn ( String nameOrArn ) {
    if ( nameOrArn.startsWith( "arn:" ) ) {
        this.iamInstanceProfileArn = nameOrArn;
    } else {
        this.iamInstanceProfileName = nameOrArn;
    }
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
}

public class GetPasswordDataType extends VmControlMessage {
  String instanceId;
}
public class GetPasswordDataResponseType extends VmControlMessage {
  
  String instanceId;
  Date timestamp;
  String output;
}
/** *******************************************************************************/
public class ReservationInfoType extends EucalyptusData {
  String reservationId;
  String ownerId;
  ArrayList<GroupItemType> groupSet = []
  ArrayList<RunningInstancesItemType> instancesSet = []

  def ReservationInfoType( String reservationId, String ownerId, Map<String,String> groupIdsToNames ) {
      this.reservationId = reservationId;
      this.ownerId = ownerId
      this.groupSet.addAll( groupIdsToNames.entrySet().collect{
        entry -> new GroupItemType( entry.key, entry.value ) } );
  }
  
  def ReservationInfoType() {
  }
}

public class GroupItemType extends EucalyptusData {
  String groupId;
  String groupName;

  GroupItemType() {}

  def GroupItemType( String groupId, String groupName ) {
    this.groupId = groupId;
    this.groupName = groupName;
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
  Boolean deleteOnTermination = Boolean.TRUE;
  String volumeType = "standard"
  Integer iops
}

public class BlockDeviceMappingItemType extends EucalyptusData {  //** added 2008-02-01  **/
  String virtualName; // ephemeralN, root, ami, swap
  String deviceName;
  Integer size; // in megabytes //TODO:GRZE: maybe remove
  String format; // optional, defaults to none (none, ext3, ntfs, swap) //TODO:GRZE: maybe remove
  Boolean noDevice; // suppress mapping, added 2013-03
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
  public StartInstancesType() {  }
}

public class ModifyInstanceAttributeType extends VmControlMessage {
  String instanceId;
  Attr element;
  String value;
  enum Attr { instanceType, kernel, ramdisk, userData, disableApiTermination, instanceInitiatedShutdownBehavior };
  @HttpEmbedded(multiple=true)
  ArrayList<BlockDeviceMappingItemType> blockDeviceMapping = new ArrayList<BlockDeviceMappingItemType>();
  public ModifyInstanceAttributeType() {  }
  public void instanceType( String value ) { this.element = Attr.instanceType; this.value = value; }
  public void kernel( String value ) { this.element = Attr.kernel; this.value = value; }
  public void ramdisk( String value ) { this.element = Attr.ramdisk; this.value = value; }
  public void userData( String value ) { this.element = Attr.userData; this.value = value; }
  public void disableApiTermination( String value ) { this.element = Attr.disableApiTermination; this.value = value; }
  public void instanceInitiatedShutdownBehavior( String value ) { this.element = Attr.instanceInitiatedShutdownBehavior; this.value = value; }
}
public class ModifyInstanceAttributeResponseType extends VmControlMessage {
  public ModifyInstanceAttributeResponseType() {  }
}

public class ResetInstanceAttributeType extends VmControlMessage {
  String instanceId;
  public ResetInstanceAttributeType() {  }
}
public class ResetInstanceAttributeResponseType extends VmControlMessage {
  public ResetInstanceAttributeResponseType() {  }
}

public class DescribeInstanceAttributeType extends VmControlMessage {
  String instanceId;
  public DescribeInstanceAttributeType() {  }
}
public class DescribeInstanceAttributeResponseType extends VmControlMessage {
  String requestId;
  String instanceId;
  String instanceType;
  String kernel;
  String ramdisk;
  String userData;
  String disableApiTermination;
  String instanceInitiatedShutdownBehavior;
  String rootDeviceName;
  ArrayList<BlockDeviceMappingItemType> blockDeviceMapping = new ArrayList<BlockDeviceMappingItemType>();
  public DescribeInstanceAttributeResponseType() {  }
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
