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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Priority;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.Configurator;
import org.apache.log4j.spi.DefaultRepositorySelector;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.RepositorySelector;
import org.apache.log4j.xml.DOMConfigurator;

import com.eucalyptus.bootstrap.SystemBootstrapper;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.system.EucaLayout;
import com.eucalyptus.system.Threads;
import com.eucalyptus.system.log.EucaHierarchy;
import com.eucalyptus.system.log.EucaLoggingOutputStream;
import com.eucalyptus.system.log.EucaRootLogger;
import com.eucalyptus.system.log.NullEucaLogger;
import com.eucalyptus.util.LogUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class Logs {
  static {
	  // Some things were done here to add %i (ThreadID) to the PatternLayout class in log4j.
	  // It was a bit complicated, requiring creating new LoggerFactory, Logger, and Hierarchy
	  // classes among other things.  Also, default initialization of log4j (basically anywhere
	  // Logger.getLogger() is called will initialize everything with default values, with no
	  // easy way to change references to things already created.
	  // Hence, we force the simplest initialization of the normal log4j. (creating only the root logger)
	  BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %p %m%n")));
	  
	  // Hack: If we pass "EXTREME" or "EXHAUST" to the dom configurator,
	  // by default it will use the DEBUG level when we want it to use the trace level
	  // So we save the old level, change it back when we are done.
	  String logLevelProp = "euca.log.level";
	  String oldLogLevel = System.getProperty(logLevelProp);
	  if ("EXHAUST".equals(oldLogLevel) || "EXTREME".equals(oldLogLevel)) {
		  System.setProperty(logLevelProp, "TRACE");
	  }
	  // Then we run the DOMConfigurator on a new LoggerRepository
	  URL url = Thread.currentThread().getContextClassLoader().getResource("log4j.xml");
	  Hierarchy eucaHierarchy = new EucaHierarchy(new EucaRootLogger(Level.DEBUG));
	  new DOMConfigurator().doConfigure(url, eucaHierarchy);
      // Then we hook the new logger repository into the LogManager. 
	  LogManager.setRepositorySelector(new DefaultRepositorySelector(eucaHierarchy), null);

	  // Now set it back
	  if ( oldLogLevel != null )
	    System.setProperty(logLevelProp, oldLogLevel);
  }
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
  
  private static final Logger nullLogger = new NullEucaLogger();

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
    try {
      final Logger stdLogger = ( Logs.isExtrrreeeme( ) ? Logger.getLogger( SystemBootstrapper.class ) : Logs.extreme( ) );
      final PrintStream oldOut = System.out;
      final PrintStream oldErr = System.err;
      if ( !System.getProperty( "euca.log.appender", "" ).equals( "console" ) ) {
        System.setOut( new PrintStream( new EucaLoggingOutputStream( stdLogger, Level.INFO ), true ) );
        System.setErr( new PrintStream( new EucaLoggingOutputStream( stdLogger, Level.ERROR ), true ) );
      }
      Logger.getRootLogger( ).info( LogUtil.subheader( "Starting system with debugging set as: " + Logs.logLevel.get( ) ) );
    } catch ( final Exception t ) {
      t.printStackTrace( );
      System.exit( 1 );//GRZE: special case, can't open log files, hosed
    }
  }

  public static void reInit( ) {
	    logLevel.recalculate( );
	    try {
	      final Logger stdLogger = ( Logs.isExtrrreeeme( ) ? Logger.getLogger( SystemBootstrapper.class ) : Logs.extreme( ) );
	      final PrintStream oldOut = System.out;
	      final PrintStream oldErr = System.err;
	      if ( !System.getProperty( "euca.log.appender", "" ).equals( "console" ) ) {
	        System.setOut( new PrintStream( new EucaLoggingOutputStream( stdLogger, Level.INFO ), true ) );
	        System.setErr( new PrintStream( new EucaLoggingOutputStream( stdLogger, Level.ERROR ), true ) );
	      }
	      Logger.getRootLogger( ).info( LogUtil.subheader( "Starting system with debugging set as: " + Logs.logLevel.get( ) ) );
	    } catch ( final Exception t ) {
	      t.printStackTrace( );
	      System.exit( 1 );//GRZE: special case, can't open log files, hosed
	    }
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
    private static final String PROP_LOG_LEVEL   = "euca.log.level";
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
      System.setProperty( "euca.exhaust.level", this.exhaustLevel( ) );
      System.setProperty( "euca.log.exhaustive", this.exhaustLevel( ) );
      System.setProperty( "euca.log.exhaustive.cc", this.exhaustLevel( ) );
      System.setProperty( "euca.log.exhaustive.user", this.exhaustLevel( ) );
      System.setProperty( "euca.log.exhaustive.db", this.exhaustLevel( ) );
      System.setProperty( "euca.log.exhaustive.external", this.exhaustLevel( ) );
      System.setProperty( "euca.log.exhaustive.user", this.exhaustLevel( ) );
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

  @Deprecated
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
