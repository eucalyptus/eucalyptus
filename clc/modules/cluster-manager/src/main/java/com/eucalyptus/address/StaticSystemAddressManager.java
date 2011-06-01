package com.eucalyptus.address;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.FakePrincipals;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.Callback;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class StaticSystemAddressManager extends AbstractSystemAddressManager {
  private static Logger LOG = Logger.getLogger( StaticSystemAddressManager.class );
  
  public StaticSystemAddressManager( ) {
    this.inheritReservedAddresses( new ArrayList<Address>() );
  }

  public List<Address> allocateSystemAddresses( String cluster, int count ) throws NotEnoughResourcesAvailable {
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
      throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (try --addressing private)" );
    }
    return addressList;
  }
  
  @Override
  public void assignSystemAddress( final VmInstance vm ) throws NotEnoughResourcesAvailable {
    final Address addr = this.allocateSystemAddresses( vm.getClusterName( ), 1 ).get( 0 );
    AsyncRequests.newRequest( addr.assign( vm ).getCallback( ) ).then( new Callback.Success<BaseMessage>() {
      public void fire( BaseMessage response ) {
        vm.updatePublicAddress( addr.getName( ) );
      }
    }).dispatch( addr.getCluster( ) );
  }
  
  private Address getNext() throws NotEnoughResourcesAvailable {
    for( Address a : Addresses.getInstance( ).listValues( ) ) {
      if( a.isSystemOwned( ) && !a.isAssigned( ) ) {
        return a.pendingAssignment( );
      }
    }
    throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (try --addressing private)" );
  }
  
  @Override
  public List<Address> getReservedAddresses( ) {
    return Lists.newArrayList( Iterables.filter( Addresses.getInstance( ).listValues( ), new Predicate<Address>() {
      @Override
      public boolean apply( Address arg0 ) {
        return arg0.isSystemOwned( );
      }
    }));
  }
  
  @Override
  public void inheritReservedAddresses( List<Address> reservedAddresses ) {
    int allocCount = Addresses.getSystemReservedAddressCount( ) - reservedAddresses.size( );
    LOG.debug( "Allocating additional " + allocCount + " addresses in static public addresing mode" );
    allocCount = Addresses.getInstance( ).listDisabledValues( ).size( ) < allocCount ? Addresses.getInstance( ).listDisabledValues( ).size( ) : allocCount;
    if ( allocCount > 0 ) {
      for ( int i = 0; i < allocCount; i++ ) {
        try {
          this.allocateNext( FakePrincipals.SYSTEM_USER_ERN );
        } catch ( NotEnoughResourcesAvailable e ) {
          break;
        }
      }
    } else {
      for ( Address addr : Addresses.getInstance( ).listValues( ) ) {
        if ( addr.getOwner( ).equals( FakePrincipals.SYSTEM_USER_ERN ) && !addr.isAssigned( ) && !addr.isPending( ) ) {
          addr.release( );
          if ( allocCount++ >= 0 ) break;
        }
      }
    }
  }

}
