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

package edu.ucsb.eucalyptus.cloud.ws;

import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentNavigableMap;

import org.apache.log4j.Logger;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Startable;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.util.LogUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.ucsb.eucalyptus.cloud.cluster.AssignAddressCallback;
import edu.ucsb.eucalyptus.cloud.cluster.ClusterEnvelope;
import edu.ucsb.eucalyptus.cloud.cluster.NotEnoughResourcesAvailable;
import edu.ucsb.eucalyptus.cloud.cluster.QueuedEvent;
import edu.ucsb.eucalyptus.cloud.cluster.UnassignAddressCallback;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.cloud.entities.Address;
import edu.ucsb.eucalyptus.cloud.exceptions.ExceptionList;
import edu.ucsb.eucalyptus.cloud.net.Addresses;
import edu.ucsb.eucalyptus.msgs.AllocateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.AllocateAddressType;
import edu.ucsb.eucalyptus.msgs.AssignAddressType;
import edu.ucsb.eucalyptus.msgs.AssociateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.AssociateAddressType;
import edu.ucsb.eucalyptus.msgs.DescribeAddressesResponseItemType;
import edu.ucsb.eucalyptus.msgs.DescribeAddressesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeAddressesType;
import edu.ucsb.eucalyptus.msgs.DisassociateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.DisassociateAddressType;
import edu.ucsb.eucalyptus.msgs.ReleaseAddressResponseType;
import edu.ucsb.eucalyptus.msgs.ReleaseAddressType;
import edu.ucsb.eucalyptus.msgs.UnassignAddressType;
import edu.ucsb.eucalyptus.util.Admin;

public class AddressManager implements Startable {

  private static Logger LOG = Logger.getLogger( AddressManager.class );

  public void start() throws MuleException {
    synchronized( AddressManager.class ) {
      EntityWrapper<Address> db = new EntityWrapper<Address>();
      try {
        List<Address> addrList = db.query( new Address() );
        db.commit();
        for ( Address addr : addrList ) {
          try {
            Addresses.getInstance().replace( addr.getName(), addr );
          } catch ( NoSuchElementException e ) {
            Addresses.getInstance().register( addr );
          }
        }
      } catch ( Throwable e ) {
        db.rollback( );
      }
    }
  }

