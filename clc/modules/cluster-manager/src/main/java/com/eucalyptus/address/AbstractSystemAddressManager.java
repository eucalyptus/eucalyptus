/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

import java.util.List;
import java.util.NoSuchElementException;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.compute.common.internal.util.NotEnoughResourcesException;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Predicate;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public abstract class AbstractSystemAddressManager {
  private final static Logger                                              LOG     = Logger.getLogger( AbstractSystemAddressManager.class );
  private static final String ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY                = "InsufficientAddressCapacity";

  public Address allocateNext( final OwnerFullName userId, final Address.Domain domain ) throws NotEnoughResourcesException {
	  int numSystemReserved=0;
	  try{
		  ConfigurableProperty p =
				  PropertyDirectory.getPropertyEntry("cloud.addresses.systemreservedpublicaddresses");
		  if(p!=null)
			  numSystemReserved= Integer.parseInt(p.getValue());
	  }catch(IllegalAccessException e)
	  {
		  LOG.error("Can't find the 'systemreservedpublicaddresses' property");
		  numSystemReserved=0;
	  }
	  if ( (Addresses.getInstance( ).listDisabledValues( ).size( ) - numSystemReserved ) < 1 ) {
		  throw new NotEnoughResourcesException( ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY );
	  }	    

	  Predicate<Address> predicate = RestrictedTypes.filterPrivileged( );    
	  final Address addr = Addresses.getInstance( ).enableFirst( predicate ).allocate( userId, domain );

	  LOG.debug( "Allocated address for public addressing: " + String.valueOf( addr ) );
	  if ( addr == null ) {
		  LOG.debug( LogUtil.header( Addresses.getInstance( ).toString( ) ) );
		  throw new NotEnoughResourcesException( ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY );
	  }
	  return addr;
  }
  
  public abstract void assignSystemAddress( final VmInstance vm ) throws NotEnoughResourcesException;
  
  public abstract List<Address> getReservedAddresses( );
  
  public abstract void inheritReservedAddresses( List<Address> previouslyReservedAddresses );
  
  public final List<Address> allocateSystemAddresses( int count ) throws NotEnoughResourcesException {
    return onAllocation( this.doAllocateSystemAddresses( count ) );
  }
  
  public final Address allocateSystemAddress( ) throws NotEnoughResourcesException {
    return onAllocation( this.doAllocateSystemAddresses( 1 ) ).get( 0 );
  }

  protected List<Address> onAllocation( final List<Address> allocated ) {
    return allocated;
  }

  protected abstract List<Address> doAllocateSystemAddresses( int count ) throws NotEnoughResourcesException;

  /**
   * Update addresses from the list assign (system) to instances if necessary.
   */
  public void update( final Iterable<String> addresses ) {
    Helper.loadStoredAddresses( );
    for ( final String address : addresses ) {
      Helper.lookupOrCreate( address );
    }
  }

  protected void doAssignSystemAddress( final VmInstance vm ) throws NotEnoughResourcesException {
    final String instanceId = vm.getInstanceId();
    final Address addr = this.allocateSystemAddress( );
    final Callback.Success<BaseMessage> onSuccess = new Callback.Success<BaseMessage>( ) {
      @Override
      public void fire( final BaseMessage response ) {
        Addresses.updatePublicIpByInstanceId( instanceId, addr.getName() );
      }
    };
    AddressingDispatcher.dispatch(
        AsyncRequests.newRequest( addr.assign( vm ).getCallback( ) ).then( onSuccess ),
        vm.getPartition() );
  }
  
  protected static class Helper {
    protected static Address lookupOrCreate( final String address ) {
      Address addr = null;
      try {
        addr = Addresses.getInstance( ).lookupDisabled( address );
        LOG.trace( "Found address in the inactive set cache: " + addr );
      } catch ( final NoSuchElementException e1 ) {
        try {
          addr = Addresses.getInstance( ).lookup( address );
          LOG.trace( "Found address in the active set cache: " + addr );
        } catch ( final NoSuchElementException e ) {}
      }
      if ( addr == null ) {
        VmInstance vm = maybeFindVm( null, address, null );
        addr = vm != null ?
            new Address( Principals.systemFullName( ), address, vm.getInstanceUuid(), vm.getInstanceId( ), vm.getPrivateAddress( ) ) :
            new Address( address );
      }
      return addr;
    }

    private static VmInstance maybeFindVm( final String instanceId, final String publicIp, final String privateIp ) {
      VmInstance vm = null;
      if ( instanceId != null ) {
        try {
          vm = VmInstances.lookup( instanceId );
        } catch ( NoSuchElementException ex ) {
          Logs.extreme( ).error( ex );
        }
      } 
      if ( vm == null && privateIp != null ) {
        try {
          vm = VmInstances.lookupByPrivateIp( privateIp );
        } catch ( NoSuchElementException ex ) {
          Logs.extreme( ).error( ex );
        }
      } 
      if ( vm == null && publicIp != null ) {
        try {
          vm = VmInstances.lookupByPublicIp( publicIp );
        } catch ( NoSuchElementException ex ) {
          Logs.extreme( ).error( ex );
        }
      }
      if ( vm != null && VmState.RUNNING.equals( vm.getState( ) ) && publicIp.equals( vm.getPublicAddress( ) ) ) {
        Logs.extreme( ).debug( "Candidate vm which claims this address: " + vm.getInstanceId( ) + " " + vm.getState( ) + " " + publicIp );
        if ( publicIp.equals( vm.getPublicAddress( ) ) ) {
          Logs.extreme( ).debug( "Found vm which claims this address: " + vm.getInstanceId( ) + " " + vm.getState( ) + " " + publicIp );
        }
        return vm;
      } else {
        return null;
      }
    }
    
    protected static void loadStoredAddresses( ) {
      final Address clusterAddr = new Address( );
      final EntityTransaction db = Entities.get( Address.class );
      try {
        for ( Address addr : Entities.query( clusterAddr ) ) {
          if ( !Addresses.getInstance( ).contains( addr.getName( ) ) ) {
            Entities.evict( addr );
            try {
              addr.init( );
            } catch ( Exception ex ) {
              LOG.error( ex, ex );
            }
          }
        }
        db.commit( );
      } catch ( final Exception e ) {
        LOG.debug( e, e );
        db.rollback( );
      }
    }
  }
  
}
