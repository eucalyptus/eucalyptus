/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.cluster;

import static com.eucalyptus.auth.policy.PolicySpec.*;
import static com.eucalyptus.compute.common.internal.vm.VmInstances.TerminatedInstanceException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.Regions;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurations;
import com.eucalyptus.cluster.common.ClusterController;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.ClientComputeException;
import com.eucalyptus.compute.ComputeException;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.ImageMetadata.Platform;
import com.eucalyptus.cluster.common.ResourceState.VmTypeAvailability;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.RegionInfoType;
import com.eucalyptus.compute.common.backend.DescribeAvailabilityZonesResponseType;
import com.eucalyptus.compute.common.backend.DescribeAvailabilityZonesType;
import com.eucalyptus.compute.common.backend.DescribeRegionsResponseType;
import com.eucalyptus.compute.common.backend.DescribeRegionsType;
import com.eucalyptus.compute.common.backend.MigrateInstancesResponseType;
import com.eucalyptus.compute.common.backend.MigrateInstancesType;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.compute.common.internal.tags.FilterSupport;
import com.eucalyptus.compute.common.internal.tags.Filters;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.NonNullFunction;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.compute.common.internal.vmtypes.VmType;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.eucalyptus.cluster.common.msgs.ClusterGetConsoleOutputResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterGetConsoleOutputType;

@ComponentNamed("computeClusterEndpoint")
public class ClusterEndpoint {
  
  private static Logger                                LOG              = Logger.getLogger( ClusterEndpoint.class );
  
  private Map<String, NonNullFunction<Predicate<Object>,List<ClusterInfoType>>> describeKeywords =
                                                                        ImmutableMap.<String, NonNullFunction<Predicate<Object>,List<ClusterInfoType>>>of(
                                                                          "verbose", new NonNullFunction<Predicate<Object>,List<ClusterInfoType>>( ) {
                                                                              @Nonnull
                                                                              @Override
                                                                              public List<ClusterInfoType> apply( final Predicate<Object> filterPredicate ) {
                                                                                List<ClusterInfoType> verbose = Lists.newArrayList( );
                                                                                for ( Cluster c : Iterables.filter( Clusters.list( ), filterPredicate ) ) {
                                                                                  verbose.addAll( describeSystemInfo.apply( c ) );
                                                                                }
                                                                                return verbose;
                                                                              }
                                                                            } );
  
  public MigrateInstancesResponseType migrateInstances( final MigrateInstancesType request ) throws EucalyptusCloudException {
    final MigrateInstancesResponseType reply = request.getReply( );
    final Context context = Contexts.lookup( );
    if ( !context.isAdministrator( ) || !Permissions.isAuthorized(
        VENDOR_EC2,
        EC2_RESOURCE_INSTANCE,
        "",
        null,
        EC2_MIGRATEINSTANCES,
        context.getAuthContext() )  ) {
      throw new EucalyptusCloudException( "Authorization failed." );
    }
    if ( !Strings.isNullOrEmpty( request.getSourceHost( ) ) ) {
      final Predicate<VmInstance> filterHost = new Predicate<VmInstance>( ) {
        @Override
        public boolean apply( VmInstance input ) {
          String vmHost = URI.create( input.getServiceTag( ) ).getHost( );
          return Strings.nullToEmpty( vmHost ).equals( request.getSourceHost( ) );
        }
      };
      for ( ServiceConfiguration ccConfig : Topology.enabledServices( ClusterController.class ) ) {
        try {
          Cluster cluster = Clusters.lookupAny( ccConfig );//lookup the cluster
          if ( !cluster.hasNode( request.getSourceHost( ) ) ) {
            continue;
          }
          final List<VmInstance> instances = VmInstances.list(filterHost);
          for(final VmInstance instance : instances){
            try{
              updatePasswordIfWindows(instance, ccConfig);
            }catch(final Exception ex){
              ;
            }
          }
          
          try {
            Migrations.using( cluster ).migrateInstances( request.getSourceHost( ), request.getAllowHosts( ), request.getDestinationHosts( ) );//submit the migration request
            return reply.markWinning( );
          } catch ( Exception ex ) {
            LOG.error( ex );
            throw new EucalyptusCloudException( "Migrating off of node "
                                                + request.getSourceHost( )
                                                + " failed because of: "
                                                + Strings.nullToEmpty( ex.getMessage( ) ).replaceAll( ".*:status=", "" ), ex );
          }
        } catch ( EucalyptusCloudException ex ) {
          throw ex;
        } catch ( NoSuchElementException ex ) {
          // Ignore and continue
        } catch ( Exception ex ) {
          LOG.error( ex );
          throw new EucalyptusCloudException( "Migrating off of node " + request.getSourceHost( ) + " failed because of: " + ex.getMessage( ), ex );
        }
      }
      throw new EucalyptusCloudException( "No ENABLED cluster found which can service the requested node: " + request.getSourceHost( ) );
    } else if ( !Strings.isNullOrEmpty( request.getInstanceId( ) ) ) {
      final VmInstance vm;
      try {
        vm = VmInstances.lookup( request.getInstanceId( ) );
        if ( !VmInstance.VmState.RUNNING.apply( vm ) ) {
          throw new EucalyptusCloudException( "Cannot migrate a " + vm.getState( ).name( ).toLowerCase( ) + " instance: " + request.getInstanceId( ) );
        }
      } catch ( TerminatedInstanceException ex ) {
        throw new EucalyptusCloudException( "Cannot migrate a terminated instance: " + request.getInstanceId( ), ex );
      } catch ( NoSuchElementException ex ) {
        throw new EucalyptusCloudException( "Failed to lookup requested instance: " + request.getInstanceId( ), ex );
      }
      try {
        ServiceConfiguration ccConfig = Topology.lookup( ClusterController.class, vm.lookupPartition( ) );
        Cluster cluster = Clusters.lookupAny( ccConfig );
        // update windows password
        try{
          updatePasswordIfWindows(vm, ccConfig);
        }catch(final Exception ex){
        }
        try {
          Migrations.using( cluster ).migrateInstance( request.getInstanceId( ), request.getAllowHosts( ), request.getDestinationHosts( ) );
          return reply.markWinning( );
        } catch ( Exception ex ) {
          LOG.error( ex );
          throw new EucalyptusCloudException( "Migrating instance "
                                              + request.getInstanceId( )
                                              + " failed because of: "
                                              + Strings.nullToEmpty( ex.getMessage( ) ).replaceAll( ".*:status=", "" ), ex );
        }
      } catch ( NoSuchElementException ex ) {
        throw new EucalyptusCloudException( "Failed to lookup ENABLED cluster for instance " + request.getInstanceId( ), ex );
      }
    } else {
      throw new EucalyptusCloudException( "Either the sourceHost or instanceId must be provided" );
    }
  }

