package com.eucalyptus.net.util;

import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentNavigableMap;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.net.Addresses;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.EucalyptusProperties.TokenState;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.ucsb.eucalyptus.cloud.cluster.AssignAddressCallback;
import edu.ucsb.eucalyptus.cloud.cluster.NotEnoughResourcesAvailable;
import edu.ucsb.eucalyptus.cloud.cluster.UnassignAddressCallback;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.cloud.entities.Address;
import edu.ucsb.eucalyptus.cloud.exceptions.ExceptionList;
import edu.ucsb.eucalyptus.cloud.ws.AddressManager;

public class AddressUtil {

  private static Logger LOG = Logger.getLogger( AddressUtil.class );

  public static void assignAddressToVm( Address address, VmInstance vm ) throws EucalyptusCloudException {
    AddressUtil.markAddressAssigned( address, vm );
    AddressUtil.dispatchAssignAddress( address, vm );
  }

  public static void dispatchAssignAddress( String addr, VmInstance vm ) {
    Address address = Addresses.getInstance( ).lookup( addr );
    AddressUtil.dispatchAssignAddress( address, vm );
  }

  public static void dispatchAssignAddress( Address address, VmInstance vm ) {
    try {
      AssignAddressCallback callback = new AssignAddressCallback( address, vm );
      Clusters.dispatchClusterEvent( address.getCluster( ), callback );
    } catch ( Throwable e ) {
      LOG.debug( e, e );
    }
  }

  public static void markAddressAssigned( Address address, VmInstance vm ) throws EucalyptusCloudException {
    address.assign( vm.getInstanceId( ), vm.getNetworkConfig( ).getIpAddress( ) );
  }

  public static void unassignAddressFromVm( Address address, VmInstance vm ) {
    try {
      AddressUtil.markAddressUnassigned( address );
    } catch ( Throwable e ) {
      LOG.debug( e, e );
    }
    AddressUtil.dispatchUnassignAddress( address, vm );
  }

  public static void dispatchUnassignAddress( Address address, VmInstance vm ) {
    if( VmInstance.DEFAULT_IP.equals( address.getInstanceAddress( ) ) ) {
      return;
    }
    try {
      UnassignAddressCallback callback = new UnassignAddressCallback( address, vm );
      Clusters.dispatchClusterEvent( address.getCluster( ), callback );
    } catch ( Throwable e ) {
      LOG.debug( e, e );
    }
  }

  public static void markAddressUnassigned( Address address ) {
    address.unassign( );
  }

  public static void tryAssignSystemAddress( final VmInstance vm ) {
    if ( !EucalyptusProperties.disableNetworking ) {
      try {
        Address newAddress = AddressUtil.allocateAddresses( 1 ).get( 0 );
        assignAddressToVm( newAddress, vm );
      } catch ( NotEnoughResourcesAvailable notEnoughResourcesAvailable ) {
        LOG.error( "Attempt to assign a system address for " + vm.getInstanceId( ) + " failed due to lack of addresses." );
      } catch ( Exception e ) {
        LOG.error( "Attempt to assign a system address for " + vm.getInstanceId( ) + " failed due to lack of addresses." );
      }
    }
  }

  public static void releaseAddress( String s ) {
    AddressUtil.releaseAddress( new Address( s ) );
  }

  public static void releaseAddress( final Address currentAddr ) {
    if ( currentAddr.isAssigned( ) ) {
      try {
        VmInstance vm = VmInstances.getInstance( ).lookup( currentAddr.getInstanceId( ) );
        unassignAddressFromVm( currentAddr, vm );
      } catch ( Throwable e ) {
        LOG.debug( e, e );
      }
    }
    EntityWrapper<Address> db = new EntityWrapper<Address>( );
    try {
      Address addr = db.getUnique( new Address( currentAddr.getName( ) ) );
      db.delete( addr );
      currentAddr.release( );
      LOG.info( String.format( EucalyptusProperties.DEBUG_FSTRING, EucalyptusProperties.TokenState.returned,
                               currentAddr ) );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback( );
    }
  }


  public synchronized static List<Address> allocateAddresses( int count ) throws NotEnoughResourcesAvailable {
    boolean doDynamic = true;
    AddressUtil.updateAddressingMode( ); //:: make sure everything is up-to-date :://
    doDynamic = edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration( ).isDoDynamicPublicAddresses( );
    List<Address> addressList = null;
    if ( doDynamic ) {
      addressList = getDynamicSystemAddresses( count );
    } else {
      addressList = getStaticSystemAddresses( count );
    }
    for ( Address address : addressList ) {
      address.allocate( Component.eucalyptus.name( ) );
    }
    return addressList;
  }

