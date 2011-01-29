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
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.msgs

import com.eucalyptus.auth.policy.PolicyAction;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;

public class VmControlMessage extends EucalyptusMessage {}
public class ResourceTagMessage extends EucalyptusMessage {}
public class VmPlacementMessage extends EucalyptusMessage {}
/** *******************************************************************************/
public class TerminateInstancesResponseType extends VmControlMessage {
  boolean terminated = false;
  ArrayList<TerminateInstancesItemType> instancesSet = new ArrayList<TerminateInstancesItemType>();
}
public class TerminateInstancesType extends VmControlMessage {

  @HttpParameterMapping (parameter = "InstanceId")
  ArrayList<String> instancesSet = new ArrayList<String>();

  def TerminateInstancesType() {}

  def TerminateInstancesType(String instanceId) {
    this.instancesSet.add(instanceId);
  }

  def TerminateInstancesType(String instanceId, EucalyptusMessage parent) {
    this.instancesSet.add(instanceId);
    this.correlationId = parent.correlationId;
    this.userId = parent.userId;
    this.effectiveUserId = parent.effectiveUserId;
  }
}
/** *******************************************************************************/
public class DescribeInstancesType extends VmControlMessage {

  @HttpParameterMapping (parameter = "InstanceId")
  ArrayList<String> instancesSet = new ArrayList<String>();
}
public class DescribeInstancesResponseType extends VmControlMessage {

  ArrayList<ReservationInfoType> reservationSet = new ArrayList<ReservationInfoType>();
}
/** *******************************************************************************/
public class RebootInstancesType extends VmControlMessage {

  @HttpParameterMapping (parameter = "InstanceId")
  ArrayList<String> instancesSet = new ArrayList<String>();

  def RebootInstancesType() {}
  def RebootInstancesType(String instanceId) {
    this.instancesSet.add(instanceId);
  }

  def RebootInstancesType(String instanceId, EucalyptusMessage parent) {
    this.instancesSet.add(instanceId);
    this.correlationId = parent.correlationId;
    this.userId = parent.userId;
    this.effectiveUserId = parent.effectiveUserId;
  }
}
public class RebootInstancesResponseType extends VmControlMessage {

  boolean _return;
}
/** *******************************************************************************/
public class RunInstancesResponseType extends VmControlMessage {

  ReservationInfoType rsvInfo;
}

@PolicyAction( vendor = PolicySpec.VENDOR_EC2, action = PolicySpec.EC2_RUNINSTANCES )
public class RunInstancesType extends VmControlMessage {

  String imageId;
  String reservationId;
  int minCount;
  int maxCount;
  String keyName;
  ArrayList<String> instanceIds = new ArrayList<String>();
  @HttpParameterMapping (parameter = "SecurityGroup")
  ArrayList<String> groupSet = new ArrayList<String>();
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
  @HttpEmbedded (multiple = true)
  ArrayList<BlockDeviceMappingItemType> blockDeviceMapping = new ArrayList<BlockDeviceMappingItemType>(); //** added 2008-02-01  **/
  Boolean monitoring = false;
  String subnetId;
  Boolean disableTerminate;
  String shutdownAction = "stop"; //or "terminate"
  /** InstanceLicenseRequest license; **/
  String privateIpAddress = "";
  String clientToken = "";

  ArrayList<Integer> networkIndexList = new ArrayList<Integer>();
  String privateMacBase;
  String publicMacBase;
  int macLimit;
  int vlan;
  VmTypeInfo vmType = new VmTypeInfo();


  public Object clone() {
    RunInstancesType c = (RunInstancesType) super.clone();
    c.instanceIds = new ArrayList<String>();
    if ( this.instanceIds != null )
    for ( String b: this.instanceIds )
      c.instanceIds.add((String) b.clone());
    c.blockDeviceMapping = new ArrayList<BlockDeviceMappingItemType>();
    if ( this.blockDeviceMapping != null )
    for ( BlockDeviceMappingItemType b: this.blockDeviceMapping )
      c.blockDeviceMapping.add((BlockDeviceMappingItemType) b.clone());
    if ( this.vmType != null )
    c.vmType = (VmTypeInfo) this.vmType.clone();
    c.networkIndexList = this.networkIndexList.clone( );
    return c;
  }

}
/** *******************************************************************************/
public class GetConsoleOutputResponseType extends VmControlMessage {

  String instanceId;
  Date timestamp;
  String output;
}
public class GetConsoleOutputType extends VmControlMessage {
  @HttpParameterMapping (parameter = "InstanceId.1")
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
  ArrayList<String> groupSet = new ArrayList<String>();
  ArrayList<RunningInstancesItemType> instancesSet = new ArrayList<RunningInstancesItemType>();

  def ReservationInfoType(final reservationId, final ownerId, final groupSet) {
    this.reservationId = reservationId;
    this.ownerId = ownerId;
    this.groupSet = groupSet;
  }

  def ReservationInfoType() {
  }
}
public class RunningInstancesItemType extends EucalyptusData {
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
  Boolean monitoring = false;
  Boolean disableApiTermination = false;
  Boolean instanceInitiatedShutdownBehavior = "stop"; //or "terminate"
}
public class EbsDeviceMapping extends EucalyptusData {  //** added 2008-02-01  **/
  String snapshotId;
  Integer volumeSize = -1;
  Boolean noDevice = true;
  Boolean deleteOnTermination = true;
}

