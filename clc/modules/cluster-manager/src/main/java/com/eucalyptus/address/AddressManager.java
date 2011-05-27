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
 *
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.address;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.FakePrincipals;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.util.async.UnconditionalCallback;
import edu.ucsb.eucalyptus.msgs.AllocateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.AllocateAddressType;
import edu.ucsb.eucalyptus.msgs.AssociateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.AssociateAddressType;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.AddressInfoType;
import edu.ucsb.eucalyptus.msgs.DescribeAddressesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeAddressesType;
import edu.ucsb.eucalyptus.msgs.DisassociateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.DisassociateAddressType;
import edu.ucsb.eucalyptus.msgs.ReleaseAddressResponseType;
import edu.ucsb.eucalyptus.msgs.ReleaseAddressType;

public class AddressManager {
  
  public static Logger LOG = Logger.getLogger( AddressManager.class );
  
  public AllocateAddressResponseType allocate( AllocateAddressType request ) throws EucalyptusCloudException {
    AllocateAddressResponseType reply = ( AllocateAddressResponseType ) request.getReply( );
    Address address;
    try {
      address = Addresses.allocate( request );
    } catch ( NotEnoughResourcesAvailable e ) {
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e );
    }
    reply.setPublicIp( address.getName( ) );
    return reply;
  }
  
  public ReleaseAddressResponseType release( ReleaseAddressType request ) throws EucalyptusCloudException {
    ReleaseAddressResponseType reply = ( ReleaseAddressResponseType ) request.getReply( );
    reply.set_return( false );
    Addresses.updateAddressingMode( );
    Address address = Addresses.restrictedLookup( request, request.getPublicIp( ) );
    Addresses.release( address );
    reply.set_return( true );
    return reply;
  }
  
  public DescribeAddressesResponseType describe( DescribeAddressesType request ) throws EucalyptusCloudException {
    DescribeAddressesResponseType reply = ( DescribeAddressesResponseType ) request.getReply( );
    Addresses.updateAddressingMode( );
    Context ctx = Contexts.lookup( );
    boolean isAdmin = ctx.hasAdministrativePrivileges( );
    User requestUser = ctx.getUser( );
    String action = PolicySpec.requestToAction( request );
    for ( Address address : Addresses.getInstance( ).listValues( ) ) {
      //TODO:GRZE:FIXME this is not going to last this way.
      Account addrAccount = null;
      if ( !FakePrincipals.NOBODY_ACCOUNT.getAccountNumber( ).equals( address.getOwnerAccountId( ) ) ) {
        try {
        addrAccount = Accounts.lookupAccountById( address.getOwnerAccountId( ) );
        } catch ( AuthException e ) {}
      }
      if ( addrAccount != null
           && ( isAdmin || Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_ADDRESS, address.getName( ), addrAccount, action, requestUser ) ) ) {
        reply.getAddressesSet( ).add( isAdmin
            ? address.getAdminDescription( )
            : address.getDescription( ) );
      } else if ( isAdmin ) {
        reply.getAddressesSet( ).add( isAdmin
                                      ? address.getAdminDescription( )
                                      : address.getDescription( ) );
      }
    }
    if ( isAdmin ) {
      for ( Address address : Addresses.getInstance( ).listDisabledValues( ) ) {
        reply.getAddressesSet( ).add( new AddressInfoType( address.getName( ), FakePrincipals.NOBODY_USER_ERN.getUserName( ) ) );
      }
    }
    return reply;
  }
  
  @SuppressWarnings( "unchecked" )
  public AssociateAddressResponseType associate( final AssociateAddressType request ) throws Exception {
    AssociateAddressResponseType reply = ( AssociateAddressResponseType ) request.getReply( );
    reply.set_return( false );
    Addresses.updateAddressingMode( );
    final Address address = Addresses.restrictedLookup( request, request.getPublicIp( ) );//TODO: test should throw error.
    final VmInstance vm = VmInstances.restrictedLookup( request, request.getInstanceId( ) );
    final VmInstance oldVm = findCurrentAssignedVm( address );
    final Address oldAddr = findVmExistingAddress( vm );
    final boolean oldAddrSystem = oldAddr != null
      ? oldAddr.isSystemOwned( )
      : false;
    reply.set_return( true );
    
    final UnconditionalCallback assignTarget = new UnconditionalCallback( ) {
      public void fire( ) {
        AsyncRequests.newRequest( address.assign( vm ).getCallback( ) ).then( new Callback.Success<BaseMessage>( ) {
          public void fire( BaseMessage response ) {
            vm.updatePublicAddress( address.getName( ) );
          }
        } ).dispatch( address.getCluster( ) );
        if ( oldVm != null ) {
          Addresses.system( oldVm );
        }
      }
    };
    
    final UnconditionalCallback unassignBystander = new UnconditionalCallback( ) {
      public void fire( ) {
        if ( oldAddr != null ) {
          AsyncRequests.newRequest( oldAddr.unassign( ).getCallback( ) ).then( assignTarget ).dispatch( oldAddr.getCluster( ) );
        } else {
          assignTarget.fire( );
        }
      }
    };
    if ( address.isAssigned( ) ) {
      AsyncRequests.newRequest( address.unassign( ).getCallback( ) ).then( unassignBystander ).dispatch( address.getCluster( ) );
    } else {
      unassignBystander.fire( );
    }
    return reply;
  }
  
  private Address findVmExistingAddress( final VmInstance vm ) {
    Address oldAddr = null;
    if ( vm.hasPublicAddress( ) ) {
      try {
        oldAddr = Addresses.getInstance( ).lookup( vm.getPublicAddress( ) );
      } catch ( Exception e ) {
        LOG.debug( e, e );
      }
    }
    return oldAddr;
  }
  
  private VmInstance findCurrentAssignedVm( Address address ) {
    VmInstance oldVm = null;
    if ( address.isAssigned( ) && !address.isPending( ) ) {
      try {
        oldVm = VmInstances.getInstance( ).lookup( address.getInstanceId( ) );
      } catch ( Exception e ) {
        LOG.error( e, e );
      }
    }
    return oldVm;
  }
  
  public DisassociateAddressResponseType disassociate( DisassociateAddressType request ) throws EucalyptusCloudException {
    DisassociateAddressResponseType reply = ( DisassociateAddressResponseType ) request.getReply( );
    reply.set_return( false );
    Addresses.updateAddressingMode( );
    Context ctx = Contexts.lookup( );
    final Address address = Addresses.restrictedLookup( request, request.getPublicIp( ) );
    reply.set_return( true );
    final String vmId = address.getInstanceId( );
    if ( address.isSystemOwned( ) && !ctx.hasAdministrativePrivileges( ) ) {
      throw new EucalyptusCloudException( "Only administrators can unassign system owned addresses: " + address.toString( ) );
    } else {
      try {
        if ( address.isSystemOwned( ) ) {
          AsyncRequests.newRequest( address.unassign( ).getCallback( ) ).then( new UnconditionalCallback( ) {
            public void fire( ) {
              try {
                Addresses.system( VmInstances.getInstance( ).lookup( vmId ) );
              } catch ( Exception e ) {
                LOG.debug( e, e );
              }
            }
          } ).dispatch( address.getCluster( ) );
        } else {
          AsyncRequests.newRequest( address.unassign( ).getCallback( ) ).then( new UnconditionalCallback( ) {
            @Override
            public void fire( ) {
              try {
                Addresses.system( VmInstances.getInstance( ).lookup( vmId ) );
              } catch ( Exception e ) {
                LOG.debug( e, e );
              }
            }
          } ).dispatch( address.getCluster( ) );
        }
      } catch ( Throwable e ) {
        LOG.debug( e, e );
      }
    }
    return reply;
  }
}