  private void updatePasswordIfWindows(final VmInstance vm, final ServiceConfiguration ccConfig) throws Exception{
    if ( Platform.windows.name().equals(vm.getPlatform()) && (vm.getPasswordData( ) == null || vm.getPasswordData().length()<=0) ) {
      try {
        final ClusterGetConsoleOutputResponseType consoleOutput =
            AsyncRequests.sendSync( ccConfig, new ClusterGetConsoleOutputType( vm.getInstanceId() ) );
        final String tempCo = B64.standard.decString( String.valueOf( consoleOutput.getOutput( ) ) ).replaceAll( "[\r\n]*", "" );
        final String passwordData = tempCo.replaceAll( ".*<Password>", "" ).replaceAll( "</Password>.*", "" );
        if ( tempCo.matches( ".*<Password>[\\w=+/]*</Password>.*" ) ) {
          Entities.asTransaction( VmInstance.class, new Predicate<String>() {
            @Override
            public boolean apply( final String passwordData ) {
              final VmInstance vmMerge = Entities.merge( vm );
              vmMerge.updatePasswordData( passwordData );
              return true;
            }
          } ).apply( passwordData );
          vm.updatePasswordData( passwordData );
        }
      } catch ( Exception e ) {
        throw new ComputeException( "InternalError", "Error processing request: " + e.getMessage( ) );
      }
    }
  }
  
  public DescribeAvailabilityZonesResponseType DescribeAvailabilityZones( DescribeAvailabilityZonesType request ) throws EucalyptusCloudException {
    final DescribeAvailabilityZonesResponseType reply = request.getReply( );
    final List<String> args = request.getAvailabilityZoneSet( );
    final Predicate<Object> filterPredicate = Filters.generateFor( request.getFilterSet(), Cluster.class )
        .withOptionalInternalFilter(
            "zone-name",
            Iterables.filter( args, Predicates.not( Predicates.in( describeKeywords.keySet( ) ) ) ) )
        .generate( )
        .asPredicate( );

    final boolean admin = Contexts.lookup( ).hasAdministrativePrivileges( );
    for ( String keyword : describeKeywords.keySet( ) ) {
      if ( args.remove( keyword ) && admin ) {
        reply.getAvailabilityZoneInfo( ).addAll( describeKeywords.get( keyword ).apply( filterPredicate ) );
        return reply;
      }
    }

    final List<Cluster> clusters;
    if ( args.isEmpty( ) ) {
      clusters = Clusters.list( );
      Iterables.addAll( clusters, Iterables.filter(
          Clusters.listDisabled( ),
          Predicates.not(
              CollectionUtils.propertyPredicate(
                  Collections2.transform( clusters, CloudMetadatas.toDisplayName() ),
                  CloudMetadatas.toDisplayName() ) ) ) );
    } else {
      clusters = Lists.newArrayList();
      for ( final String partitionName : request.getAvailabilityZoneSet( ) ) {
        try {
          clusters.add( Iterables.find( Clusters.list( ), new Predicate<Cluster>( ) {
            @Override
            public boolean apply( Cluster input ) {
              return partitionName.equals( input.getConfiguration( ).getPartition( ) );
            }
          } ) );
        } catch ( NoSuchElementException e ) {
          try {
            clusters.add( Clusters.lookupAny( partitionName ) );
          } catch ( NoSuchElementException ex ) {
            if ( !describeKeywords.containsValue( partitionName ) ) {
              throw new ClientComputeException("InvalidParameterValue", "Invalid availability zone: [" + partitionName + "]");
            }
          }
        }
      }
    }

    for ( final Cluster c : Iterables.filter( clusters, filterPredicate ) ) {
      reply.getAvailabilityZoneInfo( ).addAll( this.getDescriptionEntry( c ) );
    }

    return reply;
  }
  
