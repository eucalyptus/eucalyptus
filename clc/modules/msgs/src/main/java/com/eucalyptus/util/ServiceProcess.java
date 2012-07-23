/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.Threads;
import com.google.common.collect.Lists;

public class ServiceProcess implements Runnable {
  private static Logger LOG = Logger.getLogger( ServiceProcess.class );
  private Threads.ThreadPool threads;
  private Process     self;
  private File        pwd;
  private Integer     returnCode;
  private String[]    args;
  private String[]    envp;
  private String      name;
  private PrintStream out, err;
  private List        listeners = Lists.newArrayList( );
  
  public static ServiceProcess exec( String name, File pwd, String[] args, String[] envp ) {
    return new ServiceProcess( name, pwd, args, envp ).exec();
  }
  private ServiceProcess( String name, File pwd, String[] args, String[] envp ) {
    super( );
    this.threads = Threads.lookup( Empyrean.class, ServiceProcess.class, name );
    this.pwd = pwd;
    this.returnCode = null;
    this.args = args;
    this.envp = envp;
    this.name = name;
    this.out = System.out;
    this.err = System.err;
  }
  
  public void kill( ) {
    if( this.self != null ) {
      this.self.destroy( );
    }
  }
  
  public ServiceProcess exec() {
    this.threads.submit( this );
    return this;
  }

  public void run( ) {
    if ( this.self != null ) {
      throw new RuntimeException( "Process already running." );
    }
    this.returnCode = null;
    try {
      this.self = Runtime.getRuntime( ).exec( this.args, this.envp, this.pwd );
      this.threads.submit( new IOMonitor( this.self.getInputStream( ) ) );
      this.threads.submit( new IOMonitor( this.self.getErrorStream( ) ) );
      try {
        this.returnCode = this.self.waitFor( );
      } catch ( InterruptedException e ) {
        Thread.currentThread( ).interrupt( );
        LOG.debug( e, e );
      }
    } catch ( Exception e ) {
      LOG.error( e, e );
      this.threads.getExecutorService( ).submit( this );
    }    
  }
  
  private static class IOMonitor implements Runnable {
    private AtomicBoolean finished = new AtomicBoolean( false );
    private BufferedReader   in;
    
    public IOMonitor( InputStream in ) {
      this.in = new BufferedReader( new InputStreamReader( in ) );
    }
    
    public boolean isFinished() {
      return this.finished.get( );
    }
    
    @Override
    public void run( ) {
      byte[] buf = new byte[1024];
      while ( !this.finished.get( ) ) {
        try {
          if( !in.ready( ) ) {
            TimeUnit.MILLISECONDS.sleep( 100 );
          }
          String line = "";
          if( ( line = in.readLine( ) ) == null ) {
            LOG.info( "I/O Stream closed for: " + in.toString( ) );
            this.finished.set( true );
            return;
          }
          LOG.debug( line );
        } catch ( InterruptedException e ) {
          Thread.currentThread( ).interrupt( );
          LOG.debug( e, e );
          this.finished.set( true );
        } catch ( Exception e ) {
          LOG.debug( e, e );
          this.finished.set( true );
        }
      }
    }
    
  }
  
}
