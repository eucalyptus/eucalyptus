/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.cluster;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Authentication;
import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.cluster.event.NewClusterEvent;
import com.eucalyptus.cluster.event.TeardownClusterEvent;
import com.eucalyptus.cluster.handlers.ClusterCertificateHandler;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.event.LifecycleEvent;
import com.eucalyptus.component.event.StartComponentEvent;
import com.eucalyptus.component.event.StopComponentEvent;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.EventVetoedException;
import com.eucalyptus.event.GenericEvent;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.ServiceVerifyBootstrapper;
import edu.ucsb.eucalyptus.msgs.EventRecord;

@Provides( Component.eucalyptus )
@RunDuring( Bootstrap.Stage.RemoteServicesInit )
@DependsLocal( Component.eucalyptus )
public class ClusterBootstrapper extends Bootstrapper implements EventListener {
  public static Logger LOG         = Logger.getLogger( ClusterBootstrapper.class );
  private boolean      initialized = false;
  private boolean      finished    = false;
  
  public static void register( ) {
    ListenerRegistry.getInstance( ).register( Component.cluster, new ClusterBootstrapper( ) );
  }
  
  private void registerClusterStateHandler( Cluster newCluster ) throws EventVetoedException {
    try {
      ClusterCertificateHandler cc = new ClusterCertificateHandler( newCluster );
      ListenerRegistry.getInstance( ).register( NewClusterEvent.class, cc );
      ListenerRegistry.getInstance( ).register( TeardownClusterEvent.class, cc );
      ListenerRegistry.getInstance( ).register( ClockTick.class, cc );
    } catch ( BindingException e1 ) {
      LOG.error( e1, e1 );
    }
  }
  
  private void deregisterClusterStateHandler( Cluster removeCluster ) throws EventVetoedException {
    try {
      ClusterCertificateHandler cc = new ClusterCertificateHandler( removeCluster );
      ListenerRegistry.getInstance( ).deregister( NewClusterEvent.class, cc );
      ListenerRegistry.getInstance( ).deregister( TeardownClusterEvent.class, cc );
      ListenerRegistry.getInstance( ).deregister( ClockTick.class, cc );
    } catch ( BindingException e1 ) {
      LOG.error( e1, e1 );
    }
  }
  
  @Override
  public boolean load( Stage current ) throws Exception {
    LOG.info( "Loading clusters." );
    try {
      for ( ClusterConfiguration c : Configuration.getClusterConfigurations( ) ) {
        Cluster newCluster = ClusterBootstrapper.lookupCluster( c );
        this.registerClusterStateHandler( newCluster );
        ListenerRegistry.getInstance( ).fireEvent( new NewClusterEvent( ).setMessage( newCluster ) );
      }
    } catch ( Throwable e ) {
      LOG.debug( e, e );
    }
    return true;
  }
  
  @Override
  public boolean start( ) throws Exception {
    return true;
  }
  
  @Override
  public void advertiseEvent( Event event ) {}
  
  @Override
  public void fireEvent( Event event ) {
    if( event instanceof LifecycleEvent ) {
      LifecycleEvent lifeEvent = ((LifecycleEvent) event );
      if( lifeEvent.getConfiguration( ) instanceof ClusterConfiguration ) {
        ClusterConfiguration conf = ( ClusterConfiguration ) lifeEvent.getConfiguration( );        
        if ( event instanceof StartComponentEvent ) {
          LOG.info( "Starting up cluster: " + LogUtil.dumpObject( lifeEvent ) );
          EventRecord.here( ClusterBootstrapper.class, EventType.COMPONENT_SERVICE_START_REMOTE, conf.getComponent( ).name( ), conf.getName( ), conf.getUri( ) ).info( );
          try {
            Cluster newCluster = ClusterBootstrapper.lookupCluster( conf );
            this.registerClusterStateHandler( newCluster );
            ListenerRegistry.getInstance( ).fireEvent( new NewClusterEvent( ).setMessage( newCluster ) );
          } catch ( EucalyptusCloudException e ) {
            LOG.error( e, e );
            return;
          } catch ( EventVetoedException e ) {
            LOG.error( e, e );
            return;
          }          
        } else if ( event instanceof StopComponentEvent ) {
          LOG.info( "Tearing down cluster: " + LogUtil.dumpObject( lifeEvent ) );
          EventRecord.here( ClusterBootstrapper.class, EventType.COMPONENT_SERVICE_STOP_REMOTE, conf.getComponent( ).name( ), conf.getName( ), conf.getUri( ) ).info( );
          Cluster c = Clusters.getInstance( ).lookup( conf.getName( ) );
          Clusters.getInstance( ).deregister( c.getName( ) );
          c.stop();
          try {
            ListenerRegistry.getInstance( ).fireEvent( new TeardownClusterEvent( ).setMessage( c ) );
            this.deregisterClusterStateHandler( c );
            return;
          } catch ( EventVetoedException e1 ) {
            return;
          }          
        }
      }
    }
  }
  
  private void fireClusterStateEvent( Cluster newCluster, GenericEvent<Cluster> e ) throws EventVetoedException {
    try {
      ListenerRegistry.getInstance( ).register( e.getClass( ), new ClusterCertificateHandler( newCluster ) );
      ListenerRegistry.getInstance( ).fireEvent( e.setMessage( newCluster ) );
    } catch ( BindingException e1 ) {
      LOG.error( e1, e1 );
    }
  }
  
  private static Cluster lookupCluster( ClusterConfiguration c ) throws EucalyptusCloudException {
    ClusterCredentials credentials = null;
    EntityWrapper<ClusterCredentials> credDb = Authentication.getEntityWrapper( );
    try {
      credentials = credDb.getUnique( new ClusterCredentials( c.getName( ) ) );
      credDb.rollback( );
    } catch ( EucalyptusCloudException e ) {
      LOG.error( "Failed to load credentials for cluster: " + c.getName( ) );
      credDb.rollback( );
      throw e;
    }
    Cluster newCluster = new Cluster( c, credentials );
    Clusters.getInstance( ).register( newCluster );
    return newCluster;
  }
  
}
