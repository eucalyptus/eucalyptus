package com.eucalyptus.address;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.eucalyptus.vm.VmState;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.exceptions.ExceptionList;

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
  public abstract List<Address> getReservedAddresses( );
  public abstract void inheritReservedAddresses( List<Address> previouslyReservedAddresses );
  public abstract List<Address> allocateSystemAddresses( String cluster, int count ) throws NotEnoughResourcesAvailable;
  
  public void update( Cluster cluster, List<ClusterAddressInfo> ccList ) {
    if ( !cluster.getState( ).isAddressingInitialized( ) ) {
      Helper.loadStoredAddresses( cluster );
      cluster.getState( ).setAddressingInitialized( true );
    }
    for ( ClusterAddressInfo addrInfo : ccList ) {
      try {
        Address address = Helper.lookupOrCreate( cluster, addrInfo );
        if ( address.isAssigned( ) ) {
          if ( Address.UNALLOCATED_USERID.equals( address.getUserId( ) ) ) {
            Helper.markAsAllocated( cluster, addrInfo, address );
          }
          try {
            VmInstance vm = VmInstances.getInstance( ).lookupByInstanceIp( addrInfo.getInstanceIp( ) );
            cluster.getState( ).clearOrphan( addrInfo );
          } catch ( NoSuchElementException e ) {
            InetAddress addr = null;
            try {
              addr = Inet4Address.getByName( addrInfo.getInstanceIp( ) );
            } catch ( UnknownHostException e1 ) {
              LOG.debug( e1, e1 );
            }
            if ( addr == null || !addr.isLoopbackAddress( ) ) {
              cluster.getState( ).handleOrphan( addrInfo );
            }
          }
        } else if ( address.isAllocated( ) && Address.UNALLOCATED_USERID.equals( address.getUserId( ) ) && !address.isPending( ) ) {
          Helper.markAsAllocated( cluster, addrInfo, address );
        }
      } catch ( Throwable e ) {
        LOG.debug( e, e );
      }
    }
  }
    
  protected static class Helper {
    protected static Address lookupOrCreate( Cluster cluster, ClusterAddressInfo addrInfo ) {
      Address addr = null;
      VmInstance vm = null;
      try {
        addr = Addresses.getInstance( ).lookupDisabled( addrInfo.getAddress( ) );
        LOG.trace( "Found address in the inactive set cache: " + addr );
      } catch ( NoSuchElementException e1 ) {
        try {
          addr = Addresses.getInstance( ).lookup( addrInfo.getAddress( ) );
          LOG.trace( "Found address in the active set cache: " + addr );
        } catch ( NoSuchElementException e ) {}
      }
      Helper.checkUniqueness( addrInfo );
      if ( addrInfo.hasMapping( ) ) {
        vm = Helper.maybeFindVm( addrInfo.getAddress( ), addrInfo.getInstanceIp( ) );
        if ( addr != null && vm != null ) {
          Helper.ensureAllocated( addr, vm );
          cluster.getState( ).clearOrphan( addrInfo );
        } else if ( addr != null && vm != null && vm.getState( ).ordinal( ) > VmState.RUNNING.ordinal( ) ) {
          cluster.getState( ).handleOrphan( addrInfo );
        } else if ( addr != null && vm == null ) {
          cluster.getState( ).handleOrphan( addrInfo );
        } else if ( addr == null && vm != null ) {
          addr = new Address( addrInfo.getAddress( ), cluster.getName( ), Component.eucalyptus.name( ), vm.getInstanceId( ), vm.getPrivateAddress( ) );
          cluster.getState( ).clearOrphan( addrInfo );
        } else if( addr == null && vm == null ) {
          addr = new Address( addrInfo.getAddress( ), cluster.getName( ) );
          cluster.getState().handleOrphan( addrInfo );
        }
      } else {
        if( addr != null && addr.isAssigned( ) && !addr.isPending( ) ) {
          cluster.getState( ).handleOrphan( addrInfo );
        } else if( addr != null && !addr.isAssigned( ) && !addr.isPending( ) && addr.isSystemOwned( ) ) {
          try {
            addr.release( );
          } catch ( Exception ex ) {
            LOG.error( ex );
          }
        } else if( addr != null && Address.Transition.system.equals( addr.getTransition( ) ) ) {
          cluster.getState( ).handleOrphan( addrInfo );
        } else if( addr == null ) {
          addr = new Address( addrInfo.getAddress( ), cluster.getName( ) );
          Helper.clearVmState( addrInfo );
        }
      } 
      return addr;
    }
    
    private static void markAsAllocated( Cluster cluster, ClusterAddressInfo addrInfo, Address address ) {
      try {
        if( !address.isPending( ) ) {
          for ( VmInstance vm : VmInstances.getInstance( ).listValues( ) ) {
            if ( addrInfo.getInstanceIp( ).equals( vm.getPrivateAddress( ) ) && VmState.RUNNING.equals( vm.getState( ) ) ) {
              LOG.warn( "Out of band address state change: " + LogUtil.dumpObject( addrInfo ) + " address=" + address + " vm=" + vm );
              if( !address.isAllocated( ) ) {
                address.pendingAssignment( ).assign( vm.getInstanceId( ), vm.getPrivateAddress( ) ).clearPending( );
              } else {
                address.assign( vm.getInstanceId( ), vm.getPrivateAddress( ) ).clearPending( );
              }
              cluster.getState( ).clearOrphan( addrInfo );
              return;
            }
          }
        }
      } catch ( IllegalStateException e ) {
        LOG.error( e );
      }
    }

    private static void clearAddressCachedState( Address addr ) {
      try {
        if( !addr.isPending( ) ) {
          addr.unassign( ).clearPending( );
        }
      } catch ( Throwable t ) {
        LOG.trace( t, t );
      }
    }

    private static void clearVmState( ClusterAddressInfo addrInfo ) {
      try {
        VmInstance vm = VmInstances.getInstance( ).lookupByPublicIp( addrInfo.getAddress( ) );
        vm.updatePublicAddress( vm.getPrivateAddress( ) );
      } catch ( NoSuchElementException e ) {
      }
    }
    
    private static VmInstance maybeFindVm( String publicIp, String privateIp ) {
      VmInstance vm = null;
      try {
        vm = VmInstances.getInstance( ).lookupByInstanceIp( privateIp );
        LOG.trace( "Candidate vm which claims this address: " + vm.getInstanceId( ) + " " + vm.getState( ) + " " + publicIp );
        if ( publicIp.equals( vm.getPublicAddress( ) ) ) {
          LOG.trace( "Found vm which claims this address: " + vm.getInstanceId( ) + " " + vm.getState( ) + " " + publicIp );
          return vm;
        }
      } catch ( NoSuchElementException e ) {}
      return null;
    }
    
    private static void ensureAllocated( Address addr, VmInstance vm ) {
      if ( !addr.isAllocated( ) && !addr.isPending( ) ) {
        try {
          if ( !addr.isAssigned( ) && !addr.isPending( ) ) {
            addr.pendingAssignment( );
            try {
              addr.assign( vm.getInstanceId( ), vm.getPrivateAddress( ) ).clearPending( );
            } catch ( Throwable e1 ) {
              LOG.debug( e1, e1 );
            }
          }
        } catch ( Throwable e1 ) {
          LOG.debug( e1, e1 );
        }
      } else if ( !addr.isAssigned( ) ) {
        try {
          addr.assign( vm.getInstanceId( ), vm.getPrivateAddress( ) ).clearPending( );
        } catch ( Throwable e1 ) {
          LOG.debug( e1, e1 );
        }
      } else {
        LOG.debug( "Address usage checked: " + addr );
      }
    }
    
    private static void checkUniqueness( ClusterAddressInfo addrInfo ) {
      int vmCount = VmInstances.getInstance( ).countByPublicIp( addrInfo.getAddress( ) );
      if ( vmCount > 1 ) {
        String vmList = "";
        for ( VmInstance v : VmInstances.getInstance( ).listValues( ) ) {
          if ( addrInfo.getAddress( ).equals( v.getPublicAddress( ) ) && ( VmState.PENDING.equals( v.getState( ) ) || VmState.RUNNING.equals( v.getState( ) ) ) ) {
            vmList += " " + v.getInstanceId( ) + "(" + v.getState( ) + ")";
          }
        }
        LOG.error( "Found " + vmCount + " vms with the same address: " + addrInfo + " -> " + vmList );
        //TODO: handle reconciling state.
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
        for ( Address addr : addrList ) {
          try {
            LOG.info( "Restoring persistent address info for: " + addr );
            Addresses.getInstance( ).lookup( addr.getName( ) );
            addr.init( );
          } catch ( Throwable e ) {
            addr.init( );
          }
        }
      } catch ( Throwable e ) {
        LOG.debug( e, e );
      }
    }
  }
  
}
