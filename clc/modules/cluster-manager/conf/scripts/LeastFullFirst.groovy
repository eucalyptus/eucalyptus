package com.eucalyptus.sla;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import com.eucalyptus.util.NotEnoughResourcesAvailable;

public class LeastFullFirst implements ResourceAllocator {
  
  @Override
  public void allocate( VmAllocationInfo vmInfo ) throws Exception {
    def clusterMap = [:]
    Clusters.getInstance().listValues().collect{ Cluster it ->
      clusterMap[it] = it.getNodeState( ).getAvailability( vmInfo.getRequest( ).getInstanceType( ) )
    }
    
    def leastFull = clusterMap.sort{ it.value }.find{ it }
    def amount = [leastFull.value.getAvailable(), vmInfo.request.maxCount].min();
    if( amount < vmInfo.request.minCount ) {
      throw new NotEnoughResourcesAvailable("Not enough resources (${leastFull.value} < ${vmInfo.request.minCount}: vm instances.")  ;
    }
    def allocation = leastFull.key.getNodeState().getResourceAllocation(vmInfo.request.correlationId, vmInfo.request.userId, vmInfo.request.instanceType, amount)
    vmInfo.getAllocationTokens( ).add( allocation );
  }
  
  @Override
  public void fail( VmAllocationInfo vmInfo, Throwable t ) {
    vmInfo.allocationTokens.each  { ResourceToken it ->
      Clusters.getInstance().lookup( it.getCluster( ) ).getNodeState( ).releaseToken( it )
    }
  }
  
}
