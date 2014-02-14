/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
  
} //** added 2008-02-01  **/
public class AllocateAddressResponseType extends VmAddressMessage { //** added 2008-02-01  **/
  String publicIp;
}
/** *******************************************************************************/

public class ReleaseAddressType extends VmAddressMessage { //** added 2008-02-01  **/
  String publicIp;
  
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
  Boolean allowReassociation = Boolean.FALSE
  
  def AssociateAddressType(final publicIp, final instanceId) {
    this.publicIp = publicIp;
    this.instanceId = instanceId;
  }
  
  def AssociateAddressType() {
  }
}
public class AssociateAddressResponseType extends VmAddressMessage { //** added 2008-02-01  **/
}
/** *******************************************************************************/

public class DisassociateAddressType extends VmAddressMessage {  //** added 2008-02-01  **/
  String publicIp;
}
public class DisassociateAddressResponseType extends VmAddressMessage { //** added 2008-02-01  **/
}
/** *******************************************************************************/
public class AddressInfoType extends EucalyptusData {  //** added 2008-02-01  **/
  String publicIp;
  String instanceId;
  
  def AddressInfoType(final publicIp, final instanceId)
  {
    this.publicIp = publicIp;
    this.instanceId = instanceId;
  }
  
  def AddressInfoType()
  {
  }
  
}
