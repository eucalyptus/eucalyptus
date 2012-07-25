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

package com.eucalyptus.upgrade;

import edu.emory.mathcs.backport.java.util.Collections;
import groovy.sql.Sql;
import java.io.File;
import java.security.Security;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.netty.util.internal.ConcurrentHashMap;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.DatabaseAuthProvider;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.ComponentDiscovery;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.entities.PersistenceContextDiscovery;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.scripting.ScriptExecutionFailedException;
import com.eucalyptus.system.BaseDirectory;
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
  
  public static void main( String[] args ) throws Exception {
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
      dest.getDb().stop();
      System.exit(0);
    } catch ( Exception e ) {
      LOG.error( e, e );
      e.printStackTrace( );
      dest.getDb().stop();
      System.exit( -1 );
    }
  }
  
  public static void runUpgrade( ) {
    Collections.sort(upgradeScripts);
    LOG.info( upgradeScripts );
    for( UpgradeScript up : upgradeScripts ) {
      up.setLogger(LOG);
      up.upgrade( oldLibDir, newLibDir );
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
    if ( newSql == null ) { return null; }
    Sql conn = sqlConnections.putIfAbsent( persistenceContext, newSql );
    if ( conn != null ) {
      newSql.close( );
    } else {
      conn = newSql;
      LOG.info( "Created new connection for: " + persistenceContext + " to " + conn.getConnection( ).getMetaData( ).getURL( ) );
    }
    return conn;
  }
  
  public static void setupProviders( ) {
    DatabaseAuthProvider dbAuth = new DatabaseAuthProvider( );
    Accounts.setAccountProvider( dbAuth );
  }
  
  private static void setupOldDatabase( ) throws Exception {
    if (eucaSource.startsWith("groovy:")) {
        source = Groovyness.newInstance( eucaSource.replace("groovy:", "") );
    } else {
    	source = ( DatabaseSource ) ClassLoader.getSystemClassLoader( ).loadClass( eucaSource ).newInstance( );
    }
    /** Register a shutdown hook which closes all source-sql sessions **/
    Runtime.getRuntime( ).addShutdownHook( new Thread( ) {
      @Override
      public void run( ) {
        for ( Sql s : StandalonePersistence.sqlConnections.values( ) ) {
          try {
            s.close( );
          } catch ( Exception e ) {
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
  
  public static void setupNewDatabase( ) throws Exception {
    dest = ( DatabaseDestination ) ClassLoader.getSystemClassLoader( ).loadClass( eucaDest ).newInstance( );
    dest.initialize( );    
  }
  
  public static void setupInitProviders( ) throws Exception {
    if ( !new File( SystemCredentials.getKeyStore( ).getFileName( ) ).exists( ) ) {
      throw new RuntimeException( "Database upgrade must be preceded by a key upgrade." );
    }
    SystemCredentials.initialize( );
    LOG.debug( "Initializing SSL just in case: " + ClassLoader.getSystemClassLoader( ).loadClass( "com.eucalyptus.crypto.util.SslSetup" ) );
    LOG.debug( "Initializing db password: " + ClassLoader.getSystemClassLoader( ).loadClass( "com.eucalyptus.auth.util.Hashes" ) );
  }
  
  static void setupSystemProperties( ) {
    /** Pre-flight configuration for system **/
    System.setProperty( "euca.home", eucaHome );
    System.setProperty( "euca.log.appender", "upgrade" );
    System.setProperty( "euca.log.exhaustive.cc", "FATAL" );
    System.setProperty( "euca.log.exhaustive.db", "FATAL" );
    System.setProperty( "euca.log.exhaustive.external", "FATAL" );
    System.setProperty( "euca.log.exhaustive.user", "FATAL" );
    System.setProperty( "euca.var.dir", eucaHome + "/var/lib/eucalyptus/" );
    System.setProperty( "euca.conf.dir", eucaHome + "/etc/eucalyptus/cloud.d/" );
    System.setProperty( "euca.log.dir", eucaHome + "/var/log/eucalyptus/" );
    System.setProperty( "euca.lib.dir", eucaHome + "/usr/share/eucalyptus/" );
    String logLevel = System.getProperty( "euca.log.level", "INFO" ).toUpperCase();

    // Keep logs off the console
    // Logger.getRootLogger().removeAllAppenders();
    LOG = Logger.getLogger( StandalonePersistence.class );
    LOG.info( String.format( "%-20.20s %s", "New install directory:", eucaHome ) );
    LOG.info( String.format( "%-20.20s %s", "Old install directory:", eucaOld ) );
    LOG.info( String.format( "%-20.20s %s", "Upgrade data source:", eucaSource ) );
    LOG.info( String.format( "%-20.20s %s", "Upgrade data destination:", eucaDest ) );
    oldLibDir = getAndCheckLibDirectory( eucaOld );
    newLibDir = getAndCheckLibDirectory( eucaHome );
  }
  
  static void setupConfigurations( ) {
	ServiceJarDiscovery.doSingleDiscovery( new ComponentDiscovery( ) );
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
    runSetupDiscovery( );
    for( File script : SubDirectory.UPGRADE.getFile( ).listFiles( ) ) {
      if (! script.getPath().endsWith(".groovy")) {
        LOG.debug("Skipping " + script.getAbsolutePath( ) + " due to file extension.");
        continue;
      }
      LOG.debug( "Trying to load what looks like an upgrade script: " + script.getAbsolutePath( ) );
      try {
        UpgradeScript u = Groovyness.newInstance( script.getAbsolutePath( ) );
        registerUpgradeScript( u );
      } catch ( ScriptExecutionFailedException e ) {
        LOG.error( e, e );
        e.printStackTrace( );
        System.exit( -1 );
      }
    }
  }

  public static void runSetupDiscovery( ) {
    List<Class> classList = ServiceJarDiscovery.classesInDir( new File( BaseDirectory.LIB.toString( ) ) );
    for( ServiceJarDiscovery d : Lists.newArrayList( new PersistenceContextDiscovery( ), new UpgradeScriptDiscovery( ) ) ) {
      for ( Class c : classList ) {
        try {
          d.processClass( c );
        } catch ( Exception t ) {
          if( t instanceof ClassNotFoundException ) {
          } else {
            t.printStackTrace( );
            LOG.debug( t, t );
          }
        }
      }
    }
  }
  
}
