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

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Depends;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;
import com.eucalyptus.cluster.event.NewClusterEvent;
import com.eucalyptus.cluster.event.TeardownClusterEvent;
import com.eucalyptus.cluster.handlers.ClusterCertificateHandler;
import com.eucalyptus.cluster.util.ClusterUtil;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.EventVetoedException;
import com.eucalyptus.event.GenericEvent;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.StartComponentEvent;
import com.eucalyptus.event.StopComponentEvent;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.BindingException;
import com.eucalyptus.ws.client.LocalDispatcher;
import com.eucalyptus.ws.client.RemoteDispatcher;
import com.eucalyptus.ws.client.ServiceDispatcher;
import com.google.common.collect.Lists;

@Provides( resource = Resource.RemoteServices )
@Depends( local = Component.eucalyptus )
public class ClusterBootstrapper extends Bootstrapper implements EventListener {
  public static Logger LOG         = Logger.getLogger( ClusterBootstrapper.class );
  private boolean       initialized = false;
  private boolean       finished    = false;
  
  public static void register() {
    ListenerRegistry.getInstance( ).register( Component.cluster, new ClusterBootstrapper() );
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
  public boolean load( Resource current ) throws Exception {
    LOG.info( "Loading clusters." );
    for ( ClusterConfiguration c : Configuration.getClusterConfigurations( ) ) {
      Cluster newCluster = ClusterUtil.createCluster( c );
      this.registerClusterStateHandler( newCluster );
      ListenerRegistry.getInstance( ).fireEvent( new NewClusterEvent( ).setMessage( newCluster ) );
    }
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
//    for( Cluster c : Clusters.getInstance( ).getEntries( ) ) {
//      c.getThreadGroup( ).startThreads();
//    }
    return true;
  }

  @Override
  public void advertiseEvent( Event event ) {
    if ( event instanceof StartComponentEvent ) {
      StartComponentEvent e = ( StartComponentEvent ) event;
      if ( Component.cluster.equals( e.getComponent( ) ) ) {
        try {
          Cluster c = Clusters.getInstance( ).lookup( e.getConfiguration( ).getName( ) );
          e.veto( "Cluster by that name already exists: " + LogUtil.dumpObject( c ) );
        } catch ( NoSuchElementException e1 ) {
        }
      }
    } else if ( event instanceof StopComponentEvent ) {
      StopComponentEvent e = ( StopComponentEvent ) event;
      if ( Component.cluster.equals( e.getComponent( ) ) ) {
        try {
          Cluster c = Clusters.getInstance( ).lookup( e.getConfiguration( ).getName( ) );
        } catch ( NoSuchElementException e1 ) {
          e.veto( "No cluster by that name exists: " + e.getConfiguration( ).getName( ) );
        }
      }
    }
  }

  @Override
  public void fireEvent( Event event ) {
    if ( event instanceof StartComponentEvent ) {
      StartComponentEvent e = ( StartComponentEvent ) event;
      if ( Component.cluster.equals( e.getComponent( ) ) && e.getConfiguration( ) instanceof ClusterConfiguration ) {
        try {
          Cluster newCluster = ClusterUtil.createCluster( ( ClusterConfiguration ) e.getConfiguration( ) );
          this.registerClusterStateHandler( newCluster );
          ListenerRegistry.getInstance( ).fireEvent( new NewClusterEvent( ).setMessage( newCluster ) );
          return;
        } catch ( EucalyptusCloudException e1 ) {
          e.setFail( e1 );
          return;
        } catch ( EventVetoedException e1 ) {
          e.setFail( e1 );
          return;
        }
      }
    } else if ( event instanceof StopComponentEvent ) {
      StopComponentEvent e = ( StopComponentEvent ) event;
      if ( Component.cluster.equals( e.getComponent( ) ) && e.getConfiguration( ) instanceof ClusterConfiguration ) {
        Cluster c = Clusters.getInstance( ).lookup( e.getConfiguration( ).getName( ) );
        Clusters.getInstance( ).deregister( c.getName( ) );
        c.stop();
        try {
          ListenerRegistry.getInstance( ).fireEvent( new TeardownClusterEvent( ).setMessage( c ) );
          this.deregisterClusterStateHandler( c );
          return;
        } catch ( EventVetoedException e1 ) {
          e.setFail( e1 );
          return;
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

}
