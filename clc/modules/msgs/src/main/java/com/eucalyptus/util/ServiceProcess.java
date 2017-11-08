/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
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
