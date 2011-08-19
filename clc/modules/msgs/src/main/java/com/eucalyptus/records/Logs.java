package com.eucalyptus.records;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.ResourceBundle;
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
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.system.EucaLayout;
import com.eucalyptus.util.LogUtil;
import com.google.common.base.Joiner;

public class Logs {
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
  
  private static final ConsoleAppender console                 = new ConsoleAppender( new EucaLayout( ), "System.out" ) {
                                                                 {
                                                                   setThreshold( Priority.toPriority( System.getProperty( "euca.log.level" ), Priority.INFO ) );
                                                                   setName( "console" );
                                                                   setImmediateFlush( false );
                                                                   setFollow( false );
                                                                 }
                                                               };
  private static final String          DEFAULT_LOG_LEVEL       = ( ( System.getProperty( "euca.log.level" ) == null )
                                                                 ? "INFO"
                                                                 : System.getProperty( "euca.log.level" ).toUpperCase( ) );
  private static final String          DEFAULT_LOG_PATTERN     = "%d{EEE MMM d HH:mm:ss yyyy} %5p [%c{1}:%t] %m%n";
  private static final String          DEFAULT_LOG_MAX_BACKUPS = "25";
  private static final String          DEFAULT_LOG_MAX_SIZE    = "10MB";
  
  private enum LogProps {
    threshold, pattern, filesize, maxbackups;
  }
  
  private enum Appenders {
    OUTPUT, ERROR, EXHAUST, CLUSTER, DEBUG, BOOTSTRAP;
    private final String  prop;
    private final String  threshold;
    private final String  pattern;
    private final String  fileSize;
    private final Integer backups;
    private final String  fileName;
    private Appender      appender;
    
    Appenders( ) {
      this.prop = "euca.log." + this.name( ).toLowerCase( ) + ".";
      this.threshold = this.getProperty( LogProps.threshold, DEFAULT_LOG_LEVEL ).toUpperCase( );
      this.pattern = this.getProperty( LogProps.pattern, DEFAULT_LOG_PATTERN );
      this.backups = Integer.parseInt( this.getProperty( LogProps.maxbackups, DEFAULT_LOG_MAX_BACKUPS ) );
      this.fileSize = this.getProperty( LogProps.filesize, DEFAULT_LOG_MAX_SIZE );
      this.fileName = BaseDirectory.LOG.getChildPath( this.getAppenderName( ) + ".log" );
    }
    
