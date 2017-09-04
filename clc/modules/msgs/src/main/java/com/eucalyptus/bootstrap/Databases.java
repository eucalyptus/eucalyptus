/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

package com.eucalyptus.bootstrap;

import groovy.sql.Sql;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.Database;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.scripting.ScriptExecutionFailedException;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.Strings;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class Databases {
  
  private static final Logger                 LOG                       = Logger.getLogger( Databases.class );
  private static final int                    MAX_TX_START_SYNC_RETRIES = 120;
  private static final ScriptedDbBootstrapper singleton                 = new ScriptedDbBootstrapper( );
  private static final AtomicBoolean          volatileAtomic            = new AtomicBoolean( true );
  
  private static Predicate<StackTraceElement> notStackFilterYouAreLookingFor = Predicates.or(
      Threads.filterStackByQualifiedName( "com\\.eucalyptus\\.entities\\..*" ),
      Threads.filterStackByQualifiedName( "java\\.lang\\.Thread.*" ),
      Threads.filterStackByQualifiedName( "com\\.eucalyptus\\.system\\.Threads.*" ),
      Threads.filterStackByQualifiedName( "com\\.eucalyptus\\.bootstrap\\.Databases.*" ) );
  private static Predicate<StackTraceElement> stackFilter                    = Predicates.not( notStackFilterYouAreLookingFor );

  public static class DatabaseStateException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    public DatabaseStateException( String string ) {
      super( string );
    }

  }

  public enum Events {
    INSTANCE;
    public static Sql getConnection( ) throws Exception {
      return Databases.getBootstrapper( ).getConnection( INSTANCE.getName( ), null );
    }

    public String getName( ) {
      return "database_events";
    }

    public static void create( ) {
      if ( !getBootstrapper( ).listDatabases( ).contains( INSTANCE.getName( ) ) ) {
        try {
          getBootstrapper( ).createDatabase( INSTANCE.getName( ) );
        } catch ( Exception ex ) {
          LOG.error( ex , ex );
        }
      }
    }
  }
  
  public static Iterable<String> databases( ) {
    return Sets.newTreeSet( Iterables.transform( PersistenceContexts.list( ), PersistenceContexts.toDatabaseName( ) ) );
  }
  
  @Provides( Empyrean.class )
  @RunDuring( Bootstrap.Stage.PoolInit )
  public static class DatabasePoolBootstrapper extends Bootstrapper.Simple {
    
    @Override
    public boolean load( ) throws Exception {
      Hosts.awaitDatabases( );

      Groovyness.run( "setup_dbpool" );
      return true;
    }
  }

  static boolean shouldInitialize( ) {//GRZE:WARNING:HACKHACKHACK do not duplicate pls thanks.
    final String context = "eucalyptus_config";
    final String databaseName = PersistenceContexts.toDatabaseName( ).apply( context );
    final String schemaName = PersistenceContexts.toSchemaName( ).apply( context );
    final String schemaPrefix = schemaName == null ? "" : schemaName + ".";
    for ( final Host h : Hosts.listActiveDatabases( ) ) {
      final String url = String.format( "jdbc:%s", ServiceUris.remote( Database.class, h.getBindAddress( ), databaseName ) );
      try ( final Connection conn = DriverManager.getConnection( url, Databases.getUserName( ), Databases.getPassword( ) ) ) {
        try ( final PreparedStatement statement = conn.prepareStatement( "select config_component_hostname from " +
            schemaPrefix + "config_component_base where config_component_partition='eucalyptus';" ) ) {
          try ( final ResultSet result = statement.executeQuery( ) ) {
            while ( result.next( ) ) {
              final Object columnValue = result.getObject( 1 );
              if ( Internets.testLocal( columnValue.toString( ) ) ) {
                return true;
              }
            }
          }
        }
      } catch ( final Exception ex ) {
        LOG.error( ex, ex );
      }
    }
    return false;
  }

  /**
   * List all known databases.
   */
  static Set<String> listDatabases( ) {
    final Set<String> dbNames = Sets.newHashSet();
    final Predicate<String> dbNamePredicate = Predicates.or(
        Strings.startsWith( "eucalyptus_" ),
        Predicates.equalTo( "database_events" ) );

    for ( final Host h : Hosts.listActiveDatabases( ) ) {
      Iterables.addAll(
          dbNames,
          Iterables.filter(
              Databases.getBootstrapper().listDatabases( h.getBindAddress( ) ),
              dbNamePredicate ) );
    }

    return dbNames;
  }
  
  public static Boolean isVolatile( ) {
    return  !BootstrapArgs.isUpgradeSystem( ) && !Bootstrap.isShuttingDown( ) && volatileAtomic.get( );
  }
  
  public static void setVolatile( boolean isVolatile ) {
    if ( volatileAtomic.compareAndSet( !isVolatile, isVolatile ) ) {
      LOG.debug( "Database availability changed to: " + !isVolatile );
    }
  }

  public static void awaitSynchronized( ) {
    if ( !isVolatile( ) ) {
      return;
    } else {
      Collection<StackTraceElement> stack = Threads.filteredStack( stackFilter );
      String caller = ( stack.isEmpty( )
        ? ""
        : stack.iterator( ).next( ).toString( ) );
      for ( int i = 0; i < MAX_TX_START_SYNC_RETRIES && isVolatile( ); i++ ) {
        try {
          TimeUnit.MILLISECONDS.sleep( 1000 );
          LOG.debug( "Transaction blocked on sync: " + caller );
        } catch ( InterruptedException ex ) {
          Exceptions.maybeInterrupted( ex );
          return;
        }
      }
      if ( isVolatile( ) ) {
        throw new DatabaseStateException( "Transaction begin failed due to concurrent database synchronization: " + Hosts.listDatabases( )
                                          + " for caller:\n"
                                          + Joiner.on( "\n\tat " ).join( stack ) );
      }
    }
  }
  
  public static String getUserName( ) {
    return singleton.getUserName( );
  }
  
  public static String getPassword( ) {
    return singleton.getPassword( );
  }
  
  public static String getDriverName( ) {
    return singleton.getDriverName( );
  }

  public static String getDefaultSchemaName( ) {
    return singleton.getDefaultSchemaName( );
  }

  public static String getJdbcDialect( ) {
    return singleton.getJdbcDialect( );
  }
  
  public static String getHibernateDialect( ) {
    return singleton.getHibernateDialect( );
  }
  
  public static DatabaseBootstrapper getBootstrapper( ) {
    return singleton;
  }
  
  public static void initialize( ) {
    singleton.init( );
    Databases.Events.create( );
  }
  
  @RunDuring( Bootstrap.Stage.DatabaseInit )
  @Provides( Empyrean.class )
  @DependsLocal( Eucalyptus.class )
  public static class ScriptedDbBootstrapper extends Bootstrapper.Simple implements DatabaseBootstrapper {
    DatabaseBootstrapper db;
    
    public ScriptedDbBootstrapper( ) {
      try {
        this.db = Groovyness.newInstance( "setup_db" );
      } catch ( ScriptExecutionFailedException ex ) {
        LOG.error( ex, ex );
      }
    }
    
    @Override
    public boolean load( ) throws Exception {
      boolean result = this.db.load( );
      Databases.Events.create( );
      return result;
    }
    
    @Override
    public boolean start( ) throws Exception {
      return this.db.start( );
    }
    
    @Override
    public boolean stop( ) throws Exception {
      return this.db.stop( );
    }
    
    @Override
    public void destroy( ) throws Exception {
      this.db.destroy( );
    }
    
    @Override
    public boolean isRunning( ) {
      return this.db.isRunning( );
    }
    
    @Override
    public void hup( ) {
      this.db.hup( );
    }
    
    @Override
    public String getUserName( ) {
      return db.getUserName( );
    }
    
    @Override
    public String getPassword( ) {
      return db.getPassword( );
    }

    @Override
    public String getDefaultSchemaName( ) {
      return this.db.getDefaultSchemaName( );
    }

    @Override
    public String getDriverName( ) {
      return this.db.getDriverName( );
    }
    
    @Override
    public String getJdbcDialect( ) {
      return this.db.getJdbcDialect( );
    }
    
    @Override
    public String getHibernateDialect( ) {
      return this.db.getHibernateDialect( );
    }
    
    @Override
    public void init( ) {
      try {
        this.db.init( );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
    }
    
    public static DatabaseBootstrapper getInstance( ) {
      return singleton;
    }
    
    @Override
    public String getServicePath( String... pathParts ) {
      return this.db.getServicePath( pathParts );
    }
    
    @Override
    public Map<String, String> getJdbcUrlQueryParameters( ) {
      return this.db.getJdbcUrlQueryParameters( );
    }
    
    @Override
    public boolean check( ) throws Exception {
      return this.db.isRunning( );
    }
    
    /**
     * @see DatabaseBootstrapper#getJdbcScheme()
     */
    @Override
    public String getJdbcScheme( ) {
      return this.db.getJdbcScheme( );
    }

    @Override
    public List<String> listDatabases() {
      return db.listDatabases();
    }

    @Override
    public List<String> listDatabases( final InetAddress host ) {
      return db.listDatabases( host );
    }

    @Override
    public List<String> listSchemas( final String database ) {
      return db.listSchemas( database );
    }

    @Override
    public List<String> listSchemas( final InetAddress host, final String database ) {
      return db.listSchemas( host, database );
    }

    @Override
    public List<String> listTables( final String database, final String schema ) {
      return db.listTables( database, schema );
    }

    @Override
    public void createDatabase( final String database ) {
      db.createDatabase( database );
    }

    @Override
    public void deleteDatabase( final String database ) {
      db.deleteDatabase( database );
    }

    @Override
    public void renameDatabase( final String from, final String to ) {
      db.renameDatabase( from, to );
    }

    @Override
    public void copyDatabase( final String sourceDatabase,
                              final String destinationDatabase ) {
      db.copyDatabase( sourceDatabase, destinationDatabase );
    }

    @Override
    public void copyDatabaseSchema( final String sourceDatabase,
                                    final String sourceSchema,
                                    final String destinationDatabase,
                                    final String destinationSchema ) {
      db.copyDatabaseSchema( sourceDatabase, sourceSchema, destinationDatabase, destinationSchema );
    }

    @Override
    public Sql getConnection( final String context ) throws Exception {
      return db.getConnection( context );
    }

    @Override
    public Sql getConnection( final String database, final String schema ) throws Exception {
      return db.getConnection( database, schema );
    }
  }
  
  public static boolean isRunning( ) {
    try {
      return singleton.check( );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      return false;
    }
  }
  
  public static String getServicePath( String... pathParts ) {
    return singleton.getServicePath( pathParts );
  }
  
  public static Map<String, String> getJdbcUrlQueryParameters( ) {
    return singleton.getJdbcUrlQueryParameters();
  }
  
  public static String getJdbcScheme( ) {
    return singleton.getJdbcScheme( );
  }
  
}
