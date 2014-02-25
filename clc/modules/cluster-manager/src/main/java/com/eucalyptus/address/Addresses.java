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

import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.ResourceType.Address;
import static com.eucalyptus.address.Address.UNASSIGNED_INSTANCEADDR;
import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.compute.common.CloudMetadata.AddressMetadata;
import com.eucalyptus.cloud.util.NotEnoughResourcesException;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.component.Partition;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.event.AbstractNamedRegistry;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.event.SystemConfigurationEvent;
import com.eucalyptus.records.Logs;
import com.eucalyptus.reporting.event.ResourceAvailabilityEvent;
import com.eucalyptus.tags.FilterSupport;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes.QuantityMetricFunction;
import com.eucalyptus.util.RestrictedTypes.Resolver;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.UnconditionalCallback;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstance.VmStateSet;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

@SuppressWarnings( "serial" )
public class Addresses extends AbstractNamedRegistry<Address> implements EventListener {
  
  public static Logger     LOG       = Logger.getLogger( Addresses.class );
  private static Addresses singleton = Addresses.getInstance( );
  
  public static Addresses getInstance( ) {
    synchronized ( Addresses.class ) {
      if ( singleton == null ) {
        singleton = new Addresses( );
        ListenerRegistry.getInstance( ).register( SystemConfigurationEvent.class, singleton );
      }
    }
    return singleton;
  }
  
  private static AbstractSystemAddressManager systemAddressManager; //TODO: set a default value here.
                                                                    
  public static AbstractSystemAddressManager getAddressManager( ) {
    synchronized ( Addresses.class ) {
      if ( systemAddressManager == null ) {
        systemAddressManager = getProvider( );
      }
    }
    return systemAddressManager;
  }
  
  public static Address allocateSystemAddress( final Partition partition ) throws NotEnoughResourcesException {
    return getAddressManager( ).allocateSystemAddresses( partition, 1 ).get( 0 );
  }
  
  private static AbstractSystemAddressManager getProvider( ) {
    final boolean hasAddressing = Iterables.all( Clusters.getInstance( ).listValues( ), new Predicate<Cluster>( ) {
      @Override
      public boolean apply( Cluster arg0 ) {
        return arg0.getState( ).isAddressingInitialized( ) && arg0.getState( ).hasPublicAddressing( );
      }
    } );
    Class<? extends AbstractSystemAddressManager> newManager;
    if ( AddressingConfiguration.getInstance( ).getDoDynamicPublicAddresses( ) ) {
      newManager = DynamicSystemAddressManager.class;
    } else {
      newManager = StaticSystemAddressManager.class;
    }
    if ( Addresses.systemAddressManager == null ) {
      systemAddressManager = Classes.newInstance( newManager );
    } else if ( !newManager.equals( systemAddressManager.getClass( ) ) ) {
      final AbstractSystemAddressManager oldMgr = systemAddressManager;
      systemAddressManager = Classes.newInstance( newManager );
      systemAddressManager.inheritReservedAddresses( oldMgr.getReservedAddresses( ) );
    } else {
      return systemAddressManager;
    }
    LOG.info( "Setting the address manager to be: " + newManager.getSimpleName( ) );
    return systemAddressManager;
  }
  
  public static int getSystemReservedAddressCount( ) {
    return AddressingConfiguration.getInstance( ).getSystemReservedPublicAddresses( );
  }
  
  public static void updateAddressingMode( ) {
    getProvider( );
  }
  
  @Resolver( Address.class )
  public enum Lookup implements Function<String, Address> {
    INSTANCE;
    
    @Override
    public Address apply( final String input ) {
      final Address address = Addresses.getInstance( ).lookup( input );
      return address;
    }
    
  }
  
  @QuantityMetricFunction( AddressMetadata.class )
  public enum CountAddresses implements Function<OwnerFullName, Long> {
    INSTANCE;
    
    @SuppressWarnings( "unchecked" )
    @Override
    public Long apply( final OwnerFullName input ) {
      int i = 0;
      for ( final Address addr : Addresses.getInstance( ).listValues( ) ) {
        if ( addr.isAllocated( ) && addr.getOwnerAccountNumber( ).equals( input.getAccountNumber( ) ) ) {
          i++;
        }
      }
      return ( long ) i;
    }
    
  }
  
  public enum Allocator implements Supplier<Address>, Predicate<Address> {
    INSTANCE;
    @Override
    public Address get( ) {
      final Context ctx = Contexts.lookup( );
      try {
        return Addresses.getAddressManager( ).allocateNext( ctx.getUserFullName( ) );
      } catch ( final Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }
    
    @Override
    public boolean apply( final Address input ) {
      try {
        input.release( );
      } catch ( final Exception ex ) {
        LOG.error( ex, ex );
      }
      return true;
    }
  }
  
  public static void system( final VmInstance vm ) {
    try {
      if ( !vm.isUsePrivateAddressing() &&
          (VmState.PENDING.equals( vm.getState( ) ) || VmState.RUNNING.equals( vm.getState( ) ) ) ) {
        Addresses.getAddressManager( ).assignSystemAddress( vm );
      }
    } catch ( final NotEnoughResourcesException e ) {
      LOG.warn( "No addresses are available to provide a system address for: " + LogUtil.dumpObject( vm ) );
      LOG.debug( e, e );
    }
  }
  
