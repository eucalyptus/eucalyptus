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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.msgs

import edu.ucsb.eucalyptus.annotation.HttpEmbedded;
import edu.ucsb.eucalyptus.annotation.HttpParameterMapping;

public class VmControlMessage extends EucalyptusMessage {}
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
  @HttpEmbedded (multiple = true)
  ArrayList<BlockDeviceMappingItemType> blockDeviceMapping = new ArrayList<BlockDeviceMappingItemType>(); //** added 2008-02-01  **/
  boolean monitoring = false;


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
  boolean monitoring = false;
}
public class BlockDeviceMappingItemType extends EucalyptusData {  //** added 2008-02-01  **/
  String virtualName;
  String deviceName;

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

