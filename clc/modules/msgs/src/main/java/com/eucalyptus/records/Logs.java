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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.records;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Priority;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.Configurator;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggingEvent;
import com.eucalyptus.bootstrap.SystemBootstrapper;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.system.EucaLayout;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.LogUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class Logs {
  private static Logger       LOG                     = Logger.getLogger( Logs.class );
  /**
   * <pre>
   *   <appender name="cloud-cluster" class="org.apache.log4j.RollingFileAppender">
   *     <param name="File" value="${euca.log.dir}/cloud-cluster.log" />
   *     <param name="MaxFileSize" value="10MB" />
   *     <param name="MaxBackupIndex" value="25" />
   *     <param name="Threshold" value="${euca.log.level}" />
   *     <layout class="org.apache.log4j.PatternLayout">
   *       <param name="ConversionPattern" value="%d{EEE MMM d HH:mm:ss yyyy} %5p [%c{1}:%t] %m%n" />
   *     </layout>
   *   </appender>
   *   <appender name="cloud-exhaust" class="org.apache.log4j.RollingFileAppender">
   *     <param name="File" value="${euca.log.dir}/cloud-exhaust.log" />
   *     <param name="MaxFileSize" value="10MB" />
   *     <param name="MaxBackupIndex" value="25" />
   *     <param name="Threshold" value="${euca.exhaust.level}" />
   *     <layout class="org.apache.log4j.PatternLayout">
   *       <param name="ConversionPattern" value="${euca.log.exhaust.pattern}" />
   *     </layout>
   *   </appender>
   * </pre>
   */
  
  private static final String DEFAULT_LOG_PATTERN     = "%d{EEE MMM d HH:mm:ss yyyy} %5p [%c{1}:%t] %m%n";
  private static final String DEBUG_LOG_PATTERN       = "%d{EEE MMM d HH:mm:ss yyyy} %5p [%c{1}:%t] [%C.%M(%F):%L] %m%n";
  private static final String DEFAULT_LOG_MAX_BACKUPS = "25";
  private static final String DEFAULT_LOG_MAX_SIZE    = "10MB";
  
  private enum LogProps {
    threshold,
    pattern,
    filesize,
    maxbackups;
  }
  
  private enum Appenders {
    OUTPUT,
    ERROR,
    EXHAUST,
    CLUSTER,
    DEBUG,
    BOOTSTRAP;
    private final String  prop;
    private final String  threshold;
    private final String  pattern;
    private final String  fileSize;
    private final Integer backups;
    private final String  fileName;
    private Appender      appender;
    
    Appenders( ) {
      this.prop = "euca.log." + this.name( ).toLowerCase( ) + ".";
      this.threshold = this.getProperty( LogProps.threshold, LOG_LEVEL.get( ).level( ) ).toUpperCase( );
      this.pattern = this.getProperty( LogProps.pattern, DEFAULT_LOG_PATTERN );
      this.backups = Integer.parseInt( this.getProperty( LogProps.maxbackups, DEFAULT_LOG_MAX_BACKUPS ) );
      this.fileSize = this.getProperty( LogProps.filesize, DEFAULT_LOG_MAX_SIZE );
      this.fileName = BaseDirectory.LOG.getChildPath( this.getAppenderName( ) + ".log" );
    }
    
    private String getProperty( final LogProps p, final String defaultValue ) {
      return ( System.getProperty( this.prop + p.name( ) ) == null )
                                                                    ? defaultValue
                                                                    : System.getProperty( this.prop + p.name( ) );
    }
    
    public String getAppenderName( ) {
      return "cloud-" + Appenders.this.name( ).toLowerCase( );
    }
    
    public Appender getAppender( ) throws IOException {
      return ( this.appender = ( this.appender != null )
                                                        ? this.appender
                                                        : new RollingFileAppender( new PatternLayout( this.pattern ), this.fileName, true ) {
                                                          {
                                                            this.setImmediateFlush( false );
                                                            this.setMaxBackupIndex( Appenders.this.backups );
                                                            this.setMaxFileSize( Appenders.this.fileSize );
                                                            this.setName( Appenders.this.getAppenderName( ) );
                                                            this.setThreshold( Priority.toPriority( Appenders.this.threshold ) );
//            setBufferedIO( true );
//            setBufferSize( 1024 );
                                                          }
                                                        } );
    }
  }
  
  public static class LogConfigurator implements Configurator {
    private static final ConsoleAppender console = new ConsoleAppender( new EucaLayout( ), "System.out" ) {
                                                   {
                                                     this.setThreshold( Priority.toPriority( LOG_LEVEL.get( ).level( ), Priority.INFO ) );
                                                     this.setName( "console" );
                                                     this.setImmediateFlush( false );
                                                     this.setFollow( false );
                                                   }
                                                 };
    
    @Override
    public void doConfigure( final URL arg0, final LoggerRepository arg1 ) {
      arg1.getRootLogger( ).addAppender( console );
    }
    
  }
  
  private static final Logger nullLogger = new Logger( "/dev/null" ) {
                                           
                                           @Override
                                           public boolean isTraceEnabled( ) {
                                             return false;
                                           }
                                           
                                           @Override
                                           public void trace( final Object message, final Throwable t ) {}
                                           
                                           @Override
                                           public void trace( final Object message ) {}
                                           
                                           @Override
                                           public synchronized void addAppender( final Appender newAppender ) {}
                                           
                                           @Override
                                           public void assertLog( final boolean assertion, final String msg ) {}
                                           
                                           @Override
                                           public void callAppenders( final LoggingEvent arg0 ) {}
                                           
                                           @Override
                                           public void debug( final Object message, final Throwable t ) {}
                                           
                                           @Override
                                           public void debug( final Object message ) {}
                                           
                                           @Override
                                           public void error( final Object message, final Throwable t ) {}
                                           
                                           @Override
                                           public void error( final Object message ) {}
                                           
                                           @Override
                                           public void fatal( final Object message, final Throwable t ) {}
                                           
                                           @Override
                                           public void fatal( final Object message ) {}
                                           
                                           @Override
                                           protected void forcedLog( final String fqcn, final Priority level, final Object message, final Throwable t ) {}
                                           
                                           @Override
                                           public boolean getAdditivity( ) {
                                             return false;
                                           }
                                           
                                           @Override
                                           public synchronized Enumeration getAllAppenders( ) {
                                             return super.getAllAppenders( );
                                           }
                                           
                                           @Override
                                           public synchronized Appender getAppender( final String name ) {
                                             return super.getAppender( name );
                                           }
                                           
                                           @Override
                                           public Priority getChainedPriority( ) {
                                             return super.getChainedPriority( );
                                           }
                                           
                                           @Override
                                           public Level getEffectiveLevel( ) {
                                             return super.getEffectiveLevel( );
                                           }
                                           
                                           @Override
                                           public LoggerRepository getHierarchy( ) {
                                             return super.getHierarchy( );
                                           }
                                           
                                           @Override
                                           public LoggerRepository getLoggerRepository( ) {
                                             return super.getLoggerRepository( );
                                           }
                                           
                                           @Override
                                           public ResourceBundle getResourceBundle( ) {
                                             return super.getResourceBundle( );
                                           }
                                           
                                           @Override
                                           protected String getResourceBundleString( final String arg0 ) {
                                             return super.getResourceBundleString( arg0 );
                                           }
                                           
                                           @Override
                                           public void info( final Object message, final Throwable t ) {}
                                           
                                           @Override
                                           public void info( final Object message ) {}
                                           
                                           @Override
                                           public boolean isAttached( final Appender appender ) {
                                             return super.isAttached( appender );
                                           }
                                           
                                           @Override
                                           public boolean isDebugEnabled( ) {
                                             return super.isDebugEnabled( );
                                           }
                                           
                                           @Override
                                           public boolean isEnabledFor( final Priority level ) {
                                             return super.isEnabledFor( level );
                                           }
                                           
                                           @Override
                                           public boolean isInfoEnabled( ) {
                                             return super.isInfoEnabled( );
                                           }
                                           
                                           @Override
                                           public void l7dlog( final Priority arg0, final String arg1, final Object[] arg2, final Throwable arg3 ) {}
                                           
                                           @Override
                                           public void l7dlog( final Priority arg0, final String arg1, final Throwable arg2 ) {}
                                           
                                           @Override
                                           public void log( final Priority priority, final Object message, final Throwable t ) {}
                                           
                                           @Override
                                           public void log( final Priority priority, final Object message ) {}
                                           
                                           @Override
                                           public void log( final String callerFQCN, final Priority level, final Object message, final Throwable t ) {}
                                           
                                           @Override
                                           public synchronized void removeAllAppenders( ) {}
                                           
                                           @Override
                                           public synchronized void removeAppender( final Appender appender ) {}
                                           
                                           @Override
                                           public synchronized void removeAppender( final String name ) {}
                                           
                                           @Override
                                           public void setAdditivity( final boolean additive ) {}
                                           
                                           @Override
                                           public void setLevel( final Level level ) {}
                                           
                                           @Override
                                           public void setPriority( final Priority priority ) {}
                                           
                                           @Override
                                           public void setResourceBundle( final ResourceBundle bundle ) {}
                                           
                                           @Override
                                           public void warn( final Object message, final Throwable t ) {}
                                           
                                           @Override
                                           public void warn( final Object message ) {}
                                           
                                         };
  
  public static Logger extreme( ) {
    return LogLevel.EXTREME.logger( );
  }
  
  public static Logger exhaust( ) {
    return LogLevel.EXHAUST.logger( );
  }
  
  public static Logger bootstrap( ) {
    return Logger.getLogger( "BOOTSTRAP" );
  }
  
  private static class LoggingOutputStream extends OutputStream {
    private static final int DEFAULT_BUFFER_LENGTH = 2048;
    private boolean          hasBeenClosed         = false;
    private byte[]           buf;
    private int              count;
    private int              curBufLength;
    private final Logger     log;
    private final Level      level;
    
    LoggingOutputStream( final Logger log, final Level level ) throws IllegalArgumentException {
      if ( ( log == null ) || ( level == null ) ) {
        throw new IllegalArgumentException( "Logger or log level must be not null" );
      }
      this.log = log;
      this.level = level;
      this.curBufLength = DEFAULT_BUFFER_LENGTH;
      this.buf = new byte[this.curBufLength];
      this.count = 0;
    }
    
    public void write( final int b ) throws IOException {
      if ( this.hasBeenClosed ) {
        throw new IOException( "The stream has been closed." );
      }
      // don't log nulls
      if ( b == 0 ) {
        return;
      }
      // would this be writing past the buffer?
      if ( this.count == this.curBufLength ) {
        // grow the buffer
        final int newBufLength = this.curBufLength +
                                 DEFAULT_BUFFER_LENGTH;
        final byte[] newBuf = new byte[newBufLength];
        System.arraycopy( this.buf, 0, newBuf, 0, this.curBufLength );
        this.buf = newBuf;
        this.curBufLength = newBufLength;
      }
      
      this.buf[this.count] = ( byte ) b;
      this.count++;
    }
    
    public void flush( ) {
      if ( this.count == 0 ) {
        return;
      }
      final byte[] bytes = new byte[this.count];
      System.arraycopy( this.buf, 0, bytes, 0, this.count );
      final String str = new String( bytes );
      this.log.log( this.level, str );
      this.count = 0;
    }
    
    public void close( ) {
      this.flush( );
      this.hasBeenClosed = true;
    }
  }
  
  public static void init( ) {
    logLevel.get( );
    //    System.setProperty( "log4j.configurationClass", "com.eucalyptus.util.Logs.LogConfigurator" );
    try {
      final Logger stdLogger = ( Logs.isExtrrreeeme( ) ? Logger.getLogger( SystemBootstrapper.class ) : Logs.extreme( ) );
      final PrintStream oldOut = System.out;
      final PrintStream oldErr = System.err;
      if ( !System.getProperty( "euca.log.appender", "" ).equals( "console" ) ) {
        System.setOut( new PrintStream( new LoggingOutputStream( stdLogger, Level.INFO ) ) );
        System.setErr( new PrintStream( new LoggingOutputStream( stdLogger, Level.ERROR ) ) );
      }
      Logger.getRootLogger( ).info( LogUtil.subheader( "Starting system with debugging set as: " + Logs.logLevel.get( ) ) );
    } catch ( final Exception t ) {
      t.printStackTrace( );
      System.exit( 1 );//GRZE: special case, can't open log files, hosed
    }
  }
  
  private static LogLevel LOG_LEVEL = LogLevel.INFO;
  
  enum LogLevel implements Callable<Boolean> {
    EXHAUST {
      @Override
      String level( ) {
        return TRACE.name( );
      }
      
      @Override
      String exhaustLevel( ) {
        return this.level( );
      }
    },
    EXTREME {
      
      @Override
      String level( ) {
        return TRACE.name( );
      }
      
      @Override
      String exhaustLevel( ) {
        return this.level( );
      }
      
      @Override
      public String pattern( ) {
        return DEBUG_LOG_PATTERN;
      }
      
    },
    TRACE {
      @Override
      public String pattern( ) {
        return DEBUG_LOG_PATTERN;
      }
      
    },
    DEBUG {
      @Override
      public String pattern( ) {
        return DEBUG_LOG_PATTERN;
      }
    },
    INFO,
    WARN,
    ERROR,
    FATAL;
    private static final String PROP_LOG_PATTERN = "euca.log.pattern";
    private static final String PROP_LOG_LEVEL   = "euca.log.level";
    private final Logger        logger;
    
    /**
     * 
     */
    LogLevel( ) {
      this.logger = Logger.getLogger( this.name( ) );
    }
    
    @Override
    public Boolean call( ) {
      return logLevel.get( ).ordinal( ) <= this.ordinal( );
    }
    
    public Logger logger( ) {
      if ( this.call( ) ) {
        return this.logger;
      } else {
        return nullLogger;
      }
    }
    
    private LogLevel init( ) {
      System.setProperty( PROP_LOG_LEVEL, this.level( ) );
      System.setProperty( PROP_LOG_PATTERN, this.pattern( ) );
      System.setProperty( "euca.exhaust.level", this.exhaustLevel( ) );
      System.setProperty( "euca.log.exhaustive", this.exhaustLevel( ) );
      System.setProperty( "euca.log.exhaustive.cc", this.exhaustLevel( ) );
      System.setProperty( "euca.log.exhaustive.user", this.exhaustLevel( ) );
      System.setProperty( "euca.log.exhaustive.db", this.exhaustLevel( ) );
      System.setProperty( "euca.log.exhaustive.external", this.exhaustLevel( ) );
      System.setProperty( "euca.log.exhaustive.user", this.exhaustLevel( ) );
      return this;
    }
    
    public String pattern( ) {
      return DEFAULT_LOG_PATTERN;
    }
    
    String level( ) {
      return this.name( );
    }
    
    String exhaustLevel( ) {
      return this.ordinal( ) <= TRACE.ordinal( ) ? TRACE.name( )
                                                : FATAL.name( );
    }
    
    static LogLevel get( ) {
      try {
        return LogLevel.valueOf( System.getProperty( PROP_LOG_LEVEL ).toUpperCase( ) ).init( );
      } catch ( final IllegalArgumentException ex ) {
        if ( EXTREME.name( ).equals( System.getProperty( PROP_LOG_LEVEL ).toUpperCase( ) ) ) {
          return EXTREME.init( );
        } else {
          throw ex;
        }
      } catch ( final NullPointerException ex ) {
        return LogLevel.INFO.init( );
      }
    }
  }
  
  private static final Supplier<LogLevel> logLevel = Suppliers.memoize( new Supplier<LogLevel>( ) {
                                                     
                                                     @Override
                                                     public LogLevel get( ) {
                                                       return LogLevel.get( );
                                                     }
                                                   } );
  
  public static boolean isExtrrreeeme( ) {
    return LogLevel.EXTREME.call( );
  }
  
  public static boolean isDebug( ) {
    return LogLevel.DEBUG.call( );
  }
  
  public static boolean isTrace( ) {
    return LogLevel.TRACE.call( );
  }
  
  public static String dump( final Object o ) {
    String ret = null;
    if ( ( ret = groovyDump( o ) ) != null ) {
      return ret;
    } else if ( ( ret = groovyInspect( o ) ) != null ) {
      return ret;
    } else {
      return ( o == null
                        ? Threads.currentStackFrame( 1 ) + ": null"
                        : "" + o );
    }
  }
  
  public static String groovyDump( final Object o ) {
    final HashMap ctx = new HashMap( ) {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;
      
      {
        this.put( "o", o );
      }
    };
    try {
      return ""
             + Groovyness.eval( "try {return o.dump()" +
                                ".replaceAll(\"<\",\"[\")" +
                                ".replaceAll(\">\",\"]\")" +
                                ".replaceAll(\"[\\\\w\\\\.]+\\\\.(\\\\w+)@\\\\w*\", { Object[] it -> it[1] })" +
                                ".replaceAll(\"class:class [\\\\w\\\\.]+\\\\.(\\\\w+),\", { Object[] it -> it[1] });" +
                                "} catch( Exception e ) {return \"\"+o;}", ctx );
    } catch ( final Exception ex ) {
      LOG.error( ex, ex );
      return null;
    }
  }
  
  public static String groovyInspect( final Object o ) {
    final HashMap ctx = new HashMap( ) {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;
      
      {
        this.put( "o", o );
      }
    };
    try {
      return "" + Groovyness.eval( "try{return o.inspect();}catch(Exception e){return \"\"+o;}", ctx );
    } catch ( final Exception ex ) {
      LOG.error( ex, ex );
      return null;
    }
  }
}
