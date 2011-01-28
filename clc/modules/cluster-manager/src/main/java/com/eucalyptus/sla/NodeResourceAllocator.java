package com.eucalyptus.sla;

import java.util.NavigableMap;
import java.util.NoSuchElementException;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.VmTypeAvailability;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;

public class NodeResourceAllocator implements ResourceAllocator {
  private static String ALLOCATOR = "euca.cluster.allocator";
  
  @Override
  public void allocate( VmAllocationInfo vmInfo ) throws Exception {
    String clusterName = vmInfo.getRequest( ).getAvailabilityZone( );
    String vmTypeName = vmInfo.getRequest( ).getInstanceType( );
    Integer minAmount = vmInfo.getRequest( ).getMinCount( );
    Integer maxAmount = vmInfo.getRequest( ).getMaxCount( );
    Cluster authorizedCluster = this.doPrivilegedLookup( clusterName, vmTypeName );
    VmTypeAvailability vmAvailability = authorizedCluster.getNodeState( ).getAvailability( vmTypeName );
    Context ctx = Contexts.lookup( );
    ResourceToken token = authorizedCluster.getNodeState( ).getResourceAllocation( ctx.getCorrelationId( ), ctx.getUser( ).getName( ), vmTypeName, minAmount, maxAmount );
    vmInfo.getAllocationTokens( ).add( token );
  }
  
  private Cluster doPrivilegedLookup( String clusterName, String vmTypeName ) throws NotEnoughResourcesAvailable {
    if ( clusterName != null && !"default".equals( clusterName ) ) {
      try {
        final Cluster cluster = Clusters.getInstance( ).lookup( clusterName );
        return cluster;
      } catch ( NoSuchElementException e ) {
        throw new NotEnoughResourcesAvailable( "Not enough resources: request cluster does not exist " + clusterName );
      }
    } else {
      Iterable<Cluster> authorizedClusters = Clusters.getInstance( ).listValues( );
      NavigableMap<VmTypeAvailability, Cluster> sorted = Maps.newTreeMap( );
      for ( Cluster c : authorizedClusters ) {
        sorted.put( c.getNodeState( ).getAvailability( vmTypeName ), c );
      }
      if( sorted.isEmpty( ) ) {
        throw new NotEnoughResourcesAvailable( "Not enough resources: no cluster is available on which you have permissions to run instances." );
      } else {
        return sorted.firstEntry( ).getValue( );
      }
    }
  }
  
  @Override
  public void fail( VmAllocationInfo vmInfo, Throwable t ) {
    for ( ResourceToken token : vmInfo.getAllocationTokens( ) ) {
      Clusters.getInstance( ).lookup( token.getCluster( ) ).getNodeState( ).releaseToken( token );
    }
  }
  
}
