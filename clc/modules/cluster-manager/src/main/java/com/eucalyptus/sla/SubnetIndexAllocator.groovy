package com.eucalyptus.sla;

import org.apache.log4j.Logger;
import com.eucalyptus.cluster.ClusterState;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import edu.ucsb.eucalyptus.cloud.Network;
import com.eucalyptus.cluster.Networks;
import edu.ucsb.eucalyptus.cloud.NetworkToken;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;

public class SubnetIndexAllocator implements ResourceAllocator {
  private static Logger LOG = Logger.getLogger( SubnetIndexAllocator.class );
  @Override
  public void allocate( VmAllocationInfo vmInfo ) throws Exception {
    Network firstNet = Networks.getInstance().lookup(vmInfo.getNetworks( ).first().getName());
    vmInfo.allocationTokens.each { ResourceToken it ->
      it.amount.times{ i -> 
        Integer addrIndex = firstNet.allocateNetworkIndex( it.getCluster( ) );
        if ( addrIndex == null ) throw new NotEnoughResourcesAvailable( "Not enough addresses left in the network subnet assigned to requested group: " + firstNet.getNetworkName( ) );
        it.getPrimaryNetwork( ).indexes += addrIndex;
      }
    }
  }
  
  @Override
  public void fail( VmAllocationInfo vmInfo, Throwable t ) {
    Network firstNet = Networks.getInstance().lookup(vmInfo.getNetworks().first().getName());
    vmInfo.allocationTokens.each { ResourceToken it ->
      it.getPrimaryNetwork()?.getIndexes( ).each{ Integer i ->
        firstNet?.returnNetworkIndex( i );
      }
      it.getPrimaryNetwork()?.getIndexes( ).clear( );
    }
  }
  
  
}
