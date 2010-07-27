package com.eucalyptus.upgrade;

import edu.emory.mathcs.backport.java.util.Collections;
import groovy.sql.Sql;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Security;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.netty.util.internal.ConcurrentHashMap;
import com.eucalyptus.auth.DatabaseAuthProvider;
import com.eucalyptus.auth.Groups;
import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.UserInfoStore;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.crypto.Hmacs;
import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.DispatcherFactory;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.entities.PersistenceContextDiscovery;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.scripting.ScriptExecutionFailedException;
import com.eucalyptus.scripting.groovy.GroovyUtil;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.system.LogLevels;
import com.eucalyptus.system.SubDirectory;
import com.google.common.collect.Lists;

public class StandalonePersistence {
  private static Logger                     LOG;
  private static ConcurrentMap<String, Sql> sqlConnections = new ConcurrentHashMap<String, Sql>( );
  private static List<UpgradeScript> upgradeScripts = Lists.newArrayList( );
  static {
    Security.addProvider( new BouncyCastleProvider( ) );
  }
  public static String                      eucaHome, eucaOld, eucaSource, eucaDest, eucaOldVersion, eucaNewVersion;
  public static File                        oldLibDir, newLibDir;
  private static DatabaseSource             source;
  private static DatabaseDestination        dest;
  
  public static void main( String[] args ) throws Throwable {
    if ( ( eucaHome = System.getProperty( "euca.upgrade.new.dir" ) ) == null ) {
      throw new RuntimeException( "Failed to find required 'euca.upgrade.new.dir' property: " + eucaHome );
    } else if ( ( eucaOld = System.getProperty( "euca.upgrade.old.dir" ) ) == null ) {
      throw new RuntimeException( "Failed to find required 'euca.upgrade.old.dir' property: " + eucaHome );
    } else if ( ( eucaNewVersion = System.getProperty( "euca.upgrade.new.version" ) ) == null ) {
      throw new RuntimeException( "Failed to find required 'euca.upgrade.new.version' property: " + eucaHome );
    } else if ( ( eucaOldVersion = System.getProperty( "euca.upgrade.old.version" ) ) == null ) {
      throw new RuntimeException( "Failed to find required 'euca.upgrade.old.version' property: " + eucaHome );
    } else if ( ( eucaSource = System.getProperty( "euca.upgrade.source" ) ) == null ) {
      throw new RuntimeException( "Failed to find required 'euca.upgrade.source' property: " + eucaHome );
    } else if ( ( eucaDest = System.getProperty( "euca.upgrade.destination" ) ) == null ) {
      throw new RuntimeException( "Failed to find required 'euca.upgrade.destination' property: " + eucaHome );
    } else {
      StandalonePersistence.setupSystemProperties( );
      StandalonePersistence.setupConfigurations( );
      StandalonePersistence.setupInitProviders( );
    }
    /** Prepare for database upgrade **/
    try {
      /** Setup the persistence contexts **/
      StandalonePersistence.runDiscovery( );
      /** Setup some system mechanisms after starting the true destination db **/
      StandalonePersistence.setupProviders( );
      /** Create connections for each of the source databases **/
      StandalonePersistence.setupNewDatabase( );
      StandalonePersistence.setupOldDatabase( );
      StandalonePersistence.runUpgrade( );
      System.exit(0);
    } catch ( Throwable e ) {
      e.printStackTrace( );
      System.exit( -1 );
    }
  }
  
  public static void runUpgrade( ) {
	Collections.sort(upgradeScripts);
    LOG.info( upgradeScripts );
    for( UpgradeScript up : upgradeScripts ) {
      try {
        up.upgrade( oldLibDir, newLibDir );
      } catch ( Throwable e ) {
        LOG.error( e, e );
      }
    }
    LOG.info( "=============================" );
    LOG.info( "= DATABASE UPGRADE COMPLETE =" );
    LOG.info( "=============================" );
  }
  