  public static void updateAddressingMode() {
    int allocatedCount = 0;
    List<Address> activeList = Addresses.getInstance().listValues();
    for( Address allocatedAddr : activeList ) {
      if( Component.eucalyptus.name().equals( allocatedAddr.getUserId() ) ) {
        allocatedCount++;
          if( edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration( ).isDoDynamicPublicAddresses() && !allocatedAddr.isAssigned() && !allocatedAddr.isPending() ) {
            //:: deallocate unassigned addresses owned by eucalyptus when switching to dynamic public addressing :://
            LOG.debug("Deallocating unassigned public address in dynamic public addressing mode: " + allocatedAddr.getName() );
            EntityWrapper<Address> db = new EntityWrapper<Address>();
            try {
              Address dbAddr = db.getUnique( allocatedAddr );
              db.delete( dbAddr );
              db.commit();
            } catch ( EucalyptusCloudException e ) {
              db.rollback();
            }
            allocatedAddr.release();
            Addresses.getInstance().disable( allocatedAddr.getName() );
          }
      }
    }
    LOG.debug("Found " + allocatedCount + " addresses allocated to eucalyptus" );
      if( !edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration( ).isDoDynamicPublicAddresses() ) {
        int allocCount = edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration( ).getSystemReservedPublicAddresses() - allocatedCount;
        LOG.debug("Allocating additional " + allocCount + " addresses in static public addresing mode" );
        ConcurrentNavigableMap<String, Address> unusedAddresses = Addresses.getInstance().getDisabledMap();
        allocCount = unusedAddresses.size() < allocCount ? unusedAddresses.size() : allocCount;
        if( allocCount > 0 ) {
          List<Map.Entry<String, Address>> addressList = Lists.newArrayList();
          for ( int i = 0; i < allocCount; i++ ) {
            Map.Entry<String, Address> addressEntry = unusedAddresses.pollFirstEntry();
            if ( addressEntry != null ) {
              addressList.add( addressEntry );
            } else {
              break; //:: out of unused addresses :://
            }
          }
          NavigableSet<String> ipList = Sets.newTreeSet();
          for ( Map.Entry<String, Address> addressEntry : addressList ) {
            LOG.debug("Allocating address for static public addressing: " + addressEntry.getValue().getName() );
            Address address = addressEntry.getValue();
            address.allocate( Component.eucalyptus.name( ) );
            EntityWrapper<Address> db = new EntityWrapper<Address>();
            try {
              Address addr = db.getUnique( new Address( address.getName() ) );
              addr.allocate( Component.eucalyptus.name( ));
            } catch ( EucalyptusCloudException e ) {
              db.merge( address );
            }
            db.commit();
            ipList.add( address.getName() );
            try {
              Addresses.getInstance().register( address );
            } catch ( Exception e ) {
            }
          }
        } else {
          for( String ipAddr : Addresses.getInstance().getActiveMap().descendingKeySet() ) {
            Address addr = Addresses.getInstance().getActiveMap().get( ipAddr );
            if( Component.eucalyptus.name( ).equals( addr.getUserId() ) && !addr.isAssigned() && !addr.isPending() ) {
              if( allocCount++ >= 0 ) break;
              EntityWrapper<Address> db = new EntityWrapper<Address>();
              try {
                Address dbAddr = db.getUnique( new Address(addr.getName()) );
                db.delete( dbAddr );
                db.commit();
              } catch ( EucalyptusCloudException e ) {
                db.rollback();
              }
              addr.release();
              Addresses.getInstance().disable( addr.getName() );
            }
          }
        }
      }
  }

  public synchronized static NavigableSet<String> allocateAddresses( int count ) throws NotEnoughResourcesAvailable {
    boolean doDynamic = true;
    updateAddressingMode();  //:: make sure everything is up-to-date :://
    doDynamic = edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration( ).isDoDynamicPublicAddresses();
    NavigableSet<String> ipList = Sets.newTreeSet();
    List<Address> addressList = Lists.newArrayList();
    if( doDynamic ) {
      ConcurrentNavigableMap<String, Address> unusedAddresses = Addresses.getInstance().getDisabledMap();
      //:: try to fail fast if needed :://
      if ( unusedAddresses.size() < count ) throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (try --addressing private)" );
      for ( int i = 0; i < count; i++ ) {
        Map.Entry<String, Address> addressEntry = unusedAddresses.pollFirstEntry();
        if ( addressEntry != null ) {
          Address addr = addressEntry.getValue();
          addressList.add( addr );
          ipList.add( addr.getName() );
        } else {
          for ( Address a : addressList ) {
            unusedAddresses.putIfAbsent( a.getName(), a );
          }
          throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (try --addressing private)" );
        }
      }
    } else {
      List<Address> allocatedAddresses = Addresses.getInstance().listValues();
      for( Address addr : allocatedAddresses ) {
        if( !addr.isAssigned() && !addr.isPending() && Component.eucalyptus.name().equals( addr.getUserId() ) ) {
          Addresses.getInstance().deregister( addr.getName() );
          ipList.add( addr.getName() );
          addressList.add( addr );
          if( addressList.size() >= count ) break;
        }
      }
      if( addressList.size() < count ) {
        for( Address putBackAddr : addressList ) {
          Addresses.getInstance().register( putBackAddr );
        }
        throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (try --addressing private)" );
      }
    }
    for ( Address address : addressList ) {
      assignSystemPublicAddress( address );
    }
    return ipList;
  }

