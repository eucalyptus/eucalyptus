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

import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.RemoteConfiguration;
import com.eucalyptus.config.StorageControllerConfiguration;
import com.eucalyptus.config.WalrusConfiguration;
import com.eucalyptus.event.EventVetoedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.StartComponentEvent;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.NetworkUtil;
import com.eucalyptus.ws.client.ServiceDispatcher;
import edu.ucsb.eucalyptus.msgs.EventRecord;

@Provides(com.eucalyptus.bootstrap.Component.any)
@RunDuring(Bootstrap.Stage.RemoteServicesInit)
public class ServiceDispatchBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( ServiceDispatchBootstrapper.class );
  
  @Override
  public boolean load( Stage current ) throws Exception {    
    LOG.trace( "Touching class: " + ServiceDispatcher.class);
    Component eucalyptus = Components.lookup( Components.delegate.eucalyptus );
    Component db = Components.lookup( Components.delegate.db );
    for( com.eucalyptus.bootstrap.Component c : com.eucalyptus.bootstrap.Component.values( ) ) {
      if( c.isDummy() ) {
        LOG.info( EventRecord.here( ServiceVerifyBootstrapper.class, EventType.COMPONENT_INFO, c.name( ), "dummy" ) );
        continue;
      } else {
        try {
          LOG.info( EventRecord.here( ServiceVerifyBootstrapper.class, EventType.COMPONENT_INFO, c.name( ), c.getUri( ).toASCIIString( ) ) );
          Component comp = Components.lookup( c );
          if( eucalyptus.getLifecycle( ).isLocal( ) ) {
            
          } else {
            
          }
          eucalyptus.getLifecycle( ).setHost( db.getLifecycle( ).getHost( ) );
        } catch ( NoSuchElementException e ) {
          throw Exceptions.uncatchable( "Failed to lookup required component: " + c.name( ) );
        }
      }
    }
    if( !eucalyptus.getLifecycle( ).isLocal( ) ) {
      eucalyptus.getLifecycle( ).setHost( db.getLifecycle( ).getHost( ) );
      registerComponent( Components.delegate.eucalyptus, new RemoteConfiguration( Components.delegate.eucalyptus, eucalyptus.getLifecycle( ).getUri( ) ) );
      Components.delegate.jetty.setHostAddress( Components.delegate.db.getHostAddress( ) );
      registerComponent( Components.delegate.jetty, new RemoteConfiguration( Components.delegate.jetty, Components.delegate.jetty.getUri( ) ) );
      Components.delegate.cluster.setHostAddress( Components.delegate.db.getHostAddress( ) );
      registerComponent( Components.delegate.cluster, new RemoteConfiguration( Components.delegate.cluster, Components.delegate.cluster.getUri( ) ) );
      Components.delegate.dns.setHostAddress( Components.delegate.db.getHostAddress( ) );
      registerComponent( Components.delegate.dns, new RemoteConfiguration( Components.delegate.dns, Components.delegate.dns.getUri( ) ) );
    } else if( Components.delegate.eucalyptus.isLocal( ) ) {
      try {
        registerLocalComponent( Components.delegate.db );
        Components.delegate.db.setHostAddress( "127.0.0.1" ); //reset this afterwards due to brain damages.
        System.setProperty( "euca.db.url", Components.delegate.db.getUri( ).toASCIIString( ) );
        registerLocalComponent( Components.delegate.dns );
        registerLocalComponent( Components.delegate.eucalyptus );
        registerLocalComponent( Components.delegate.cluster );
        registerLocalComponent( Components.delegate.jetty );
      } catch ( Exception e ) {
        LOG.fatal( e, e );
        return false;
      }      
    }

    if( !Components.delegate.walrus.isEnabled( ) || !Components.delegate.walrus.isLocal( ) ) {
      List<WalrusConfiguration> walri = Configuration.getWalrusConfigurations( );
      for( WalrusConfiguration w : walri ) {
        try {
          if( NetworkUtil.testLocal( w.getHostName( ) )) {
            Components.delegate.walrus.markLocal( );
            registerLocalComponent( Components.delegate.walrus );
            break;
          } else {
            Components.delegate.walrus.setHostAddress( w.getHostName( ) );
            registerComponent( Components.delegate.walrus, w );
            break;
          }
        } catch ( Exception e ) {
          LOG.error( "Failed to create walrus service proxy: " + e );
        }
        break;
      }
    } else {
      registerLocalComponent( Components.delegate.walrus );
    }

    List<StorageControllerConfiguration> scs = Configuration.getStorageControllerConfigurations( );
    boolean hasLocal = false;
    for( StorageControllerConfiguration sc : scs ) {
      try {
        if( NetworkUtil.testLocal( sc.getHostName( ) )) { 
          hasLocal = true;
        } else {
          registerComponent( Components.delegate.storage, sc );
        }
      } catch ( Exception e ) {
        LOG.error( "Failed to create storage controller "+sc.getName( )+" service proxy: " + e );
      }
      if( hasLocal ) {
        Components.delegate.storage.markLocal( );
        System.setProperty( "euca.storage.name", sc.getName( ) );
        LOG.info(LogUtil.subheader( "Setting euca.storage.name="+sc.getName( ) + " for: " + LogUtil.dumpObject( sc ) ));
        registerLocalComponent( Components.delegate.storage );
        hasLocal = false;
      }
    }
    return true;
  }

  
  
  private void registerLocalComponent( com.eucalyptus.bootstrap.Component component ) throws EventVetoedException {
    ListenerRegistry.getInstance( ).fireEvent( component, StartComponentEvent.getLocal( component ) );
  }

  private void registerLocalComponent( ComponentConfiguration componentConfiguration ) throws EventVetoedException {
    ListenerRegistry.getInstance( ).fireEvent( componentConfiguration.getComponent( ), StartComponentEvent.getLocal( componentConfiguration ));
  }
  
  private void registerComponent( com.eucalyptus.bootstrap.Component component, ComponentConfiguration componentConfiguration ) throws Exception {
    ListenerRegistry.getInstance( ).fireEvent( componentConfiguration.getComponent( ), StartComponentEvent.getRemote( componentConfiguration ) );
  }

  @Override
  public boolean start( ) throws Exception {
    return true;
  }

}