  public static Collection<Sql> listConnections( ) {
    return sqlConnections.values( );
  }
  public static Sql getConnection( String persistenceContext ) throws SQLException {
    Sql newSql = source.getSqlSession( persistenceContext );
    Sql conn = sqlConnections.putIfAbsent( persistenceContext, newSql );
    if ( conn != null ) {
      newSql.close( );
    } else {
      conn = newSql;
      LOG.info( "Created new connection for: " + persistenceContext + " to " + conn.getConnection( ).getMetaData( ).getURL( ) );
    }
    return conn;
  }
  
  private static void setupProviders( ) {
    DatabaseAuthProvider dbAuth = new DatabaseAuthProvider( );
    Users.setUserProvider( dbAuth );
    Groups.setGroupProvider( dbAuth );
    UserInfoStore.setUserInfoProvider( dbAuth );
  }
  
  private static void setupOldDatabase( ) throws Exception {
    source = ( DatabaseSource ) ClassLoader.getSystemClassLoader( ).loadClass( eucaSource ).newInstance( );
    /** Register a shutdown hook which closes all source-sql sessions **/
    Runtime.getRuntime( ).addShutdownHook( new Thread( ) {
      @Override
      public void run( ) {
        for ( Sql s : StandalonePersistence.sqlConnections.values( ) ) {
          try {
            s.close( );
          } catch ( Throwable e ) {
            LOG.debug( e, e );
          }
        }
      }
    } );
    /** open connection for each context **/
    List<String> oldContexts = ServiceJarDiscovery.contextsInDir( oldLibDir );
    for ( String ctx : oldContexts ) {
      StandalonePersistence.getConnection( ctx );
    }
  }
  
  private static void setupNewDatabase( ) throws Exception {
    dest = ( DatabaseDestination ) ClassLoader.getSystemClassLoader( ).loadClass( eucaDest ).newInstance( );
    dest.initialize( );    
    Runtime.getRuntime( ).addShutdownHook( new Thread( ) {
      @Override
      public void run( ) {
        PersistenceContexts.shutdown( );
      }
    } );
  }
  
  private static void setupInitProviders( ) throws Exception {
    if ( !new File( EucaKeyStore.getInstance( ).getFileName( ) ).exists( ) ) {
      throw new RuntimeException( "Database upgrade must be preceded by a key upgrade." );
    }
    new SystemCredentialProvider( ).load( Bootstrap.Stage.Anonymous );
    DispatcherFactory.setFactory( ( DispatcherFactory ) ClassLoader.getSystemClassLoader( ).loadClass( "com.eucalyptus.ws.client.DefaultDispatcherFactory" ).newInstance( ) );
    LOG.debug( "Initializing SSL just in case: " + ClassLoader.getSystemClassLoader( ).loadClass( "com.eucalyptus.auth.util.SslSetup" ) );
    LOG.debug( "Initializing db password: " + ClassLoader.getSystemClassLoader( ).loadClass( "com.eucalyptus.auth.util.Hashes" ) );
  }
  
  private static void setupSystemProperties( ) {
    /** Pre-flight configuration for system **/
    System.setProperty( "euca.home", eucaHome );
    System.setProperty( "euca.log.level", "TRACE" );
    System.setProperty( "euca.log.appender", "console" );
    System.setProperty( "euca.log.exhaustive.cc", "FATAL" );
    System.setProperty( "euca.log.exhaustive.db", "FATAL" );
    System.setProperty( "euca.log.exhaustive.external", "FATAL" );
    System.setProperty( "euca.log.exhaustive.user", "FATAL" );
    System.setProperty( "euca.var.dir", eucaHome + "/var/lib/eucalyptus/" );
    System.setProperty( "euca.conf.dir", eucaHome + "/etc/eucalyptus/cloud.d/" );
    System.setProperty( "euca.log.dir", eucaHome + "/var/log/eucalyptus/" );
    System.setProperty( "euca.lib.dir", eucaHome + "/usr/share/eucalyptus/" );
    boolean doTrace = "TRACE".equals( System.getProperty( "euca.log.level" ) );
    boolean doDebug = "DEBUG".equals( System.getProperty( "euca.log.level" ) ) || doTrace;
    LogLevels.DEBUG = doDebug;
    LogLevels.TRACE = doDebug;
    StandalonePersistence.LOG = Logger.getLogger( StandalonePersistence.class );
    LOG.info( String.format( "%-20.20s %s", "New install directory:", eucaHome ) );
    LOG.info( String.format( "%-20.20s %s", "Old install directory:", eucaOld ) );
    LOG.info( String.format( "%-20.20s %s", "Upgrade data source:", eucaSource ) );
    LOG.info( String.format( "%-20.20s %s", "Upgrade data destination:", eucaDest ) );
    oldLibDir = getAndCheckLibDirectory( eucaOld );
    newLibDir = getAndCheckLibDirectory( eucaHome );
  }
  
