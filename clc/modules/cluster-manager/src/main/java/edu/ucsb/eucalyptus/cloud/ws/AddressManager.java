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
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 *
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package edu.ucsb.eucalyptus.cloud.ws;

import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Startable;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.net.Addresses;
import com.eucalyptus.net.util.AddressUtil;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.EucalyptusProperties;

import edu.ucsb.eucalyptus.cloud.cluster.ClusterEnvelope;
import edu.ucsb.eucalyptus.cloud.cluster.QueuedEvent;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.cloud.entities.Address;
import edu.ucsb.eucalyptus.cloud.exceptions.ExceptionList;
import edu.ucsb.eucalyptus.msgs.AllocateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.AllocateAddressType;
import edu.ucsb.eucalyptus.msgs.AssociateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.AssociateAddressType;
import edu.ucsb.eucalyptus.msgs.DescribeAddressesResponseItemType;
import edu.ucsb.eucalyptus.msgs.DescribeAddressesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeAddressesType;
import edu.ucsb.eucalyptus.msgs.DisassociateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.DisassociateAddressType;
import edu.ucsb.eucalyptus.msgs.ReleaseAddressResponseType;
import edu.ucsb.eucalyptus.msgs.ReleaseAddressType;

public class AddressManager {

  public static Logger LOG = Logger.getLogger( AddressManager.class );

  public AllocateAddressResponseType AllocateAddress( AllocateAddressType request ) throws EucalyptusCloudException {
    AddressUtil.updateAddressingMode();
    int addrCount = 0;
    for( Address a : Addresses.getInstance().listValues() ) {
      if( request.getUserId().equals( a.getUserId() ) ) addrCount++;
    }
    if( addrCount >= edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration( ).getMaxUserPublicAddresses() && !request.isAdministrator() )
      throw new EucalyptusCloudException( ExceptionList.ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY );

    String userId = request.getUserId( );
    Address address = AddressUtil.nextAvailableAddress( userId );

    try {
      Addresses.getInstance().register( address );
    } catch ( Exception e ) {
    }

    AllocateAddressResponseType reply = ( AllocateAddressResponseType ) request.getReply();
    reply.setPublicIp( address.getName() );
    return reply;
  }

  public ReleaseAddressResponseType ReleaseAddress( ReleaseAddressType request ) throws EucalyptusCloudException {
    AddressUtil.updateAddressingMode();
    ReleaseAddressResponseType reply = ( ReleaseAddressResponseType ) request.getReply();
    reply.set_return( false );

    Address address = null;
    try {
      //:: find the addr :://
      address = Addresses.getInstance().lookup( request.getPublicIp() );
      if ( !request.isAdministrator() && !address.getUserId().equals( request.getUserId() ) )
        return reply;
      //:: dispatch the unassign if needed :://
      if ( address.isAssigned() && !address.isPending() ) {
        try {
          VmInstance oldVm = VmInstances.getInstance().lookup( address.getInstanceId() );
          AddressUtil.unassignAddressFromVm( address, oldVm );
          AddressUtil.tryAssignSystemAddress( oldVm );
        } catch ( NoSuchElementException e ) {}
      }

      if( Component.eucalyptus.name().equals( address.getUserId() ) && !edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration( ).isDoDynamicPublicAddresses() ) {
        LOG.debug( "Not de-allocating system owned address in static public addressing mode: " + address.getName() );
        return reply;
      }
      AddressUtil.releaseAddress( address );
      reply.set_return( true );
    }
    catch ( NoSuchElementException e ) {
      return reply;
    }

    return reply;
  }

  public DescribeAddressesResponseType DescribeAddresses( DescribeAddressesType request ) throws EucalyptusCloudException {
    AddressUtil.updateAddressingMode();
    DescribeAddressesResponseType reply = ( DescribeAddressesResponseType ) request.getReply();


    boolean isAdmin = request.isAdministrator();
    for ( Address address : Addresses.getInstance().listValues() ) {
      if( address.isAssigned( ) ) {
        try {
          VmInstances.getInstance().lookup( address.getInstanceId() );
        } catch ( NoSuchElementException e ) {
          AddressUtil.markAddressUnassigned( address );
        }
      }
      if ( isAdmin || address.getUserId().equals( request.getUserId() ) ) {
        reply.getAddressesSet().add( address.getDescription( isAdmin ) );
      }
    }
    if ( request.isAdministrator() )
      for ( Address address : Addresses.getInstance().listDisabledValues() )
        reply.getAddressesSet().add( new DescribeAddressesResponseItemType( address.getName(), Address.UNALLOCATED_USERID ) );

    return reply;
  }

