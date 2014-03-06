/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

package com.eucalyptus.address;

import java.util.Collections;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.compute.ClientComputeException;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.identifier.InvalidResourceIdentifier;
import com.eucalyptus.compute.identifier.ResourceIdentifiers;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.records.Logs;
import com.eucalyptus.tags.Filters;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.UnconditionalCallback;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import edu.ucsb.eucalyptus.msgs.AddressInfoType;
import edu.ucsb.eucalyptus.msgs.AllocateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.AllocateAddressType;
import edu.ucsb.eucalyptus.msgs.AssociateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.AssociateAddressType;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.DescribeAddressesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeAddressesType;
import edu.ucsb.eucalyptus.msgs.DisassociateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.DisassociateAddressType;
import edu.ucsb.eucalyptus.msgs.ReleaseAddressResponseType;
import edu.ucsb.eucalyptus.msgs.ReleaseAddressType;

public class AddressManager {
  
  public static Logger LOG = Logger.getLogger( AddressManager.class );
  
  public AllocateAddressResponseType allocate( final AllocateAddressType request ) throws Exception {
    AllocateAddressResponseType reply = ( AllocateAddressResponseType ) request.getReply( );
    try{
	    Address address = RestrictedTypes.allocateNamedUnitlessResources( 1, Addresses.Allocator.INSTANCE, Addresses.Allocator.INSTANCE ).get( 0 );	    
	    reply.setPublicIp( address.getName( ) );
    }catch(RuntimeException e){
    	if(e.getCause()!=null)
    		throw new EucalyptusCloudException(e.getCause());
    	else
    		throw new EucalyptusCloudException("couldn't allocate addresses");    		
    }catch(Exception e){
    	throw e;
    }
    return reply;
  }
  
  public ReleaseAddressResponseType release( ReleaseAddressType request ) throws Exception {
    ReleaseAddressResponseType reply = ( ReleaseAddressResponseType ) request.getReply( );
    reply.set_return( false );
    Address address = RestrictedTypes.doPrivileged( request.getPublicIp( ), Address.class );
    if ( address.isPending( ) ) {
      address.clearPending( );
    }
    Addresses.release( address );
    reply.set_return( true );
    return reply;
  }
  
  public DescribeAddressesResponseType describe( DescribeAddressesType request ) throws EucalyptusCloudException {
    final DescribeAddressesResponseType reply = ( DescribeAddressesResponseType ) request.getReply( );
    final Context ctx = Contexts.lookup( );
    final boolean isAdmin = ctx.isAdministrator( );
    final boolean verbose = isAdmin && request.getPublicIpsSet().remove( "verbose" ) ;
    final Predicate<? super Address> filter = CloudMetadatas.filteringFor( Address.class )
        .byId( request.getPublicIpsSet() )
        .byPredicate( Filters.generate( request.getFilterSet(), Address.class ).asPredicate() )
        .byOwningAccount( verbose ?
            Collections.<String>emptyList() :
            Collections.singleton( ctx.getAccount().getAccountNumber() ) )
        .byPrivileges( )
        .buildPredicate( );
    for ( Address address : Iterables.filter( Addresses.getInstance( ).listValues( ), filter ) ) {
      reply.getAddressesSet( ).add( verbose
          ? address.getAdminDescription( )
          : address.getDescription( ) );
    }
    if ( verbose ) {
      for ( Address address : Iterables.filter( Addresses.getInstance( ).listDisabledValues( ), filter ) ) {
        reply.getAddressesSet( ).add( new AddressInfoType( address.getName( ), Principals.nobodyFullName( ).getUserName( ) ) );
      }
    }
    return reply;
  }
  
