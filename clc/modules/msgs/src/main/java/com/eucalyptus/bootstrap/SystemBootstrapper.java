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
package com.eucalyptus.bootstrap;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.mule.api.MuleContext;

import com.eucalyptus.util.DebugUtil;
import com.eucalyptus.util.GroovyUtil;
import com.eucalyptus.util.LogUtil;
import com.google.common.collect.Lists;

import edu.emory.mathcs.backport.java.util.Arrays;

public class SystemBootstrapper {
  private static Logger             LOG = Logger.getLogger( SystemBootstrapper.class );
  private static SystemBootstrapper singleton;
  private static ThreadGroup singletonGroup;
  private static DatabaseBootstrapper singletonDb;
  public static DatabaseBootstrapper getDatabaseBootstrapper() {
    return singletonDb;
  }
  public static void setDatabaseBootstrapper( DatabaseBootstrapper dbBootstrapper ) {
    singletonDb = dbBootstrapper;
  } 
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
  public static ThreadGroup getThreadGroup() {
    synchronized ( SystemBootstrapper.class ) {
      if ( singletonGroup == null ) {
        singletonGroup = new EucalyptusThreadGroup( );
        LOG.info( "Creating Bootstrapper instance." );
      } else {
        LOG.info( "Returning Bootstrapper instance." );
      }
    }
    return singletonGroup;
  }
  public static Thread makeSystemThread( Runnable r ) {
    return new Thread( getThreadGroup( ), r );
  }
  static class EucalyptusThreadGroup extends ThreadGroup {
    EucalyptusThreadGroup( ) {
      super( "Eucalyptus" );
    }    
  }
  
  public SystemBootstrapper( ) {
  }

  public boolean destroy( ) {
    return true;
  }

  public boolean stop( ) throws Exception {
    ServiceBootstrapper.getInstance( ).stop( );
    return true;
  }

  public boolean init( ) throws Exception {
    boolean doTrace = "TRACE".equals( System.getProperty( "euca.log.level" ) );
    boolean doDebug = "DEBUG".equals( System.getProperty( "euca.log.level" ) ) || doTrace;
    LOG.info( LogUtil.subheader( "Starting system with debugging set as: " + doDebug ) );
    DebugUtil.DEBUG = doDebug;
    DebugUtil.TRACE = doDebug;
    try {
      LOG.info( LogUtil.header( "Initializing resource providers." ) );
      BootstrapFactory.initResourceProviders( );
      LOG.info( LogUtil.header( "Initializing configuration resources." ) );
      BootstrapFactory.initConfigurationResources( );
      LOG.info( LogUtil.header( "Initializing bootstrappers." ) );
      BootstrapFactory.initBootstrappers( );
      return true;
    } catch ( Throwable e ) {
      LOG.fatal( e, e );
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
    try {
      for ( Resource r : Resource.values( ) ) {
        if ( r.getBootstrappers( ).isEmpty( ) ) {
          LOG.info( "Skipping " + r + "... nothing to do." );
        } else {
          LOG.info( LogUtil.header( "Loading " + r ) );
        }
        for ( Bootstrapper b : r.getBootstrappers( ) ) {
          if( !Bootstrapper.delayedDependsCheck( b ) ) {
            LOG.info( "-X Skipping load since depends check failed: " + b.getClass( ) );            
          } else {
            try {
              LOG.info( "-> load: " + b.getClass( ) );
              boolean result = b.load( r );
            } catch ( Exception e ) {
              LOG.error( b.getClass( ).getSimpleName( ) + " threw an error in load( ): " + e.getMessage( ), e );
              return false;
            }
          }
        }
      }
    } catch ( Throwable e ) {
      LOG.info( e, e );
    }
    return true;
  }

  public boolean start( ) throws Exception {
    try {
      for ( Resource r : Resource.values( ) ) {
        if ( r.getBootstrappers( ).isEmpty( ) ) {
          LOG.info( "Skipping " + r + "... nothing to do." );
        } else {
          LOG.info( LogUtil.header( "Starting " + r ) );
        }
        for ( Bootstrapper b : r.getBootstrappers( ) ) {
          if( !Bootstrapper.delayedDependsCheck( b ) ) {
            LOG.info( "-X Skipping start since depends check failed: " + b.getClass( ) );            
          } else {
            try {
              LOG.info( "-> start: " + b.getClass( ) );
              boolean result = b.start( );
            } catch ( Exception e ) {
              LOG.error( b.getClass( ).getSimpleName( ) + " threw an error in start( ): " + e.getMessage( ), e );
              System.exit( 123 );
            }
          }
        }
      }
    } catch ( Throwable e ) {
      LOG.info( e, e );
      System.exit( 123 );
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
  
  public static void main( String[] args ) throws Exception {
    SystemBootstrapper b = SystemBootstrapper.getInstance( );
    b.init( );
    b.load( );
    b.start( );
  }
}
