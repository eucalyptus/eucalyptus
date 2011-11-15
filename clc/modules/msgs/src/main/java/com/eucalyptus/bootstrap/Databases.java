/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.bootstrap;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import net.sf.hajdbc.InactiveDatabaseMBean;
import net.sf.hajdbc.sql.DriverDatabaseClusterMBean;
import org.apache.log4j.Logger;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.Eucalyptus.Database;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.records.Logs;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.scripting.ScriptExecutionFailedException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.Mbeans;
import com.eucalyptus.util.async.Futures;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

public class Databases {
  private static final ScriptedDbBootstrapper     singleton       = new ScriptedDbBootstrapper( );
  private static Logger                           LOG             = Logger.getLogger( Databases.class );
  private static final String                     DB_NAME         = "eucalyptus";
  private static final String                      DB_USERNAME     = DB_NAME;
  private static final String                     jdbcJmxDomain   = "net.sf.hajdbc";
  private static final ExecutorService            dbSyncExecutors = Executors.newCachedThreadPool( );                     //NOTE:GRZE:special case thread handling.
  private static final AtomicReference<SyncState> syncState       = new AtomicReference<SyncState>( SyncState.NOTSYNCED );
  
  enum SyncState {
    NOTSYNCED,
    SYNCING,
    DESYNCING,
    SYNCED
  }
  
  enum ExecuteRunnable implements Function<Runnable, Future<Runnable>> {
    INSTANCE;
    
    @Override
    public Future<Runnable> apply( Runnable input ) {
      return dbSyncExecutors.submit( input, input );
    }
  }
  
  @Provides( Empyrean.class )
  @RunDuring( Bootstrap.Stage.PoolInit )
  public static class DatabasePoolBootstrapper extends Bootstrapper.Simple {
    
    @Override
    public boolean load( ) throws Exception {
      Groovyness.run( "setup_dbpool.groovy" );
      return true;
    }
    
    @Override
    public boolean check( ) throws Exception {
      return super.check( );
    }
    
  }
  
  static DriverDatabaseClusterMBean lookup( final String ctx ) throws NoSuchElementException {
    final DriverDatabaseClusterMBean cluster = Mbeans.lookup( Databases.jdbcJmxDomain,
                                                              ImmutableMap.builder( ).put( "cluster", ctx ).build( ),
                                                              DriverDatabaseClusterMBean.class );
    return cluster;
  }
  
  private static void runDbStateChange( Function<String, Runnable> runnableFunction ) {
    Map<Runnable, Future<Runnable>> runnables = Maps.newHashMap( );
    for ( final String ctx : PersistenceContexts.list( ) ) {
      Runnable run = runnableFunction.apply( ctx );
      runnables.put( run, ExecuteRunnable.INSTANCE.apply( run ) );
      LOG.debug( "RUNNING: " + run );
    }
    Map<Runnable, Future<Runnable>> succeeded = Futures.waitAll( runnables );
    MapDifference<Runnable, Future<Runnable>> failed = Maps.difference( runnables, succeeded );
    StringBuilder builder = new StringBuilder( );
    builder.append( Joiner.on( "\nSUCCESS: " ).join( succeeded.keySet( ) ) );
    builder.append( Joiner.on( "\nFAILED:  " ).join( failed.entriesOnlyOnLeft( ).keySet( ) ) );
    if ( failed.entriesOnlyOnLeft( ).isEmpty( ) ) {
      LOG.info( builder.toString( ) );
    } else {
      throw Exceptions.toUndeclared( builder.toString( ) );
    }
  }
  
