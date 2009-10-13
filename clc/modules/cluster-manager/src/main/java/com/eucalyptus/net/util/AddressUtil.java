package com.eucalyptus.net.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.net.Addresses;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.cloud.Pair;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.cluster.AssignAddressCallback;
import edu.ucsb.eucalyptus.cloud.cluster.UnassignAddressCallback;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.cloud.entities.Address;
import edu.ucsb.eucalyptus.constants.VmState;

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

  private static ConcurrentMap<String,AtomicBoolean> clusterInit = getClusterAddressMap();
  public static boolean initialize( String cluster, List<Pair> ccList  ) {
    if( AddressUtil.tryInit( cluster ) ) {
      try {
        List<Address> addrList = getStoredAddresses( cluster );
        List<String> ccListAddrs = Lists.transform( ccList, new Function<Pair,String>() {
          @Override public String apply( Pair p ) { return p.getLeft( ); }          
        });
        for ( Address addr : addrList ) {
          if( ccListAddrs.contains( addr.getName( ) ) ) {
            Pair current = checkHasCurrentState( addr.getName( ), ccList );
            if( current != null ) {
              try {
                VmInstance vm = VmInstances.getInstance( ).lookupByInstanceIp( current.getRight( ) );
                addr.setAssigned( vm.getInstanceId( ), current.getRight( ) );
              } catch ( Exception e ) {
                addr.doUnassign( );
              }
            }
            addr.init( );
            ccList.remove( current );
          } else {
            try {
              addr.release( );
              Addresses.getInstance( ).deregister( addr.getName( ) );
            } catch ( Throwable e ) {
              LOG.debug( e );
            }
          }
        }
        for( Pair current : ccList ) {
          Address addr = AddressUtil.lookupOrCreate( cluster, current );
          try {
            VmInstance vm = VmInstances.getInstance( ).lookupByInstanceIp( current.getRight( ) );
            addr.allocate( Component.eucalyptus.name( ) );
            addr.setAssigned( vm.getInstanceId( ), current.getRight( ) );
          } catch ( Exception e ) {
            addr.doUnassign( );
          }
          addr.init( );
        }
      } catch ( Throwable e ) {
        clusterInit.get( cluster ).set( false );
      }
      return true;
    } else {
      return false;
    }
  }

  private static List<Address> getStoredAddresses( String cluster ) {
    EntityWrapper<Address> db = new EntityWrapper<Address>();
    Address clusterAddr = new Address();
    clusterAddr.setCluster( cluster );
    List<Address> addrList = Lists.newArrayList( );
    try {
      addrList = db.query( clusterAddr );
      db.commit();
    } catch ( Exception e1 ) {
      db.rollback( );
    }
    return addrList;
  }

  private static Pair checkHasCurrentState( String publicAddress, List<Pair> list ) {
    for( Pair p : list ) {
      if( p.getLeft( ).equals( publicAddress ) ) {
        return p;
      }
    }
    return null;
  }

  private static ConcurrentMap<String, AtomicBoolean> getClusterAddressMap( ) {
    synchronized(AddressUtil.class) {
      if( clusterInit == null ) {
        clusterInit = new ConcurrentHashMap<String,AtomicBoolean>();
        for( String cluster : Clusters.getInstance( ).listKeys( ) ) {
          clusterInit.put( cluster, new AtomicBoolean( false ) );
        }      
      }
    }
    return clusterInit;
  }
  
  public static Address lookupOrCreate( String cluster, Pair p ) {
    Address address;
    try {
      try {
        address = Addresses.getInstance( ).lookup( p.getLeft( ) );
      } catch ( NoSuchElementException e1 ) {
        address = Addresses.getInstance( ).lookupDisabled( p.getLeft( ) );
      }
    } catch ( NoSuchElementException e ) {
      LOG.debug( e );
      address = new Address( p.getLeft( ), cluster );
      try {
        VmInstance vm = VmInstances.getInstance( ).lookupByInstanceIp( p.getRight( ) );
        address.allocate( Component.eucalyptus.name( ) );
        address.setAssigned( vm.getInstanceId( ), p.getRight( ) );
      } catch ( Exception e1 ) {
        //TODO: dispatch unassign for unknown address.
      }      
      address.init( );
    }
    return address;
  }

  
  private static boolean tryInit( String cluster ) {
    clusterInit.putIfAbsent( cluster, new AtomicBoolean( false ) );
    return clusterInit.get( cluster ).compareAndSet( false, true );
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

  private static ConcurrentNavigableMap<String,Integer> orphans = new ConcurrentSkipListMap<String,Integer>();
  private static void handleOrphan( String cluster, Address address ) {
    Integer orphanCount = 1;
    orphanCount = orphans.putIfAbsent( address.getName( ), orphanCount );
    orphans.put( address.getName( ), orphanCount + 1 );
    LOG.warn( "Found orphaned public ip address: " + address + " count=" + orphanCount );
    if( orphanCount > 10 ) {
      orphans.remove( address.getName( ) );
      Clusters.dispatchClusterEvent( cluster, new UnassignAddressCallback( address ) );
    }
  }
  
  public static void update( String cluster, List<Pair> ccList ) {
    List<String> ccListAddrs = Lists.transform( ccList, new Function<Pair,String>() {
      @Override public String apply( Pair p ) { return p.getLeft( ); }          
    });
    for ( Pair p : ccList ) {
      Address address = AddressUtil.lookupOrCreate( cluster, p );
      try {
        InetAddress addr = Inet4Address.getByName( p.getRight( ) );
        VmInstance vm;
        try {
          vm = VmInstances.getInstance( ).lookupByInstanceIp( p.getRight( ) );
          if( Address.UNALLOCATED_USERID.equals( address.getUserId( ) ) ) {
            address.allocate( Component.eucalyptus.name() );
          }
          if( !address.isAssigned( ) ) {
            address.setAssigned( vm.getInstanceId( ), p.getRight( ) );
          }
          orphans.remove( address.getName( ) );
        } catch ( Exception e1 ) {
          if( !addr.isLoopbackAddress( ) && !AddressUtil.checkForPendingVm() ) {
            AddressUtil.handleOrphan( cluster, address );
          } else {
            orphans.remove( address.getName( ) );
          }
        }
      } catch ( UnknownHostException e1 ) {
        LOG.debug( e1, e1 );
        orphans.remove( address.getName( ) );
      }
    }          
  }

  private static boolean checkForPendingVm() {
    for ( VmInstance vm : VmInstances.getInstance( ).listValues( ) ) {
      if ( VmState.PENDING.equals( vm.getState( ) ) || VmState.RUNNING.equals( vm.getState( ) ) ) {
        return true;
      }
    }
    return false;
  }

}
