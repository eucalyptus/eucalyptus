/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;
import org.apache.log4j.Logger;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Startable;
import com.eucalyptus.cloud.CloudMetadatas;
import com.eucalyptus.cluster.ResourceState.VmTypeAvailability;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.node.Nodes;
import com.eucalyptus.objectstorage.Walrus;
import com.eucalyptus.tags.Filter;
import com.eucalyptus.tags.FilterSupport;
import com.eucalyptus.tags.Filters;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vm.VmInstances.TerminatedInstanceException;
import com.eucalyptus.vmtypes.VmType;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.NodeInfo;
import edu.ucsb.eucalyptus.msgs.ClusterInfoType;
import edu.ucsb.eucalyptus.msgs.DescribeAvailabilityZonesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeAvailabilityZonesType;
import edu.ucsb.eucalyptus.msgs.DescribeRegionsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeRegionsType;
import edu.ucsb.eucalyptus.msgs.MigrateInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.MigrateInstancesType;
import edu.ucsb.eucalyptus.msgs.NodeCertInfo;
import edu.ucsb.eucalyptus.msgs.NodeLogInfo;
import edu.ucsb.eucalyptus.msgs.RegionInfoType;

public class ClusterEndpoint implements Startable {
  
  private static Logger                                LOG              = Logger.getLogger( ClusterEndpoint.class );
  
  private Map<String, Supplier<List<ClusterInfoType>>> describeKeywords = new HashMap<String, Supplier<List<ClusterInfoType>>>( ) {
                                                                          {
                                                                            put( "verbose", new Supplier<List<ClusterInfoType>>( ) {
                                                                              
                                                                              @Override
                                                                              public List<ClusterInfoType> get( ) {
                                                                                List<ClusterInfoType> verbose = Lists.newArrayList( );
                                                                                for ( Cluster c : Clusters.getInstance( ).listValues( ) ) {
                                                                                  verbose.addAll( describeSystemInfo.apply( c ) );
                                                                                }
                                                                                return verbose;
                                                                              }
                                                                            } );
                                                                          }
                                                                        };
  
  public void start( ) throws MuleException {
    Clusters.getInstance( );
  }
  
