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

import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentId.ComponentPart;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceBuilder;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.empyrean.DescribeServicesType;
import com.eucalyptus.empyrean.DisableServiceType;
import com.eucalyptus.empyrean.EnableServiceType;
import com.eucalyptus.empyrean.ServiceId;
import com.eucalyptus.empyrean.ServiceTransitionType;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncRequests;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * @todo doc
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
@ComponentPart( NodeController.class )
@Handles( { RegisterNodeControllerType.class,
           DeregisterNodeControllerType.class,
           DescribeNodeControllersType.class,
           ModifyNodeControllerAttributeType.class } )
public class NodeControllerConfigurationBuilder implements ServiceBuilder<NodeControllerConfiguration> {
  
  @Override
  public ComponentId getComponentId( ) {
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
  public void fireLoad( ServiceConfiguration parent ) throws ServiceRegistrationException {}
  
  /**
   * Here we do nothing for now.
   */
  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {
//    sendNodeServiceRequest( config, new StartServiceType( ) ); 
  }
  
  /**
   * Here we do nothing for now.
   */
  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {
//    sendNodeServiceRequest( config, new StopServiceType( ) ); 
  }
  
  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {
    sendNodeServiceRequest( config, new EnableServiceType( ) ); 
  }

  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {
    sendNodeServiceRequest( config, new DisableServiceType( ) ); 
  }
  
  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {
    sendNodeServiceRequest( config, new DescribeServicesType( ) ); 
  }
  
  @Override
  public NodeControllerConfiguration newInstance( String partition, String name, String host, Integer port ) {
    return new NodeControllerConfiguration( partition, host );
  }
  
  @Override
  public NodeControllerConfiguration newInstance( ) {
    return new NodeControllerConfiguration( );
  }
  
  private static <T extends BaseMessage> T sendNodeServiceRequest( ServiceConfiguration config, ServiceTransitionType msg ) throws RuntimeException {
    ServiceConfiguration ccConfig = Topology.lookup( ClusterController.class, Partitions.lookupByName( config.getPartition( ) ) );
    ServiceId serviceId = ServiceConfigurations.ServiceConfigurationToServiceId.INSTANCE.apply( config );
    msg.getServices().add( serviceId );
    try {
      return AsyncRequests.sendSync( ccConfig, msg );
    } catch ( Exception ex ) {
      throw Exceptions.toUndeclared( ex );
    }
  }
  
}
