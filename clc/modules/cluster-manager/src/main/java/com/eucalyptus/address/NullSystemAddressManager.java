package com.eucalyptus.address;

import java.util.List;
import com.eucalyptus.cloud.util.NotEnoughResourcesException;
import com.eucalyptus.component.Partition;
import com.eucalyptus.vm.VmInstance;
import com.google.common.collect.Lists;

public class NullSystemAddressManager extends AbstractSystemAddressManager {
  
  @Override
  public List<Address> allocateSystemAddresses( Partition partition, int count ) throws NotEnoughResourcesException {
    throw new RuntimeException( "The system is not configured to support public addresses." );
    //TODO: add some output to help figure out why.
  }
  
  @Override
  public void assignSystemAddress( VmInstance vm ) {
    throw new RuntimeException( "The system is not configured to support public addresses." );
    //TODO: add some output to help figure out why.
  }
  
  @Override
  public List<Address> getReservedAddresses( ) {
    return Lists.newArrayList( );
  }
  
  @SuppressWarnings( "unchecked" )
  @Override
  public void inheritReservedAddresses( List<Address> previouslyReservedAddresses ) {
    for( final Address addr : previouslyReservedAddresses ) {
      Addresses.release( addr );
    }
  }

}
