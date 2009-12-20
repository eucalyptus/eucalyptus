package com.eucalyptus.address;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.net.util.ClusterAddressInfo;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.cluster.AssignAddressCallback;
import edu.ucsb.eucalyptus.cloud.cluster.QueuedEventCallback;
import edu.ucsb.eucalyptus.cloud.cluster.UnassignAddressCallback;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.cloud.exceptions.ExceptionList;
import edu.ucsb.eucalyptus.constants.VmState;

public abstract class AbstractSystemAddressManager {
  static Logger LOG = Logger.getLogger( AbstractSystemAddressManager.class );  
  public Address allocateNext( String userId ) throws NotEnoughResourcesAvailable {
    Address addr = Addresses.getInstance( ).enableFirst( ).allocate( userId );
    LOG.debug( "Allocated address for public addressing: " + addr.toString( ) );
    if ( addr == null ) {
      LOG.debug( LogUtil.header( Addresses.getInstance( ).toString( ) ) );
      throw new NotEnoughResourcesAvailable( ExceptionList.ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY );
    }
    return addr;
  }

  public abstract void assignSystemAddress( final VmInstance vm ) throws NotEnoughResourcesAvailable;
//  public abstract void assignSystemAddress( final VmInstance vm ) throws NotEnoughResourcesAvailable;
  public abstract List<Address> getReservedAddresses();
  public abstract void inheritReservedAddresses( List<Address> previouslyReservedAddresses );
  public abstract List<Address> allocateSystemAddresses( String cluster, int count ) throws NotEnoughResourcesAvailable;
  
  public void update( Cluster cluster, List<ClusterAddressInfo> ccList ) {
    if ( !cluster.getState( ).isAddressingInitialized( ) ) {
      Helper.loadStoredAddresses( cluster );
      cluster.getState( ).setAddressingInitialized( true );
    }
    for( ClusterAddressInfo addrInfo : ccList ) {
      try {
        Address address = Helper.lookupOrCreate( cluster, addrInfo );
        if( address.isAllocated( ) && Address.UNALLOCATED_USERID.equals( address.getUserId( ) ) ) {
          address.allocate( Component.eucalyptus.name( ) );
          address.clearPending( );
          cluster.getState( ).clearOrphan( addrInfo );
        } else if ( address.isAssigned( ) ) {
          try {
            VmInstance vm = VmInstances.getInstance( ).lookupByInstanceIp( addrInfo.getInstanceIp( ) );
            if ( !address.isAssigned( ) ) {
              address.assign( vm.getInstanceId( ), addrInfo.getInstanceIp( ) );
              address.clearPending( );
            }
            cluster.getState( ).clearOrphan( addrInfo );
          } catch ( NoSuchElementException e ) {
            InetAddress addr = null;
            try {
              addr = Inet4Address.getByName( addrInfo.getInstanceIp( ) );
            } catch ( UnknownHostException e1 ) {
              LOG.debug( e1, e1 );
            }
            if ( addr == null || !addr.isLoopbackAddress( )  ) {
              cluster.getState().handleOrphan( addrInfo );
            }
          }
        }
      } catch ( Throwable e ) {
        LOG.debug( e, e );
      }
    }
  }
  
  protected static class Helper {
    protected static Address lookupOrCreate( Cluster cluster, ClusterAddressInfo addrInfo ) {
      try {
        return Addresses.getInstance( ).lookupDisabled( addrInfo.getAddress( ) );
      } catch ( NoSuchElementException e2 ) {}
      try {
        return Addresses.getInstance( ).lookup( addrInfo.getAddress( ) );
      } catch ( NoSuchElementException e ) {}
      try {
        if( addrInfo.getInstanceIp( ) != null &&  !"".equals(addrInfo.getInstanceIp( ))) {
          VmInstance vm = VmInstances.getInstance( ).lookupByInstanceIp( addrInfo.getInstanceIp( ) );
          return new Address( addrInfo.getAddress( ), cluster.getName( ), Component.eucalyptus.name( ), vm.getInstanceId( ), vm.getNetworkConfig( ).getIpAddress( ) );/*TODO: this can't be true... all owned by eucalyptus?*/
        } else {
          return new Address( addrInfo.getAddress( ), cluster.getName( ) );
        }
      } catch( NoSuchElementException e ) {
        new UnassignAddressCallback( addrInfo ).dispatch( cluster );//TODO: review this degenerate case.
        Address address = new Address( addrInfo.getAddress( ), cluster.getName( ) );
        return address;
      }
    }
    protected static void loadStoredAddresses( Cluster cluster ) {
      try {
        EntityWrapper<Address> db = new EntityWrapper<Address>( );
        Address clusterAddr = new Address( );
        clusterAddr.setCluster( cluster.getName( ) );
        List<Address> addrList = Lists.newArrayList( );
        try {
          addrList = db.query( clusterAddr );
          db.commit( );
        } catch ( Exception e1 ) {
          db.rollback( );
        }
        for( Address addr : addrList ) {
          try {
            Addresses.getInstance( ).lookup( addr.getName( ) );
          } catch ( Exception e ) {
            addr.init( );
          }
        }
      } catch ( Throwable e ) {
        LOG.debug( e, e );
      }
    }
    protected static boolean checkActiveVm( ) {//TODO: review this degenerate case.
      for ( VmInstance vm : VmInstances.getInstance( ).listValues( ) ) {
        if ( VmState.PENDING.equals( vm.getState( ) ) || VmState.RUNNING.equals( vm.getState( ) ) ) {
          return true;
        }
      }
      return false;
    }    
  }
  
}
