package com.eucalyptus.address;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.cloud.util.NotEnoughResourcesException;
import com.eucalyptus.component.Partition;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.vm.VmInstance;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class DynamicSystemAddressManager extends AbstractSystemAddressManager {
  private static Logger LOG = Logger.getLogger( DynamicSystemAddressManager.class );
  
  @Override
  public List<Address> allocateSystemAddresses( final Partition partition, int count ) throws NotEnoughResourcesException {
    if ( Addresses.getInstance( ).listDisabledValues( ).size( ) < count ) {
      throw new NotEnoughResourcesException( "Not enough resources available: addresses (try --addressing private)" );
    } else {
      final List<Address> addressList = Lists.newArrayList( );
      for ( final Address addr : Addresses.getInstance( ).listDisabledValues( ) ) {
        try {
          if ( addressList.add( addr.pendingAssignment( ) )
               && ( --count == 0 ) ) {
            break;
          }
        } catch ( final IllegalStateException e ) {
          LOG.trace( e, e );
        }
      }
      if ( count != 0 ) {
        for ( final Address addr : addressList ) {
          try {
            addr.release( );
          } catch ( final IllegalStateException e ) {
            LOG.error( e, e );
          }
        }
        throw new NotEnoughResourcesException( "Not enough resources available: addresses (try --addressing private)" );
      }
      return addressList;
    }
  }
  
  @Override
  public void assignSystemAddress( final VmInstance vm ) throws NotEnoughResourcesException {
    final Address addr = this.allocateSystemAddress( vm.lookupPartition( ) );
    AsyncRequests.newRequest( addr.assign( vm ).getCallback( ) ).then( new Callback.Success<BaseMessage>( ) {
      @Override
      public void fire( final BaseMessage response ) {
        vm.updatePublicAddress( addr.getName( ) );
      }
    } ).dispatch( vm.getPartition( ) );
  }
  
  @Override
  public List<Address> getReservedAddresses( ) {
    return Lists.newArrayList( Iterables.filter( Addresses.getInstance( ).listValues( ), new Predicate<Address>( ) {
      @Override
      public boolean apply( final Address arg0 ) {
        return arg0.isSystemOwned( );
      }
    } ) );
  }
  
  @SuppressWarnings( "unchecked" )
  @Override
  public void inheritReservedAddresses( final List<Address> previouslyReservedAddresses ) {
    for ( final Address addr : previouslyReservedAddresses ) {
      if ( !addr.isAssigned( ) && !addr.isPending( ) && addr.isSystemOwned( ) && Address.UNASSIGNED_INSTANCEID.equals( addr.getInstanceId( ) ) ) {
        Addresses.release( addr );
      }
    }
  }
  
}
