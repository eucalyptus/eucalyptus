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
package com.eucalyptus.net;

/*
 *
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */


import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.event.AbstractNamedRegistry;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.cloud.entities.Address;
import edu.ucsb.eucalyptus.cloud.exceptions.ExceptionList;

public class Addresses extends AbstractNamedRegistry<Address> {
  public static Logger    LOG       = Logger.getLogger( Addresses.class );
  private static Addresses singleton = Addresses.getInstance( );
  
  public static Addresses getInstance( ) {
    synchronized ( Addresses.class ) {
      if ( singleton == null ) singleton = new Addresses( );
    }
    return singleton;
  }
  
  public Address getNextAvailable( String userId ) throws EucalyptusCloudException {
    ConcurrentNavigableMap<String, Address> unusedAddresses = Addresses.getInstance().getDisabledMap();
    Map.Entry<String, Address> addressEntry = unusedAddresses.pollFirstEntry();

    if ( addressEntry == null ) {
      LOG.debug( LogUtil.header( Addresses.getInstance( ).toString( ) ) );
      throw new EucalyptusCloudException( ExceptionList.ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY );
    }

    Address address = addressEntry.getValue();
    address.allocate( userId );
    return address;
  }

  public List<Address> getDynamicSystemAddresses( String cluster, int count ) throws NotEnoughResourcesAvailable {
    List<Address> addressList = Lists.newArrayList( );
    if ( this.getDisabledMap( ).size( ) < count ) throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (try --addressing private)" );
    this.canHas.writeLock( ).lock( );
    try {
      for( Address addr : this.listDisabledValues( ) ) {
        if( cluster.equals( addr.getCluster( ) ) ) {
          addressList.add( addr );
          if( --count == 0 ) {
            break;
          }
        }
      }
      if( count != 0 ) {
        throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (try --addressing private)" );        
      } else {
        for ( Address a : addressList ) {
          a.allocate( Component.eucalyptus.name( ) );
          a.assign( Address.PENDING_ASSIGNMENT, Address.PENDING_ASSIGNMENT );//FIXME: lame hack.
        }
      }
    } finally {
      this.canHas.writeLock( ).unlock( );
    }
    return addressList;
  }

  public List<Address> getStaticSystemAddresses( int count ) throws NotEnoughResourcesAvailable {
    List<Address> addressList = Lists.newArrayList( );
    this.canHas.writeLock( ).lock( );
    try {
      for ( Address addr : Addresses.getInstance( ).listValues( ) ) {
        if ( !addr.isAssigned( ) && !addr.isPending( ) && Component.eucalyptus.name( ).equals( addr.getUserId( ) ) ) {
          addr.assign( Address.PENDING_ASSIGNMENT, Address.PENDING_ASSIGNMENT );//FIXME: lame hack.
          addressList.add( addr );
          if ( addressList.size( ) == count ) {
            break;
          }
        }
      }
      if ( addressList.size( ) < count ) {
        for ( Address putBackAddr : addressList ) {
          putBackAddr.unassign( );
          putBackAddr.clearPending( );
        }
        throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (try --addressing private)" );
      }
    } finally {
      this.canHas.writeLock( ).unlock( );
    }
    return addressList;
  }

  public static int clearUnusedSystemAddresses( ) {
    int allocatedCount = 0;
    for ( Address allocatedAddr : getInstance( ).listValues( ) ) {
      if ( allocatedAddr.isSystemAllocated( ) ) {
        allocatedCount++;
        if ( doDynamicAddressing( ) && !allocatedAddr.isAssigned( ) && !allocatedAddr.isPending( ) ) {
          //:: deallocate unassigned addresses owned by eucalyptus when switching to dynamic public addressing :://
          LOG.debug( "Deallocating unassigned public address in dynamic public addressing mode: " + allocatedAddr.getName( ) );
          allocatedAddr.release( );
        }
      }
    }
    return allocatedCount;
  }

  public static boolean doDynamicAddressing() {
    return edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration( ).isDoDynamicPublicAddresses( );
  }
  
  public void doStaticAddressing( int allocatedCount ) {
    int allocCount = Addresses.getSystemReservedAddressCount( ) - allocatedCount;
    LOG.debug( "Allocating additional " + allocCount + " addresses in static public addresing mode" );
    this.canHas.writeLock( ).lock( );
    try {
      ConcurrentNavigableMap<String, Address> unusedAddresses = Addresses.getInstance( ).getDisabledMap( );
      allocCount = unusedAddresses.size( ) < allocCount ? unusedAddresses.size( ) : allocCount;
      if ( allocCount > 0 ) {
        List<Map.Entry<String, Address>> addressList = Lists.newArrayList( );
        for ( int i = 0; i < allocCount; i++ ) {
          Map.Entry<String, Address> addressEntry = unusedAddresses.pollFirstEntry( );
          if ( addressEntry != null ) {
            addressList.add( addressEntry );
          } else {
            break; //:: out of unused addresses :://
          }
        }
        for ( Map.Entry<String, Address> addressEntry : addressList ) {
          LOG.debug( "Allocating address for static public addressing: " + addressEntry.getValue( ).getName( ) );
          Address address = addressEntry.getValue( );
          address.allocate( Component.eucalyptus.name( ) );
        }
      } else {
        for ( String ipAddr : Addresses.getInstance( ).getActiveMap( ).descendingKeySet( ) ) {
          Address addr = Addresses.getInstance( ).getActiveMap( ).get( ipAddr );
          if ( Component.eucalyptus.name( ).equals( addr.getUserId( ) ) && !addr.isAssigned( ) && !addr.isPending( ) ) {
            if ( allocCount++ >= 0 ) break;
            addr.release( );
          }
        }
      }
    } finally {
      this.canHas.writeLock( ).unlock( );
    }
  }
  
  public static int getSystemReservedAddressCount() {
    return edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration( ).getSystemReservedPublicAddresses( );
  }

}