  private static void assignSystemPublicAddress( final Address address ) {
    address.allocate( Component.eucalyptus.name() );
    address.assign( Address.PENDING_ASSIGNMENT, Address.PENDING_ASSIGNMENT );
    EntityWrapper<Address> db = new EntityWrapper<Address>();
    try {
      Address addr = db.getUnique( new Address( address.getName() ) );
      addr.allocate( Component.eucalyptus.name() );
    } catch ( EucalyptusCloudException e ) {
      db.merge( address );
    }
    db.commit();
    try {
      Addresses.getInstance().register( address );
    } catch ( Exception e ) {
    }
  }

  public AllocateAddressResponseType AllocateAddress( AllocateAddressType request ) throws EucalyptusCloudException {
    AddressManager.updateAddressingMode();
    int addrCount = 0;
    for( Address a : Addresses.getInstance().listValues() ) {
      if( request.getUserId().equals( a.getUserId() ) ) addrCount++;
    }
    if( addrCount >= edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration( ).getMaxUserPublicAddresses() && !request.isAdministrator() )
      throw new EucalyptusCloudException( ExceptionList.ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY );

    ConcurrentNavigableMap<String, Address> unusedAddresses = Addresses.getInstance().getDisabledMap();
    Map.Entry<String, Address> addressEntry = unusedAddresses.pollFirstEntry();

    //:: address is null -- disabled map is empty :://
    if ( addressEntry == null ) {
      LOG.debug( LogUtil.header( LogUtil.dumpObject( Addresses.getInstance( ) ) ) );
      throw new EucalyptusCloudException( ExceptionList.ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY );
    }

    Address address = addressEntry.getValue();
    address.allocate( request.getUserId() );
    EntityWrapper<Address> db = new EntityWrapper<Address>();
    try {
      Address addr = db.getUnique( new Address( address.getName() ) );
      addr.allocate( request.getUserId() );
    } catch ( EucalyptusCloudException e ) {
      db.merge( address );
    }
    db.commit();

    try {
      Addresses.getInstance().register( address );
    } catch ( Exception e ) {
    }

    AllocateAddressResponseType reply = ( AllocateAddressResponseType ) request.getReply();
    reply.setPublicIp( address.getName() );
    return reply;
  }

  public ReleaseAddressResponseType ReleaseAddress( ReleaseAddressType request ) throws EucalyptusCloudException {
    AddressManager.updateAddressingMode();
    ReleaseAddressResponseType reply = ( ReleaseAddressResponseType ) request.getReply();
    reply.set_return( false );

    Address address = null;
    try {
      //:: find the addr :://
      address = Addresses.getInstance().lookup( request.getPublicIp() );
      if ( !request.isAdministrator() && !address.getUserId().equals( request.getUserId() ) )
        return reply;
      //:: dispatch the unassign if needed :://
      if ( address.isAssigned() && !address.isPending() ) {
        try {
          VmInstance oldVm = VmInstances.getInstance().lookup( address.getInstanceId() );
          AddressManager.unassignAddressFromVm( address, oldVm );
          AddressManager.tryAssignSystemAddress( oldVm );
        } catch ( NoSuchElementException e ) {}
      }

      if( Component.eucalyptus.name().equals( address.getUserId() ) && !edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration( ).isDoDynamicPublicAddresses() ) {
        LOG.debug( "Not de-allocating system owned address in static public addressing mode: " + address.getName() );
        return reply;
      }
      AddressManager.releaseAddress( address );
      reply.set_return( true );
    }
    catch ( NoSuchElementException e ) {
      return reply;
    }

    return reply;
  }

  public DescribeAddressesResponseType DescribeAddresses( DescribeAddressesType request ) throws EucalyptusCloudException {
    AddressManager.updateAddressingMode();
    DescribeAddressesResponseType reply = ( DescribeAddressesResponseType ) request.getReply();


    boolean isAdmin = request.isAdministrator();
    for ( Address address : Addresses.getInstance().listValues() ) {
      try {
        VmInstances.getInstance().lookup( address.getInstanceId() );
      } catch ( NoSuchElementException e ) {
        EntityWrapper<Address> db = new EntityWrapper<Address>();
        try {
          Address addr = db.getUnique( new Address( address.getName() ) );
          addr.unassign();
          db.commit();
        } catch ( EucalyptusCloudException ex ) {
          db.rollback();
        }
        address.unassign();
      }
      if ( isAdmin || address.getUserId().equals( request.getUserId() ) ) {
        reply.getAddressesSet().add( address.getDescription( isAdmin ) );
      }
    }
    if ( request.isAdministrator() )
      for ( Address address : Addresses.getInstance().listDisabledValues() )
        reply.getAddressesSet().add( new DescribeAddressesResponseItemType( address.getName(), Address.UNALLOCATED_USERID ) );

    return reply;
  }

