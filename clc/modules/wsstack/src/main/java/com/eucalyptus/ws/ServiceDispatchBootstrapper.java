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
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.Registry;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.StaticApplicationContext;

import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;
import com.eucalyptus.bootstrap.ServiceBootstrapper;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.RemoteConfiguration;
import com.eucalyptus.config.StorageControllerConfiguration;
import com.eucalyptus.config.WalrusConfiguration;
import com.eucalyptus.event.EventVetoedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.StartComponentEvent;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.NetworkUtil;
import com.eucalyptus.ws.client.LocalDispatcher;
import com.eucalyptus.ws.client.RemoteDispatcher;
import com.eucalyptus.ws.client.ServiceDispatcher;

@Provides(resource=Resource.RemoteServices)
public class ServiceDispatchBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( ServiceDispatchBootstrapper.class );
  
  @Override
  public boolean load( Resource current ) throws Exception {
    
    LOG.trace( "Touching class: " + ServiceDispatcher.class);
    for( Component v : Component.values( ) ) {
      LOG.info("Ensure component is initialized: " + LogUtil.dumpObject( v ) );
    }
    if( !Component.eucalyptus.isLocal( ) ) {
      Component.eucalyptus.setHostAddress( Component.db.getHostAddress( ) );
      registerComponent( Component.eucalyptus, new RemoteConfiguration( Component.eucalyptus, Component.eucalyptus.getUri( ) ) );
      Component.jetty.setHostAddress( Component.db.getHostAddress( ) );
      registerComponent( Component.jetty, new RemoteConfiguration( Component.jetty, Component.jetty.getUri( ) ) );
      Component.cluster.setHostAddress( Component.db.getHostAddress( ) );
      registerComponent( Component.cluster, new RemoteConfiguration( Component.cluster, Component.cluster.getUri( ) ) );
      Component.cluster.setHostAddress( Component.db.getHostAddress( ) );
      registerComponent( Component.dns, new RemoteConfiguration( Component.dns, Component.dns.getUri( ) ) );
    } else if( Component.eucalyptus.isLocal( ) ) {
      try {
        registerLocalComponent( Component.db );
        Component.db.setHostAddress( "127.0.0.1" ); //reset this afterwards due to brain damages.
        System.setProperty( "euca.db.url", Component.db.getUri( ).toASCIIString( ) );
        registerLocalComponent( Component.dns );
        registerLocalComponent( Component.eucalyptus );
        registerLocalComponent( Component.cluster );
        registerLocalComponent( Component.jetty );
      } catch ( Exception e ) {
        LOG.fatal( e, e );
        return false;
      }      
    }

    if( !Component.walrus.isLocal( ) ) {
      List<WalrusConfiguration> walri = Configuration.getWalrusConfigurations( );
      for( WalrusConfiguration w : walri ) {
        try {
          if( NetworkUtil.testLocal( w.getHostName( ) )) {
            Component.walrus.markLocal( );
            registerLocalComponent( Component.walrus );
            break;
          } else {
            Component.walrus.setHostAddress( w.getHostName( ) );
            registerComponent( Component.walrus, w );
            break;
          }
        } catch ( Exception e ) {
          LOG.error( "Failed to create walrus service proxy: " + e );
        }
        break;
      }
    } else {
      registerLocalComponent( Component.walrus );
    }

    List<StorageControllerConfiguration> scs = Configuration.getStorageControllerConfigurations( );
    boolean hasLocal = false;
    for( StorageControllerConfiguration sc : scs ) {
      try {
        if( NetworkUtil.testLocal( sc.getHostName( ) )) { 
          hasLocal = true;
        } else {
          registerComponent( Component.storage, sc );
        }
      } catch ( Exception e ) {
        LOG.error( "Failed to create storage controller "+sc.getName( )+" service proxy: " + e );
      }
      if( hasLocal ) {
        Component.storage.markLocal( );
        System.setProperty( "euca.storage.name", sc.getName( ) );
        LOG.info(LogUtil.subheader( "Setting euca.storage.name="+sc.getName( ) + " for: " + LogUtil.dumpObject( sc ) ));
        registerLocalComponent( Component.storage );
        hasLocal = false;
      }
    }
    return true;
  }

  private void registerLocalComponent( Component component ) throws EventVetoedException {
    ListenerRegistry.getInstance( ).fireEvent( component, StartComponentEvent.getLocal( component ) );
  }

  private void registerLocalComponent( ComponentConfiguration componentConfiguration ) throws EventVetoedException {
    ListenerRegistry.getInstance( ).fireEvent( componentConfiguration.getComponent( ), StartComponentEvent.getLocal( componentConfiguration ));
  }
  
  private void registerComponent( Component component, ComponentConfiguration componentConfiguration ) throws Exception {
    ListenerRegistry.getInstance( ).fireEvent( componentConfiguration.getComponent( ), StartComponentEvent.getRemote( componentConfiguration ) );
  }

  @Override
  public boolean start( ) throws Exception {
    return true;
  }

}
