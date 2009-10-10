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
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.eucalyptus.util.EucalyptusProperties.TokenState;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.cluster.AssignAddressCallback;
import edu.ucsb.eucalyptus.cloud.cluster.UnassignAddressCallback;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.cloud.entities.Address;
import edu.ucsb.eucalyptus.cloud.exceptions.ExceptionList;
import edu.ucsb.eucalyptus.cloud.ws.AddressManager;

public class AddressUtil {

  public static Logger LOG = Logger.getLogger( AddressUtil.class );

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
    AddressUtil.markAddressUnassigned( address );
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
  public static void dispatchUnassignAddress( Address address ) {
    if( VmInstance.DEFAULT_IP.equals( address.getInstanceAddress( ) ) ) {
      return;
    }
    try {
      UnassignAddressCallback callback = new UnassignAddressCallback( address  );
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
        Address newAddress = AddressUtil.allocateAddresses( vm.getPlacement( ), 1 ).get( 0 );
        newAddress.setInstanceId( vm.getInstanceId( ) );
        newAddress.setInstanceAddress( vm.getNetworkConfig( ).getIpAddress( ) );
        AddressUtil.dispatchAssignAddress( newAddress, vm );
      } catch ( NotEnoughResourcesAvailable notEnoughResourcesAvailable ) {
        LOG.error( "Attempt to assign a system address for " + vm.getInstanceId( ) + " failed due to lack of addresses." );
      } catch ( Exception e ) {
        LOG.error( "Attempt to assign a system address for " + vm.getInstanceId( ) + " failed due to lack of addresses." );
      }
    }
  }

  public static List<Address>  tryAssignSystemAddresses( ResourceToken token ) throws Exception {
    if ( !EucalyptusProperties.disableNetworking ) {
      try {
        List<Address> newAddresses = AddressUtil.allocateAddresses( token.getCluster(), token.getAmount( ) );
        return newAddresses;
      } catch ( Exception e ) {
        throw e;
      }
    } else {
      throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (not supported by cluster)." );
    }
  }

  
  public static void releaseAddress( String s ) {
    try {
      Address addr = Addresses.getInstance( ).lookup( s );
      AddressUtil.releaseAddress( addr );
    } catch ( NoSuchElementException e ) {
      LOG.debug( e, e );
    }
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
    currentAddr.release( );
  }

  public synchronized static List<Address> allocateAddresses( String cluster, int count ) throws NotEnoughResourcesAvailable {
    boolean doDynamic = true;
    AddressUtil.updateAddressingMode( );
    doDynamic = Addresses.doDynamicAddressing();
    List<Address> addressList = null;
    if ( doDynamic ) {
      addressList = Addresses.getInstance().getDynamicSystemAddresses( cluster, count );
    } else {
      addressList = Addresses.getInstance().getStaticSystemAddresses( count );
    }
    return addressList;
  }

  public static void updateAddressingMode( ) {
    int allocatedCount = Addresses.clearUnusedSystemAddresses( );
    LOG.debug( "Found " + allocatedCount + " addresses allocated to eucalyptus" );
    if ( Addresses.doDynamicAddressing( ) ) {
      return;
    } else {
      Addresses.getInstance().doStaticAddressing( allocatedCount );
    }
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
            Addresses.getInstance().replace( addr );
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

  public static void clearAddress( Address address ) {
    if( !address.isPending( ) ) {
      try {
        markAddressUnassigned( address );
        dispatchUnassignAddress( address );
      } catch ( Throwable e1 ) {
        LOG.debug( e1, e1 );
      }
    } else if( Address.UNALLOCATED_USERID.equals( address.getUserId( ) ) ){
      address.clean( );
    }
  }
}