  public AssociateAddressResponseType AssociateAddress( AssociateAddressType request ) throws Exception {
    AddressManager.updateAddressingMode();
    AssociateAddressResponseType reply = ( AssociateAddressResponseType ) request.getReply();
    reply.set_return( false );


    LOG.debug( "Associate: " + request.getPublicIp() + " => " + request.getInstanceId() );
    Address address = null;
    try {
      address = Addresses.getInstance().lookup( request.getPublicIp() );
    } catch ( NoSuchElementException e ) {
      return reply;
    }
    LOG.debug( "Found address: " + address );

    VmInstance vm = null;
    try {
      vm = VmInstances.getInstance().lookup( request.getInstanceId() );
    } catch ( NoSuchElementException e ) {
      return reply;
    }
    LOG.debug( "Found vm: " + vm );

    if ( !request.isAdministrator() && !( request.getUserId().equals( address.getUserId() ) && request.getUserId().equals( vm.getOwnerId() ) ) )
      return reply;

    //:: operation should be idempotent; request is legitimate so return true :://
    reply.set_return( true );

    //:: handle the address which may be currently assigned to the vm :://
    if( !vm.getNetworkConfig().getIpAddress().equals( vm.getNetworkConfig().getIgnoredPublicIp() ) && !VmInstance.DEFAULT_IP.equals( vm.getNetworkConfig().getIgnoredPublicIp() ) ) {
      String currentPublicIp = vm.getNetworkConfig().getIgnoredPublicIp();
      try {
        Address currentAddr = Addresses.getInstance().lookup( currentPublicIp );
        boolean release = Component.eucalyptus.name().equals( currentAddr.getUserId() ) && edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration( ).isDoDynamicPublicAddresses();
        LOG.debug( "Dispatching unassign message for: " + address );
        AddressManager.unassignAddressFromVm( currentAddr, vm );
        if( release ) {
          AddressManager.releaseAddress( currentAddr );
        }
      } catch ( NoSuchElementException e ) {
        return reply;
      }
    }
    //:: handle the vm which the requested address may be assigned to :://
    if ( address.isAssigned() && address.getUserId().equals( request.getUserId() ) && !address.isPending() ) {
      LOG.debug( "Dispatching unassign message for: " + address );
      try {
        VmInstance oldVm = VmInstances.getInstance().lookup( address.getInstanceId() );
        AddressManager.unassignAddressFromVm( address, oldVm );
        AddressManager.tryAssignSystemAddress( oldVm );

        if( !EucalyptusProperties.disableNetworking ) {
        }
      } catch ( NoSuchElementException e ) {
        LOG.error( e, e );
      }
    }

    AddressManager.assignAddressToVm( address, vm );

    return reply;
  }

  public static void releaseAddress( final Address currentAddr ) {
    EntityWrapper<Address> db = new EntityWrapper<Address>();
    try {
      Address addr = db.getUnique( new Address( currentAddr.getName() ) );
      currentAddr.unassign();
      addr.unassign();
      db.delete( addr );
      currentAddr.release();
      Addresses.getInstance().disable( currentAddr.getName() );
      db.commit();
    } catch ( EucalyptusCloudException e ) {
      db.rollback();
    }
  }
  
  public static void releaseAddress( String s ) {
    AddressManager.releaseAddress( new Address( s ) );
  }


