package com.eucalyptus.util;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Priority;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.Configurator;
import org.apache.log4j.spi.LoggerRepository;
import com.eucalyptus.bootstrap.SystemBootstrapper;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.system.EucaLayout;
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
  
  public static boolean DEBUG   = false;                                                                    //TODO:get rid of this non-sense
  public static boolean TRACE   = false;                                                                    //TODO:get rid of this non-sense
  public static boolean EXTREME = "EXTREME".equals( System.getProperty( "euca.log.level" ).toUpperCase( ) );
  
  public static Logger exhaust( ) {
    return Logger.getLogger( "EXHAUST" );
  }
  
  public static Logger bootstrap( ) {
    return Logger.getLogger( "BOOTSTRAP" );
  }
  
  public static void init( ) {
    Logs.EXTREME = "EXTREME".equals( System.getProperty( "euca.log.level" ).toUpperCase( ) );
    Logs.TRACE = "TRACE".equals( System.getProperty( "euca.log.level" ).toUpperCase( ) ) || Logs.EXTREME;
    Logs.DEBUG = "DEBUG".equals( System.getProperty( "euca.log.level" ).toUpperCase( ) ) || Logs.TRACE;
    if ( Logs.EXTREME ) {
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
      System.setOut( new PrintStream( System.out ) {
        public void print( final String string ) {
          if ( string.replaceAll( "\\s*", "" ).length( ) > 2 ) {
            Logs.exhaust( ).info( SystemBootstrapper.class + " " + EventType.STDOUT + " " + ( string == null
              ? "null"
              : string.replaceAll( "\\n*\\z", "" ) ) );
          }
        }
      }

      );
      
      System.setErr( new PrintStream( System.err ) {
        public void print( final String string ) {
          if ( string.replaceAll( "\\s*", "" ).length( ) > 2 ) {
            Logs.exhaust( ).error( SystemBootstrapper.class + " " + EventType.STDERR + " " + ( string == null
              ? "null"
              : string.replaceAll( "\\n*\\z", "" ) ) );
          }
        }
      }
            );
      
      Logger.getRootLogger( ).info( LogUtil.subheader( "Starting system with debugging set as: " + Joiner.on( "\n" ).join( Logs.class.getDeclaredFields( ) ) ) );
    } catch ( Throwable t ) {
      t.printStackTrace( );
      System.exit( 1 );
    }
  }
}
