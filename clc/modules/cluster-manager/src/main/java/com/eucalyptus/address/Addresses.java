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
package com.eucalyptus.address;

/*
 *
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.SuccessCallback;
import com.eucalyptus.cluster.UnconditionalCallback;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.event.AbstractNamedRegistry;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.SystemConfigurationEvent;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.eucalyptus.vm.VmState;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import edu.ucsb.eucalyptus.cloud.exceptions.ExceptionList;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

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
    
  @SuppressWarnings( { "unchecked" } )
  private static Map<String, Class> managerMap = new HashMap<String, Class>( ) {
                                                 { //TODO: this is primitive and temporary.
                                                   put( "truetrue", DynamicSystemAddressManager.class );
                                                   put( "falsetrue", StaticSystemAddressManager.class );
                                                   put( "falsefalse", NullSystemAddressManager.class );
                                                   put( "truefalse", NullSystemAddressManager.class );
                                                 }
                                               };
  
  public static List<Address> allocateSystemAddresses( String cluster, int count ) throws NotEnoughResourcesAvailable {
    return getAddressManager( ).allocateSystemAddresses( cluster, count );
  }
  
  private static AbstractSystemAddressManager getProvider( ) {
    String provider = "" + edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration.getSystemConfiguration( ).isDoDynamicPublicAddresses( )
                      + Iterables.all( Clusters.getInstance( ).listValues( ), new Predicate<Cluster>( ) {
                        @Override
                        public boolean apply( Cluster arg0 ) {
                          return arg0.getState( ).isAddressingInitialized( ) ? arg0.getState( ).hasPublicAddressing( ) : true;
                        }
                      } );
    try {
      if ( Addresses.systemAddressManager == null ) {
        Addresses.systemAddressManager = ( AbstractSystemAddressManager ) managerMap.get( provider ).newInstance( );
        LOG.info( "Setting the address manager to be: " + systemAddressManager.getClass( ).getSimpleName( ) );
      } else if ( !Addresses.systemAddressManager.getClass( ).equals( managerMap.get( provider ) ) ) {
        AbstractSystemAddressManager oldMgr = Addresses.systemAddressManager;
        Addresses.systemAddressManager = ( AbstractSystemAddressManager ) managerMap.get( provider ).newInstance( );
        Addresses.systemAddressManager.inheritReservedAddresses( oldMgr.getReservedAddresses( ) );
        LOG.info( "Setting the address manager to be: " + systemAddressManager.getClass( ).getSimpleName( ) );
      } 
    } catch ( Throwable e ) {
      LOG.debug( e, e );
    }
    return Addresses.systemAddressManager;
  }
  
  public static int getSystemReservedAddressCount( ) {
    return edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration.getSystemConfiguration( ).getSystemReservedPublicAddresses( );
  }
  
  public static int getUserMaxAddresses( ) {
    return edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration.getSystemConfiguration( ).getMaxUserPublicAddresses( );
  }
  
  //TODO: add config change event listener ehre.
  public static void updateAddressingMode( ) {
    getProvider( );
  }
  
  private static void policyLimits( String userId, boolean isAdministrator ) throws EucalyptusCloudException {
    int addrCount = 0;
    for ( Address a : Addresses.getInstance( ).listValues( ) ) {
      if ( userId.equals( a.getUserId( ) ) ) addrCount++;
    }
    if ( addrCount >= Addresses.getUserMaxAddresses( ) && !isAdministrator ) {
      throw new EucalyptusCloudException( ExceptionList.ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY );
    }
    
  }
  public static Address restrictedLookup( String userId, boolean isAdmin, String addr ) throws EucalyptusCloudException {
    Address address = null;
    try {
      address = Addresses.getInstance( ).lookup( addr );
    } catch ( NoSuchElementException e ) {
      throw new EucalyptusCloudException( "Permission denied while trying to release address: " + addr );
    }
    if ( !isAdmin && !address.getUserId( ).equals( userId ) ) {
      throw new EucalyptusCloudException( "Permission denied while trying to release address: " + addr );
      //    } else if ( address.isPending( ) ) {
      //      throw new EucalyptusCloudException( "A previous assign/unassign is still pending for this address: " + address.getName( ) );
    } else if ( address.isSystemOwned( ) && !isAdmin ) {
      throw new EucalyptusCloudException( "Cannot manipulate system owned address: " + address.getName( ) );
    }
    return address;
  }
    
  public static Address allocate( String userId, boolean isAdministrator ) throws EucalyptusCloudException, NotEnoughResourcesAvailable {
    Addresses.policyLimits( userId, isAdministrator );
    return Addresses.getAddressManager( ).allocateNext( userId );
  }
  
  //TODO: add return of callback, use reassign, special case for now
  public static void system( VmInstance vm ) {
    try {
      if( VmState.PENDING.equals( vm.getState() ) || VmState.RUNNING.equals( vm.getState() ) ) {
        Addresses.getInstance( ).getAddressManager( ).assignSystemAddress( vm );
      } 
    } catch ( NotEnoughResourcesAvailable e ) {
      LOG.warn( "No addresses are available to provide a system address for: " + LogUtil.dumpObject( vm ) );
      LOG.debug( e, e );
    }
  }
  
  public static void release( final Address addr ) {
    try {
      final String instanceId = addr.getInstanceId( );
      if ( addr.isAssigned( ) ) {
        addr.unassign( ).getCallback( ).then( new UnconditionalCallback( ) {
          @Override
          public void apply( ) {
            try {
              final VmInstance vm = VmInstances.getInstance( ).lookup( instanceId );
              Addresses.system( vm );
            } catch ( NoSuchElementException ex ) {
                                              }
                                          }
        } ).dispatch( addr.getCluster( ) );
      } else {
        addr.release( );
      }
    } catch ( IllegalStateException e ) {
      LOG.debug( e, e );
    }
  }
  
  @Override public void advertiseEvent( Event event ) {}

  @Override public void fireEvent( Event event ) {
    if( event instanceof SystemConfigurationEvent ) {
      Addresses.systemAddressManager = Addresses.getProvider( );
    }
  }
}
