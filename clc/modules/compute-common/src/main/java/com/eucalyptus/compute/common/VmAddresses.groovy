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
package com.eucalyptus.compute.common;

import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.binding.HttpEmbedded
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID;

public class VmAddressMessage extends ComputeMessage{
  
  public VmAddressMessage( ) {
    super( );
  }
  
  public VmAddressMessage( ComputeMessage msg ) {
    super( msg );
  }
  
  public VmAddressMessage( String userId ) {
    super( userId );
  }
}
/** *******************************************************************************/

public class AllocateAddressType extends VmAddressMessage {
  String domain
} //** added 2008-02-01  **/
public class AllocateAddressResponseType extends VmAddressMessage { //** added 2008-02-01  **/
  String publicIp;
  String domain;
  String allocationId;
}
/** *******************************************************************************/

public class ReleaseAddressType extends VmAddressMessage { //** added 2008-02-01  **/
  String publicIp;
  String allocationId;
  
  def ReleaseAddressType(final publicIp) {
    this.publicIp = publicIp;
  }
  
  def ReleaseAddressType() {}
}
public class ReleaseAddressResponseType extends VmAddressMessage { //** added 2008-02-01  **/
}
/** *******************************************************************************/

public class DescribeAddressesType extends VmAddressMessage { //** added 2008-02-01  **/
  @HttpParameterMapping (parameter = "PublicIp")
  ArrayList<String> publicIpsSet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  @HttpParameterMapping (parameter = "AllocationId")
  ArrayList<String> allocationIds = new ArrayList<String>();
  
}
public class DescribeAddressesResponseType extends VmAddressMessage { //** added 2008-02-01  **/
  ArrayList<AddressInfoType> addressesSet = new ArrayList<AddressInfoType>();
}
/** *******************************************************************************/

public class AssociateAddressType extends VmAddressMessage { //** added 2008-02-01  **/
  String publicIp;
  String instanceId;
  String allocationId
  String networkInterfaceId
  String privateIpAddress
  Boolean allowReassociation = Boolean.TRUE
  
  def AssociateAddressType(final publicIp, final instanceId) {
    this.publicIp = publicIp;
    this.instanceId = instanceId;
  }
  
  def AssociateAddressType() {
  }
}
public class AssociateAddressResponseType extends VmAddressMessage { //** added 2008-02-01  **/
  String associationId
}
/** *******************************************************************************/

public class DisassociateAddressType extends VmAddressMessage {  //** added 2008-02-01  **/
  String publicIp;
  String associationId;
}
public class DisassociateAddressResponseType extends VmAddressMessage { //** added 2008-02-01  **/
}
/** *******************************************************************************/
public class AddressInfoType extends EucalyptusData {  //** added 2008-02-01  **/
  String publicIp;
  String allocationId
  String domain
  String instanceId;
  String associationId
  String networkInterfaceId
  String networkInterfaceOwnerId
  String privateIpAddress

  def AddressInfoType( final String publicIp, final String domain, final String instanceId )
  {
    this.publicIp = publicIp
    this.domain = domain
    this.instanceId = instanceId
  }
  
  def AddressInfoType()
  {
  }
  
}

class DescribeMovingAddressesType extends VmAddressMessage {
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  @HttpParameterMapping (parameter = "PublicIp")
  ArrayList<String> publicIpsSet = new ArrayList<String>();
  Integer maxResults
  String nextToken
}

class DescribeMovingAddressesResponseType extends VmAddressMessage {
}

class MoveAddressToVpcType extends VmAddressMessage {
  String publicIp
}

class MoveAddressToVpcResponseType extends VmAddressMessage {

}

class RestoreAddressToClassicType extends VmAddressMessage {
  String publicIp
}

class RestoreAddressToClassicResponseType extends VmAddressMessage {

}