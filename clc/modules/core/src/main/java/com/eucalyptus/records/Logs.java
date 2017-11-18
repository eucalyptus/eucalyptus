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

package com.eucalyptus.records;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.SystemBootstrapper;
import com.eucalyptus.system.log.EucaLoggingOutputStream;
import com.eucalyptus.system.log.NullEucaLogger;
import com.eucalyptus.util.LogUtil;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class Logs {
  public static final String PROP_LOG_LEVEL           = "euca.log.level";
  public static final String PROP_LOG_EXHAUST_LEVEL   = "euca.exhaust.level";
  public static final String RESOURCE_LOG4J_EXTRA     = "/eucalyptus.log.properties";

  private static final Logger nullLogger              = new NullEucaLogger();
  private static final AtomicBoolean initialized      = new AtomicBoolean( false );

  public static Logger extreme( ) {
    return LogLevel.EXTREME.logger( );
  }
  
  public static Logger exhaust( ) {
    return LogLevel.EXHAUST.logger( );
  }
  
  public static Logger bootstrap( ) {
    return Logger.getLogger( "BOOTSTRAP" );
  }
  
  public static void init( ) {
    logLevel.get( );
    if ( initialized.compareAndSet( false, true ) ) try {
      final Logger stdLogger = ( Logs.isExtrrreeeme( ) ? Logger.getLogger( SystemBootstrapper.class ) : Logs.extreme( ) );
      if ( !System.getProperty( "euca.log.appender", "" ).equals( "console" ) ) {
        System.setOut( new PrintStream( new EucaLoggingOutputStream( stdLogger, Level.INFO ), true ) );
        System.setErr( new PrintStream( new EucaLoggingOutputStream( stdLogger, Level.ERROR ), true ) );
      }
      Logger.getRootLogger( ).info( LogUtil.subheader( "Starting system with debugging set as: " + Logs.logLevel.get( ) ) );
      loadLevels( );
    } catch ( final Exception t ) {
      t.printStackTrace( );
      System.exit( 1 );//GRZE: special case, can't open log files, hosed
    }
  }

  /**
   * Applies levels from file, set level to NONE to clear, e.g.
   *
   * com.eucalyptus.somepackage = NONE
   * com.eucalyptus.somepackage.SomeClass = NONE
   */
  public static void loadLevels( ) {
    Logger.getRootLogger( ).info( LogUtil.subheader( "Loading log levels from classpath: " + RESOURCE_LOG4J_EXTRA ) );
    try {
      final URL loggingPropertiesUrl = Logs.class.getResource( RESOURCE_LOG4J_EXTRA );
      if ( loggingPropertiesUrl != null ) {
        try ( final InputStream loggingPropertiesIn = loggingPropertiesUrl.openStream( ) ) {
          Properties logLevelProperties = new Properties(  );
          logLevelProperties.load( loggingPropertiesIn );
          logLevelProperties.forEach( ( logger, level ) -> {
            Logger.getLogger( String.valueOf( logger ).trim( ) ).setLevel(
                Level.toLevel( String.valueOf( level ).trim( ), null )
            );
          } );
        }
      }
    } catch ( final Exception e ) {
      Logger.getRootLogger( ).error( "Error Loading log levels", e );
    }
  }

  public static void resetLevel( ) {
    logLevel.recalculate( );
  }

  enum LogLevel implements Callable<Boolean> {
    ALL,
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

    },
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL,
    OFF;
    private final Logger        logger;
    
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
      System.setProperty( PROP_LOG_EXHAUST_LEVEL, this.exhaustLevel( ) );
      return this;
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

  private static final class LogLevelResupplier {
	  private Supplier<LogLevel> realLogLevel;
	  LogLevelResupplier() {
		  recalculate();
	  }
	  
	  public LogLevel get( ) {
		  return realLogLevel.get();
		  
	  }
	  public void recalculate() {
		  realLogLevel = Suppliers.memoize( new Supplier<LogLevel>( ) {
              @Override
              public LogLevel get( ) {
                return LogLevel.get( );
              }
            } );
	  }
  }
  private static final LogLevelResupplier logLevel = new LogLevelResupplier();
                                                     
  public static boolean isExtrrreeeme( ) {
    return LogLevel.EXTREME.call( );
  }
  
  public static boolean isDebug( ) {
    return LogLevel.DEBUG.call( );
  }
  
  public static boolean isTrace( ) {
    return LogLevel.TRACE.call( );
  }
}