  public DisassociateAddressResponseType DisassociateAddress( DisassociateAddressType request ) throws EucalyptusCloudException {
    AddressManager.updateAddressingMode();
    DisassociateAddressResponseType reply = ( DisassociateAddressResponseType ) request.getReply();
    reply.set_return( false );

    LOG.debug( "Disassociate: " + request.getPublicIp()  );
    Address address = null;
    try {
      address = Addresses.getInstance().lookup( request.getPublicIp() );
    } catch ( NoSuchElementException e ) {
      return reply;
    }
    LOG.debug( "Found address: " + address );

    VmInstance vm = null;
    try {
      vm = VmInstances.getInstance().lookup( address.getInstanceId() );
    } catch ( NoSuchElementException e ) {
      return reply;
    }
    LOG.debug( "Found vm: " + vm );

    if ( !request.isAdministrator() && !( request.getUserId().equals( address.getUserId() ) && request.getUserId().equals( vm.getOwnerId() ) ) )
      return reply;

    if( VmInstance.DEFAULT_IP.equals( vm.getInstanceId() ) )
        return reply;

    reply.set_return( true );
    AddressManager.unassignAddressFromVm( address, vm );
    AddressManager.tryAssignSystemAddress( vm );
    return reply;
  }

  private static void tryAssignSystemAddress( final VmInstance vm ) {
    if( !EucalyptusProperties.disableNetworking ) {
      try {
        String newAddr = allocateAddresses( 1 ).pollFirst();
        Address newAddress = Addresses.getInstance().lookup( newAddr );
        AddressManager.assignAddressToVm( newAddress, vm );
      } catch ( NotEnoughResourcesAvailable notEnoughResourcesAvailable ) {
        LOG.error( "Attempt to assign a system address for " + vm.getInstanceId() + " failed due to lack of addresses." );
      } catch ( NoSuchElementException e ) {
        LOG.error( "Attempt to assign a system address for " + vm.getInstanceId() + " failed due to lack of addresses." );
      }
    }
  }

  public static void unassignAddressFromVm( Address address, VmInstance vm ) {
    EntityWrapper<Address> db = new EntityWrapper<Address>();
    try {
      try {
        UnassignAddressType unassignMsg = Admin.makeMsg( UnassignAddressType.class, address.getName(), address.getInstanceAddress() );
        QueuedEvent q = QueuedEvent.make( new UnassignAddressCallback( address ), unassignMsg );
        Clusters.sendClusterEvent( address.getCluster( ), q );
        q.getCallback( ).getResponse( );
      } catch ( Throwable e ) {
        LOG.debug( e, e );
      }
      vm.getNetworkConfig( ).setIgnoredPublicIp( vm.getNetworkConfig( ).getIpAddress( ) );
      Address addr = db.getUnique( new Address( address.getName() ) );
      addr.unassign();
      address.unassign();
      db.commit();
    } catch ( EucalyptusCloudException e ) {
      db.rollback();
    }
  }

  private static void assignAddressToVm( Address address, VmInstance vm ) {
    //:: record the assignment :://
    EntityWrapper<Address> db = new EntityWrapper<Address>();
    try {
      Address addr = db.getUnique( new Address( address.getName() ) );
      addr.unassign();
      addr.assign( vm.getInstanceId(), vm.getNetworkConfig().getIpAddress() );
      address.assign( vm.getInstanceId(), vm.getNetworkConfig().getIpAddress() );
      //:: dispatch the request to the cluster that owns the address :://
      try {
        AssignAddressType assignMsg = Admin.makeMsg( AssignAddressType.class, address.getName(), address.getInstanceAddress(), address.getInstanceId( ) );
        QueuedEvent q = QueuedEvent.make( new AssignAddressCallback( vm ), assignMsg );
        Clusters.sendClusterEvent( address.getCluster( ), q );
        q.getCallback( ).getResponse( );
      } catch ( Throwable e ) {
        LOG.debug( e, e );
      }
      db.commit();
    } catch ( EucalyptusCloudException e ) {
      db.rollback();
    }
  }


}
