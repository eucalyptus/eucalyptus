package com.eucalyptus.address;

import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.cloud.util.NotEnoughResourcesAvailable;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.async.AsyncRequests;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class DynamicSystemAddressManager extends AbstractSystemAddressManager {
  private static Logger LOG = Logger.getLogger( DynamicSystemAddressManager.class );
  
  @Override
  public List<Address> allocateSystemAddresses( Partition partition, int count ) throws NotEnoughResourcesAvailable {
    if ( Addresses.getInstance( ).listDisabledValues( ).size( ) < count ) {
      throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (try --addressing private)" );
    } else {
      List<Address> addressList = Lists.newArrayList( );
      for ( Address addr : Addresses.getInstance( ).listDisabledValues( ) ) {
        try {
          if ( partition.getName( ).equals( addr.getPartition( ) )
               && addressList.add( addr.pendingAssignment( ) ) 
               && --count == 0 ) {
            break;
          }
        } catch ( IllegalStateException e ) {
          LOG.trace( e, e );
        }
      }
      if ( count != 0 ) {
        for ( Address addr : addressList ) {
          try {
            addr.release( );
          } catch ( IllegalStateException e ) {
            LOG.error( e, e );
          }
        }
        throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (try --addressing private)" );
      }
      return addressList;
    }
  }
  
  @Override
  public void assignSystemAddress( final VmInstance vm ) throws NotEnoughResourcesAvailable {
    final Address addr = this.allocateSystemAddress( vm.getPartition( ) );
    AsyncRequests.newRequest( addr.assign( vm ).getCallback( ) ).then( new Callback.Success<BaseMessage>( ) {
      public void fire( BaseMessage response ) {
        vm.updatePublicAddress( addr.getName( ) );
      }
    } ).dispatch( addr.getPartition( ) );
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
      if ( !addr.isAssigned( ) && !addr.isPending( ) && addr.isSystemOwned( ) && Address.UNASSIGNED_INSTANCEID.equals( addr.getInstanceId( ) ) ) {
        Addresses.release( addr );
      }
    }
  }
  
}