  public static void release( final Address addr ) {
    try {
      final String instanceId = addr.getInstanceId( );
      if ( addr.isReallyAssigned() ) {
        boolean unassign = false;
        final boolean wasSystem = addr.isSystemOwned();
        final String wasOwnerUserId = addr.getOwnerUserId( );
        try {
          final VmInstance vm = VmInstances.lookup( instanceId );
          final String vmIp = Objects.firstNonNull( vm.getPublicAddress( ), UNASSIGNED_INSTANCEADDR );
          if ( VmStateSet.RUN.apply( vm ) ) {
            AddressingDispatcher.dispatch(
              AsyncRequests.newRequest( addr.unassign( ).getCallback( ) ).then( 
                new UnconditionalCallback( ) {
                  @Override
                  public void fire( ) {
                    try {
                      // Do not attempt to assign a system address if releasing a
                      // system address unless the instance was actually using the
                      // address. If address assignment is failing then looping
                      // can occur if retried here when the original assignment
                      // was unsuccessful.
                      if ( !wasSystem || vmIp.equals( addr.getName( ) ) ) {
                        Addresses.system( vm );
                      }
                    } finally {
                      // If not allocated then the address was already released.
                      // If assigned then the address was released and already
                      // reused.
                      // If different owner address was already released.
                      if ( addr.isAllocated( ) && !addr.isAssigned( ) &&
                          wasOwnerUserId.equals( addr.getOwnerUserId( ) ) ) try {
                          addr.release( );
                      } catch ( Exception e ) {
                        LOG.error( "Error releasing address after unassign", e );
                      }
                    }
                  }
              } ), 
              vm.getPartition( ) );
          } else {
            unassign = true;
          }
        } catch ( NoSuchElementException e ) {
          Logs.extreme().debug( e, e );
          unassign = true;
        }
        if ( unassign ) {
          addr.unassign( ).clearPending( );
          addr.release( );
        }
      } else {
        addr.release( );
      }
    } catch ( final Exception e ) {
      LOG.debug( e, e );
    }
  }
  
  @Override
  public void fireEvent( final Event event ) {
    if ( event instanceof SystemConfigurationEvent ) {
      Addresses.systemAddressManager = Addresses.getProvider( );
    }
  }

  public static void updatePublicIpByInstanceId( final String instanceId,
                                                 final String publicIp ) {
    Entities.asTransaction( VmInstance.class, new Predicate<String>() {
      @Override
      public boolean apply( final String publicAddress ) {
        final VmInstance vm = VmInstances.lookup( instanceId );
        vm.updatePublicAddress( publicAddress );
        return true;
      }
    } ).apply( publicIp );
  }
  
  public static void updatePublicIP( final String privateIp,
                                     final String publicIp ) {
    updatePublicIPOnMatch( privateIp, null, publicIp );
  }

  public static void updatePublicIPOnMatch( final String privateIp,
                                            @Nullable final String expectedPublicIp,
                                            final String publicIp
  ) {
    Entities.asTransaction( VmInstance.class, new Predicate<Void>() {
      @Override
      public boolean apply( @Nullable final Void nothing ) {
        try {
          final VmInstance vm = VmInstances.lookupByPrivateIp( privateIp );
          if ( expectedPublicIp == null || expectedPublicIp.equals( vm.getPublicAddress() ) ) {
            vm.updatePublicAddress( publicIp );
          }
        } catch ( NoSuchElementException e ) {
          LOG.debug( "Instance not found for private IP " + privateIp );
        } catch ( Exception t ) {
          LOG.error( t, t );
        }
        return true;
      }
    } ).apply( null );
  }

  public static class AddressAvailabilityEventListener implements EventListener<ClockTick> {

    public static void register( ) {
      Listeners.register(ClockTick.class, new AddressAvailabilityEventListener());
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      if ( Bootstrap.isFinished() && Hosts.isCoordinator() ) {
        final List<Address> addresses = Addresses.getInstance( ).listValues( );
        final List<Address> disabledAddresses = Addresses.getInstance( ).listDisabledValues( );
        final long total = addresses.size() + disabledAddresses.size();
        final long available = Iterators.size( Iterators.filter( Iterators.concat( addresses.iterator(), disabledAddresses.iterator()), new Predicate<com.eucalyptus.address.Address>() {
          @Override
          public boolean apply( final Address address ) {
            return !address.isAllocated();
          }
        } ) );

        try {
          ListenerRegistry.getInstance( ).fireEvent( new ResourceAvailabilityEvent( Address, new ResourceAvailabilityEvent.Availability( total, available ) ) );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    }
  }
  
  public static class AddressFilterSupport extends FilterSupport<Address> {
    public AddressFilterSupport() {
      super( builderFor( Address.class )
          .withConstantProperty( "domain", "standard" )
          .withStringProperty( "instance-id", FilterFunctions.INSTANCE_ID )
          .withStringProperty( "public-ip", FilterFunctions.PUBLIC_IP )
          .withUnsupportedProperty( "allocation-id" )
          .withUnsupportedProperty( "association-id" )
          .withUnsupportedProperty( "network-interface-id" )
          .withUnsupportedProperty( "network-interface-owner-id" )
          .withUnsupportedProperty( "private-ip-address" ) );
    }
  }
  
  private enum FilterFunctions implements Function<Address,String> {
    INSTANCE_ID {
      @Override
      public String apply( final Address address ) {
        return address.getInstanceId();
      }
    },
    PUBLIC_IP {
      @Override
      public String apply( final Address address ) {
        return address.getDisplayName();
      }
    }
  }
}