  @SuppressWarnings( "unchecked" )
  public AssociateAddressResponseType associate( final AssociateAddressType request ) throws Exception {
    AssociateAddressResponseType reply = ( AssociateAddressResponseType ) request.getReply( );
    reply.set_return( false );
    final String instanceId = normalizeInstanceIdentifier( request.getInstanceId( ) );
    final Address address = RestrictedTypes.doPrivileged( request.getPublicIp( ), Address.class );
    if ( !address.isAllocated( ) ) {
      throw new EucalyptusCloudException( "Cannot associate an address which is not allocated: " + request.getPublicIp( ) );
    } else if ( !Contexts.lookup( ).isAdministrator( ) && !Contexts.lookup( ).getUserFullName( ).asAccountFullName( ).getAccountNumber( ).equals( address.getOwner( ).getAccountNumber( ) ) ) {
      throw new EucalyptusCloudException( "Cannot associate an address which is not allocated to your account: " + request.getPublicIp( ) );
    }
    final VmInstance vm = RestrictedTypes.doPrivileged( instanceId, VmInstance.class );
    final VmInstance oldVm = findCurrentAssignedVm( address );
    final Address oldAddr = findVmExistingAddress( vm );
    final boolean oldAddrSystem = oldAddr != null
      ? oldAddr.isSystemOwned( )
      : false;
    reply.set_return( true );
    
    if ( oldAddr != null && address.equals( oldAddr ) ) {
      return reply;
    }

    final UnconditionalCallback assignTarget = new UnconditionalCallback( ) {
      public void fire( ) {
        AddressingDispatcher.dispatch(            AsyncRequests.newRequest( address.assign( vm ).getCallback() ).then(
                new Callback.Success<BaseMessage>() {
                  @Override
                  public void fire( BaseMessage response ) {
                    Addresses.updatePublicIpByInstanceId( vm.getInstanceId(), address.getName() );
                  }
                }
            ),
            vm.getPartition() );
        if ( oldVm != null ) {
          Addresses.system( oldVm );
        }
      }
    };
    
    final UnconditionalCallback unassignBystander = new UnconditionalCallback( ) {
      public void fire( ) {
        if ( oldAddr != null ) {
          AddressingDispatcher.dispatch(
              AsyncRequests.newRequest( oldAddr.unassign().getCallback() ).then( assignTarget ),
              vm.getPartition() );
        } else {
          assignTarget.fire( );
        }
      }
    };
    
    if ( address.isAssigned( ) ) {
      AddressingDispatcher.dispatch(
          AsyncRequests.newRequest( address.unassign().getCallback() ).then( unassignBystander ),
          oldVm.getPartition() );
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
        oldVm = VmInstances.lookup( address.getInstanceId( ) );
      } catch ( Exception e ) {
        LOG.error( e, e );
      }
    }
    return oldVm;
  }
  
  public DisassociateAddressResponseType disassociate( final DisassociateAddressType request ) throws Exception {
    final DisassociateAddressResponseType reply = request.getReply( );
    reply.set_return( false );
    final Context ctx = Contexts.lookup( );
    final Address address = RestrictedTypes.doPrivileged( request.getPublicIp( ), Address.class );
    reply.set_return( true );
    final String vmId = address.getInstanceId( );
    if ( address.isSystemOwned( ) && !ctx.isAdministrator( ) ) {
      throw new EucalyptusCloudException( "Only administrators can unassign system owned addresses: " + address.toString( ) );
    } else {
      try {
        final VmInstance vm = VmInstances.lookup( vmId );
        final UnconditionalCallback<BaseMessage> systemAddressAssignmentCallback = new UnconditionalCallback<BaseMessage>( ) {
          @Override
          public void fire( ) {
            try {
              Addresses.system( VmInstances.lookup( vmId ) );
            } catch ( NoSuchElementException e ) {
              LOG.debug( e, e );
            } catch ( Exception e ) {
              LOG.error("Error assigning system address for instance " + vm.getInstanceId(), e);
            }
          }
        };

        AddressingDispatcher.dispatch(
            AsyncRequests.newRequest( address.unassign().getCallback() ).then( systemAddressAssignmentCallback ),
            vm.getPartition() ); 
      } catch ( Exception e ) {
        LOG.debug( e );
        Logs.extreme( ).debug( e, e );
        address.unassign( ).clearPending( );
      }
    }
    return reply;
  }

  private static String normalizeIdentifier( final String identifier,
                                             final String prefix,
                                             final boolean required,
                                             final String message ) throws ClientComputeException {
    try {
      return Strings.emptyToNull( identifier ) == null && !required ?
          null :
          ResourceIdentifiers.parse( prefix, identifier ).getIdentifier( );
    } catch ( final InvalidResourceIdentifier e ) {
      throw new ClientComputeException( "InvalidParameterValue", String.format( message, e.getIdentifier( ) ) );
    }
  }

  private static String normalizeInstanceIdentifier( final String identifier ) throws EucalyptusCloudException {
    return normalizeIdentifier(
        identifier, VmInstance.ID_PREFIX, true, "Value (%s) for parameter instanceId is invalid. Expected: 'i-...'." );
  }

}