public class BlockDeviceMappingItemType extends EucalyptusData {  //** added 2008-02-01  **/
  String virtualName; // ephemeralN, root, ami, swap
  String deviceName;
  Integer size; // in megabytes
  String format; // optional, defaults to none (none, ext3, ntfs, swap)
  EbsDeviceMapping ebsDev;
  def BlockDeviceMappingItemType(final virtualName, final deviceName) {
    this.virtualName = virtualName;
    this.deviceName = deviceName;
  }

  def BlockDeviceMappingItemType() {
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
}

public class StopInstancesResponseType extends VmControlMessage{
  ArrayList<TerminateInstancesItemType> instancesSet = new ArrayList<TerminateInstancesItemType>();
  public StopInstancesResponseType() {  }
}
public class StopInstancesType extends VmControlMessage{
  ArrayList<String> instancesSet = new ArrayList<String>();
  Boolean force;
  public StopInstancesType() {  }
}
public class StartInstancesResponseType extends VmControlMessage{
  ArrayList<TerminateInstancesItemType> instancesSet = new ArrayList<TerminateInstancesItemType>();
  public StartInstancesResponseType() {  }
}
public class StartInstancesType extends VmControlMessage{
  ArrayList<String> instancesSet = new ArrayList<String>();
  public StartInstancesType() {  }
}

public class ModifyInstanceAttributeType extends VmControlMessage {
  String instanceId;
  String element;
  String value;
  ArrayList<BlockDeviceMappingItemType> blockDeviceMapping = new ArrayList<BlockDeviceMappingItemType>();
  public ModifyInstanceAttributeType() {  }
  public void instanceType() { this.element = "instanceType"; }
  public void kernel() { this.element = "kernel"; }
  public void ramdisk() { this.element = "ramdisk"; }
  public void userData() { this.element = "userData"; }
  public void disableApiTermination() { this.element = "disableApiTermination"; }
  public void instanceInitiatedShutdownBehavior() { this.element = "instanceInitiatedShutdownBehavior"; }
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
public class MonitorInstancesResponseType extends VmControlMessage {
  ArrayList<MonitorInstanceState> instancesSet = new ArrayList<MonitorInstanceState>();
  public MonitorInstancesResponseType() {  }
}
public class MonitorInstancesType extends VmControlMessage {
  ArrayList<String> instancesSet = new ArrayList<String>();
  public MonitorInstancesType() {  }
}
public class UnmonitorInstancesResponseType extends VmControlMessage {
  ArrayList<MonitorInstanceState> instancesSet = new ArrayList<MonitorInstanceState>();
  public UnmonitorInstancesResponseType() {  }
}
public class UnmonitorInstancesType extends VmControlMessage {
  ArrayList<String> instancesSet = new ArrayList<String>();
  public MonitorInstancesType() {  }
}

public class CreateTagsResponseType extends ResourceTagMessage  {
  public CreateTagsResponseType() {  }
}
public class CreateTagsType extends ResourceTagMessage  {
  ArrayList<String> resourcesSet = new ArrayList<String>();
  ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>();
  public CreateTagsType() {  }
}

public class FilterType extends EucalyptusData {
  String name;
  ArrayList<String> valueSet = new ArrayList<String>();
  public FilterType() {  }
}
public class ResourceTag extends EucalyptusData {
  String key;
  String value;
  public ResourceTagSetItemType() {  }
}
public class DescribeTagsType extends ResourceTagMessage  {
  ArrayList<FilterType> filterSet = new ArrayList<FilterType>();
  public DescribeTagsType() {  }
}
public class DescribeTagsResponseType extends ResourceTagMessage  {
  String requestId;
  ArrayList<TagInfo> tagSet = new ArrayList<TagInfo>( );
  public DescribeTagsResponseType() {  }
}
public class DeleteTagsResponseType extends ResourceTagMessage {
  String requestId;
  Boolean _return;
  public DeleteTagsResponseType() {  }
}
public class DeleteTagsType extends ResourceTagMessage {
  ArrayList<String> resourcesSet = new ArrayList<String>();
  ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>();
  public DeleteTagsType() {  }
}
public class TagInfo extends EucalyptusData {
  String resourceId;
  String resourceType;
  String key;
  String value;
  public TagSetItemType() {  }
}
public class CreatePlacementGroupType extends VmPlacementMessage {
  String groupName;
  String strategy;
  public CreatePlacementGroupType() {  }
}
public class CreatePlacementGroupResponseType extends VmPlacementMessage {
  public CreatePlacementGroupResponseType() {  }
}
public class DeletePlacementGroupType extends VmPlacementMessage {
  String groupName;
  public DeletePlacementGroupType() {  }
}
public class DeletePlacementGroupResponseType extends VmPlacementMessage {
  public DeletePlacementGroupResponseType() {  }
}
public class PlacementGroupInfo extends EucalyptusData {
  String groupName;
  String strategy;
  String state;
  public PlacementGroupInfoType() {  }
}
public class DescribePlacementGroupsType extends VmPlacementMessage {
  ArrayList<String> placementGroupSet = new ArrayList<String>();
  ArrayList<FilterType> filterSet = new ArrayList<FilterType>();
  public DescribePlacementGroupsType() {  }
}
public class DescribePlacementGroupsResponseType extends VmPlacementMessage {
  ArrayList<PlacementGroupInfo> placementGroupSet = new ArrayList<PlacementGroupInfo>();
  public DescribePlacementGroupsResponseType() {  }
}
