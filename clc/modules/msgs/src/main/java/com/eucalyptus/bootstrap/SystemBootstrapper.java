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

import java.security.Security;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Lifecycles;
import com.eucalyptus.context.ServiceContext;
import com.eucalyptus.system.LogLevels;
import com.eucalyptus.util.LogUtil;

public class SystemBootstrapper {
  private static Logger               LOG          = Logger.getLogger( SystemBootstrapper.class );
  
  private static SystemBootstrapper   singleton;
  private static ThreadGroup          singletonGroup;

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
  
  public SystemBootstrapper( ) {}
  
  public boolean init( ) throws Exception {
    try {
      boolean doTrace = "TRACE".equals( System.getProperty( "euca.log.level" ) );
      boolean doDebug = "DEBUG".equals( System.getProperty( "euca.log.level" ) ) || doTrace;
      LOG.info( LogUtil.subheader( "Starting system with debugging set as: " + doDebug ) );
      Security.addProvider( new BouncyCastleProvider( ) );
      LogLevels.DEBUG = doDebug;
      LogLevels.TRACE = doDebug;
      System.setProperty( "euca.ws.port", "8773" );
    } catch ( Throwable t ) {
      t.printStackTrace( );
      System.exit( 1 );
    }
    try {
      Bootstrap.initialize( );
      Bootstrap.Stage stage = Bootstrap.transition( );
      stage.load();
      return true;
    } catch ( BootstrapException e ) {
      e.printStackTrace( );
      throw e;
    } catch ( Throwable t ) {
      t.printStackTrace( );
      LOG.fatal( t, t );
      System.exit( 1 );
      return false;
    }
  }

  public boolean load( ) throws Throwable {
    try {
      /** @NotNull */
      Bootstrap.Stage stage = Bootstrap.transition( );
      do {
        stage.load( );
      } while( ( stage = Bootstrap.transition( ) ) != null );
    } catch ( BootstrapException e ) {
      throw e;
    } catch ( Throwable t ) {
      t.printStackTrace( );
      LOG.fatal( t, t );
      System.exit( 1 );
      throw t;
    }
    Lifecycles.State.INITIALIZED.to( Lifecycles.State.LOADED ).transition( Components.list( ) );
    return true;
  }
    
  public boolean start( ) throws Throwable {
    try {
      /** @NotNull */
      Bootstrap.Stage stage = Bootstrap.transition( );
      do {
        stage.start( );
      } while( ( stage = Bootstrap.transition( ) ) != null );
    } catch ( BootstrapException e ) {
      e.printStackTrace( );
      throw e;
    } catch ( Throwable t ) {
      LOG.fatal( t, t );
      System.exit( 1 );
      throw t;
    }
    Lifecycles.State.LOADED.to( Lifecycles.State.STARTED ).transition( Components.list( ) );
    return true;
  }
  
  public String getVersion( ) {
    return System.getProperty( "euca.version" );
  }
  
  public boolean check( ) {
    return true;
  }

  public boolean destroy( ) {
    return true;
  }
  
  public boolean stop( ) throws Exception {
    ServiceContext.stopContext( );
    return true;
  }
  
  
  private static native void shutdown( boolean reload );
  
  public static native void hello( );
  
}
