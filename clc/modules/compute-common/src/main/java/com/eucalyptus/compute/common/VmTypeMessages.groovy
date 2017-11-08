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
