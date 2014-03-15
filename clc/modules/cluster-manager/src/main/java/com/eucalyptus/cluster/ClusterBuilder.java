/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

import java.util.NoSuchElementException;

import com.eucalyptus.component.*;
import com.eucalyptus.node.Nodes;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.config.DeregisterClusterType;
import com.eucalyptus.config.DescribeClustersType;
import com.eucalyptus.config.ModifyClusterAttributeType;
import com.eucalyptus.config.RegisterClusterType;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;

@ComponentPart( ClusterController.class )
@Handles( { RegisterClusterType.class,
           DeregisterClusterType.class,
           DescribeClustersType.class,
           ModifyClusterAttributeType.class } )
public class ClusterBuilder extends AbstractServiceBuilder<ClusterConfiguration> {
  static Logger LOG = Logger.getLogger( ClusterBuilder.class );
  
  @Override
  public boolean checkAdd( final String partition, final String name, final String host, final Integer port ) throws ServiceRegistrationException {
    try {
      final Partition part = Partitions.lookup( this.newInstance( partition, name, host, port ) );
      part.syncKeysToDisk( );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      throw new ServiceRegistrationException( String.format( "Unexpected error caused cluster registration to fail for: partition=%s name=%s host=%s port=%d",
                                                             partition, name, host, port ), ex );
    }
    return super.checkAdd( partition, name, host, port );
  }
  
  @Override
  public ClusterConfiguration newInstance( ) {
    return new ClusterConfiguration( );
  }
  
  @Override
  public ClusterConfiguration newInstance( final String partition, final String name, final String host, final Integer port ) {
    return new ClusterConfiguration( partition, name, host, port );
  }
  
  @Override
  public ComponentId getComponentId( ) {
    return ComponentIds.lookup( ClusterController.class );
  }
  
  @Override
  public void fireStart( final ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.info( "Starting cluster: " + config );
    EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_START, config.getComponentId( ).name( ), config.getName( ),
                      ServiceUris.remote( config ).toASCIIString( ) ).info( );
    try {
      if ( !Clusters.getInstance( ).contains( config.getName( ) ) ) {
        final Cluster newCluster = new Cluster( ( ClusterConfiguration ) config );//TODO:GRZE:fix the type issue here.
        newCluster.start( );
      } else {
        try {
          final Cluster newCluster = Clusters.getInstance( ).lookupDisabled( config.getName( ) );
          Clusters.getInstance( ).deregister( config.getName( ) );
          newCluster.start( );
        } catch ( final NoSuchElementException ex ) {
          final Cluster newCluster = Clusters.getInstance( ).lookup( config.getName( ) );
          Clusters.getInstance( ).deregister( config.getName( ) );
          newCluster.start( );
        }
      }
    } catch ( final NoSuchElementException ex ) {
      LOG.error( ex, ex );
    }
  }
  
  @Override
  public void fireEnable( final ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.info( "Enabling cluster: " + config );
    EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_ENABLED, config.getComponentId( ).name( ), config.getName( ),
                      ServiceUris.remote( config ).toASCIIString( ) ).info( );
    Cluster newCluster = null;
    try {
      try {
        newCluster = Clusters.getInstance( ).lookupDisabled( config.getName( ) );
        newCluster.enable( );
      } catch ( final NoSuchElementException ex ) {
        newCluster = Clusters.getInstance().lookup( config.getName() );
        newCluster.enable( );
      }
    } catch ( final Exception ex ) {
      LOG.error( ex );
      if ( newCluster != null ) {
        Nodes.clusterCleanup( newCluster, ex );
        throw ex;
      }
    }
  }
  
  @Override
  public void fireDisable( final ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.info( "Disabling cluster: " + config );
    EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_DISABLED, config.getComponentId( ).name( ), config.getName( ),
                      ServiceUris.remote( config ).toASCIIString( ) ).info( );
    try {
      if ( Clusters.getInstance( ).contains( config.getName( ) ) ) {
        try {
          final Cluster newCluster = Clusters.getInstance( ).lookup( config.getName( ) );
          newCluster.disable( );
        } catch ( final NoSuchElementException ex ) {
          final Cluster newCluster = Clusters.getInstance( ).lookupDisabled( config.getName( ) );
          newCluster.disable( );
        }
      }
    } catch ( final NoSuchElementException ex ) {
      LOG.error( ex, ex );
    }
  }
  
  @Override
  public void fireStop( final ServiceConfiguration config ) throws ServiceRegistrationException {
    try {
      LOG.info( "Tearing down cluster: " + config );
      final Cluster cluster = Clusters.getInstance( ).lookupDisabled( config.getName( ) );
      EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_STOPPED, config.getComponentId( ).name( ), config.getName( ),
                        ServiceUris.remote( config ).toASCIIString( ) ).info( );
      cluster.stop( );
    } catch ( final NoSuchElementException ex ) {
      LOG.error( ex, ex );
    } catch ( final Exception ex ) {
      LOG.error( ex, ex );
    }
  }
  
  @Override
  public void fireCheck( final ServiceConfiguration config ) throws ServiceRegistrationException {
    final Cluster cluster = Clusters.lookup( config );
    try {
      try {
        cluster.check();
      } catch ( final NoSuchElementException ex ) {
        throw Faults.failure( config, ex );
      } catch ( final IllegalStateException ex ) {
        Logs.exhaust( ).error( ex, ex );
        throw Faults.failure( config, ex );
      } catch ( final Exception ex ) {
        Logs.exhaust( ).error( ex, ex );
        throw Faults.failure( config, ex );
      }
    } catch ( Faults.CheckException e ) {
      Nodes.clusterCleanup( cluster, e );
      throw e;
    }
  }

}
