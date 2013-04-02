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

package com.eucalyptus.node;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.*;
import com.eucalyptus.component.ComponentId.ComponentPart;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.empyrean.*;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;

/**
 * @author chris grzegorczyk <grze@eucalyptus.com>
 * @todo doc
 */
@ComponentPart(NodeController.class)
@Handles({RegisterNodeControllerType.class,
         DeregisterNodeControllerType.class,
         DescribeNodeControllersType.class,
         ModifyNodeControllerAttributeType.class})
public class NodeControllerConfigurationBuilder implements ServiceBuilder<NodeControllerConfiguration> {
  private static Logger LOG = Logger.getLogger( NodeControllerConfigurationBuilder.class );

  @Override
  public ComponentId getComponentId() {
    return ComponentIds.lookup( NodeController.class );
  }

  @Override
  public Boolean checkAdd( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    throw new ServiceRegistrationException( "Node Controllers must currently be registered " +
                                            "on the Cluster Controllers for the partition." +
                                            "Apologies.  Please see the documentation." );
  }

  /**
   * Here we do nothing for now.
   */
  @Override
  public void fireLoad( ServiceConfiguration parent ) throws ServiceRegistrationException {
  }

  /**
   * Here we do nothing for now.
   */
  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {
//GRZE: we dont send this at the moment -- it is a NOOP at the CC
//    send( config, new StartServiceType( ) );
  }

  /**
   * Here we do nothing for now.
   */
  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {
    if ( Bootstrap.isOperational() ) {
      Nodes.send( config, new StopServiceType() );
    }
  }

  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {
    Nodes.send( config, new EnableServiceType() );
  }

  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {
//GRZE: we dont send this at the moment -- it is a NOOP at the CC
//    send( config, new DisableServiceType( ) );
  }

  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {
    try {
      final ServiceConfiguration ccConfig = Topology.lookup( ClusterController.class, Partitions.lookupByName( config.getPartition() ) );
      DescribeServicesResponseType reply = Nodes.send( config, new DescribeServicesType( ) );
      for ( ServiceStatusType status : reply.getServiceStatuses( ) ) {
        if ( config.getName( ).equals( status.getServiceId( ).getName( ) ) ) {
          Component.State reportedState = Component.State.ENABLED;
          try {
            reportedState = Component.State.valueOf( Strings.nullToEmpty( status.getLocalState( ) ).toUpperCase( ) );
            LOG.debug( "Found service status for " + config.getName( ) + ": " + reportedState );
          } catch ( IllegalArgumentException e ) {
            LOG.debug( "Failed to get service status for " + config.getName( ) + "; got " + status.getLocalState( ) );
          }
          if ( Component.State.NOTREADY.equals( reportedState ) ) {
            throw Faults.failure( config, new RuntimeException( Joiner.on( "," ).join( status.getDetails() ) ) );
          } else {
            throw Faults.advisory( config, new RuntimeException( Joiner.on( "," ).join( status.getDetails() ) ) );
          }
        }
      }
    } catch ( Faults.CheckException e ) {
      throw e;
    } catch ( Exception e ) {
      LOG.error( e );
      LOG.debug( e, e );
      throw Faults.failure( config, e );
    }
  }

  @Override
  public NodeControllerConfiguration newInstance( String partition, String name, String host, Integer port ) {
    return new NodeControllerConfiguration( partition, host );
  }

  @Override
  public NodeControllerConfiguration newInstance() {
    return new NodeControllerConfiguration();
  }

}