  public AssociateAddressResponseType AssociateAddress( AssociateAddressType request ) throws Exception {
    AddressUtil.updateAddressingMode();
    AssociateAddressResponseType reply = ( AssociateAddressResponseType ) request.getReply();
    reply.set_return( false );


    LOG.debug( "Associate: " + request.getPublicIp() + " => " + request.getInstanceId() );
    Address address = null;
    try {
      address = Addresses.getInstance().lookup( request.getPublicIp() );
    } catch ( NoSuchElementException e ) {
      return reply;
    }
    LOG.debug( "Found address: " + address );

    VmInstance vm = null;
    try {
      vm = VmInstances.getInstance().lookup( request.getInstanceId() );
    } catch ( NoSuchElementException e ) {
      return reply;
    }
    LOG.debug( "Found vm: " + vm );

    if ( !request.isAdministrator() && !( request.getUserId().equals( address.getUserId() ) && request.getUserId().equals( vm.getOwnerId() ) ) )
      return reply;

    //:: operation should be idempotent; request is legitimate so return true :://
    reply.set_return( true );

    //:: handle the address which may be currently assigned to the vm :://
    if( !vm.getNetworkConfig().getIpAddress().equals( vm.getNetworkConfig().getIgnoredPublicIp() ) && !VmInstance.DEFAULT_IP.equals( vm.getNetworkConfig().getIgnoredPublicIp() ) ) {
      String currentPublicIp = vm.getNetworkConfig().getIgnoredPublicIp();
      try {
        Address currentAddr = Addresses.getInstance().lookup( currentPublicIp );
                LOG.debug( "Dispatching unassign message for: " + address );
        if( currentAddr.isAssigned( ) ) {
          AddressUtil.unassignAddressFromVm( currentAddr, vm );
        } else {
          AddressUtil.dispatchUnassignAddress( currentAddr, vm );
        }
      } catch ( NoSuchElementException e ) {
      }
    }
    //:: handle the vm which the requested address may be assigned to :://
    if ( address.isAssigned() && address.getUserId().equals( request.getUserId() ) && !address.isPending() ) {
      LOG.debug( "Dispatching unassign message for: " + address );
      try {
        VmInstance oldVm = VmInstances.getInstance().lookup( address.getInstanceId() );
        if( address.isAssigned( ) ) {
          AddressUtil.unassignAddressFromVm( address, oldVm );
        } else {
          AddressUtil.dispatchUnassignAddress( address, oldVm );
        }
        AddressUtil.tryAssignSystemAddress( oldVm );

//        if( !EucalyptusProperties.disableNetworking ) {
//        }
      } catch ( NoSuchElementException e ) {
        LOG.error( e, e );
      }
    }

    AddressUtil.assignAddressToVm( address, vm );

    return reply;
  }

  public DisassociateAddressResponseType DisassociateAddress( DisassociateAddressType request ) throws EucalyptusCloudException {
    AddressUtil.updateAddressingMode();
    DisassociateAddressResponseType reply = ( DisassociateAddressResponseType ) request.getReply();
    reply.set_return( false );

    LOG.debug( "Disassociate: " + request.getPublicIp()  );
    Address address = null;
    try {
      address = Addresses.getInstance().lookup( request.getPublicIp() );
    } catch ( NoSuchElementException e ) {
      return reply;
    }
    LOG.debug( "Found address: " + address );

    VmInstance vm = null;
    try {
      vm = VmInstances.getInstance().lookup( address.getInstanceId() );
    } catch ( NoSuchElementException e ) {
      return reply;
    }
    LOG.debug( "Found vm: " + vm );

    if ( !request.isAdministrator() && !( request.getUserId().equals( address.getUserId() ) && request.getUserId().equals( vm.getOwnerId() ) ) )
      return reply;

    if( VmInstance.DEFAULT_IP.equals( vm.getInstanceId() ) )
        return reply;

    reply.set_return( true );
    AddressUtil.unassignAddressFromVm( address, vm );
    AddressUtil.tryAssignSystemAddress( vm );
    return reply;
  }


}