  enum DeactivateHostFunction implements Function<Host, Function<String, Runnable>> {
    INSTANCE;
    /**
     * @see com.google.common.base.Function#apply(java.lang.Object)
     */
    @Override
    public Function<String, Runnable> apply( final Host host ) {
      return new Function<String, Runnable>( ) {
        
        @Override
        public Runnable apply( String ctx ) {
          final String hostName = host.getBindAddress( ).getHostAddress( );
          final String contextName = ctx.startsWith( "eucalyptus_" )
            ? ctx
            : "eucalyptus_" + ctx;
          Runnable removeRunner = new Runnable( ) {
            
            @Override
            public void run( ) {
              try {
                final DriverDatabaseClusterMBean cluster = lookup( contextName );
                
                try {
                  if ( cluster.getActiveDatabases( ).contains( hostName ) ) {
                    LOG.debug( "Deactivating database connections for: " + host.getDisplayName( ) + " to context: " + contextName );
                    cluster.deactivate( hostName );
                  }
                  LOG.debug( "Removing database connections for: " + host.getDisplayName( ) + " to context: " + contextName );
                  cluster.remove( hostName );
                } catch ( final Exception ex ) {
                  LOG.error( ex, ex );
                }
              } catch ( final NoSuchElementException ex1 ) {
                LOG.error( ex1, ex1 );
              } catch ( final Exception ex1 ) {
                LOG.error( ex1, ex1 );
              }
            }
            
            @Override
            public String toString( ) {
              return "Databases.disable(): " + host.getDisplayName( ) + " " + contextName;
            }
            
          };
          return removeRunner;
        }
      };
    }
  }
  
  enum ActivateHostFunction implements Function<Host, Function<String, Runnable>> {
    INSTANCE;
    /**
     * @see com.google.common.base.Function#apply(java.lang.Object)
     */
    @Override
    public Function<String, Runnable> apply( final Host host ) {
      return new Function<String, Runnable>( ) {
        
        @Override
        public Runnable apply( String ctx ) {
          final String hostName = host.getBindAddress( ).getHostAddress( );
          final String contextName = ctx.startsWith( "eucalyptus_" )
            ? ctx
            : "eucalyptus_" + ctx;
          Runnable removeRunner = new Runnable( ) {
            
            @Override
            public void run( ) {
              try {
                DriverDatabaseClusterMBean cluster = LookupPersistenceContextDatabaseCluster.INSTANCE.apply( contextName );
                final String dbUrl = "jdbc:" + ServiceUris.remote( Database.class, host.getBindAddress( ), contextName );
                final String dbPass = SystemIds.databasePassword( );
                final String realJdbcDriver = Databases.getDriverName( );
                if ( !cluster.getActiveDatabases( ).contains( hostName ) && !cluster.getInactiveDatabases( ).contains( hostName ) ) {
                  cluster.add( hostName, realJdbcDriver, dbUrl );
                } else if ( cluster.getActiveDatabases( ).contains( hostName ) ) {
                  cluster.deactivate( hostName );
                }
                final InactiveDatabaseMBean database = Databases.lookupDisabled( contextName, hostName );
                database.setUser( "eucalyptus" );
                database.setPassword( dbPass );
                if ( !Hosts.isCoordinator( ) && host.isLocalHost( ) && BootstrapArgs.isCloudController( ) && !Databases.isSynchronized( ) ) {
                  cluster.activate( hostName, "full" );
                } else if ( host.hasSynced( ) ) {
                  cluster.activate( hostName, "passive" );
                }
              } catch ( final NoSuchElementException ex1 ) {
                LOG.error( ex1, ex1 );
              } catch ( final Exception ex1 ) {
                LOG.error( ex1, ex1 );
              }
            }
            
            @Override
            public String toString( ) {
              return "Databases.enable(): " + host.getDisplayName( ) + " " + contextName;
            }
            
          };
          return removeRunner;
        }
      };
    }
  }
  
  private static InactiveDatabaseMBean lookupDisabled( final String contextName, final String hostName ) throws NoSuchElementException {
    final InactiveDatabaseMBean database = Mbeans.lookup( jdbcJmxDomain,
                                                          ImmutableMap.builder( )
                                                                      .put( "cluster", contextName )
                                                                      .put( "database", hostName )
                                                                      .build( ),
                                                          InactiveDatabaseMBean.class );
    return database;
  }
  
  static void disable( final Host host ) {
    if ( !host.hasDatabase( ) ) {
      return;
    } else {
      if ( host.isLocalHost( ) ) {
        syncState.set( SyncState.DESYNCING );
        try {
          runDbStateChange( DeactivateHostFunction.INSTANCE.apply( host ) );
          syncState.set( SyncState.NOTSYNCED );
        } catch ( Exception ex ) {
          LOG.error( ex );
          Logs.exhaust( ).error( ex, ex );
          syncState.set( SyncState.NOTSYNCED );
        }
      } else {
        runDbStateChange( DeactivateHostFunction.INSTANCE.apply( host ) );
      }
    }
  }
  
