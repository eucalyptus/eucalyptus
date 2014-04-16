/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.cloud.util.NotEnoughResourcesException;
import com.eucalyptus.component.Partition;
import com.eucalyptus.vm.VmInstance;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class StaticSystemAddressManager extends AbstractSystemAddressManager {
  private static Logger LOG = Logger.getLogger( StaticSystemAddressManager.class );
  
  public StaticSystemAddressManager( ) {
    this.inheritReservedAddresses( new ArrayList<Address>( ) );
  }

  @Override
  protected List<Address> doAllocateSystemAddresses(
      final Partition partition,
      final int count
  ) throws NotEnoughResourcesException {
    List<Address> addressList = Lists.newArrayList( );
    for ( Address addr : Addresses.getInstance( ).listValues( ) ) {
      if ( addr.isSystemOwned( ) && !addr.isAssigned( ) ) {
        addr.pendingAssignment( );
        addressList.add( addr );
        if ( addressList.size( ) == count ) {
          break;
        }
      }
    }
    if ( addressList.size( ) < count ) {
      for ( Address putBackAddr : addressList ) {
        putBackAddr.clearPending( );
      }
      throw new NotEnoughAddressResourcesException( );
    }
    return addressList;
  }
  
  @Override
  public void assignSystemAddress( final VmInstance vm ) throws NotEnoughResourcesException {
    super.doAssignSystemAddress( vm );
  }
  
  @Override
  public List<Address> getReservedAddresses( ) {
    return Lists.newArrayList( Iterables.filter( Addresses.getInstance( ).listValues( ), new Predicate<Address>( ) {
      @Override
      public boolean apply( Address arg0 ) {
        return arg0.isSystemOwned( );
      }
    } ) );
  }
  
  @Override
  public void inheritReservedAddresses( List<Address> reservedAddresses ) {
    int allocCount = Addresses.getSystemReservedAddressCount( ) - reservedAddresses.size( );
    LOG.debug( "Allocating additional " + allocCount + " addresses in static public addresing mode" );
    allocCount = Addresses.getInstance( ).listDisabledValues( ).size( ) < allocCount
      ? Addresses.getInstance( ).listDisabledValues( ).size( )
      : allocCount;
    if ( allocCount > 0 ) {
      for ( int i = 0; i < allocCount; i++ ) {
        try {
          this.allocateNext( Principals.systemFullName( ) );
        } catch ( NotEnoughResourcesException e ) {
          break;
        }
      }
    } else {
      for ( Address addr : Addresses.getInstance( ).listValues( ) ) {
        if ( addr.getOwner( ).equals( Principals.systemFullName( ) ) && !addr.isAssigned( ) && !addr.isPending( ) ) {
          addr.release( );
          if ( allocCount++ >= 0 ) break;
        }
      }
    }
  }
  
}
