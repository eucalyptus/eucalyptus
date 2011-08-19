package com.eucalyptus.address;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.FakePrincipals;
import com.eucalyptus.cloud.util.NotEnoughResourcesAvailable;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.vm.VmState;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.exceptions.ExceptionList;
import edu.ucsb.eucalyptus.msgs.ClusterAddressInfo;

public abstract class AbstractSystemAddressManager {
  private final static Logger LOG = Logger.getLogger( AbstractSystemAddressManager.class );
  
  public Address allocateNext( final OwnerFullName userId ) throws NotEnoughResourcesAvailable {
    final Address addr = Addresses.getInstance( ).enableFirst( ).allocate( userId );
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
  
  public abstract List<Address> allocateSystemAddresses( Partition partition, int count ) throws NotEnoughResourcesAvailable;
  
  public Address allocateSystemAddress( final String partition ) throws NotEnoughResourcesAvailable {
    return this.allocateSystemAddresses( Partitions.lookupByName( partition ), 1 ).get( 0 );
    
  }
  
  public void update( final Cluster cluster, final List<ClusterAddressInfo> ccList ) {
    if ( !cluster.getState( ).isAddressingInitialized( ) ) {
      Helper.loadStoredAddresses( cluster );
      cluster.getState( ).setAddressingInitialized( true );
    }
    for ( final ClusterAddressInfo addrInfo : ccList ) {
      try {
        final Address address = Helper.lookupOrCreate( cluster, addrInfo );
        if ( address.isAssigned( ) && !address.isPending( ) ) {
          if ( FakePrincipals.nobodyFullName( ).equals( address.getOwner( ) ) ) {
            Helper.markAsAllocated( cluster, addrInfo, address );
          }
          try {
            final VmInstance vm = VmInstances.lookupByInstanceIp( addrInfo.getInstanceIp( ) );
            cluster.getState( ).clearOrphan( addrInfo );
          } catch ( final NoSuchElementException e ) {
            InetAddress addr = null;
            try {
              addr = Inet4Address.getByName( addrInfo.getInstanceIp( ) );
            } catch ( final UnknownHostException e1 ) {
              LOG.debug( e1, e1 );
            }
            if ( ( addr == null ) || !addr.isLoopbackAddress( ) ) {
              cluster.getState( ).handleOrphan( addrInfo );
            }
          }
        } else if ( address.isAllocated( ) && FakePrincipals.nobodyFullName( ).equals( address.getOwner( ) ) && !address.isPending( ) ) {
          Helper.markAsAllocated( cluster, addrInfo, address );
        }
      } catch ( final Exception e ) {
        LOG.debug( e, e );
      }
    }
  }
  
  protected static class Helper {
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
      Helper.checkUniqueness( addrInfo );
      if ( addrInfo.hasMapping( ) ) {
        vm = Helper.maybeFindVm( addrInfo.getAddress( ), addrInfo.getInstanceIp( ) );
        if ( ( addr != null ) && ( vm != null ) ) {
          Helper.ensureAllocated( addr, vm );
          cluster.getState( ).clearOrphan( addrInfo );
        } else if ( ( addr != null ) && ( vm != null ) && ( vm.getState( ).ordinal( ) > VmState.RUNNING.ordinal( ) ) ) {
          cluster.getState( ).handleOrphan( addrInfo );
        } else if ( ( addr != null ) && ( vm == null ) ) {
          cluster.getState( ).handleOrphan( addrInfo );
        } else if ( ( addr == null ) && ( vm != null ) ) {
          addr = new Address( FakePrincipals.systemFullName( ), addrInfo.getAddress( ), cluster.getPartition( ), vm.getInstanceId( ), vm.getPrivateAddress( ) );
          cluster.getState( ).clearOrphan( addrInfo );
        } else if ( ( addr == null ) && ( vm == null ) ) {
          addr = new Address( addrInfo.getAddress( ), cluster.getPartition( ) );
          cluster.getState( ).handleOrphan( addrInfo );
        }
      } else {
        if ( ( addr != null ) && addr.isAssigned( ) && !addr.isPending( ) ) {
          cluster.getState( ).handleOrphan( addrInfo );
        } else if ( ( addr != null ) && !addr.isAssigned( ) && !addr.isPending( ) && addr.isSystemOwned( ) ) {
          try {
            addr.release( );
          } catch ( final Exception ex ) {
            LOG.error( ex );
          }
        } else if ( ( addr != null ) && Address.Transition.system.equals( addr.getTransition( ) ) ) {
          cluster.getState( ).handleOrphan( addrInfo );
        } else if ( addr == null ) {
          addr = new Address( addrInfo.getAddress( ), cluster.getPartition( ) );
          Helper.clearVmState( addrInfo );
        }
      }
      return addr;
    }
    
    private static void markAsAllocated( final Cluster cluster, final ClusterAddressInfo addrInfo, final Address address ) {
      try {
        if ( !address.isPending( ) ) {
          for ( final VmInstance vm : VmInstances.listValues( ) ) {
            if ( addrInfo.getInstanceIp( ).equals( vm.getPrivateAddress( ) ) && VmState.RUNNING.equals( vm.getState( ) ) ) {
              LOG.warn( "Out of band address state change: " + LogUtil.dumpObject( addrInfo ) + " address=" + address + " vm=" + vm );
              if ( !address.isAllocated( ) ) {
                address.pendingAssignment( ).assign( vm ).clearPending( );
              } else {
                address.assign( vm ).clearPending( );
              }
              cluster.getState( ).clearOrphan( addrInfo );
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
        vm.updatePublicAddress( vm.getPrivateAddress( ) );
      } catch ( final NoSuchElementException e ) {}
    }
    
    private static VmInstance maybeFindVm( final String publicIp, final String privateIp ) {
      VmInstance vm = null;
      try {
        vm = VmInstances.lookupByInstanceIp( privateIp );
        LOG.trace( "Candidate vm which claims this address: " + vm.getInstanceId( ) + " " + vm.getState( ) + " " + publicIp );
        if ( publicIp.equals( vm.getPublicAddress( ) ) ) {
          LOG.trace( "Found vm which claims this address: " + vm.getInstanceId( ) + " " + vm.getState( ) + " " + publicIp );
          return vm;
        }
      } catch ( final NoSuchElementException e ) {}
      return null;
    }
    
    private static void ensureAllocated( final Address addr, final VmInstance vm ) {
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
      } else if ( !addr.isAssigned( ) ) {
        try {
          addr.assign( vm ).clearPending( );
        } catch ( final Exception e1 ) {
          LOG.debug( e1, e1 );
        }
      } else {
        LOG.debug( "Address usage checked: " + addr );
      }
    }
    
    private static void checkUniqueness( final ClusterAddressInfo addrInfo ) {
      final Collection<VmInstance> matches = Collections2.filter( VmInstances.listValues( ), VmInstances.withPrivateAddress( addrInfo.getAddress( ) ) );
      if ( matches.size( ) > 1 ) {
        LOG.error( "Found " + matches.size( ) + " vms with the same address: " + addrInfo + " -> " + matches );
      }
    }
    
    protected static void loadStoredAddresses( final Cluster cluster ) {
      try {
        final EntityWrapper<Address> db = EntityWrapper.get( Address.class );
        final Address clusterAddr = new Address( );
        clusterAddr.setCluster( cluster.getPartition( ) );
        List<Address> addrList = Lists.newArrayList( );
        try {
          addrList = db.query( clusterAddr );
          db.commit( );
        } catch ( final Exception e1 ) {
          db.rollback( );
        }
        for ( final Address addr : addrList ) {
          try {
            LOG.info( "Restoring persistent address info for: " + addr );
            Addresses.getInstance( ).lookup( addr.getName( ) );
            addr.init( );
          } catch ( final Exception e ) {
            addr.init( );
          }
        }
      } catch ( final Exception e ) {
        LOG.debug( e, e );
      }
    }
  }
  
}
