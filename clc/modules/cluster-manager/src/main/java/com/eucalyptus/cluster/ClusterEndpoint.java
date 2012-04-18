/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;
import org.apache.log4j.Logger;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Startable;
import com.eucalyptus.cluster.ResourceState.VmTypeAvailability;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.Walrus;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.vm.VmType;
import com.eucalyptus.vm.VmTypes;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.NodeInfo;
import edu.ucsb.eucalyptus.msgs.ClusterInfoType;
import edu.ucsb.eucalyptus.msgs.DescribeAvailabilityZonesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeAvailabilityZonesType;
import edu.ucsb.eucalyptus.msgs.DescribeRegionsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeRegionsType;
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
  
  public DescribeAvailabilityZonesResponseType DescribeAvailabilityZones( DescribeAvailabilityZonesType request ) {
    DescribeAvailabilityZonesResponseType reply = ( DescribeAvailabilityZonesResponseType ) request.getReply( );
    List<String> args = request.getAvailabilityZoneSet( );
    
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
    
    if ( args.isEmpty( ) ) {
      for ( Cluster c : Clusters.getInstance( ).listValues( ) ) {
        reply.getAvailabilityZoneInfo( ).addAll( this.getDescriptionEntry( c, args ) );
      }
    } else {
      for ( final String partitionName : request.getAvailabilityZoneSet( ) ) {
        try {
          Cluster c = Iterables.find( Clusters.getInstance( ).listValues( ), new Predicate<Cluster>( ) {
            @Override
            public boolean apply( Cluster input ) {
              return partitionName.equals( input.getConfiguration( ).getPartition( ) );
            }
          } );
          reply.getAvailabilityZoneInfo( ).addAll( this.getDescriptionEntry( c, args ) );
        } catch ( NoSuchElementException e ) {
          try {
            Cluster c = Clusters.getInstance( ).lookup( partitionName );
            reply.getAvailabilityZoneInfo( ).addAll( this.getDescriptionEntry( c, args ) );
          } catch ( NoSuchElementException ex ) {}
        }
      }
    }
    return reply;
  }
  
  private List<ClusterInfoType> getDescriptionEntry( Cluster c, List<String> args ) {
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
  
  public DescribeRegionsResponseType DescribeRegions( DescribeRegionsType request ) {//TODO:GRZE:URGENT fix the behaviour here.
    DescribeRegionsResponseType reply = ( DescribeRegionsResponseType ) request.getReply( );
    try {//TODO:GRZE:wtfugly
      Component euca = Components.lookup( Eucalyptus.class );
      NavigableSet<ServiceConfiguration> configs = euca.services( );
      if ( !configs.isEmpty( ) && Component.State.ENABLED.equals( configs.first( ).lookupState( ) ) ) {
        reply.getRegionInfo( ).add( new RegionInfoType( euca.getComponentId( ).name( ), ServiceUris.remotePublicify( configs.first( ) ).toASCIIString( ) ) );
      }
    } catch ( NoSuchElementException ex ) {
      LOG.error( ex, ex );
    }
    try {//TODO:GRZE:wtfugly
      Component walrus = Components.lookup( Walrus.class );
      NavigableSet<ServiceConfiguration> configs = walrus.services( );
      if ( !configs.isEmpty( ) && Component.State.ENABLED.equals( configs.first( ).lookupState( ) ) ) {
        reply.getRegionInfo( ).add( new RegionInfoType( walrus.getComponentId( ).name( ), ServiceUris.remotePublicify( configs.first( ) ).toASCIIString( ) ) );
      }
    } catch ( NoSuchElementException ex ) {
      LOG.error( ex, ex );
    }
    return reply;
  }
}