  private static List<Address> getDynamicSystemAddresses( int count ) throws NotEnoughResourcesAvailable {
    ConcurrentNavigableMap<String, Address> unusedAddresses = Addresses.getInstance( ).getDisabledMap( );
    //:: try to fail fast if needed :://
    if ( unusedAddresses.size( ) < count ) throw new NotEnoughResourcesAvailable(
      "Not enough resources available: addresses (try --addressing private)" );
    List<Address> addressList = Lists.newArrayList( );
    for ( int i = 0; i < count; i++ ) {
      Map.Entry<String, Address> addressEntry = unusedAddresses.pollFirstEntry( );
      if ( addressEntry != null ) {
        Address addr = addressEntry.getValue( );
        addressList.add( addr );
      } else {
        for ( Address a : addressList ) {
          unusedAddresses.putIfAbsent( a.getName( ), a );
        }
        throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (try --addressing private)" );
      }
    }
    return addressList;
  }

  private static List<Address> getStaticSystemAddresses( int count ) throws NotEnoughResourcesAvailable {
    List<Address> addressList = Lists.newArrayList( );
    for ( Address addr : Addresses.getInstance( ).listValues( ) ) {
      if ( !addr.isAssigned( ) && !addr.isPending( ) && Component.eucalyptus.name( ).equals( addr.getUserId( ) ) ) {
        Addresses.getInstance( ).deregister( addr.getName( ) );
        addressList.add( addr );
        if ( addressList.size( ) >= count ) break;
      }
    }
    if ( addressList.size( ) < count ) {
      for ( Address putBackAddr : addressList ) {
        Addresses.getInstance( ).register( putBackAddr );
      }
      throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (try --addressing private)" );
    }
    return addressList;
  }

  public static void updateAddressingMode( ) {
    int allocatedCount = AddressUtil.clearUnusedSystemAddresses( );
    LOG.debug( "Found " + allocatedCount + " addresses allocated to eucalyptus" );
    if ( edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration( ).isDoDynamicPublicAddresses( ) ) return;
    
    
    int allocCount = edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration( ).getSystemReservedPublicAddresses( ) - allocatedCount;
    LOG.debug( "Allocating additional " + allocCount + " addresses in static public addresing mode" );
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
          releaseAddress( addr );
        }
      }
    }
  }

  public static int clearUnusedSystemAddresses( ) {
    int allocatedCount = 0;
    for ( Address allocatedAddr : Addresses.getInstance( ).listValues( ) ) {
      if ( allocatedAddr.isSystemAllocated( ) ) {
        allocatedCount++;
        if ( edu.ucsb.eucalyptus.util.EucalyptusProperties.getSystemConfiguration( ).isDoDynamicPublicAddresses( ) && !allocatedAddr.isAssigned( ) && !allocatedAddr.isPending( ) ) {
          //:: deallocate unassigned addresses owned by eucalyptus when switching to dynamic public addressing :://
          LOG.debug( "Deallocating unassigned public address in dynamic public addressing mode: " + allocatedAddr.getName( ) );
          allocatedAddr.release( );
        }
      }
    }
    return allocatedCount;
  }

  public static Address nextAvailableAddress( String userId ) throws EucalyptusCloudException {
    ConcurrentNavigableMap<String, Address> unusedAddresses = Addresses.getInstance().getDisabledMap();
    Map.Entry<String, Address> addressEntry = unusedAddresses.pollFirstEntry();
  
    //:: address is null -- disabled map is empty :://
    if ( addressEntry == null ) {
      AddressManager.LOG.debug( LogUtil.header( LogUtil.dumpObject( Addresses.getInstance( ) ) ) );
      throw new EucalyptusCloudException( ExceptionList.ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY );
    }
  
    Address address = addressEntry.getValue();
    address.allocate( userId );
    return address;
  }

  private static volatile boolean init = false;
  public static boolean initialize() {
    if( !init ) {
      EntityWrapper<Address> db = new EntityWrapper<Address>();
      try {
        List<Address> addrList = db.query( new Address() );
        db.commit();
        for ( Address addr : addrList ) {
          addr.init( );
          try {
            Addresses.getInstance().replace( addr.getName(), addr );
          } catch ( NoSuchElementException e ) {
            Addresses.getInstance().register( addr );
          }
        }
      } catch ( Throwable e ) {
        db.rollback( );
      }
      init = true;
      return true;
    } else {
      return false;
    }
  }
}
