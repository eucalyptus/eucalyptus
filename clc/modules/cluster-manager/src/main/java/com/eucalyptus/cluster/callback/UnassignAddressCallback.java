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

package com.eucalyptus.cluster.callback;

import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import edu.ucsb.eucalyptus.msgs.ClusterAddressInfo;
import com.eucalyptus.address.Address.Transition;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Expendable;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.MessageCallback;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vm.VmNetworkConfig;
import com.google.common.base.Function;
import edu.ucsb.eucalyptus.msgs.UnassignAddressResponseType;
import edu.ucsb.eucalyptus.msgs.UnassignAddressType;

public class UnassignAddressCallback extends MessageCallback<UnassignAddressType, UnassignAddressResponseType> implements Expendable<UnassignAddressCallback> {
  
  private static Logger LOG = Logger.getLogger( UnassignAddressCallback.class );
  private Address       address;
  private final boolean system;
  
  public UnassignAddressCallback( String addr, String vmIp ) {
    super( new UnassignAddressType( addr, vmIp ) );
    try {
      this.address = Addresses.getInstance( ).lookup( addr );
    } catch ( Exception e ) {
      try {
        this.address = Addresses.getInstance( ).lookupDisabled( addr );
      } catch ( Exception ex ) {
        LOG.error( "Failed to prepare unassign for: " + addr + "=>" + vmIp );
        Logs.extreme( ).error( "Failed to prepare unassign for: " + addr + "=>" + vmIp, e );
        Logs.extreme( ).error( "Failed to prepare unassign for: " + addr + "=>" + vmIp, ex );
        throw new CancellationException( ex.getMessage( ) );
      }
    }
    this.system = this.address.isSystemOwned( );
  }
  
  public UnassignAddressCallback( String addr ) {
    this( addr, Addresses.getInstance( ).lookup( addr ).getInstanceAddress( ) );
  }
  
  public UnassignAddressCallback( ClusterAddressInfo addrInfo ) {
    this( addrInfo.getAddress( ), addrInfo.getInstanceIp( ) );
  }
  
  public UnassignAddressCallback( final Address address ) {
    this( address.getName( ), address.getInstanceAddress( ) );
  }
  
  @Override
  public void initialize( UnassignAddressType msg ) throws Exception {
    try {
      EventRecord.here( UnassignAddressCallback.class, EventType.ADDRESS_UNASSIGNING, Transition.unassigning.toString( ), address.toString( ) ).info( );
    } catch ( Exception ex ) {
      LOG.error( ex , ex );
    }
  }
  
  public void clearVmAddress( ) {
    final String privateIp = super.getRequest( ).getDestination( );
    final String publicIp = super.getRequest( ).getSource( );
    Addresses.updatePublicIPOnMatch( privateIp, publicIp, new Function<VmInstance,String>(){
      @Override
      public String apply( final VmInstance vmInstance ) {
        return vmInstance.getPrivateAddress();
      }
    } );
  }
  
  @Override
  public void fire( UnassignAddressResponseType reply ) {
    try {
//      this.sendSecondaryUnassign( );
      this.address.clearPending( );
      this.clearVmAddress( );
    } catch ( IllegalStateException t ) {
      LOG.debug( t );
    } catch ( Exception t ) {
      LOG.warn( t.getMessage( ) );
      EventRecord.here( UnassignAddressCallback.class, EventType.ADDRESS_STATE, "broken", this.address.toString( ) ).warn( );
      LOG.trace( t, t );
    } finally {
      if ( this.system ) {
        try {
          if ( !this.address.isPending( ) && this.address.isAssigned( ) ) {
            this.address.unassign( ).clearPending( ).release( );
          } else {
            this.address.release( );
          }
        } catch ( Exception t ) {
          LOG.warn( "Failed to release orphan address: " + this.address, t);
        }
      }
    }
  }

  @Override
  public void fireException( Throwable e ) {
    try {
      Addresses.updatePublicIP( super.getRequest( ).getDestination( ), VmNetworkConfig.DEFAULT_IP );
    } catch ( Exception t ) {
      LOG.debug( t, t );
    } finally {
      if ( this.address.isPending( ) ) {
        try {
          this.address.clearPending( );
        } catch ( Exception ex ) {
        }
      }
      if ( this.system ) {
        try {
          VmInstances.lookupByPublicIp( this.address.getDisplayName( ) );
        } catch ( NoSuchElementException ex ) {
          this.address.release( );
        }
      }
    }
    LOG.error( e, e );
    LOG.warn( "Address potentially in an inconsistent state: " + LogUtil.dumpObject( this.address ) );
  }

  /**
   * @see com.eucalyptus.util.Expendable#duplicateOf
   */
  @Override
  public boolean duplicateOf( UnassignAddressCallback that ) {
    return this.getRequest( ).getSource( ).equals( that.getRequest( ).getSource( ) )
           && this.getRequest( ).getDestination( ).equals( that.getRequest( ).getDestination( ) );
  }
}
