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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
package com.eucalyptus.bootstrap;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.mule.api.MuleContext;

import com.eucalyptus.util.LogUtils;
import com.google.common.collect.Lists;

import edu.emory.mathcs.backport.java.util.Arrays;

public class SystemBootstrapper {
  private static Logger             LOG = Logger.getLogger( SystemBootstrapper.class );
  private static SystemBootstrapper singleton;

  public static SystemBootstrapper getInstance( ) {
    synchronized ( SystemBootstrapper.class ) {
      if ( singleton == null ) {
        singleton = new SystemBootstrapper( );
        LOG.info( "Creating Bootstrapper instance." );
      } else {
        LOG.info( "Returning Bootstrapper instance." );
      }
    }
    return singleton;
  }

  private MuleContext context;

  public SystemBootstrapper( ) {
  }

  public boolean destroy( ) {
    return true;
  }

  public boolean stop( ) throws Exception {
    this.context.stop( );
    return true;
  }

  public boolean init( ) throws Exception {
    Component hi = Component.valueOf( "bootstrap" );
    LOG.info( hi );
    try {
      LOG.info( LogUtils.header( "Initializing resource providers." ) );
      BootstrapFactory.initResourceProviders( );
      LOG.info( LogUtils.header( "Initializing configuration resources." ) );
      BootstrapFactory.initConfigurationResources( );
      LOG.info( LogUtils.header( "Initializing bootstrappers." ) );
      BootstrapFactory.initBootstrappers( );
      return true;
    } catch ( Exception e ) {
      LOG.info( e, e );
      return false;
    }
  }

  /*
   * bind privileged ports
   * generate/waitfor credentials
   * start database server
   * configure db/load bootstrap stack & wait for dbconfig
   * TODO: discovery persistence contexts
   * TODO: determine the role of this component
   * TODO: depends callbacks
   * TODO: remote config
   * TODO: bootstrap bindings
   */
  public boolean load( ) throws Exception {
    for ( Resource r : Resource.values( ) ) {
      if ( r.getBootstrappers( ).isEmpty( ) ) {
        LOG.info( "Skipping " + r + "... nothing to do." );
      } else {
        LOG.info( LogUtils.header( "Loading " + r ) );
      }
      for ( Bootstrapper b : r.getBootstrappers( ) ) {
        try {
          LOG.info( "-> load: " + b.getClass( ) );
          boolean result = b.load( r );
        } catch ( Exception e ) {
          LOG.error( b.getClass( ).getSimpleName( ) + " threw an error in load( ): " + e.getMessage( ), e );
          return false;
        }
      }
    }
    return true;
  }

  public boolean start( ) throws Exception {
    for ( Resource r : Resource.values( ) ) {
      if ( r.getBootstrappers( ).isEmpty( ) ) {
        LOG.info( "Skipping " + r + "... nothing to do." );
      } else {
        LOG.info( LogUtils.header( "Starting " + r ) );
      }
      for ( Bootstrapper b : r.getBootstrappers( ) ) {
        try {
          LOG.info( "-> start: " + b.getClass( ) );
          boolean result = b.start( );
        } catch ( Exception e ) {
          LOG.error( b.getClass( ).getSimpleName( ) + " threw an error in start( ): " + e.getMessage( ), e );
          return false;
        }
      }
    }
    return true;
  }

  public String getVersion( ) {
    return System.getProperty( "euca.version" );
  }

  public boolean check( ) {
    return true;
  }

  private static native void shutdown( boolean reload );

  public static native void hello( );
}
