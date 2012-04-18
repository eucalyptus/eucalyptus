package com.eucalyptus.address;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.cloud.util.NotEnoughResourcesException;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.vm.VmInstance;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class StaticSystemAddressManager extends AbstractSystemAddressManager {
  private static Logger LOG = Logger.getLogger( StaticSystemAddressManager.class );
  
  public StaticSystemAddressManager( ) {
    this.inheritReservedAddresses( new ArrayList<Address>( ) );
  }
  
  public List<Address> allocateSystemAddresses( Partition partition, int count ) throws NotEnoughResourcesException {
    List<Address> addressList = Lists.newArrayList( );
    for ( Address addr : Addresses.getInstance( ).listValues( ) ) {
      if ( addr.isSystemOwned( ) && !addr.isAssigned( ) ) {
        addr.pendingAssignment( );
        addressList.add( addr );
        if ( addressList.size( ) == count ) {
          break;
        }
      }
    }
    if ( addressList.size( ) < count ) {
      for ( Address putBackAddr : addressList ) {
        putBackAddr.clearPending( );
      }
      throw new NotEnoughResourcesException( "Not enough resources available: addresses (try --addressing private)" );
    }
    return addressList;
  }
  
  @Override
  public void assignSystemAddress( final VmInstance vm ) throws NotEnoughResourcesException {
    final Address addr = this.allocateSystemAddress( vm.lookupPartition( ) );
    AsyncRequests.newRequest( addr.assign( vm ).getCallback( ) ).then( new Callback.Success<BaseMessage>( ) {
      public void fire( BaseMessage response ) {
        vm.updatePublicAddress( addr.getName( ) );
      }
    } ).dispatch( vm.getPartition( ) );
  }
  
  private Address getNext( ) throws NotEnoughResourcesException {
    for ( Address a : Addresses.getInstance( ).listValues( ) ) {
      if ( a.isSystemOwned( ) && !a.isAssigned( ) ) {
        return a.pendingAssignment( );
      }
    }
    throw new NotEnoughResourcesException( "Not enough resources available: addresses (try --addressing private)" );
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
  
  @Override
  public void inheritReservedAddresses( List<Address> reservedAddresses ) {
    int allocCount = Addresses.getSystemReservedAddressCount( ) - reservedAddresses.size( );
    LOG.debug( "Allocating additional " + allocCount + " addresses in static public addresing mode" );
    allocCount = Addresses.getInstance( ).listDisabledValues( ).size( ) < allocCount
      ? Addresses.getInstance( ).listDisabledValues( ).size( )
      : allocCount;
    if ( allocCount > 0 ) {
      for ( int i = 0; i < allocCount; i++ ) {
        try {
          this.allocateNext( Principals.systemFullName( ) );
        } catch ( NotEnoughResourcesException e ) {
          break;
        }
      }
    } else {
      for ( Address addr : Addresses.getInstance( ).listValues( ) ) {
        if ( addr.getOwner( ).equals( Principals.systemFullName( ) ) && !addr.isAssigned( ) && !addr.isPending( ) ) {
          addr.release( );
          if ( allocCount++ >= 0 ) break;
        }
      }
    }
  }
  
}
