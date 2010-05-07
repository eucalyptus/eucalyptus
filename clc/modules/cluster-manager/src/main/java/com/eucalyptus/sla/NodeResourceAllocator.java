package com.eucalyptus.sla;

import java.util.List;
import java.util.NavigableMap;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import edu.ucsb.eucalyptus.cloud.cluster.VmTypeAvailability;

public class NodeResourceAllocator implements ResourceAllocator {
  private static String ALLOCATOR = "euca.cluster.allocator";
  
  @Override
  public void allocate( VmAllocationInfo vmInfo ) throws Exception {
    String clusterName = vmInfo.getRequest( ).getAvailabilityZone( );
    String vmTypeName = vmInfo.getRequest( ).getVmType( ).getName( );
    Integer amount = vmInfo.getRequest( ).getMinCount( );
    Cluster authorizedCluster = this.doPrivilegedLookup( clusterName, vmTypeName );
    VmTypeAvailability vmAvailability = authorizedCluster.getNodeState( ).getAvailability( vmTypeName );
    if ( vmAvailability.getAvailable( ) < amount ) {
      throw new NotEnoughResourcesAvailable( "Not enough resources (" + vmAvailability.getAvailable( ) + " < " + amount + ": vm instances." );
    }
    Context ctx = Contexts.lookup( );
    ResourceToken token = authorizedCluster.getNodeState( ).getResourceAllocation( ctx.getCorrelationId( ), ctx.getUser( ).getName( ), vmTypeName, amount );
    vmInfo.getAllocationTokens( ).add( token );
  }
  
  private Cluster doPrivilegedLookup( String clusterName, String vmTypeName ) throws NotEnoughResourcesAvailable {
    if ( clusterName != null && !"default".equals( clusterName ) ) {
      final Cluster cluster = Clusters.getInstance( ).lookup( clusterName );
      if ( Iterables.any( Contexts.lookup( ).getAuthorizations( ), new Predicate<Authorization>( ) {
        @Override
        public boolean apply( Authorization arg0 ) {
          return arg0.check( cluster );
        }
      } ) ) {
        return cluster;
      } else {
        throw new NotEnoughResourcesAvailable( "Not enough resources: request cluster does not exist " + clusterName );
      }
    } else {
//      Iterable<Cluster> authorizedClusters = Iterables.filter( Clusters.getInstance( ).listValues( ), new Predicate<Cluster>( ) {
//        @Override
//        public boolean apply( final Cluster c ) {
//          return Iterables.any( Contexts.lookup( ).getAuthorizations( ), new Predicate<Authorization>( ) {
//            @Override
//            public boolean apply( Authorization arg0 ) {
//              return arg0.check( c );
//            }
//          } );
//        }
//      } );
      List<Cluster> authorizedClusters = Lists.newArrayList( Clusters.getInstance( ).listValues( ) );
      NavigableMap<VmTypeAvailability, Cluster> sorted = Maps.newTreeMap( );
      for ( Cluster c : authorizedClusters ) {
        sorted.put( c.getNodeState( ).getAvailability( vmTypeName ), c );
      }
      return sorted.firstEntry( ).getValue( );
    }
  }
  
  @Override
  public void fail( VmAllocationInfo vmInfo, Throwable t ) {
    for ( ResourceToken token : vmInfo.getAllocationTokens( ) ) {
      Clusters.getInstance( ).lookup( token.getCluster( ) ).getNodeState( ).releaseToken( token );
    }
  }
  
}
