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

import java.lang.reflect.UndeclaredThrowableException;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ComponentRegistrationHandler;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceBuilder;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.ServiceTransitions;
import com.eucalyptus.config.ConfigurationService;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

@Provides( Empyrean.class )
@RunDuring( Bootstrap.Stage.RemoteServicesInit )
public class ServiceBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( ServiceBootstrapper.class );
  
  enum ShouldLoad implements Predicate<ServiceConfiguration> {
    INSTANCE {
      
      @Override
      public boolean apply( final ServiceConfiguration config ) {
        boolean ret = config.getComponentId( ).isAlwaysLocal( ) || config.isVmLocal( ) || Bootstrap.isCloudController( );
        LOG.debug( "ServiceBootstrapper.shouldLoad("+config.toString( )+"):" + ret );
        return ret;
      }
    };
  }
  
  @Override
  public boolean load( ) {
    ServiceBootstrapper.execute( new Predicate<ServiceConfiguration>( ) {
      
      @Override
      public boolean apply( final ServiceConfiguration config ) {
        final Component comp = config.lookupComponent( );
        LOG.debug( "load(): " + config );
        try {
          comp.loadService( config ).get( );
          return true;
        } catch ( ServiceRegistrationException ex ) {
          config.error( ex );
          return false;
        } catch ( Throwable ex ) {
          Exceptions.trace( "load(): Building service failed: " + Components.Functions.componentToString( ).apply( comp ), ex );
          config.error( ex );
          return false;
        }
      }
    } );
    return true;
  }
  
  @Override
  public boolean start( ) throws Exception {
    ServiceBootstrapper.execute( new Predicate<ServiceConfiguration>( ) {
      
      @Override
      public boolean apply( final ServiceConfiguration config ) {
        final Component comp = config.lookupComponent( );
        try {
          final CheckedListenableFuture<ServiceConfiguration> future = ServiceTransitions.transitionChain( config, Component.State.NOTREADY );
          Threads.lookup( ConfigurationService.class, ServiceBootstrapper.class ).submit( getTransitionRunnable( config, comp, future ) );
          return true;
        } catch ( Exception e ) {
          LOG.error( e, e );
          return false;
        }
      }

      private Runnable getTransitionRunnable( final ServiceConfiguration config, final Component comp, final CheckedListenableFuture<ServiceConfiguration> future ) {
        Runnable followRunner = new Runnable( ) {
          @Override
          public void run( ) {
            try {
              future.get( );
              try {
                ServiceTransitions.transitionChain( config, Component.State.ENABLED ).get( );
              } catch ( IllegalStateException ex ) {
                LOG.error( ex , ex );
              } catch ( InterruptedException ex ) {
                LOG.error( ex , ex );
              } catch ( ExecutionException ex ) {
                LOG.error( ex , ex );
              }
            } catch ( Exception ex ) {
              LOG.error( ex, ex );
            }
          }
        };
        return followRunner;
      }
    } );
    return true;
  }
  
  private static void execute( final Predicate<ServiceConfiguration> predicate ) throws NoSuchElementException {
    for ( final ComponentId compId : ComponentIds.list( ) ) {
      Component comp = Components.lookup( compId );
      if ( compId.isRegisterable( ) ) {
        ServiceBuilder<? extends ServiceConfiguration> builder = comp.getBuilder( );
        try {
          for ( ServiceConfiguration config : Iterables.filter( builder.list( ), ShouldLoad.INSTANCE ) ) {
            try {
              predicate.apply( config );
            } catch ( Throwable ex ) {
              LOG.error( ex , ex );
            }
          }
        } catch ( ServiceRegistrationException ex ) {
          LOG.error( ex, ex );
        }
      } else if ( comp.hasLocalService( ) ) {
        final ServiceConfiguration config = comp.getLocalServiceConfiguration( );
        if ( config.isVmLocal( ) || ( Bootstrap.isCloudController( ) && config.isHostLocal( ) ) ) {
          predicate.apply( config );
        }
      }
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
