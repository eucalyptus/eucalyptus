package com.eucalyptus.sla;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.ClusterNodeState;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.util.FailScriptFailException;
import com.eucalyptus.util.GroovyUtil;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;

public class NodeResourceAllocator implements ResourceAllocator {
  private static String ALLOCATOR = "euca.cluster.allocator";
  
  @Override
  public void allocate( VmAllocationInfo vmInfo ) throws Exception {
    String clusterName = vmInfo.getRequest( ).getAvailabilityZone( );
    if ( clusterName != null && !"default".equals( clusterName ) ) {
      singleCluster( vmInfo );
    } else {
      scriptedAllocator( vmInfo );
    }
  }
  
  private void scriptedAllocator( VmAllocationInfo vmInfo ) throws Exception {
    ResourceAllocator blah = ( ResourceAllocator ) GroovyUtil.newInstance( System.getProperty( ALLOCATOR ) != null ? System.getProperty( ALLOCATOR ) : "LeastFullFirst" );
    blah.allocate( vmInfo );
  }
  
  private void singleCluster( VmAllocationInfo vmInfo ) throws NotEnoughResourcesAvailable {
    RunInstancesType request = vmInfo.getRequest( );
    String clusterName = request.getAvailabilityZone( );
    try {
      Cluster cluster = Clusters.getInstance( ).lookup( clusterName );
      ClusterNodeState clusterState = cluster.getNodeState( );
      int available = clusterState.getAvailability( request.getInstanceType( ) ).getAvailable( );
      if ( available < request.getMinCount( ) ) {
        throw new NotEnoughResourcesAvailable( "Not enough resources: vm resources in the requested cluster " + clusterName );
      }
      int count = available > request.getMaxCount( ) ? request.getMaxCount( ) : available;
      ResourceToken token = clusterState.getResourceAllocation( request.getCorrelationId( ), request.getUserId( ), request.getInstanceType( ), count );
      vmInfo.getAllocationTokens( ).add( token );
    } catch ( NoSuchElementException e ) {
      throw new NotEnoughResourcesAvailable( "Not enough resources: request cluster does not exist " + clusterName );
    }
  }
  
  @Override
  public void fail( VmAllocationInfo vmInfo, Throwable t ) {
    for ( ResourceToken token : vmInfo.getAllocationTokens( ) ) {
      Clusters.getInstance( ).lookup( token.getCluster( ) ).getNodeState( ).releaseToken( token );
    }
  }
  
}
