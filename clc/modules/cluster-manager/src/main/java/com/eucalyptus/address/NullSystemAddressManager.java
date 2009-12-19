package com.eucalyptus.address;

import java.util.List;
import com.eucalyptus.cluster.SuccessCallback;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.cluster.QueuedEventCallback;
import edu.ucsb.eucalyptus.cloud.cluster.UnassignAddressCallback;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.msgs.UnassignAddressResponseType;

public class NullSystemAddressManager extends AbstractSystemAddressManager {
  
  @Override
  public List<Address> allocateSystemAddresses( String cluster, int count ) throws NotEnoughResourcesAvailable {
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