  private List<ClusterInfoType> getDescriptionEntry( Cluster c ) {
    final List<ClusterInfoType> ret = Lists.newArrayList( );
    ret.add( new ClusterInfoType( c.getConfiguration( ).getPartition( ), ClusterFunctions.STATE.apply( c ), region( ) ) );
    NavigableSet<String> tagList = new ConcurrentSkipListSet<>( );
    if ( tagList.size( ) == 1 )
      tagList = c.getNodeTags( );
    else tagList.retainAll( c.getNodeTags( ) );
    return ret;
  }
  
  private static String INFO_FSTRING  = "|- %s";
  private static String HEADER_STRING = "free / max   cpu   ram  disk";
  private static String STATE_FSTRING = "%04d / %04d  %2d   %4d  %4d";
  
  private static ClusterInfoType s( String left, String right ) {
    return new ClusterInfoType( String.format( INFO_FSTRING, left ), right );
  }

  private static String region( ) {
    return RegionConfigurations.getRegionName( ).or( "" );
  }
  
  private static NonNullFunction<Cluster, List<ClusterInfoType>> describeSystemInfo = new NonNullFunction<Cluster, List<ClusterInfoType>>( ) {
                                                                               @Nonnull
                                                                               @Override
                                                                               public List<ClusterInfoType> apply( Cluster cluster ) {
                                                                                 List<ClusterInfoType> info = new ArrayList<>( );
                                                                                 try {
                                                                                   info.add( new ClusterInfoType(
                                                                                                                  cluster.getConfiguration( ).getPartition( ),
                                                                                                                  cluster.getConfiguration( ).getHostName( )
                                                                                                                      + " "
                                                                                                                      + cluster.getConfiguration( ).getFullName( ) ) );
                                                                                   info.add( new ClusterInfoType( String.format( INFO_FSTRING, "vm types" ),
                                                                                                                  HEADER_STRING ) );
                                                                                   for ( VmType v : VmTypes.list( ) ) {
                                                                                     VmTypeAvailability va = cluster.getNodeState( ).getAvailability( v );
                                                                                     info.add( s( v.getName( ),
                                                                                                  String.format( STATE_FSTRING, va.getAvailable( ),
                                                                                                                 va.getMax( ), v.getCpu( ), v.getMemory( ),
                                                                                                                 v.getDisk( ) ) ) );
                                                                                   }
                                                                                 } catch ( Exception e ) {
                                                                                   LOG.error( e, e );
                                                                                 }
                                                                                 
                                                                                 return info;
                                                                               }
                                                                             };
  
  public DescribeRegionsResponseType DescribeRegions(
      final DescribeRegionsType request
  ) throws EucalyptusCloudException {
    final DescribeRegionsResponseType reply = request.getReply( );
    try {
      final List<com.eucalyptus.auth.RegionService> regions =
          Regions.getRegionServicesByType( ComponentIds.lookup( Compute.class ).name( ) );
      final Predicate<Object>  filterPredicate =
          Filters.generateFor( request.getFilterSet(), com.eucalyptus.auth.RegionService.class )
          .withOptionalInternalFilter( "region-name", request.getRegions() )
          .generate()
          .asPredicate();
      for ( final com.eucalyptus.auth.RegionService item : Iterables.filter( regions, filterPredicate ) ) {
        reply.getRegionInfo( ).add( new RegionInfoType( item.getRegionName( ), item.getServiceEndpoint( ) ) );
      }
    } catch ( Exception ex ) {
      LOG.error( "Error describing regions: " + ex.getMessage( ), ex );
    }
    return reply;
  }

  public static class RegionFilterSupport extends FilterSupport<com.eucalyptus.auth.RegionService> {
    public RegionFilterSupport() {
      super( builderFor( com.eucalyptus.auth.RegionService.class )
          .withStringProperty( "endpoint", com.eucalyptus.auth.RegionService.serviceEndpoint( ) )
          .withStringProperty( "region-name", com.eucalyptus.auth.RegionService.regionName( ) ) );
    }
  }

  private enum ClusterFunctions implements Function<Cluster,String> {
    STATE {
      @Override
      public String apply( final Cluster cluster ) {
        return Clusters.list( ).contains( cluster ) ?
            "available" :
            "unavailable";
      }
    },
  }

  public static class AvailabilityZoneFilterSupport extends FilterSupport<Cluster> {
    public AvailabilityZoneFilterSupport() {
      super( builderFor( Cluster.class )
          .withUnsupportedProperty( "message" )
          .withStringProperty( "region-name", (r)->region( ) )
          .withStringProperty( "state", ClusterFunctions.STATE )
          .withStringProperty( "zone-name", CloudMetadatas.toDisplayName() ) );
    }
  }
}
