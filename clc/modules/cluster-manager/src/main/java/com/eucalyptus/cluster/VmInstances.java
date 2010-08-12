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

package com.eucalyptus.cluster;

import java.security.MessageDigest;
import java.util.NoSuchElementException;
import java.util.zip.Adler32;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.AddressCategory;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.auth.crypto.Digest;
import com.eucalyptus.cluster.callback.QueuedEventCallback;
import com.eucalyptus.cluster.callback.StopNetworkCallback;
import com.eucalyptus.cluster.callback.TerminateCallback;
import com.eucalyptus.event.AbstractNamedRegistry;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.vm.SystemState;
import com.eucalyptus.vm.VmState;
import com.eucalyptus.vm.SystemState.Reason;
import com.eucalyptus.records.EventRecord;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.cloud.NetworkToken;

public class VmInstances extends AbstractNamedRegistry<VmInstance> {
  private static Logger      LOG       = Logger.getLogger( VmInstances.class );
  private static VmInstances singleton = getInstance( );
  
  public static VmInstances getInstance( ) {
    synchronized ( VmInstances.class ) {
      if ( singleton == null ) singleton = new VmInstances( );
    }
    return singleton;
  }
  
  public static String getId( Long rsvId, int launchIndex ) {
    String vmId = null;
    do {
      MessageDigest digest = Digest.MD5.get( );
      digest.reset( );
      digest.update( Long.toString( rsvId + launchIndex + System.currentTimeMillis( ) ).getBytes( ) );
      
      Adler32 hash = new Adler32( );
      hash.reset( );
      hash.update( digest.digest( ) );
      vmId = String.format( "i-%08X", hash.getValue( ) );
    } while ( VmInstances.getInstance( ).contains( vmId ) );
    return vmId;
  }
  
  public static String getAsMAC( String instanceId ) {
    String mac = String.format( "d0:0d:%s:%s:%s:%s", instanceId.substring( 2, 4 ), instanceId.substring( 4, 6 ), instanceId.substring( 6, 8 ),
                                instanceId.substring( 8, 10 ) );
    return mac;
  }
  
  public VmInstance lookupByInstanceIp( String ip ) throws NoSuchElementException {
    for ( VmInstance vm : this.listValues( ) ) {
      if ( ip.equals( vm.getPrivateAddress( ) ) && ( VmState.PENDING.equals( vm.getState( ) ) || VmState.RUNNING.equals( vm.getState( ) ) ) ) {
        return vm;
      }
    }
    throw new NoSuchElementException( "Can't find registered object with ip:" + ip + " in " + this.getClass( ).getSimpleName( ) );
  }
  
  public int countByPublicIp( String ip ) throws NoSuchElementException {
    int count = 0;
    for ( VmInstance vm : this.listValues( ) ) {
      if ( ip.equals( vm.getPublicAddress( ) ) && ( VmState.PENDING.equals( vm.getState( ) ) || VmState.RUNNING.equals( vm.getState( ) ) ) ) {
        count++;
      }
    }
    return count;
  }
  
  public VmInstance lookupByPublicIp( String ip ) throws NoSuchElementException {
    for ( VmInstance vm : this.listValues( ) ) {
      if ( ip.equals( vm.getPublicAddress( ) ) && ( VmState.PENDING.equals( vm.getState( ) ) || VmState.RUNNING.equals( vm.getState( ) ) ) ) {
        return vm;
      }
    }
    throw new NoSuchElementException( "Can't find registered object with public ip:" + ip + " in " + this.getClass( ).getSimpleName( ) );
  }
  
