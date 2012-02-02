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
import edu.ucsb.eucalyptus.cloud.exceptions.ExceptionList;
import edu.ucsb.eucalyptus.msgs.ClusterAddressInfo;

public abstract class AbstractSystemAddressManager {
  private final static Logger                                              LOG     = Logger.getLogger( AbstractSystemAddressManager.class );
  private static final ConcurrentNavigableMap<ClusterAddressInfo, Integer> orphans = new ConcurrentSkipListMap<ClusterAddressInfo, Integer>( );
  
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
            AsyncRequests.newRequest( new UnassignAddressCallback( address ) ).sendSync( cluster.getConfiguration( ) );
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
		  throw new NotEnoughResourcesException( ExceptionList.ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY );
	  }	    

	  Predicate<Address> predicate = RestrictedTypes.filterPrivileged( );    
	  final Address addr = Addresses.getInstance( ).enableFirst( predicate ).allocate( userId );   

	  LOG.debug( "Allocated address for public addressing: " + addr.toString( ) );
	  if ( addr == null ) {
		  LOG.debug( LogUtil.header( Addresses.getInstance( ).toString( ) ) );
		  throw new NotEnoughResourcesException( ExceptionList.ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY );
	  }
	  return addr;
  }
  
  public abstract void assignSystemAddress( final VmInstance vm ) throws NotEnoughResourcesException;
  
  public abstract List<Address> getReservedAddresses( );
  
  public abstract void inheritReservedAddresses( List<Address> previouslyReservedAddresses );
  
  public abstract List<Address> allocateSystemAddresses( Partition partition, int count ) throws NotEnoughResourcesException;
  
  public Address allocateSystemAddress( final Partition partition ) throws NotEnoughResourcesException {
    return this.allocateSystemAddresses( partition, 1 ).get( 0 );
    
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
          addr = new Address( Principals.systemFullName( ), addrInfo.getAddress( ), vm.getInstanceId( ), vm.getPrivateAddress( ) );
          clearOrphan( addrInfo );
        } else if ( ( addr == null ) && ( vm == null ) ) {
          addr = new Address( addrInfo.getAddress( ), cluster.getPartition( ) );
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
          addr = new Address( addrInfo.getAddress( ), cluster.getPartition( ) );
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
        vm.updatePublicAddress( vm.getPrivateAddress( ) );
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
    }
    
    protected static void loadStoredAddresses( ) {
      final Address clusterAddr = new Address( );
      final EntityTransaction db = Entities.get( Address.class );
      try {
        for ( Address addr : Entities.query( clusterAddr ) ) {
          if ( !Addresses.getInstance( ).contains( addr.getName( ) ) ) {
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
