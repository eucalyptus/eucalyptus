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

import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import org.hsqldb.Server;
import org.hsqldb.persist.HsqlProperties;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.scripting.ScriptExecutionFailedException;
import com.eucalyptus.scripting.groovy.GroovyUtil;
import com.eucalyptus.system.Threads;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

@Provides(Component.db)
@RunDuring(Bootstrap.Stage.DatabaseInit)
@DependsLocal(Component.eucalyptus)
public class LocalDatabaseBootstrapper extends Bootstrapper implements EventListener, Runnable, DatabaseBootstrapper {
  private static Logger             LOG = Logger.getLogger( LocalDatabaseBootstrapper.class );
  private static DatabaseBootstrapper singleton;

  public static DatabaseBootstrapper getInstance( ) {
    synchronized ( LocalDatabaseBootstrapper.class ) {
      if ( singleton == null ) {
        singleton = new LocalDatabaseBootstrapper( );
      }
    }
    return singleton;
  }

  private static void setDefault( String key, Object value ) {
    if( System.getProperties( ).containsKey( key ) )  System.setProperty( key, value.toString() );
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
  public boolean load( Stage current ) throws Exception {
    try {
      LOG.debug( "Initializing SSL just in case: " + ClassLoader.getSystemClassLoader().loadClass( "com.eucalyptus.auth.util.SslSetup" ) );
    } catch ( Throwable t ) {}
    try {
      LOG.debug( "Initializing db password: " + ClassLoader.getSystemClassLoader().loadClass( "com.eucalyptus.auth.util.Hashes" ) );
    } catch ( Throwable t ) {}
    createDatabase( );
    return true;
  }

  public boolean isRunning( ) {
    try {
      if ( this.db != null ) this.db.checkRunning( true );
      return true;
    } catch ( RuntimeException e ) {
      return false;
    }
  }

  private static AtomicBoolean hup = new AtomicBoolean( false );
  public void hup( ) {
    if( hup.compareAndSet( false, true ) ) {
      try {
        if ( this.db != null ) { 
          this.db.checkRunning( true );
          this.db.start( );
        }
      } catch ( RuntimeException e ) {
        this.db.stop( );
        this.createDatabase( );
        this.waitForDatabase( );        
      }
    } else {
      LOG.info("Already scheduled a database restart.");
    }
  }

  private void createDatabase( ) {    
    try {
      GroovyUtil.evaluateScript( "before_database.groovy" );//TODO: move this ASAP!
    } catch ( ScriptExecutionFailedException e ) {
      LOG.fatal( e, e );
      LOG.fatal( "Failed to initialize the database layer." );
      System.exit( -1 );
    }
    this.db = new Server( );
    this.db.setProperties( new HsqlProperties( DatabaseConfig.getProperties( ) ) );
    Threads.newThread( this ).start( );
    try {
      GroovyUtil.evaluateScript( "after_database.groovy" );//TODO: move this ASAP!
    } catch ( ScriptExecutionFailedException e ) {
      LOG.fatal( e, e );
      LOG.fatal( "Failed to initialize the persistence layer." );
      System.exit( -1 );
    }
  }

  @Override
  public boolean start( ) throws Exception {
    this.waitForDatabase( );
    try {
      GroovyUtil.evaluateScript( "after_persistence.groovy" );//TODO: move this ASAP!
    } catch ( ScriptExecutionFailedException e ) {
      LOG.fatal( e, e );
      LOG.fatal( "Failed to initialize the persistence layer." );
      System.exit( -1 );
    }
    return true;
  }

  private void waitForDatabase( ) {
    while ( this.db.getState( ) != 1 ) {
      Throwable t = this.db.getServerError( );
      if ( t != null ) {
        LOG.error( t, t );
        throw new RuntimeException( t );
      }
      try {
        TimeUnit.SECONDS.sleep( 1 );
      } catch ( InterruptedException e ) {
        Thread.currentThread( ).interrupt( );
      }
      LOG.info( "Waiting for database to start..." );
    } 
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
        LOG.trace( "-> Ping database." );
        this.isRunning( );
      } catch ( RuntimeException e ) {
        LOG.fatal( e, e );
      }
    }
  }

}
