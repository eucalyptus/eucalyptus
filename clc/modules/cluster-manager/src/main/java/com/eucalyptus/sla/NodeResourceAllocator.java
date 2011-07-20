package com.eucalyptus.sla;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.ClusterNodeState;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.VmTypeAvailability;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.images.BlockStorageImageInfo;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;

public class NodeResourceAllocator implements ResourceAllocator {
  private static Logger LOG       = Logger.getLogger( NodeResourceAllocator.class );
  private static String ALLOCATOR = "euca.cluster.allocator";
  
  @Override
  public void allocate( Allocation allocInfo ) throws Exception {
    RunInstancesType request = allocInfo.getRequest( );
    String clusterName = request.getAvailabilityZone( );
    String vmTypeName = request.getInstanceType( );
    final int minAmount = request.getMinCount( );
    final int maxAmount = request.getMaxCount( );
    Context ctx = Contexts.lookup( );
    //if ( ctx.getGroups( ).isEmpty( ) ) {
    if ( false ) {
      throw new NotEnoughResourcesAvailable( "Not authorized: you do not have sufficient permission to use " + clusterName );
    } else {
      String zoneName = ( clusterName != null )
        ? clusterName
        : "default";
      String action = PolicySpec.requestToAction( request );
      User requestUser = ctx.getUser( );
      List<Cluster> authorizedClusters = this.doPrivilegedLookup( zoneName, vmTypeName, action, requestUser );
      int remaining = maxAmount;
      int available = 0;
      LOG.info( "Found authorized clusters: " + Iterables.transform( authorizedClusters, new Function<Cluster, String>( ) {
        @Override
        public String apply( Cluster arg0 ) {
          return arg0.getName( );
        }
      } ) );
      if ( ( available = checkAvailability( vmTypeName, authorizedClusters ) ) < minAmount ) {
        throw new NotEnoughResourcesAvailable( "Not enough resources (" + available + " in " + zoneName + " < " + minAmount + "): vm instances." );
      } else {
        for ( Cluster cluster : authorizedClusters ) {
          if( remaining <= 0 ) {
            break;
          } else {
            ClusterNodeState state = cluster.getNodeState( );
            Partition partition = cluster.getConfiguration( ).lookupPartition( );
            if ( allocInfo.getBootSet( ).getMachine( ) instanceof BlockStorageImageInfo ) {
              try {
                ServiceConfiguration sc = Partitions.lookupService( Storage.class, partition );
              } catch ( Exception ex ) {
                throw new NotEnoughResourcesAvailable( "Not enough resources: " + ex.getMessage( ), ex );
              }
            }
            try {
              int tryAmount = ( remaining > state.getAvailability( vmTypeName ).getAvailable( ) )
                ? state.getAvailability( vmTypeName ).getAvailable( )
                : remaining;
              
              ResourceToken token = allocInfo.requestResourceToken( state, vmTypeName, tryAmount, maxAmount );
              remaining -= token.getAmount( );
            } catch ( Throwable t ) {
              if ( ( ( available = checkAvailability( vmTypeName, authorizedClusters ) ) < remaining ) || remaining > 0 ) {
                allocInfo.releaseAllocationTokens( );
                throw new NotEnoughResourcesAvailable( "Not enough resources (" + available + " in " + zoneName + " < " + minAmount + "): vm instances." );
              } else {
                LOG.error( t, t );
                throw new NotEnoughResourcesAvailable( "Not enough resources (" + available + " in " + zoneName + " < " + minAmount + "): vm instances." );
              }
            }
          }
        }
      }
    }
  }
  
  private int checkAvailability( String vmTypeName, List<Cluster> authorizedClusters ) throws NotEnoughResourcesAvailable {
    int available = 0;
    for ( Cluster authorizedCluster : authorizedClusters ) {
      VmTypeAvailability vmAvailability = authorizedCluster.getNodeState( ).getAvailability( vmTypeName );
      available += vmAvailability.getAvailable( );
      LOG.info( "Availability: " + authorizedCluster.getName( ) + " -> " + vmAvailability.getAvailable( ) );
    }
    return available;
  }
  
  private List<Cluster> doPrivilegedLookup( String clusterName, String vmTypeName, final String action, final User requestUser ) throws NotEnoughResourcesAvailable {
    if ( "default".equals( clusterName ) ) {
      Iterable<Cluster> authorizedClusters = Iterables.filter( Clusters.getInstance( ).listValues( ), new Predicate<Cluster>( ) {
        @Override
        public boolean apply( final Cluster c ) {
          try {
            return Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_AVAILABILITYZONE, c.getName( ), null, action, requestUser );
          } catch ( Exception e ) {}
          return false;
        }
      } );
      Multimap<VmTypeAvailability, Cluster> sorted = TreeMultimap.create( );
      for ( Cluster c : authorizedClusters ) {
        sorted.put( c.getNodeState( ).getAvailability( vmTypeName ), c );
      }
      if ( sorted.isEmpty( ) ) {
        throw new NotEnoughResourcesAvailable( "Not enough resources: no availability zone is available in which you have permissions to run instances." );
      } else {
        return Lists.newArrayList( sorted.values( ) );
      }
    } else {
      Cluster cluster = Clusters.getInstance( ).lookup( Partitions.lookupService( ClusterController.class, clusterName ) );
      if ( cluster == null ) {
        throw new NotEnoughResourcesAvailable( "Can't find cluster " + clusterName );
      }
      if ( !Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_AVAILABILITYZONE, clusterName, null, action, requestUser ) ) {
        throw new NotEnoughResourcesAvailable( "Not authorized to use cluster " + clusterName + " for " + requestUser.getName( ) );
      }
      return Lists.newArrayList( cluster );
    }
  }
  
  @Override
  public void fail( Allocation allocInfo, Throwable t ) {
    allocInfo.releaseAllocationTokens( );
  }
  
}
