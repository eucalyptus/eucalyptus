package com.eucalyptus.records;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
  
  public static void init( ) {
    logLevel.get( );
    //    System.setProperty( "log4j.configurationClass", "com.eucalyptus.util.Logs.LogConfigurator" );
    try {
      final PrintStream oldOut = System.out;
      final ByteArrayOutputStream bos = new ByteArrayOutputStream( ) {
        @Override
        public synchronized void reset( ) {
          super.buf = new byte[4096];
          super.reset( );
        }
      };
      System.setOut( new PrintStream( bos, true ) {
        @Override
        public void flush( ) {
          Logs.exhaust( ).info( SystemBootstrapper.class + " " + EventType.STDOUT + " " + bos.toString( ) );
          bos.reset( );
          super.flush( );
        }
        
        @Override
        public void close( ) {
          Logs.exhaust( ).info( SystemBootstrapper.class + " " + EventType.STDOUT + " " + bos.toString( ) );
          bos.reset( );
          super.close( );
        }
      }

      );
      
      final PrintStream oldErr = System.err;
      final ByteArrayOutputStream bosErr = new ByteArrayOutputStream( ) {       
        @Override
        public synchronized void reset( ) {
          super.buf = new byte[4096];
          super.reset( );
        }
      };
      System.setErr( new PrintStream( bosErr, true ) {
        
        @Override
        public void flush( ) {
          Logs.exhaust( ).error( SystemBootstrapper.class + " " + EventType.STDERR + " " + bosErr.toString( ) );
          bosErr.reset( );
          super.flush( );
        }
        
        @Override
        public void close( ) {
          Logs.exhaust( ).error( SystemBootstrapper.class + " " + EventType.STDERR + " " + bosErr.toString( ) );
          bosErr.reset( );
          super.close( );
        }
      }
            );
      
      Logger.getRootLogger( ).info( LogUtil.subheader( "Starting system with debugging set as: " + Joiner.on( "\n" ).join( Logs.class.getDeclaredFields( ) ) ) );
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
        return level( );
      }
    },
    EXTREME {
      
      @Override
      String level( ) {
        return TRACE.name( );
      }
      
      @Override
      String exhaustLevel( ) {
        return level( );
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
      System.setProperty( "euca.exhaust.level", exhaustLevel( ) );
      System.setProperty( "euca.log.exhaustive", exhaustLevel( ) );
      System.setProperty( "euca.log.exhaustive.cc", exhaustLevel( ) );
      System.setProperty( "euca.log.exhaustive.user", exhaustLevel( ) );
      System.setProperty( "euca.log.exhaustive.db", exhaustLevel( ) );
      System.setProperty( "euca.log.exhaustive.external", exhaustLevel( ) );
      System.setProperty( "euca.log.exhaustive.user", exhaustLevel( ) );
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
      } catch ( IllegalArgumentException ex ) {
        if ( EXTREME.name( ).equals( System.getProperty( PROP_LOG_LEVEL ).toUpperCase( ) ) ) {
          return EXTREME.init( );
        } else {
          throw ex;
        }
      } catch ( NullPointerException ex ) {
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
    HashMap ctx = new HashMap( ) {
      {
        put( "o", o );
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
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      return null;
    }
  }
  
  public static String groovyInspect( final Object o ) {
    HashMap ctx = new HashMap( ) {
      {
        put( "o", o );
      }
    };
    try {
      return "" + Groovyness.eval( "try{return o.inspect();}catch(Exception e){return \"\"+o;}", ctx );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      return null;
    }
  }
}
