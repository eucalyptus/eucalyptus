package com.eucalyptus.sla;

import java.util.NavigableMap;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.AvailabilityZonePermission;
import com.eucalyptus.auth.principal.Group;
import java.util.NoSuchElementException;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.ClusterNodeState;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.VmTypeAvailability;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;

public class NodeResourceAllocator implements ResourceAllocator {
  private static Logger LOG       = Logger.getLogger( NodeResourceAllocator.class );
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
    if ( ctx.getGroups( ).isEmpty( ) ) {
      throw new NotEnoughResourcesAvailable( "Not authorized: you do not have sufficient permission to use " + clusterName );
    } else {
      String zoneName = ( clusterName != null )
        ? clusterName
        : "default";
      List<Cluster> authorizedClusters = this.doPrivilegedLookup( zoneName, vmTypeName );
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
        List<ResourceToken> tokens = Lists.newArrayList( );
        for ( ClusterNodeState state : Lists.transform( authorizedClusters, new Function<Cluster, ClusterNodeState>( ) {
          @Override
          public ClusterNodeState apply( Cluster arg0 ) {
            return arg0.getNodeState( );
          }
        } ) ) {
          try {
            int tryAmount = ( remaining > state.getAvailability( vmTypeName ).getAvailable( ) )
              ? state.getAvailability( vmTypeName ).getAvailable( )
              : remaining;
            ResourceToken token = state.getResourceAllocation( ctx.getCorrelationId( ), ctx.getUser( ).getName( ), vmTypeName, tryAmount, maxAmount );
            remaining -= token.getAmount( );
            tokens.add( token );
          } catch ( Throwable t ) {
            if ( ( ( available = checkAvailability( vmTypeName, authorizedClusters ) ) < remaining ) || remaining > 0 ) {
              for ( ResourceToken token : tokens ) {
                Clusters.getInstance( ).lookup( token.getCluster( ) ).getNodeState( ).releaseToken( token );
              }
              throw new NotEnoughResourcesAvailable( "Not enough resources (" + available + " in " + zoneName + " < " + minAmount + "): vm instances." );
            } else {
              LOG.error( t, t );
              throw new NotEnoughResourcesAvailable( "Not enough resources (" + available + " in " + zoneName + " < " + minAmount + "): vm instances." );
            }
          }
        }
        vmInfo.getAllocationTokens( ).addAll( tokens );
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
  
  private List<Cluster> doPrivilegedLookup( String clusterName, String vmTypeName ) throws NotEnoughResourcesAvailable {
    if ( "default".equals( clusterName ) ) {
      Group group = Contexts.lookup( ).getGroups( ).get( 0 );
      Iterable<Cluster> authorizedClusters = Iterables.filter( Clusters.getInstance( ).listValues( ), new Predicate<Cluster>( ) {
        @Override
        public boolean apply( final Cluster c ) {
          return Iterables.any( Contexts.lookup( ).getAuthorizations( ), new Predicate<Authorization>( ) {
            @Override
            public boolean apply( Authorization arg0 ) {
              return arg0.check( new AvailabilityZonePermission( c.getName( ) ) );
            }
          } );
        }
      } );
      Multimap<VmTypeAvailability, Cluster> sorted = Multimaps.newTreeMultimap( );
      for ( Cluster c : authorizedClusters ) {
        sorted.put( c.getNodeState( ).getAvailability( vmTypeName ), c );
      }
      if ( sorted.isEmpty( ) ) {
        throw new NotEnoughResourcesAvailable( "Not enough resources: no cluster is available on which you have permissions to run instances." );
      } else {
        return Lists.newArrayList( sorted.values( ) );
      }
    } else {
      final Cluster cluster = Clusters.getInstance( ).lookup( clusterName );
      if ( Iterables.any( Contexts.lookup( ).getAuthorizations( ), new Predicate<Authorization>( ) {
        @Override
        public boolean apply( Authorization arg0 ) {
          return arg0.check( new AvailabilityZonePermission( cluster.getName( ) ) );
        }
      } ) ) {
        return Lists.newArrayList( cluster );
      } else {
        if ( Clusters.getInstance( ).contains( clusterName ) ) {
          throw new NotEnoughResourcesAvailable( "Not authorized: you do not have sufficient permission to use " + clusterName );
        } else {
          throw new NotEnoughResourcesAvailable( "Not enough resources: request cluster does not exist " + clusterName );
        }
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
