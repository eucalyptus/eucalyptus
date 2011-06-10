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
 *******************************************************************************/
/*
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ComponentRegistrationHandler;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.DummyServiceBuilder;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.config.ConfigurationService;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.CheckedListenableFuture;

@Provides( Empyrean.class )
@RunDuring( Bootstrap.Stage.RemoteServicesInit )
public class ServiceBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( ServiceBootstrapper.class );
  
  @Override
  public boolean load( ) throws Exception {
    /**
     * TODO: ultimately remove this: it is legacy and enforces a one-to-one
     * relationship between component impls
     **/
    for ( ComponentId c : ComponentIds.list( ) ) {
      if ( c.hasDispatcher( ) && c.isAlwaysLocal( ) ) {
        try {
          Component comp = Components.lookup( c );
        } catch ( NoSuchElementException e ) {
          throw BootstrapException.throwFatal( "Failed to lookup component which is alwaysLocal: " + c.name( ), e );
        }
      } else if ( c.hasDispatcher( ) ) {
        try {
          Component comp = Components.lookup( c );
        } catch ( NoSuchElementException e ) {
          Exceptions.eat( "Failed to lookup component which may have dispatcher references: " + c.name( ), e );
        }
      }
    }
    for ( final Component comp : Components.list( ) ) {
      LOG.info( "load(): " + comp );
      if ( /** Bootstrap.isCloudController( ) && **/ !( comp.getBuilder( ) instanceof DummyServiceBuilder ) ) {
        for ( ServiceConfiguration config : comp.getBuilder( ).list( ) ) {
          LOG.info( "loadService(): " + config );
          try {
            comp.loadService( config ).get( );
          } catch ( ServiceRegistrationException ex ) {
            config.error( ex );
          } catch ( Throwable ex ) {
            config.error( ex );
          }
        }
      } else if ( comp.hasLocalService( ) ) {
        LOG.info( "load(): " + comp );
        final ServiceConfiguration s = comp.getLocalServiceConfiguration( );
        if ( s.isVmLocal( ) && comp.getComponentId( ).hasDispatcher( ) ) {
          try {
            comp.loadService( s ).get( );
          } catch ( ServiceRegistrationException ex ) {
            s.error( ex );
          } catch ( Throwable ex ) {
            Exceptions.trace( "load(): Building service failed: " + Components.Functions.componentToString( ).apply( comp ), ex );
            s.error( ex );
          }
        }
      }
    }
    return true;
  }
  
  @Override
  public boolean start( ) throws Exception {
    Component euca = Components.lookup( Eucalyptus.class );
    for ( final Component comp : Components.list( ) ) {
      LOG.info( "start(): " + comp );
      for ( final ServiceConfiguration s : comp.lookupServiceConfigurations( ) ) {
        if( comp.getComponentId( ).isAlwaysLocal( ) ) {
          ServiceBootstrapper.startupService( comp, s );
        } else if ( !comp.getComponentId( ).hasDispatcher( ) ) {
          continue;
        } else if ( s.isHostLocal( ) ) {
          comp.loadService( s ).get( );
        } else if ( Bootstrap.isCloudController( ) ) {
          ServiceBootstrapper.startupService( comp, s );
        }
      }
    }
    return true;
  }

  private static void startupService( final Component comp, final ServiceConfiguration s ) {
    try {
      comp.loadService( s ).get( );
      final CheckedListenableFuture<ServiceConfiguration> future = comp.startTransition( s );
      Runnable followRunner = new Runnable( ) {
        public void run( ) {
          try {
            future.get( );
            comp.enableTransition( s );
          } catch ( Exception ex ) {
            LOG.error( ex,
                       ex );
          }
        }
      };
      Threads.lookup( ConfigurationService.class, ComponentRegistrationHandler.class, s.getFullName( ).toString( ) ).submit( followRunner );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
  }
  
  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#enable()
   */
  @Override
  public boolean enable( ) throws Exception {
    return true;
  }
  
  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#stop()
   */
  @Override
  public boolean stop( ) throws Exception {
    return true;
  }
  
  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#destroy()
   */
  @Override
  public void destroy( ) throws Exception {}
  
  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#disable()
   */
  @Override
  public boolean disable( ) throws Exception {
    return true;
  }
  
  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#check()
   */
  @Override
  public boolean check( ) throws Exception {
    return true;
  }
}
