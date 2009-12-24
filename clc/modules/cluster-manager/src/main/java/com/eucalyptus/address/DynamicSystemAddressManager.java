package com.eucalyptus.address;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.SuccessCallback;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.cluster.AssignAddressCallback;
import edu.ucsb.eucalyptus.cloud.cluster.QueuedEventCallback;
import edu.ucsb.eucalyptus.cloud.cluster.UnassignAddressCallback;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.msgs.UnassignAddressResponseType;

public class DynamicSystemAddressManager extends AbstractSystemAddressManager {
  private static Logger LOG = Logger.getLogger( DynamicSystemAddressManager.class );
  
  @Override
  public List<Address> allocateSystemAddresses( String cluster, int count ) throws NotEnoughResourcesAvailable {
    List<Address> addressList = Lists.newArrayList( );
    if ( Addresses.getInstance( ).listDisabledValues( ).size( ) < count ) throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (try --addressing private)" );
    for ( Address addr : Addresses.getInstance( ).listDisabledValues( ) ) {
      if ( cluster.equals( addr.getCluster( ) ) ) {
        addressList.add( addr.allocate( Component.eucalyptus.name( ) ) );
        addr.pendingAssignment( );
        if ( --count == 0 ) {
          break;
        }
      }
    }
    if ( count != 0 ) {
      for( Address addr : addressList ) {
        addr.release( );
      }
      throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (try --addressing private)" );
    } 
    return addressList;
  }
  @Override
  public void assignSystemAddress( VmInstance vm ) throws NotEnoughResourcesAvailable {
    Address addr = this.allocateNext( Component.eucalyptus.name( ) );
    AddressCategory.assign( addr, vm ).dispatch( addr.getCluster( ) );
  }
    
  @Override
  public List<Address> getReservedAddresses( ) {
    return Lists.newArrayList( Iterables.filter( Addresses.getInstance( ).listValues( ), new Predicate<Address>( ) {
      @Override
      public boolean apply( Address arg0 ) {
        return arg0.isSystemOwned( );
      }
    } ) );
  }
  
  @SuppressWarnings( "unchecked" )
  @Override
  public void inheritReservedAddresses( List<Address> previouslyReservedAddresses ) {
    for ( final Address addr : previouslyReservedAddresses ) {
      if( !addr.isAssigned( ) ) {
        Addresses.release( addr );
      }
    }
  }
  @Override public void releaseSystemAddress( Address addr ) {
    try {
      addr.release( );
    } catch ( Throwable e ) {
      LOG.debug( e, e );
    }
  }
  
}
