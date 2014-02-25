/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.cloud.util.NotEnoughResourcesException;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.ClusterState;
import com.eucalyptus.cluster.callback.UnassignAddressCallback;
import com.eucalyptus.component.Partition;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstance.VmStateSet;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Predicate;
import edu.ucsb.eucalyptus.msgs.ClusterAddressInfo;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public abstract class AbstractSystemAddressManager {
  private final static Logger                                              LOG     = Logger.getLogger( AbstractSystemAddressManager.class );
  private static final ConcurrentNavigableMap<ClusterAddressInfo, Integer> orphans = new ConcurrentSkipListMap<ClusterAddressInfo, Integer>( );
  private static final String ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY                = "InsufficientAddressCapacity";

  public static void clearOrphan( ClusterAddressInfo address ) {
    Integer delay = orphans.remove( address );
    delay = ( delay == null
      ? 0
      : delay );
    if ( delay > 2 ) {
      LOG.warn( "Forgetting stale orphan address mapping for " + address.toString( ) );
    }
  }
  
  public static void handleOrphan( Cluster cluster, ClusterAddressInfo address ) {
    Integer orphanCount = 1;
    orphanCount = orphans.putIfAbsent( address, orphanCount );
    orphanCount = ( orphanCount == null )
      ? 1
      : orphanCount;
    orphans.put( address, orphanCount + 1 );
    EventRecord.caller( ClusterState.class, EventType.ADDRESS_STATE,
                        "Updated orphaned public ip address: " + LogUtil.dumpObject( address ) + " count=" + orphanCount ).debug( );
    if ( orphanCount > AddressingConfiguration.getInstance( ).getMaxKillOrphans( ) ) {
      EventRecord.caller( ClusterState.class, EventType.ADDRESS_STATE,
                          "Unassigning orphaned public ip address: " + LogUtil.dumpObject( address ) + " count=" + orphanCount ).warn( );
      try {
        final Address addr = Addresses.getInstance( ).lookup( address.getAddress( ) );
        if ( addr.isPending( ) ) { 
          try {
            addr.clearPending( );
          } catch ( Exception ex ) {
          }
        }
        try {
          if ( addr.isAssigned( ) && "0.0.0.0".equals( address.getInstanceIp( ) ) ) {
            addr.unassign( ).clearPending( );
            if ( addr.isSystemOwned( ) ) {
              addr.release( );
            }
          } else if ( addr.isAssigned( ) && !"0.0.0.0".equals( address.getInstanceIp( ) ) ) {
            AddressingDispatcher.sendSync( AsyncRequests.newRequest( new UnassignAddressCallback( address ) ), cluster.getConfiguration( ) );
            if ( addr.isSystemOwned( ) ) {
              addr.release( );
            }
          } else if ( !addr.isAssigned( ) && addr.isAllocated( ) && addr.isSystemOwned( ) ) {
            addr.release( );
          }
        } catch ( ExecutionException ex ) {
          if ( !addr.isAssigned( ) && addr.isAllocated( ) && addr.isSystemOwned( ) ) {
            addr.release( );
          }
        }
      } catch ( InterruptedException ex ) {
        Exceptions.maybeInterrupted( ex );
      } catch ( NoSuchElementException ex ) {
      } finally {
        orphans.remove( address );
      }
    }
  }

  public Address allocateNext( final OwnerFullName userId ) throws NotEnoughResourcesException {
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
	  final Address addr = Addresses.getInstance( ).enableFirst( predicate ).allocate( userId );   

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
  
  public final List<Address> allocateSystemAddresses( Partition partition, int count ) throws NotEnoughResourcesException {
    return onAllocation( this.doAllocateSystemAddresses( partition, 1 ) );
  }
  
  public final Address allocateSystemAddress( final Partition partition ) throws NotEnoughResourcesException {
    return onAllocation( this.doAllocateSystemAddresses( partition, 1 ) ).get( 0 );
  }

  protected List<Address> onAllocation( final List<Address> allocated ) {
    for ( final Address address : allocated ) {
      clearOrphan( new ClusterAddressInfo( address.getDisplayName( ) ) );
    }
    return allocated;
  }

  protected abstract List<Address> doAllocateSystemAddresses( Partition partition, int count ) throws NotEnoughResourcesException;

  /**
   * Update addresses from the list assign (system) to instances if necessary.
   */
  public void update( final Iterable<String> addresses ) {
    Helper.loadStoredAddresses( );
    for ( final String address : addresses ) {
      Helper.lookupOrCreate( address, true );
    }
  }

  public void update( final Cluster cluster, final List<ClusterAddressInfo> ccList ) {
    Helper.loadStoredAddresses( );
    for ( final ClusterAddressInfo addrInfo : ccList ) {
      try {
        final Address address = Helper.lookupOrCreate( cluster, addrInfo );
        if ( address.isAssigned( ) && !addrInfo.hasMapping( ) && !address.isPending( ) ) {
          if ( Principals.nobodyFullName( ).equals( address.getOwner( ) ) ) {
            Helper.markAsAllocated( cluster, addrInfo, address );
          }
          try {
            final VmInstance vm = VmInstances.lookupByPrivateIp( addrInfo.getInstanceIp( ) );
            clearOrphan( addrInfo );
          } catch ( final NoSuchElementException e ) {
            try {
              final VmInstance vm = VmInstances.lookup( address.getInstanceId( ) );
              clearOrphan( addrInfo );
            } catch ( final NoSuchElementException ex ) {
              InetAddress addr = null;
              try {
                addr = Inet4Address.getByName( addrInfo.getInstanceIp( ) );
              } catch ( final UnknownHostException e1 ) {
                LOG.debug( e1, e1 );
              }
              if ( ( addr == null ) || !addr.isLoopbackAddress( ) ) {
                handleOrphan( cluster, addrInfo );
              }
            }
          }
        } else if ( address.isAllocated( ) && Principals.nobodyFullName( ).equals( address.getOwner( ) ) && !address.isPending( ) ) {
          Helper.markAsAllocated( cluster, addrInfo, address );
        }
      } catch ( final Exception e ) {
        LOG.debug( e, e );
      }
    }
  }

  protected void doAssignSystemAddress( final VmInstance vm ) throws NotEnoughResourcesException {
    final String instanceId = vm.getInstanceId();
    final Address addr = this.allocateSystemAddress( vm.lookupPartition( ) );
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
      return lookupOrCreate( address, false );
    }

    protected static Address lookupOrCreate( final String address,
                                             final boolean assign ) {
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
        VmInstance vm = !assign ? null : maybeFindVm( null, address, null );
        addr = vm != null ?
            new Address( Principals.systemFullName( ), address, vm.getInstanceUuid(), vm.getInstanceId( ), vm.getPrivateAddress( ) ) :
            new Address( address );
      }
      return addr;
    }

    protected static Address lookupOrCreate( final Cluster cluster, final ClusterAddressInfo addrInfo ) {
      Address addr = null;
      VmInstance vm = null;
      try {
        addr = Addresses.getInstance( ).lookupDisabled( addrInfo.getAddress( ) );
        LOG.trace( "Found address in the inactive set cache: " + addr );
      } catch ( final NoSuchElementException e1 ) {
        try {
          addr = Addresses.getInstance( ).lookup( addrInfo.getAddress( ) );
          LOG.trace( "Found address in the active set cache: " + addr );
        } catch ( final NoSuchElementException e ) {}
      }
      if ( addrInfo.hasMapping( ) ) {
        vm = Helper.maybeFindVm( addr != null
          ? addr.getInstanceId( )
          : null, addrInfo.getAddress( ), addrInfo.getInstanceIp( ) );
        if ( ( addr != null ) && ( vm != null ) ) {
          Helper.ensureAllocated( addr, vm );
          clearOrphan( addrInfo );
        } else if ( addr != null && !addr.isPending( ) && vm != null && VmStateSet.DONE.apply( vm ) ) {
          handleOrphan( cluster, addrInfo );
        } else if ( ( addr != null && addr.isAssigned( ) && !addr.isPending( ) ) && ( vm == null ) ) {
          handleOrphan( cluster, addrInfo );
        } else if ( ( addr == null ) && ( vm != null ) ) {
          addr = new Address( Principals.systemFullName( ), addrInfo.getAddress( ), vm.getInstanceUuid(), vm.getInstanceId( ), vm.getPrivateAddress( ) );
          clearOrphan( addrInfo );
        } else if ( ( addr == null ) && ( vm == null ) ) {
          addr = new Address( addrInfo.getAddress( ) );
          handleOrphan( cluster, addrInfo );
        }
      } else {
        if ( ( addr != null ) && addr.isAssigned( ) && !addr.isPending( ) ) {
          handleOrphan( cluster, addrInfo );
        } else if ( ( addr != null ) && !addr.isAssigned( ) && !addr.isPending( ) && addr.isSystemOwned( ) ) {
          try {
            addr.release( );
          } catch ( final Exception ex ) {
            LOG.error( ex );
          }
        } else if ( ( addr != null ) && Address.Transition.system.equals( addr.getTransition( ) ) ) {
          handleOrphan( cluster, addrInfo );
        } else if ( addr == null ) {
          addr = new Address( addrInfo.getAddress( ) );
          Helper.clearVmState( addrInfo );
        }
      }
      return addr;
    }
    
    private static void markAsAllocated( final Cluster cluster, final ClusterAddressInfo addrInfo, final Address address ) {
      try {
        if ( !address.isPending( ) ) {
          for ( final VmInstance vm : VmInstances.list( VmState.RUNNING ) ) {
            if ( addrInfo.getInstanceIp( ).equals( vm.getPrivateAddress( ) ) && VmState.RUNNING.equals( vm.getState( ) ) ) {
              LOG.warn( "Out of band address state change: " + LogUtil.dumpObject( addrInfo ) + " address=" + address + " vm=" + vm );
//              if ( !address.isAllocated( ) ) {
//                address.pendingAssignment( ).assign( vm ).clearPending( );
//              } else {
//                address.assign( vm ).clearPending( );
//              }
//              clearOrphan( addrInfo );
              return;
            }
          }
        }
      } catch ( final IllegalStateException e ) {
        LOG.error( e );
      }
    }
    
    private static void clearAddressCachedState( final Address addr ) {
      try {
        if ( !addr.isPending( ) ) {
          addr.unassign( ).clearPending( );
        }
      } catch ( final Exception t ) {
        LOG.trace( t, t );
      }
    }
    
    private static void clearVmState( final ClusterAddressInfo addrInfo ) {
      try {
        final VmInstance vm = VmInstances.lookupByPublicIp( addrInfo.getAddress( ) );
        vm.clearPublicAddress( );
      } catch ( final NoSuchElementException e ) {}
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
    
    private static void ensureAllocated( final Address addr, final VmInstance vm ) {
      long lastUpdate = addr.lastUpdateMillis( );
      if ( lastUpdate > 60L * 1000 * AddressingConfiguration.getInstance( ).getOrphanGrace( ) ) {
        if ( !addr.isAllocated( ) && !addr.isPending( ) ) {
          try {
            if ( !addr.isAssigned( ) && !addr.isPending( ) ) {
              addr.pendingAssignment( );
              try {
                addr.assign( vm ).clearPending( );
              } catch ( final Exception e1 ) {
                LOG.debug( e1, e1 );
              }
            }
          } catch ( final Exception e1 ) {
            LOG.debug( e1, e1 );
          }
        } else if ( !addr.isAssigned( ) && !addr.isPending() ) {
          try {
            addr.assign( vm ).clearPending( );
          } catch ( final Exception e1 ) {
            LOG.debug( e1, e1 );
          }
        } else {
          LOG.debug( "Address usage checked: " + addr );
        }
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