  private static void setupConfigurations( ) {
    Enumeration<URL> p1;
    URI u = null;
    try {
      p1 = Thread.currentThread( ).getContextClassLoader( ).getResources( "com.eucalyptus.CloudServiceProvider" );
      if ( !p1.hasMoreElements( ) ) return;
      while ( p1.hasMoreElements( ) ) {
        u = p1.nextElement( ).toURI( );
        Properties props = new Properties( );
        props.load( u.toURL( ).openStream( ) );
        String name = props.getProperty( "name" );
        if ( Components.contains( name ) ) {
          throw BootstrapException.throwFatal( "Duplicate component definition in: " + u.toASCIIString( ) );
        } else {
          try {
            LOG.debug( "Loaded " + name + " from " + u );
            Components.create( name, u );
          } catch ( ServiceRegistrationException e ) {
            LOG.debug( e, e );
            throw BootstrapException.throwFatal( "Error in component bootstrap: " + e.getMessage( ), e );
          }
        }
      }
    } catch ( IOException e ) {
      LOG.error( e, e );
      throw BootstrapException.throwFatal( "Failed to load component resources from: " + u, e );
    } catch ( URISyntaxException e ) {
      LOG.error( e, e );
      throw BootstrapException.throwFatal( "Failed to load component resources from: " + u, e );
    }
    
  }
  
  private static File getAndCheckLibDirectory( String eucaHome ) {
    String eucaLibDirPath;
    File eucaLibDir;
    if ( ( eucaLibDirPath = eucaHome + "/usr/share/eucalyptus" ) == null ) {
      throw new RuntimeException( "The source directory has not been specified." );
    } else if ( !( eucaLibDir = new File( eucaLibDirPath ) ).exists( ) ) {
      throw new RuntimeException( "The source directory does not exist: " + eucaLibDirPath );
    }
    return eucaLibDir;
  }
  
  public static void registerUpgradeScript( UpgradeScript up ) {
    if( up.accepts( eucaOldVersion, eucaNewVersion ) ) {
      LOG.info( String.format( "Found upgrade script for [%s->%s] in:      %s\n", eucaOldVersion, eucaNewVersion, up.getClass( ).getCanonicalName() ) );
      upgradeScripts.add( up );
    } else {
      LOG.info( String.format( "Ignoring upgrade script for [%s->%s] in:   %s\n", eucaOldVersion, eucaNewVersion, up.getClass( ).getCanonicalName() ) );      
    }
  }
  
  public static void runDiscovery( ) {
    List<Class> classList = ServiceJarDiscovery.classesInDir( new File( BaseDirectory.LIB.toString( ) ) );
    for( ServiceJarDiscovery d : Lists.newArrayList( new PersistenceContextDiscovery( ), new UpgradeScriptDiscovery( ) ) ) {
      for ( Class c : classList ) {
        try {
          d.processClass( c );
        } catch ( Throwable t ) {
          if( t instanceof ClassNotFoundException ) {
          } else {
            t.printStackTrace( );
            LOG.debug( t, t );
          }
        }
      }
    }
    for( File script : SubDirectory.UPGRADE.getFile( ).listFiles( ) ) {
      LOG.debug( "Trying to load what looks like an upgrade script: " + script.getAbsolutePath( ) );
      try {
        UpgradeScript u = GroovyUtil.newInstance( script.getAbsolutePath( ) );
        registerUpgradeScript( u );
      } catch ( ScriptExecutionFailedException e ) {
        LOG.debug( e, e );
      }
    }
  }
  
}
