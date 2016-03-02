/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

/**
 * Messages for operations related to reading, updating, and interrogating vm type definitions.
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

@GroovyAddClassUUID
package com.eucalyptus.compute.common

import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.util.MessageValidation
import com.google.common.collect.Maps
import edu.ucsb.eucalyptus.msgs.ComputeMessageValidation;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID
import com.google.common.collect.Lists

import javax.annotation.Nonnull

import static com.eucalyptus.util.MessageValidation.validateRecursively

import static edu.ucsb.eucalyptus.msgs.ComputeMessageValidation.FieldRange

public class VmTypeMessage extends ComputeMessage implements MessageValidation.ValidatableMessage {

  Map<String,String> validate( ) {
    validateRecursively(
        Maps.<String,String>newTreeMap( ),
        new ComputeMessageValidation.ComputeMessageValidationAssistant( ),
        "",
        this )
  }
}
public class VmTypeDetails extends EucalyptusData {
  String name;
  Integer cpu;
  Integer disk;
  Integer memory;
  Integer networkInterfaces
  ArrayList<VmTypeZoneStatus> availability = new ArrayList<VmTypeZoneStatus>( );
  ArrayList<VmTypeEphemeralDisk> ephemeralDisk = new ArrayList<VmTypeEphemeralDisk>( );
}
public class VmTypeZoneStatus extends EucalyptusData {
  String name;
  String zoneName;
  Integer max;
  Integer available;
}
public class VmTypeEphemeralDisk extends EucalyptusData {
  String virtualDeviceName;
  String deviceName;
  Integer size;
  String format;
  VmTypeEphemeralDisk( ) { }
  VmTypeEphemeralDisk( String virtualDeviceName, String deviceName, Integer size, String format ) {
    super( );
    this.virtualDeviceName = virtualDeviceName;
    this.deviceName = deviceName;
    this.size = size;
    this.format = format;
  }
}
public class ModifyInstanceTypeAttributeType extends VmTypeMessage {
  Boolean reset = false;
  Boolean force = false;
  @Nonnull
  String name;
  @FieldRange( min = 1l )
  Integer cpu;
  @FieldRange( min = 1l )
  Integer disk;
  @FieldRange( min = 1l )
  Integer memory;
  @FieldRange( min = 1l, max = 8l )
  Integer networkInterfaces;
}
public class ModifyInstanceTypeAttributeResponseType extends VmTypeMessage {
  VmTypeDetails instanceType;
  VmTypeDetails previousInstanceType;
}
public class DescribeInstanceTypesType extends VmTypeMessage {
  Boolean verbose = false;
  Boolean availability = false;
  @HttpParameterMapping(parameter="InstanceType")
  ArrayList<String> instanceTypes = new ArrayList<String>();
  DescribeInstanceTypesType () { }
  DescribeInstanceTypesType ( Collection<String> instanceTypes ) {
    this.instanceTypes.addAll( instanceTypes )
  }
}
public class DescribeInstanceTypesResponseType extends VmTypeMessage  {
  ArrayList<VmTypeDetails> instanceTypeDetails = Lists.newArrayList()
}