  public MigrateInstancesResponseType migrateInstances( final MigrateInstancesType request ) throws EucalyptusCloudException {
    MigrateInstancesResponseType reply = request.getReply( );
    if ( !Contexts.lookup( ).hasAdministrativePrivileges( ) ) {
      throw new EucalyptusCloudException( "Authorization failed." );
    }
    if ( !Strings.isNullOrEmpty( request.getSourceHost( ) ) ) {
      for ( ServiceConfiguration ccConfig : Topology.enabledServices( ClusterController.class ) ) {
        try {
          ServiceConfiguration node = Nodes.lookup( ccConfig, request.getSourceHost( ) );//found the node!
          Cluster cluster = Clusters.lookup( ccConfig );//lookup the cluster
          try {
            cluster.migrateInstances( request.getSourceHost( ), request.getAllowHosts( ), request.getDestinationHosts( ) );//submit the migration request
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
      VmInstance vm;
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
        Cluster cluster = Clusters.lookup( ccConfig );
        try {
          cluster.migrateInstance( request.getInstanceId( ), request.getAllowHosts( ), request.getDestinationHosts( ) );
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

  public DescribeAvailabilityZonesResponseType DescribeAvailabilityZones( DescribeAvailabilityZonesType request ) throws EucalyptusCloudException {
    final DescribeAvailabilityZonesResponseType reply = ( DescribeAvailabilityZonesResponseType ) request.getReply( );
    final List<String> args = request.getAvailabilityZoneSet( );
    final Filter filter = Filters.generate( request.getFilterSet(), Cluster.class );
    
    if ( Contexts.lookup( ).hasAdministrativePrivileges( ) ) {
      for ( String keyword : describeKeywords.keySet( ) ) {
        if ( args.remove( keyword ) ) {
          reply.getAvailabilityZoneInfo( ).addAll( describeKeywords.get( keyword ).get( ) );
          return reply;
        }
      }
    } else {
      for ( String keyword : describeKeywords.keySet( ) ) {
        args.remove( keyword );
      }
    }

    final List<Cluster> clusters;
    if ( args.isEmpty( ) ) {
      clusters = Clusters.getInstance( ).listValues( );
    } else {
      clusters = Lists.newArrayList();
      for ( final String partitionName : request.getAvailabilityZoneSet( ) ) {
        try {
          clusters.add( Iterables.find( Clusters.getInstance( ).listValues( ), new Predicate<Cluster>( ) {
            @Override
            public boolean apply( Cluster input ) {
              return partitionName.equals( input.getConfiguration( ).getPartition( ) );
            }
          } ) );
        } catch ( NoSuchElementException e ) {
          try {
            clusters.add( Clusters.getInstance( ).lookup( partitionName ) );
          } catch ( NoSuchElementException ex ) {}
        }
      }
    }

    for ( final Cluster c : Iterables.filter( clusters, filter.asPredicate() ) ) {
      reply.getAvailabilityZoneInfo( ).addAll( this.getDescriptionEntry( c ) );
    }

    return reply;
  }
  
  private List<ClusterInfoType> getDescriptionEntry( Cluster c ) {
    List<ClusterInfoType> ret = Lists.newArrayList( );
    String clusterName = c.getName( );
    ret.add( new ClusterInfoType( c.getConfiguration( ).getPartition( ), c.getConfiguration( ).getHostName( ) + " "
                                                                                                      + c.getConfiguration( ).getFullName( ) ) );
    NavigableSet<String> tagList = new ConcurrentSkipListSet<String>( );
    if ( tagList.size( ) == 1 )
      tagList = c.getNodeTags( );
    else tagList.retainAll( c.getNodeTags( ) );
    return ret;
  }
  
  private static String INFO_FSTRING  = "|- %s";
  private static String HEADER_STRING = "free / max   cpu   ram  disk";
  private static String STATE_FSTRING = "%04d / %04d  %2d   %4d  %4d";
  
  private static ClusterInfoType t( String left, String right ) {
    return new ClusterInfoType( left, right );
  }
  
  private static ClusterInfoType s( String left, String right ) {
    return new ClusterInfoType( String.format( INFO_FSTRING, left ), right );
  }
  
  private static Function<Cluster, List<ClusterInfoType>> describeSystemInfo = new Function<Cluster, List<ClusterInfoType>>( ) {
                                                                               @Override
                                                                               public List<ClusterInfoType> apply( Cluster cluster ) {
                                                                                 List<ClusterInfoType> info = new ArrayList<ClusterInfoType>( );
                                                                                 try {
                                                                                   info.add( new ClusterInfoType(
                                                                                                                  cluster.getConfiguration( ).getPartition( ),
                                                                                                                  cluster.getConfiguration( ).getHostName( )
                                                                                                                      + " "
                                                                                                                      + cluster.getConfiguration( ).getFullName( ) ) );
                                                                                   info.add( new ClusterInfoType( String.format( INFO_FSTRING, "vm types" ),
                                                                                                                  HEADER_STRING ) );
                                                                                   for ( VmType v : VmTypes.list( ) ) {
                                                                                     VmTypeAvailability va = cluster.getNodeState( ).getAvailability( v.getName( ) );
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
  
  private static Function<String, List<ClusterInfoType>>  describeLogInfo    = new Function<String, List<ClusterInfoType>>( ) {
                                                                               
                                                                               @Override
                                                                               public List<ClusterInfoType> apply( String serviceTag ) {
                                                                                 List<ClusterInfoType> info = new ArrayList<ClusterInfoType>( );
                                                                                 if ( Clusters.getInstance( ).contains( serviceTag ) ) {
                                                                                   Cluster c = Clusters.getInstance( ).lookup( serviceTag );
                                                                                   NodeLogInfo logInfo = c.getLastLog( );
                                                                                   info.add( t( c.getConfiguration( ).getFullName( ).toString( ),
                                                                                                " state=" + c.getState( ) ) );
                                                                                   if ( !logInfo.getCcLog( ).isEmpty( ) )
                                                                                     info.add( s( "cc.log\n", logInfo.getCcLog( ) ) );
                                                                                   info.add( t( c.getConfiguration( ).getFullName( ).toString( ),
                                                                                                " state=" + c.getState( ) ) );
                                                                                   info.add( s( "axis2.log\n", logInfo.getAxis2Log( ) ) );
                                                                                   info.add( t( c.getConfiguration( ).getFullName( ).toString( ),
                                                                                                " state=" + c.getState( ) ) );
                                                                                   info.add( s( "httpd.log\n", logInfo.getHttpdLog( ) ) );
                                                                                 } else {
                                                                                   for ( Cluster c : Clusters.getInstance( ).listValues( ) ) {
                                                                                     if ( c.getNode( serviceTag ) != null ) {
                                                                                       NodeInfo node = c.getNode( serviceTag );
                                                                                       NodeLogInfo logInfo = node.getLogs( );
                                                                                       info.add( t( node.getName( ), "last-seen=" + node.getLastSeen( ) ) );
                                                                                       if ( !logInfo.getNcLog( ).isEmpty( ) )
                                                                                         info.add( s( "nc.log\n", logInfo.getNcLog( ) ) );
                                                                                       info.add( t( node.getName( ), "last-seen=" + node.getLastSeen( ) ) );
                                                                                       info.add( s( "axis2.log\n", logInfo.getAxis2Log( ) ) );
                                                                                       info.add( t( node.getName( ), "last-seen=" + node.getLastSeen( ) ) );
                                                                                       info.add( s( "httpd.log\n", logInfo.getHttpdLog( ) ) );
                                                                                     }
                                                                                   }
                                                                                 }
                                                                                 return info;
                                                                               };
                                                                             };
  
  private static Function<String, List<ClusterInfoType>>  describeCertInfo   = new Function<String, List<ClusterInfoType>>( ) {
                                                                               
                                                                               @Override
                                                                               public List<ClusterInfoType> apply( String serviceTag ) {
                                                                                 List<ClusterInfoType> info = new ArrayList<ClusterInfoType>( );
                                                                                 if ( Clusters.getInstance( ).contains( serviceTag ) ) {
                                                                                   Cluster c = Clusters.getInstance( ).lookup( serviceTag );
                                                                                   info.add( t( c.getConfiguration( ).getFullName( ).toString( ),
                                                                                                " state=" + c.getState( ) ) );
                                                                                   info.add( s( "CC cert\n", c.getClusterCertificate( ).toString( ) ) );
                                                                                   info.add( s( "NC cert\n", c.getNodeCertificate( ).toString( ) ) );
                                                                                 } else {
                                                                                   for ( Cluster c : Clusters.getInstance( ).listValues( ) ) {
                                                                                     if ( c.getNode( serviceTag ) != null ) {
                                                                                       NodeInfo node = c.getNode( serviceTag );
                                                                                       info.add( t( node.getName( ), "last-seen=" + node.getLastSeen( ) ) );
                                                                                       NodeCertInfo certInfo = node.getCerts( );
                                                                                       info.add( s( "CC cert\n", certInfo.getCcCert( ) ) );
                                                                                       info.add( s( "NC cert\n", certInfo.getCcCert( ) ) );
                                                                                     }
                                                                                   }
                                                                                 }
                                                                                 return info;
                                                                               }
                                                                             };
  
  public DescribeRegionsResponseType DescribeRegions( final DescribeRegionsType request ) throws EucalyptusCloudException {//TODO:GRZE:URGENT fix the behaviour here.
    final DescribeRegionsResponseType reply = ( DescribeRegionsResponseType ) request.getReply( );
    for ( final Class<? extends ComponentId> componentIdClass : ImmutableList.of(Eucalyptus.class) ) {
      try {
        final Component component = Components.lookup( componentIdClass );
        final String region = component.getComponentId( ).name();
        final List<Region> regions = Lists.newArrayList();
        final NavigableSet<ServiceConfiguration> configs = component.services( );
        if ( !configs.isEmpty( ) && Component.State.ENABLED.equals( configs.first( ).lookupState( ) ) ) {
          regions.add( new Region( region, ServiceUris.remotePublicify( configs.first() ).toASCIIString() ) );
        }

        final Filter filter = Filters.generate( request.getFilterSet(), Region.class );
        final Predicate<Object> requested = Predicates.and(
            filterByName( request.getRegions() ),
            filter.asPredicate() );
        for ( final Region item : Iterables.filter( regions, requested ) ) {
          reply.getRegionInfo( ).add( new RegionInfoType( item.getDisplayName(), item.getEndpointUrl() ) );
        }
      } catch ( NoSuchElementException ex ) {
        LOG.error( ex, ex );
      }
    }
    return reply;
  }

  /**
   * This should be Predicate<Region> but JDK6 can't handle the resulting Predicate<? super Region>
   */
  private static Predicate<Object> filterByName( final Collection<String> requestedIdentifiers ) {
    return new Predicate<Object>( ) {
      @Override
      public boolean apply( Object region ) {
        return requestedIdentifiers == null || requestedIdentifiers.isEmpty( ) || requestedIdentifiers.contains( ((Region)region).getDisplayName() );
      }
    };
  }

  protected static class Region {
    private final String displayName;
    private final String endpointUrl;

    protected Region( final String displayName, final String endpointUrl ) {
      this.displayName = displayName;
      this.endpointUrl = endpointUrl;
    }

    public String getDisplayName() {
      return displayName;
    }

    public String getEndpointUrl() {
      return endpointUrl;
    }
  }

  private enum RegionFunctions implements Function<Region,String> {
    REGION_NAME {
      @Override
      public String apply( final Region region ) {
        return region.getDisplayName();
      }
    },
    ENDPOINT_URL {
      @Override
      public String apply( final Region region ) {
        return region.getEndpointUrl();
      }
    }
  }

  public static class RegionFilterSupport extends FilterSupport<Region> {
    public RegionFilterSupport() {
      super( builderFor( Region.class )
          .withStringProperty( "endpoint", RegionFunctions.ENDPOINT_URL )
          .withStringProperty( "region-name", RegionFunctions.REGION_NAME ) );
    }
  }

  public static class AvailabilityZoneFilterSupport extends FilterSupport<Cluster> {
    public AvailabilityZoneFilterSupport() {
      super( builderFor( Cluster.class )
          .withUnsupportedProperty( "message" )
          .withUnsupportedProperty( "region-name" )
          .withUnsupportedProperty( "state" )
          .withStringProperty( "zone-name", CloudMetadatas.toDisplayName() ) );
    }
  }
}
