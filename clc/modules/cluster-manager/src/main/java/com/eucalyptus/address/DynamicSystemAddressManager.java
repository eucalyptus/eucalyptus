package com.eucalyptus.address;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class DynamicSystemAddressManager extends AbstractSystemAddressManager {
  private static Logger LOG = Logger.getLogger( DynamicSystemAddressManager.class );
  
  @Override
  public List<Address> allocateSystemAddresses( String cluster, int count ) throws NotEnoughResourcesAvailable {
    List<Address> addressList = Lists.newArrayList( );
    if ( Addresses.getInstance( ).listDisabledValues( ).size( ) < count ) throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (try --addressing private)" );
    for ( Address addr : Addresses.getInstance( ).listDisabledValues( ) ) {
      try {
        if ( cluster.equals( addr.getCluster( ) ) && addressList.add( addr.pendingAssignment( ) ) && --count == 0 ) break;
      } catch ( IllegalStateException e ) {
        LOG.trace( e , e );
      }
    }
    if ( count != 0 ) {
      for( Address addr : addressList ) {
        try {
          addr.release( );
        } catch ( IllegalStateException e ) {
          LOG.error( e , e );
        }
      }
      throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (try --addressing private)" );
    } 
    return addressList;
  }
  @Override
  public void assignSystemAddress( VmInstance vm ) throws NotEnoughResourcesAvailable {
    Address addr = this.allocateSystemAddresses( vm.getPlacement( ), 1 ).get( 0 );
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
      if( !addr.isAssigned( ) && !addr.isPending() && addr.isSystemOwned() && Address.UNASSIGNED_INSTANCEID.equals( addr.getInstanceId() ) ) {
        Addresses.release( addr );
      }
    }
  }
  
}