  static void enable( final Host host ) {
    if ( !host.hasBootstrapped( ) || !host.hasDatabase( ) ) {
      return;
    } else {
      LOG.info( "Hosts.entryAdded(): Host is operational => " + host );
      if ( host.isLocalHost( ) ) {
        if ( syncState.compareAndSet( SyncState.NOTSYNCED, SyncState.SYNCING ) ) {
          try {
            runDbStateChange( ActivateHostFunction.INSTANCE.apply( host ) );
            syncState.set( SyncState.SYNCED );
          } catch ( Exception ex ) {
            runDbStateChange( DeactivateHostFunction.INSTANCE.apply( host ) );
            LOG.error( ex );
            Logs.extreme( ).error( ex, ex );
            syncState.set( SyncState.NOTSYNCED );
          }
        }
      } else {
        runDbStateChange( ActivateHostFunction.INSTANCE.apply( host ) );
      }
    }
  }
  
  enum LookupPersistenceContextDatabaseCluster implements Function<String, DriverDatabaseClusterMBean> {
    INSTANCE;
    @Override
    public DriverDatabaseClusterMBean apply( String ctx ) {
      final String contextName = ctx.startsWith( "eucalyptus_" )
        ? ctx
        : "eucalyptus_" + ctx;
      final DriverDatabaseClusterMBean cluster = lookup( contextName );
      return cluster;
    }
  }
  
  static boolean shouldInitialize( ) {//GRZE:WARNING:HACKHACKHACK do not duplicate pls thanks.
    for ( final Host h : Hosts.listActiveDatabases( ) ) {
      final String url = String.format( "jdbc:%s", ServiceUris.remote( Database.class, h.getBindAddress( ), "eucalyptus_config" ) );
      try {
        final Connection conn = DriverManager.getConnection( url, Databases.getUserName( ), Databases.getPassword( ) );
        try {
          final PreparedStatement statement = conn.prepareStatement( "select config_component_hostname from eucalyptus_config.config_component_base where config_component_partition='eucalyptus';" );
          final ResultSet result = statement.executeQuery( );
          while ( result.next( ) ) {
            final Object columnValue = result.getObject( 1 );
            if ( Internets.testLocal( columnValue.toString( ) ) ) {
              return true;
            }
          }
        } finally {
          conn.close( );
        }
      } catch ( final Exception ex ) {
        Hosts.LOG.error( ex, ex );
      }
    }
    return false;
  }
  
  /**
   * @return
   */
  public static Boolean isSynchronized( ) {
    if ( Hosts.isCoordinator( ) ) {
      syncState.set( SyncState.SYNCED );
    }
    return SyncState.SYNCED.equals( syncState.get( ) );
  }
  
  public static String getUserName( ) {
    return DB_USERNAME;
  }
  
  public static String getDatabaseName( ) {
    return DB_NAME;
  }
  
  public static String getPassword( ) {
    return SystemIds.databasePassword( );
  }
  
  public static String getDriverName( ) {
    return singleton.getDriverName( );
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
  }
  
  @RunDuring( Bootstrap.Stage.DatabaseInit )
  @Provides( Empyrean.class )
  @DependsLocal( Eucalyptus.class )
  public static class ScriptedDbBootstrapper extends Bootstrapper.Simple implements DatabaseBootstrapper {
    DatabaseBootstrapper db;
    
    public ScriptedDbBootstrapper( ) {
      super( );
      try {
        this.db = Groovyness.newInstance( "setup_db" );
      } catch ( ScriptExecutionFailedException ex ) {
        LOG.error( ex, ex );
      }
    }
    
    public boolean load( ) throws Exception {
      return this.db.load( );
    }
    
    public boolean start( ) throws Exception {
      return this.db.start( );
    }
    
    public boolean stop( ) throws Exception {
      return this.db.stop( );
    }
    
    public void destroy( ) throws Exception {
      this.db.destroy( );
    }
    
    public boolean isRunning( ) {
      return this.db.isRunning( );
    }
    
    public void hup( ) {
      this.db.hup( );
    }
    
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
    public boolean check( ) throws Exception {
      return this.db.isRunning( );
    }
    
    /**
     * @see com.eucalyptus.bootstrap.DatabaseBootstrapper#getJdbcScheme()
     */
    @Override
    public String getJdbcScheme( ) {
      return this.db.getJdbcScheme( );
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
  
  public static String getJdbcScheme( ) {
    return singleton.getJdbcScheme( );
  }
  
}
