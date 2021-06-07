/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

package com.eucalyptus.cluster.node;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cluster.common.msgs.ClusterEnableServiceType;
import com.eucalyptus.cluster.common.msgs.ClusterStopServiceType;
import com.eucalyptus.component.*;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Joiner;
import com.eucalyptus.cluster.common.msgs.NodeInfo;

/**
 * @author chris grzegorczyk <grze@eucalyptus.com>
 * @todo doc
 */
@ComponentPart(NodeController.class)
public class NodeControllerServiceBuilder implements ServiceBuilder<NodeControllerConfiguration> {
  private static Logger LOG = Logger.getLogger( NodeControllerServiceBuilder.class );

  @Override
  public ComponentId getComponentId() {
    return ComponentIds.lookup( NodeController.class );
  }

  @Override
  public boolean checkAdd( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    throw failUnsupported( );
  }

  @Override
  public boolean checkUpdate( final String partition, final String name, final String host, final Integer port ) throws ServiceRegistrationException {
    throw failUnsupported( );
  }

  private ServiceRegistrationException failUnsupported( ) throws ServiceRegistrationException {
    throw new ServiceRegistrationException(
        "Node Controllers must currently be registered " +
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
//    send( new StartServiceType( ), config );
  }

  /**
   * Here we do nothing for now.
   */
  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {
    if ( Bootstrap.isOperational() ) {
      Nodes.send( new ClusterStopServiceType( ), config );
    }
  }

  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {
    try {
      Nodes.send( new ClusterEnableServiceType(), config );
    } catch ( RuntimeException e ) {
      throw new ServiceRegistrationException( "Failed to enable node controller " + config.getFullName() + " because: " + e.getMessage(), e );
    }
  }

  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {
//GRZE: we dont send this at the moment -- it is a NOOP at the CC
//    send( config, new DisableServiceType( ) );
  }

  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {
    //GRZE: an NC should always have an ENABLED CC, otherwise it should go NOTREADY.
    //GRZE:NOTE: at this level we assume a dependence on the ServiceConfiguration of the CC, it is correct to fail if that is missing.
    final ServiceConfiguration ccConfig = Nodes.lookupClusterController( config );
    if ( !Component.State.ENABLED.apply( ccConfig ) || (Component.State.DISABLED.apply( ccConfig ) && ccConfig.lookupStateMachine().isBusy() ) ) {
      //GRZE: the CC's state should always bound the NCs from above EXCEPT when it is in transition from DISABLED->ENABLED
      throw Faults.failure( config, Exceptions.noSuchElement( "Failed to find ENABLED Cluster Controller for " + config ) );
    } else {
      NodeInfo nodeInfo = Nodes.lookupNodeInfo( ccConfig ).apply( config.getName() );
      if(nodeInfo.getLastException()==null) {
        String checkMessage = Joiner.on(' ').useForNull( "<null>" ).join( nodeInfo.getLastSeen(), nodeInfo.getLastState(), nodeInfo.getLastMessage() );
        throw Faults.advisory( config, checkMessage );
      } else {
        throw nodeInfo.getLastException();
      }
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
