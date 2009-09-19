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
package com.eucalyptus.bootstrap;

import org.apache.log4j.Logger;
import org.hsqldb.Server;
import org.hsqldb.ServerConstants;
import org.hsqldb.persist.HsqlProperties;

import com.eucalyptus.auth.util.SslSetup;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.util.GroovyUtil;
import com.eucalyptus.util.LogUtil;

@Provides( resource = Resource.Database )
@Depends( resources = Resource.SystemCredentials, local = Component.eucalyptus )
public class LocalDatabaseBootstrapper extends Bootstrapper implements EventListener, Runnable, DatabaseBootstrapper {
  private static Logger             LOG = Logger.getLogger( LocalDatabaseBootstrapper.class );
  private static DatabaseBootstrapper singleton;

  public static DatabaseBootstrapper getInstance( ) {
    synchronized ( LocalDatabaseBootstrapper.class ) {
      if ( singleton == null ) {
        singleton = new LocalDatabaseBootstrapper( );
        SystemBootstrapper.setDatabaseBootstrapper( singleton );
      }
    }
    return singleton;
  }

  private Server db;
  private String fileName;

  private LocalDatabaseBootstrapper( ) {
    ListenerRegistry.getInstance( ).register( ClockTick.class, this );
  }

  @Override
  public boolean check( ) {
    return false;
  }

  @Override
  public boolean destroy( ) throws Exception {
    return false;
  }

  @Override
  public boolean load( Resource current ) throws Exception {
    LOG.debug( "Initializing SSL just in case: " + SslSetup.class );
    this.db = new Server( );
    DatabaseConfig.initialize( );
    this.db.setProperties( new HsqlProperties( DatabaseConfig.getProperties( ) ) );
    SystemBootstrapper.makeSystemThread( this ).start( );
    return true;
    // props.setProperty( ServerConstants.SC_KEY_DATABASE + ".0",
    // SubDirectory.DB.toString( ) + File.separator + general );
    // props.setProperty( ServerConstants.SC_KEY_DBNAME + ".0", general );
    // props.setProperty( ServerConstants.SC_KEY_DATABASE + ".1",
    // SubDirectory.DB.toString( ) + File.separator + vol );
    // props.setProperty( ServerConstants.SC_KEY_DBNAME + ".1", vol );
    // props.setProperty( ServerConstants.SC_KEY_DATABASE + ".2",
    // SubDirectory.DB.toString( ) + File.separator + auth );
    // props.setProperty( ServerConstants.SC_KEY_DBNAME + ".2", auth );
    // props.setProperty( ServerConstants.SC_KEY_DATABASE + ".3",
    // SubDirectory.DB.toString( ) + File.separator + config );
    // props.setProperty( ServerConstants.SC_KEY_DBNAME + ".3", config );
    // props.setProperty( ServerConstants.SC_KEY_DATABASE + ".4",
    // SubDirectory.DB.toString( ) + File.separator + walrus );
    // props.setProperty( ServerConstants.SC_KEY_DBNAME + ".4", walrus );
    // props.setProperty( ServerConstants.SC_KEY_DATABASE + ".5",
    // SubDirectory.DB.toString( ) + File.separator + storage );
    // props.setProperty( ServerConstants.SC_KEY_DBNAME + ".5", storage );
    // props.setProperty( ServerConstants.SC_KEY_DATABASE + ".6",
    // SubDirectory.DB.toString( ) + File.separator + dns );
    // props.setProperty( ServerConstants.SC_KEY_DBNAME + ".6", dns );
  }

  public boolean isRunning( ) {
    try {
      if ( this.db != null ) this.db.checkRunning( true );
      return true;
    } catch ( RuntimeException e ) {
      return false;
    }
  }

  @Override
  public boolean start( ) throws Exception {
    while ( this.db.getState( ) != 1 ) {
      Throwable t = this.db.getServerError( );
      if ( t != null ) {
        LOG.error( t, t );
        throw new RuntimeException( t );
      }
      LOG.info( "Waiting for database to start..." );
    }
    try {
      GroovyUtil.evaluateScript( "startup.groovy" );
    } catch ( Exception e ) {
      LOG.fatal( e, e );
      System.exit( -1 );
    }
    return true;
  }

  public void run( ) {
    this.db.start( );
  }

  @Override
  public boolean stop( ) {
    return false;
  }

  public String getFileName( ) {
    return fileName;
  }

  public void setFileName( String fileName ) {
    LOG.info( "Setting hsqldb filename=" + fileName );
    this.fileName = fileName;
  }

  @Override
  public void advertiseEvent( Event event ) {}

  @Override
  public void fireEvent( Event event ) {
    if( event instanceof ClockTick && Component.eucalyptus.isLocal( ) ) {
      try {
        LOG.debug( "-> Ping database." );
        this.isRunning( );
      } catch ( RuntimeException e ) {
        LOG.info( "-> Ping database." );
        LOG.fatal( e, e );
        System.exit( 123 );
      }
    }
  }

}