    private String getProperty( LogProps p, String defaultValue ) {
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
            setImmediateFlush( false );
            setMaxBackupIndex( Appenders.this.backups );
            setMaxFileSize( Appenders.this.fileSize );
            setName( Appenders.this.getAppenderName( ) );
            setThreshold( Priority.toPriority( Appenders.this.threshold ) );
//            setBufferedIO( true );
//            setBufferSize( 1024 );
          }
        } );
    }
  }
  
  public static class LogConfigurator implements Configurator {
    
    @Override
    public void doConfigure( URL arg0, LoggerRepository arg1 ) {
      arg1.getRootLogger( ).addAppender( console );
    }
    
  }
  
  private static boolean      EXTREME    = "EXTREME".equals( System.getProperty( "euca.log.level" ).toUpperCase( ) );
  private static boolean      TRACE      = isExtrrreeeme( ) || "TRACE".equals( System.getProperty( "euca.log.level" ).toUpperCase( ) );
  private static boolean      DEBUG      = isExtrrreeeme( ) || TRACE || "DEBUG".equals( System.getProperty( "euca.log.level" ).toUpperCase( ) );
  
  private static final Logger nullLogger = new Logger( "/dev/null" ) {
                                           
                                           @Override
                                           public boolean isTraceEnabled( ) {
                                             return false;
                                           }
                                           
                                           @Override
                                           public void trace( Object message, Throwable t ) {}
                                           
                                           @Override
                                           public void trace( Object message ) {}
                                           
                                           @Override
                                           public synchronized void addAppender( Appender newAppender ) {}
                                           
                                           @Override
                                           public void assertLog( boolean assertion, String msg ) {}
                                           
                                           @Override
                                           public void callAppenders( LoggingEvent arg0 ) {}
                                           
                                           @Override
                                           public void debug( Object message, Throwable t ) {}
                                           
                                           @Override
                                           public void debug( Object message ) {}
                                           
                                           @Override
                                           public void error( Object message, Throwable t ) {}
                                           
                                           @Override
                                           public void error( Object message ) {}
                                           
                                           @Override
                                           public void fatal( Object message, Throwable t ) {}
                                           
                                           @Override
                                           public void fatal( Object message ) {}
                                           
                                           @Override
                                           protected void forcedLog( String fqcn, Priority level, Object message, Throwable t ) {}
                                           
                                           @Override
                                           public boolean getAdditivity( ) {
                                             return false;
                                           }
                                           
                                           @Override
                                           public synchronized Enumeration getAllAppenders( ) {
                                             return super.getAllAppenders( );
                                           }
                                           
                                           @Override
                                           public synchronized Appender getAppender( String name ) {
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
                                           protected String getResourceBundleString( String arg0 ) {
                                             return super.getResourceBundleString( arg0 );
                                           }
                                           
                                           @Override
                                           public void info( Object message, Throwable t ) {}
                                           
                                           @Override
                                           public void info( Object message ) {}
                                           
                                           @Override
                                           public boolean isAttached( Appender appender ) {
                                             return super.isAttached( appender );
                                           }
                                           
                                           @Override
                                           public boolean isDebugEnabled( ) {
                                             return super.isDebugEnabled( );
                                           }
                                           
                                           @Override
                                           public boolean isEnabledFor( Priority level ) {
                                             return super.isEnabledFor( level );
                                           }
                                           
                                           @Override
                                           public boolean isInfoEnabled( ) {
                                             return super.isInfoEnabled( );
                                           }
                                           
                                           @Override
                                           public void l7dlog( Priority arg0, String arg1, Object[] arg2, Throwable arg3 ) {}
                                           
                                           @Override
                                           public void l7dlog( Priority arg0, String arg1, Throwable arg2 ) {}
                                           
                                           @Override
                                           public void log( Priority priority, Object message, Throwable t ) {}
                                           
                                           @Override
                                           public void log( Priority priority, Object message ) {}
                                           
                                           @Override
                                           public void log( String callerFQCN, Priority level, Object message, Throwable t ) {}
                                           
                                           @Override
                                           public synchronized void removeAllAppenders( ) {}
                                           
                                           @Override
                                           public synchronized void removeAppender( Appender appender ) {}
                                           
                                           @Override
                                           public synchronized void removeAppender( String name ) {}
                                           
                                           @Override
                                           public void setAdditivity( boolean additive ) {}
                                           
                                           @Override
                                           public void setLevel( Level level ) {}
                                           
                                           @Override
                                           public void setPriority( Priority priority ) {}
                                           
                                           @Override
                                           public void setResourceBundle( ResourceBundle bundle ) {}
                                           
                                           @Override
                                           public void warn( Object message, Throwable t ) {}
                                           
                                           @Override
                                           public void warn( Object message ) {}
                                           
                                         };
  
  public static Logger extreme( ) {
    if ( isExtrrreeeme( ) ) {
      return Logger.getLogger( "EXTREME" );
    } else {
      return nullLogger;
    }
  }
  
  public static Logger exhaust( ) {
    return Logger.getLogger( "EXHAUST" );
  }
  
  public static Logger bootstrap( ) {
    return Logger.getLogger( "BOOTSTRAP" );
  }
  
  public static void init( ) {
    if ( Logs.isExtrrreeeme( ) ) {
      System.setProperty( "euca.log.level", "TRACE" );
      System.setProperty( "euca.exhaust.level", "TRACE" );
      System.setProperty( "euca.log.exhaustive", "TRACE" );
      System.setProperty( "euca.log.exhaustive.cc", "TRACE" );
      System.setProperty( "euca.log.exhaustive.user", "TRACE" );
      System.setProperty( "euca.log.exhaustive.db", "TRACE" );
      System.setProperty( "euca.log.exhaustive.external", "TRACE" );
      System.setProperty( "euca.log.exhaustive.user", "TRACE" );
    }//    System.setProperty( "log4j.configurationClass", "com.eucalyptus.util.Logs.LogConfigurator" );
    try {
      PrintStream oldOut = System.out;
      final ByteArrayOutputStream bos = new ByteArrayOutputStream( );
      System.setOut( new PrintStream( bos ) {
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
      
      PrintStream oldErr = System.err;
      final ByteArrayOutputStream bosErr = new ByteArrayOutputStream( );
      System.setErr( new PrintStream( bosErr ) {
        
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
    } catch ( Exception t ) {
      t.printStackTrace( );
      System.exit( 1 );
    }
  }
  
  public static void no( boolean eXTREME ) {
    EXTREME = eXTREME;
  }
  
  public static boolean isExtrrreeeme( ) {
    return EXTREME;
  }
}