  public static UnconditionalCallback getCleanUpCallback( final Address address, final VmInstance vm, final int networkIndex, final String networkFqName, final Cluster cluster ) {
    UnconditionalCallback cleanup = new UnconditionalCallback( ) {
      public void apply( ) {
        if ( address != null ) {
          try {
            if ( address.isSystemOwned( ) ) {
              EventRecord.caller( SystemState.class, EventType.VM_TERMINATING, "SYSTEM_ADDRESS", address.toString( ) ).debug( );
              Addresses.release( address );
            } else {
              EventRecord.caller( SystemState.class, EventType.VM_TERMINATING, "USER_ADDRESS", address.toString( ) ).debug( );
              AddressCategory.unassign( address ).dispatch( address.getCluster( ) );
            }
          } catch ( IllegalStateException e ) {} catch ( Throwable e ) {
            LOG.debug( e, e );
          }
        }
        vm.updateNetworkIndex( -1 );
        try {
          if ( networkFqName != null ) {
            Network net = Networks.getInstance( ).lookup( networkFqName );
            if ( networkIndex > 0 && vm.getNetworkNames( ).size( ) > 0 ) {
              net.returnNetworkIndex( networkIndex );
              EventRecord.caller( SystemState.class, EventType.VM_TERMINATING, "NETWORK_INDEX", networkFqName, Integer.toString( networkIndex ) ).debug( );
            }
            if ( !Networks.getInstance( ).lookup( networkFqName ).hasTokens( ) ) {
              StopNetworkCallback stopNet = new StopNetworkCallback( new NetworkToken( cluster.getName( ), net.getUserName( ), net.getNetworkName( ),
                                                                                       net.getVlan( ) ) );
              for ( Cluster c : Clusters.getInstance( ).listValues( ) ) {
                stopNet.newInstance( ).dispatch( cluster );
              }
            }
          }
        } catch ( NoSuchElementException e1 ) {} catch ( Throwable e1 ) {
          LOG.debug( e1, e1 );
        }
      }
    };
    return cleanup;
  }

  public static void cleanUp( final VmInstance vm ) {
    try {
      String networkFqName = !vm.getNetworks( ).isEmpty( ) ? vm.getOwnerId( ) + "-" + vm.getNetworkNames( ).get( 0 ) : null;
      Cluster cluster = Clusters.getInstance( ).lookup( vm.getPlacement( ) );
      int networkIndex = vm.getNetworkIndex( );
      Address address = null;
      QueuedEventCallback cb = new TerminateCallback( vm.getInstanceId( ) );
      if ( Clusters.getInstance( ).hasNetworking( ) ) {
        try {
          address = Addresses.getInstance( ).lookup( vm.getPublicAddress( ) );
        } catch ( NoSuchElementException e ) {} catch ( Throwable e1 ) {
          LOG.debug( e1, e1 );
        }
      }
      cb.then( VmInstances.getCleanUpCallback( address, vm, networkIndex, networkFqName, cluster ) );
      cb.dispatch( cluster );
    } catch ( Throwable e ) {
      LOG.error( e, e );
    }
  }

  public static VmInstance restrictedLookup( String userId, boolean administrator, String instanceId ) throws EucalyptusCloudException {
    VmInstance vm = VmInstances.getInstance( ).lookup( instanceId ); //TODO: test should throw error.
    if ( !administrator && !vm.getOwnerId( ).equals( userId ) ) {
      throw new EucalyptusCloudException( "Permission denied while trying to lookup vm instance: " + instanceId );
    }
    return vm;
  }
  
  public static void flushBuried( ) {
    for ( VmInstance vm : VmInstances.getInstance( ).getDisabledEntries( ) ) {
      if ( vm.getSplitTime( ) > SystemState.SHUT_DOWN_TIME && !VmState.BURIED.equals( vm.getState( ) ) ) {
        vm.setState( VmState.BURIED, Reason.BURIED );
      } else if ( vm.getSplitTime( ) > SystemState.BURY_TIME && VmState.BURIED.equals( vm.getState( ) ) ) {
        VmInstances.getInstance( ).deregister( vm.getName( ) );
      }
    }
    if ( ( float ) Runtime.getRuntime( ).freeMemory( ) / ( float ) Runtime.getRuntime( ).maxMemory( ) < 0.10f ) {
      for ( VmInstance vm : VmInstances.getInstance( ).listDisabledValues( ) ) {
        if ( VmState.BURIED.equals( vm.getState( ) ) || vm.getSplitTime( ) > SystemState.BURY_TIME ) {
          VmInstances.getInstance( ).deregister( vm.getInstanceId( ) );
          LOG.info( EventRecord.here( VmInstances.class, EventType.FLUSH_CACHE, LogUtil.dumpObject( vm ) ) );
        }
      }
    }
  }
  
  public static String asMacAddress( final String instanceId ) {
    return String
                 .format( "%s:%s:%s:%s", instanceId.substring( 2, 4 ), instanceId.substring( 4, 6 ), instanceId.substring( 6, 8 ), instanceId.substring( 8, 10 ) );
  }
  
}
