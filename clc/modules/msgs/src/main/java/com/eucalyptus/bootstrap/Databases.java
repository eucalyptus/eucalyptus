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
 * File also includes source under the following license:
 *    
 * HA-JDBC: High-Availability JDBC
 * Copyright (c) 2004-2007 Paul Ferraro
 * 
 * This library is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by the 
 * Free Software Foundation; either version 2.1 of the License, or (at your 
 * option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, 
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Contact: ferraro@users.sourceforge.net
 * @author Paul Ferraro
 *
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.bootstrap;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import javax.persistence.LockTimeoutException;
import net.sf.hajdbc.Dialect;
import net.sf.hajdbc.ForeignKeyConstraint;
import net.sf.hajdbc.InactiveDatabaseMBean;
import net.sf.hajdbc.Messages;
import net.sf.hajdbc.SequenceProperties;
import net.sf.hajdbc.SynchronizationContext;
import net.sf.hajdbc.SynchronizationStrategy;
import net.sf.hajdbc.TableProperties;
import net.sf.hajdbc.UniqueConstraint;
import net.sf.hajdbc.sql.DriverDatabaseClusterMBean;
import net.sf.hajdbc.util.SQLExceptionFactory;
import net.sf.hajdbc.util.Strings;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Hosts.DbFilter;
import com.eucalyptus.bootstrap.Hosts.SyncedDbFilter;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.Eucalyptus.Database;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.records.Logs;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.scripting.ScriptExecutionFailedException;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.Mbeans;
import com.eucalyptus.util.async.Futures;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Databases {
  public static class DatabaseStateException extends IllegalStateException {
    
    /**
     * @param string
     */
    public DatabaseStateException( String string ) {
      super( string );
    }
    
  }
  
  private static final int                    MAX_TX_START_SYNC_RETRIES = 120;
  private static final AtomicInteger          counter                   = new AtomicInteger( 500 );
  private static final Predicate<Host>        FILTER_SYNCING_DBS        = Predicates.and( DbFilter.INSTANCE, Predicates.not( SyncedDbFilter.INSTANCE ) );
  private static final ScriptedDbBootstrapper singleton                 = new ScriptedDbBootstrapper( );
  private static Logger                       LOG                       = Logger.getLogger( Databases.class );
  private static final String                 DB_NAME                   = "eucalyptus";
  private static final String                 DB_USERNAME               = DB_NAME;
  private static final String                 jdbcJmxDomain             = "net.sf.hajdbc";
  private static final ExecutorService        dbSyncExecutors           = Executors.newCachedThreadPool( );                                              //NOTE:GRZE:special case thread handling.
  private static final ReentrantReadWriteLock canHas                    = new ReentrantReadWriteLock( );
  
  enum SyncState {
    IRRELEVANT,
    NOTSYNCED,
    SYNCING {
      
      @Override
      public boolean set( ) {
        return syncState.compareAndSet( NOTSYNCED, SYNCING );
      }
    },
    DESYNCING,
    SYNCED {
      
      @Override
      public boolean isCurrent( ) {
        if ( Hosts.isCoordinator( ) ) {
          syncState.set( this );
        }
        return super.isCurrent( );
      }
      
    };
    private static final AtomicReference<SyncState> syncState = new AtomicReference<SyncState>( SyncState.NOTSYNCED );
    
    public static SyncState get( ) {
      return syncState.get( );
    }
    
    public boolean set( ) {
      syncState.set( this );
      return true;
    }
    
    public boolean isCurrent( ) {
      return this.equals( syncState.get( ) );
    }
  }
  
  enum ExecuteRunnable implements Function<Runnable, Future<Runnable>> {
    INSTANCE;
    @Override
    public Future<Runnable> apply( Runnable input ) {
      Logs.extreme( ).debug( "SUBMIT: " + input );
      return dbSyncExecutors.submit( input, input );
    }
  }
  
  @Provides( Empyrean.class )
  @RunDuring( Bootstrap.Stage.PoolInit )
  public static class DatabasePoolBootstrapper extends Bootstrapper.Simple {
    private static final int INITIAL_DB_SYNC_RETRY_WAIT = 5;
    
    @Override
    public boolean load( ) throws Exception {
      Hosts.awaitDatabases( );
      Groovyness.run( "setup_dbpool.groovy" );
      OrderedShutdown.registerShutdownHook( Empyrean.class, new Runnable( ) {
        
        @Override
        public void run( ) {
          try {
            for ( String ctx : PersistenceContexts.list( ) ) {
              try {
                DriverDatabaseClusterMBean db = Databases.lookup( ctx );
                for ( String host : db.getInactiveDatabases( ) ) {
                  Databases.disable( host );
                }
                for ( String host : db.getActiveDatabases( ) ) {
                  Databases.disable( host );
                }
              } catch ( Exception ex ) {
                LOG.error( ex );
              }
            }
          } catch ( NoSuchElementException ex ) {
            LOG.error( ex );
          }
        }
      } );
      TimeUnit.SECONDS.sleep( INITIAL_DB_SYNC_RETRY_WAIT );
      if ( !Hosts.isCoordinator( ) && Hosts.localHost( ).hasDatabase( ) ) {
        while ( !Databases.enable( Hosts.localHost( ) ) ) {
          LOG.warn( LogUtil.subheader( "Synchronization of the database failed: " + Hosts.localHost( ) ) );
          if ( counter.decrementAndGet( ) == 0 ) {
            LOG.fatal( "Restarting process to force re-synchronization." );
            System.exit( 123 );
          } else {
            LOG.warn( "Sleeping for " + INITIAL_DB_SYNC_RETRY_WAIT + " seconds before trying again." );
            TimeUnit.SECONDS.sleep( INITIAL_DB_SYNC_RETRY_WAIT );
          }
        }
        Hosts.UpdateEntry.INSTANCE.apply( Hosts.localHost( ) );
        LOG.info( LogUtil.subheader( "Database synchronization complete: " + Hosts.localHost( ) ) );
      }
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
    Logs.extreme( ).info( "DB STATE CHANGE: " + runnableFunction );
    try {
      Logs.extreme( ).info( "Attempting to acquire db state lock: " + runnableFunction );
      if ( canHas.writeLock( ).tryLock( 5, TimeUnit.MINUTES ) ) {
        try {
          Logs.extreme( ).info( "Acquired db state lock: " + runnableFunction );
          Map<Runnable, Future<Runnable>> runnables = Maps.newHashMap( );
          for ( final String ctx : PersistenceContexts.list( ) ) {
            Runnable run = runnableFunction.apply( ctx );
            runnables.put( run, ExecuteRunnable.INSTANCE.apply( run ) );
          }
          Map<Runnable, Future<Runnable>> succeeded = Futures.waitAll( runnables );
          MapDifference<Runnable, Future<Runnable>> failed = Maps.difference( runnables, succeeded );
          StringBuilder builder = new StringBuilder( );
          builder.append( Joiner.on( "\nSUCCESS: " ).join( succeeded.keySet( ) ) );
          builder.append( Joiner.on( "\nFAILED:  " ).join( failed.entriesOnlyOnLeft( ).keySet( ) ) );
          Logs.extreme( ).debug( builder.toString( ) );
          if ( !failed.entriesOnlyOnLeft( ).isEmpty( ) ) {
            throw Exceptions.toUndeclared( builder.toString( ) );
          }
        } finally {
          canHas.writeLock( ).unlock( );
        }
      } else {
        throw new LockTimeoutException( "DB STATE CHANGE ABORTED (failed to get lock): " + runnableFunction );
      }
    } catch ( RuntimeException ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
      throw ex;
    } catch ( InterruptedException ex ) {
      Exceptions.maybeInterrupted( ex );
      throw Exceptions.toUndeclared( ex );
    }
  }
  
  enum LivenessCheckHostFunction implements Function<String, Function<String, Runnable>> {
    INSTANCE;
    public Function<String, Runnable> apply( final String hostName ) {
      return new Function<String, Runnable>( ) {
        @Override
        public Runnable apply( final String ctx ) {
          final String contextName = ctx.startsWith( "eucalyptus_" ) ? ctx : "eucalyptus_" + ctx;
          Runnable removeRunner = new Runnable( ) {
            @Override
            public void run( ) {
              DriverDatabaseClusterMBean cluster = lookup( ctx );
              if ( !cluster.isAlive( contextName ) ) {
                throw Exceptions.toUndeclared( "Database on host " + hostName + " failed liveness check and will be deactived." );
              }
            }
            
            @Override
            public String toString( ) {
              return "Databases.isAlive(): " + hostName + " " + contextName;
            }
          };
          return removeRunner;
        }
      };
    }
  }
  
  enum DeactivateHostFunction implements Function<String, Function<String, Runnable>> {
    INSTANCE;
    /**
     * @see com.google.common.base.Function#apply(java.lang.Object)
     */
    @Override
    public Function<String, Runnable> apply( final String hostName ) {
      return new Function<String, Runnable>( ) {
        @Override
        public Runnable apply( final String ctx ) {
          final String contextName = ctx.startsWith( "eucalyptus_" ) ? ctx : "eucalyptus_" + ctx;
          Runnable removeRunner = new Runnable( ) {
            @Override
            public void run( ) {
              if ( Internets.testLocal( hostName ) ) {
                return;
              }
              try {
                final DriverDatabaseClusterMBean cluster = lookup( contextName );
                try {
                  cluster.getDatabase( hostName );
                } catch ( Exception ex1 ) {
                  return;
                }
                LOG.info( "Tearing down database connections for: " + hostName + " to context: " + contextName );
                for ( int i = 0; i < 10; i++ ) {
                  if ( cluster.getActiveDatabases( ).contains( hostName ) ) {
                    try {
                      Logs.extreme( ).info( "Deactivating database connections for: " + hostName + " to context: " + contextName );
                      cluster.deactivate( hostName );
                      Logs.extreme( ).info( "Deactived database connections for: " + hostName + " to context: " + contextName );
                      try {
                        if ( !Hosts.contains( hostName ) ) {
                          Logs.extreme( ).info( "Removing database connections for: " + hostName + " to context: " + contextName );
                          cluster.remove( hostName );
                          Logs.extreme( ).info( "Removed database connections for: " + hostName + " to context: " + contextName );
                        }
                        return;
                      } catch ( IllegalStateException ex ) {
                        Logs.extreme( ).debug( ex, ex );
                      }
                    } catch ( Exception ex ) {
                      LOG.error( ex );
                      Logs.extreme( ).error( ex, ex );
                    }
                  } else if ( cluster.getInactiveDatabases( ).contains( hostName ) && !Hosts.contains( hostName ) ) {
                    try {
                      Logs.extreme( ).info( "Removing database connections for: " + hostName + " to context: " + contextName );
                      cluster.remove( hostName );
                      Logs.extreme( ).info( "Removed database connections for: " + hostName + " to context: " + contextName );
                      return;
                    } catch ( Exception ex ) {
                      LOG.error( ex );
                      Logs.extreme( ).error( ex, ex );
                    }
                  }
                }
              } catch ( final Exception ex1 ) {
                LOG.error( ex1 );
                Logs.extreme( ).error( ex1, ex1 );
              }
            }
            
            @Override
            public String toString( ) {
              return "Databases.disable(): " + hostName + " " + contextName;
            }
          };
          return removeRunner;
        }
        
        @Override
        public String toString( ) {
          return "Databases.disable(): " + hostName;
        }
      };
    }
    
    @Override
    public String toString( ) {
      return "Databases.disable()";
    }
  }
  
  enum ActivateHostFunction implements Function<Host, Function<String, Runnable>> {
    INSTANCE;
    private static void prepareConnections( final Host host, final String contextName ) throws NoSuchElementException {
      final String hostName = host.getDisplayName( );
      final String dbPass = SystemIds.databasePassword( );
      final InactiveDatabaseMBean database = Databases.lookupInactiveDatabase( contextName, hostName );
      database.setUser( "eucalyptus" );
      database.setPassword( dbPass );
      database.setWeight( Hosts.isCoordinator( host ) ? 100 : 1 );
      database.setLocal( host.isLocalHost( ) );
    }
    
    @Override
    public Function<String, Runnable> apply( final Host host ) {
      return new Function<String, Runnable>( ) {
        @Override
        public Runnable apply( final String ctx ) {
          final String hostName = host.getBindAddress( ).getHostAddress( );
          final String contextName = ctx.startsWith( "eucalyptus_" ) ? ctx : "eucalyptus_" + ctx;
          Runnable removeRunner = new Runnable( ) {
            @Override
            public void run( ) {
              try {
                final boolean fullSync = !Hosts.isCoordinator( ) && host.isLocalHost( ) && BootstrapArgs.isCloudController( ) && !Databases.isSynchronized( );
                final boolean passiveSync = !fullSync && host.hasSynced( );
                if ( !fullSync && !passiveSync ) {
                  throw Exceptions.toUndeclared( "Host is not ready to be activated: " + host );
                } else {
                  DriverDatabaseClusterMBean cluster = LookupPersistenceContextDatabaseCluster.INSTANCE.apply( contextName );
                  final String dbUrl = "jdbc:" + ServiceUris.remote( Database.class, host.getBindAddress( ), contextName );
                  final String realJdbcDriver = Databases.getDriverName( );
                  String syncStrategy = "passive";
                  boolean activated = cluster.getActiveDatabases( ).contains( hostName );
                  boolean deactivated = cluster.getInactiveDatabases( ).contains( hostName );
                  syncStrategy = ( fullSync ? "full" : "passive" );
                  if ( activated ) {
//                    LOG.info( "Deactivating existing database connections to: " + host );
//                    cluster.deactivate( hostName );
//                    ActivateHostFunction.prepareConnections( host, contextName );
                    return;
                  } else if ( deactivated ) {
                    ActivateHostFunction.prepareConnections( host, contextName );
                  } else {
                    LOG.info( "Creating database connections for: " + host );
                    try {
                      cluster.getDatabase( hostName );
                    } catch ( IllegalArgumentException ex2 ) {
                      try {
                        cluster.add( hostName, realJdbcDriver, dbUrl );
                        Logs.extreme( ).debug( "Added db connections for host: " + hostName );
                      } catch ( IllegalArgumentException ex ) {
                        Logs.extreme( ).debug( "Skipping addition of db connections for host which already exists: " + hostName );
                      } catch ( IllegalStateException ex ) {
                        if ( Exceptions.isCausedBy( ex, InstanceAlreadyExistsException.class ) ) {
                          ManagementFactory.getPlatformMBeanServer( ).unregisterMBean(
                            new ObjectName( "net.sf.hajdbc:cluster=" + ctx + ",database=" + hostName ) );
                          cluster.add( hostName, realJdbcDriver, dbUrl );
                        } else {
                          throw ex;
                        }
                      }
                    }
                    ActivateHostFunction.prepareConnections( host, contextName );
                  }
                  try {
                    if ( fullSync ) {
                      LOG.info( "Full sync of database on: " + host );
                    } else {
                      LOG.info( "Passive activation of database connections to: " + host );
                    }
                    cluster.activate( hostName, syncStrategy );
                    if ( fullSync ) {
                      LOG.info( "Full sync of database on: " + host + " using " + cluster.getActiveDatabases( ) );
                    } else {
                      LOG.info( "Passive activation of database on: " + host + " using " + cluster.getActiveDatabases( ) );
                    }
                    return;
                  } catch ( Exception ex ) {
                    throw Exceptions.toUndeclared( ex );
                  }
                }
              } catch ( final NoSuchElementException ex1 ) {
                LOG.error( ex1 );
                Logs.extreme( ).error( ex1, ex1 );
                return;
              } catch ( final IllegalStateException ex1 ) {
                LOG.error( ex1 );
                Logs.extreme( ).error( ex1, ex1 );
                return;
              } catch ( final Exception ex1 ) {
                LOG.error( ex1 );
                Logs.extreme( ).error( ex1, ex1 );
                throw Exceptions.toUndeclared( "Failed to activate host " + host + " because of: " + ex1.getMessage( ), ex1 );
              }
            }
            
            @Override
            public String toString( ) {
              return "Databases.enable(): " + host.getDisplayName( ) + " " + contextName;
            }
          };
          return removeRunner;
        }
        
        @Override
        public String toString( ) {
          return "Databases.enable(): " + host;
        }
        
      };
    }
    
    @Override
    public String toString( ) {
      return "Databases.enable()";
    }
    
    private static void rollback( final Host host, Exception ex ) {
      try {
        Databases.runDbStateChange( Databases.DeactivateHostFunction.INSTANCE.apply( host.getDisplayName( ) ) );
      } catch ( LockTimeoutException ex1 ) {
        Databases.LOG.error( "Databases.enable(): failed because of: " + ex.getMessage( ) );
      } catch ( Exception ex1 ) {
        Databases.LOG.error( "Databases.enable(): failed because of: " + ex.getMessage( ) );
        Logs.extreme( ).error( ex, ex );
      }
    }
  }
  
  private static InactiveDatabaseMBean lookupInactiveDatabase( final String contextName, final String hostName ) throws NoSuchElementException {
    final InactiveDatabaseMBean database = Mbeans.lookup( jdbcJmxDomain,
                                                          ImmutableMap.builder( )
                                                                      .put( "cluster", contextName )
                                                                      .put( "database", hostName )
                                                                      .build( ),
                                                          InactiveDatabaseMBean.class );
    return database;
  }
  
  static boolean isAlive( final String hostName ) {
    if ( !Internets.testLocal( hostName ) ) {
      try {
        runDbStateChange( LivenessCheckHostFunction.INSTANCE.apply( hostName ) );
        return true;
      } catch ( Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
        return disable( hostName );
      }
    } else {
      try {
        runDbStateChange( LivenessCheckHostFunction.INSTANCE.apply( hostName ) );
        return true;
      } catch ( Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
        //GRZE:TODO: host-wide failure case here.
        return false;
      }
    }
  }
  
  static boolean disable( final String hostName ) {
    if ( !Bootstrap.isFinished( ) ) {
      return false;
    } else if ( Internets.testLocal( hostName ) && !BootstrapArgs.isCloudController( ) ) {
      return true;
    } else if ( Internets.testLocal( hostName ) && BootstrapArgs.isCloudController( ) ) {
//      SyncState.DESYNCING.set( );
//      try {
//        runDbStateChange( DeactivateHostFunction.INSTANCE.apply( hostName ) );
//        SyncState.NOTSYNCED.set( );
//        return true;
//      } catch ( Exception ex ) {
//        SyncState.NOTSYNCED.set( );
//        Logs.extreme( ).debug( ex );
//        return false;
//      }
      return true;
    } else if ( ActiveHostSet.ACTIVATED.get( ).contains( hostName ) ) {
      try {
        runDbStateChange( DeactivateHostFunction.INSTANCE.apply( hostName ) );
        return true;
      } catch ( Exception ex ) {
        Logs.extreme( ).debug( ex );
        return false;
      }
    } else {
      try {
        runDbStateChange( DeactivateHostFunction.INSTANCE.apply( hostName ) );
        return true;
      } catch ( Exception ex ) {
        Logs.extreme( ).debug( ex );
        return false;
      }
    }
  }
  
  static boolean enable( final Host host ) {
    if ( !host.hasDatabase( ) ) {
      return false;
    } else {
      if ( host.isLocalHost( ) ) {
        if ( SyncState.SYNCING.set( ) ) {
          try {
            runDbStateChange( ActivateHostFunction.INSTANCE.apply( host ) );
            SyncState.SYNCED.set( );
            return true;
          } catch ( LockTimeoutException ex ) {
            SyncState.NOTSYNCED.set( );
            return false;
          } catch ( Exception ex ) {
            SyncState.NOTSYNCED.set( );
            LOG.error( ex );
            Logs.extreme( ).error( ex, ex );
            return false;
          }
        } else if ( !SyncState.SYNCING.isCurrent( ) ) {
          try {
            runDbStateChange( ActivateHostFunction.INSTANCE.apply( host ) );
            return true;
          } catch ( LockTimeoutException ex ) {
            return false;
          } catch ( Exception ex ) {
            LOG.error( ex );
            Logs.extreme( ).error( ex, ex );
            return false;
          }
        } else {
          try {
            runDbStateChange( ActivateHostFunction.INSTANCE.apply( host ) );
            SyncState.SYNCED.set( );
            return true;
          } catch ( LockTimeoutException ex ) {
            SyncState.NOTSYNCED.set( );
            return false;
          } catch ( Exception ex ) {
            SyncState.NOTSYNCED.set( );
            LOG.error( ex, ex );
            return false;
          }
        }
      } else if ( !ActiveHostSet.ACTIVATED.get( ).contains( host.getDisplayName( ) ) ) {
        try {
          runDbStateChange( ActivateHostFunction.INSTANCE.apply( host ) );
          return true;
        } catch ( LockTimeoutException ex ) {
          return false;
        } catch ( Exception ex ) {
          Logs.extreme( ).debug( ex );
          ActivateHostFunction.rollback( host, ex );
          return false;
        }
      } else {
        return ActiveHostSet.ACTIVATED.get( ).contains( host.getDisplayName( ) );
      }
    }
  }
  
  enum LookupPersistenceContextDatabaseCluster implements Function<String, DriverDatabaseClusterMBean> {
    INSTANCE;
    @Override
    public DriverDatabaseClusterMBean apply( String ctx ) {
      final String contextName = ctx.startsWith( "eucalyptus_" ) ? ctx : "eucalyptus_" + ctx;
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
          final PreparedStatement statement = conn.prepareStatement( "select config_component_hostname from config_component_base where config_component_partition='eucalyptus';" );
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
        LOG.error( ex, ex );
      }
    }
    return false;
  }
  
  /**
   * @return
   *         LockTimeoutException
   */
  public static Boolean isSynchronized( ) {
    return SyncState.SYNCED.isCurrent( );
  }
  
  public static Boolean isVolatile( ) {
    if ( !Bootstrap.isFinished( ) || BootstrapArgs.isInitializeSystem( ) ) {
      return false;
    } else if ( !Hosts.isCoordinator( ) && BootstrapArgs.isCloudController( ) ) {
      return !isSynchronized( ) || !activeHosts.get( ).containsAll( hostDatabases.get( ) );
    } else if ( !activeHosts.get( ).equals( hostDatabases.get( ) ) ) {
      return true;
    } else {
      return !Hosts.list( FILTER_SYNCING_DBS ).isEmpty( );
    }
  }
  
  enum ActiveHostSet implements Supplier<Set<String>> {
    ACTIVATED {
      @Override
      public Set<String> get( ) {
        Set<String> hosts = DBHOSTS.get( );
        Set<String> union = Sets.newHashSet( );
        Set<String> intersection = Sets.newHashSet( hosts );
        Logs.extreme( ).debug( "ActiveHostSet: universe of db hosts: " + hosts );
        for ( String ctx : PersistenceContexts.list( ) ) {
          try {
            Set<String> activeDatabases = Databases.lookup( ctx ).getActiveDatabases( );
            union.addAll( activeDatabases );
            intersection.retainAll( activeDatabases );
          } catch ( Exception ex ) {
          }
      }
      Logs.extreme( ).debug( "ActiveHostSet: union of activated db connections: " + union );
      Logs.extreme( ).debug( "ActiveHostSet: intersection of db hosts and activated db connections: " + intersection );
      boolean dbVolatile = !hosts.equals( intersection );
      String msg = String.format( "ActiveHostSet: %-14.14s %s%s%s", dbVolatile ? "volatile" : "synchronized", hosts, dbVolatile ? "!=" : "=", intersection );
      if ( dbVolatile ) {
        if ( last.compareAndSet( false, dbVolatile ) ) {
          LOG.warn( msg );
        } else {
          LOG.debug( msg );
        }
      } else {
        if ( last.compareAndSet( true, dbVolatile ) ) {
          LOG.warn( msg );
        } else {
          Logs.extreme( ).info( msg );
        }
      }
      return intersection;
    }
    },
    DBHOSTS {
      @Override
      public Set<String> get( ) {
        return Sets.newHashSet( Collections2.transform( Hosts.listDatabases( ), Hosts.NameTransform.INSTANCE ) );
      }
    };
    private static final AtomicBoolean last = new AtomicBoolean( false );
    
    @Override
    public abstract Set<String> get( );
    
  }
  
  private static Supplier<Set<String>>        activeHosts                    = Suppliers.memoizeWithExpiration( ActiveHostSet.ACTIVATED, 2, TimeUnit.SECONDS );
  private static Supplier<Set<String>>        hostDatabases                  = Suppliers.memoizeWithExpiration( ActiveHostSet.DBHOSTS, 1, TimeUnit.SECONDS );
  
  private static Predicate<StackTraceElement> notStackFilterYouAreLookingFor = Predicates.or(
                                                                               Threads.filterStackByQualifiedName( "com\\.eucalyptus\\.entities\\..*" ),
                                                                                              Threads.filterStackByQualifiedName( "java\\.lang\\.Thread.*" ),
                                                                                              Threads.filterStackByQualifiedName( "com\\.eucalyptus\\.system\\.Threads.*" ),
                                                                                              Threads.filterStackByQualifiedName( "com\\.eucalyptus\\.bootstrap\\.Databases.*" ) );
  private static Predicate<StackTraceElement> stackFilter                    = Predicates.not( notStackFilterYouAreLookingFor );
  
  public static void awaitSynchronized( ) {
    if ( !isVolatile( ) ) {
      return;
    } else {
      Collection<StackTraceElement> stack = Threads.filteredStack( stackFilter );
      String caller = ( stack.isEmpty( ) ? "" : stack.iterator( ).next( ).toString( ) );
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
  
  public static void check( ) {
    for ( String ctx : PersistenceContexts.list( ) ) {
      try {
        DriverDatabaseClusterMBean db = lookup( ctx );
        for ( String host : db.getActiveDatabases( ) ) {
          if ( Hosts.lookup( host ) == null ) {
            disable( host );
          }
        }
      } catch ( NoSuchElementException ex ) {
        LOG.error( ex, ex );
      }
      return;
    }
  }
  
  public static final class SynchronizationSupport {
    private SynchronizationSupport( ) {
      // Hide
    }
    
    /**
     * Drop all foreign key constraints on the target database
     * * @param <D>
     * 
     * @param context a synchronization context
     * @throws SQLException if database error occurs
     */
    public static <D> void dropForeignKeys( SynchronizationContext<D> context ) throws SQLException {
      Dialect dialect = context.getDialect( );
      Connection connection = null;
      Statement statement = null;
      try {
        connection = context.getConnection( context.getTargetDatabase( ) );
        statement = connection.createStatement( );
        for ( TableProperties table : context.getTargetDatabaseProperties( ).getTables( ) ) {
          for ( ForeignKeyConstraint constraint : table.getForeignKeyConstraints( ) ) {
            String sql = dialect.getDropForeignKeyConstraintSQL( constraint );
            Logs.extreme( ).info( sql );
            statement.addBatch( sql );
          }
        }
        statement.executeBatch( );
      } catch ( SQLException sqle ) {
        LOG.error( sqle );
        Logs.extreme( ).error( sqle, sqle );
        throw sqle;
      } finally {
        try {
          statement.close( );
        } catch ( Exception e ) {
          LOG.error( e );
        }
      }
    }
    
    /**
     * Restores all foreign key constraints on the target database
     * * @param <D>
     * 
     * @param context a synchronization context
     * @throws SQLException if database error occurs
     */
    public static <D> void restoreForeignKeys( SynchronizationContext<D> context ) throws SQLException {
      Dialect dialect = context.getDialect( );
      Connection connection = null;
      Statement statement = null;
      try {
        connection = context.getConnection( context.getTargetDatabase( ) );
        statement = connection.createStatement( );
        for ( TableProperties table : context.getSourceDatabaseProperties( ).getTables( ) ) {
          for ( ForeignKeyConstraint constraint : table.getForeignKeyConstraints( ) ) {
            String sql = dialect.getCreateForeignKeyConstraintSQL( constraint );
            Logs.extreme( ).info( sql );
            statement.addBatch( sql );
          }
        }
        statement.executeBatch( );
      } catch ( SQLException sqle ) {
        LOG.error( sqle );
        Logs.extreme( ).error( sqle, sqle );
        throw sqle;
      } finally {
        try {
          statement.close( );
        } catch ( Exception e ) {
          LOG.error( e );
        }
      }
      
    }
    
    /**
     * Synchronizes the sequences on the target database with the source database.
     * * @param <D>
     * 
     * @param context a synchronization context
     * @throws SQLException if database error occurs
     */
    public static <D> void synchronizeSequences( final SynchronizationContext<D> context ) throws SQLException {
      Collection<SequenceProperties> sequences = context.getSourceDatabaseProperties( ).getSequences( );
      if ( !sequences.isEmpty( ) ) {
        net.sf.hajdbc.Database<D> sourceDatabase = context.getSourceDatabase( );
        Set<net.sf.hajdbc.Database<D>> databases = context.getActiveDatabaseSet( );
        ExecutorService executor = context.getExecutor( );
        Dialect dialect = context.getDialect( );
        Map<SequenceProperties, Long> sequenceMap = new HashMap<SequenceProperties, Long>( );
        Map<net.sf.hajdbc.Database<D>, Future<Long>> futureMap = new HashMap<net.sf.hajdbc.Database<D>, Future<Long>>( );
        for ( SequenceProperties sequence : sequences ) {
          final String sql = dialect.getNextSequenceValueSQL( sequence );
          Logs.extreme( ).info( sql );
          for ( final net.sf.hajdbc.Database<D> database : databases ) {
            Callable<Long> task = new Callable<Long>( )
            {
              public Long call( ) throws SQLException
              {
                Statement statement = null;
                ResultSet resultSet = null;
                try {
                  statement = context.getConnection( database ).createStatement( );
                  resultSet = statement.executeQuery( sql );
                  resultSet.next( );
                  long value = resultSet.getLong( 1 );
                  return value;
                } catch ( SQLException sqle ) {
                  LOG.error( sqle );
                  Logs.extreme( ).error( sqle, sqle );
                  throw sqle;
                } finally {
                  try {
                    statement.close( );
                  } catch ( Exception e ) {
                    LOG.error( e );
                  }
                }
              }
            };
            futureMap.put( database, executor.submit( task ) );
          }
          try {
            Long sourceValue = futureMap.get( sourceDatabase ).get( );
            sequenceMap.put( sequence, sourceValue );
            for ( net.sf.hajdbc.Database<D> database : databases ) {
              if ( !database.equals( sourceDatabase ) ) {
                Long value = futureMap.get( database ).get( );
                if ( !value.equals( sourceValue ) ) {
                  throw new SQLException( Messages.getMessage( Messages.SEQUENCE_OUT_OF_SYNC, sequence, database, value, sourceDatabase, sourceValue ) );
                }
              }
            }
          } catch ( InterruptedException e ) {
            throw SQLExceptionFactory.createSQLException( e );
          } catch ( ExecutionException e ) {
            throw SQLExceptionFactory.createSQLException( e.getCause( ) );
          }
        }
        Connection targetConnection = null;
        Statement targetStatement = null;
        try {
          targetConnection = context.getConnection( context.getTargetDatabase( ) );
          targetStatement = targetConnection.createStatement( );
          for ( SequenceProperties sequence : sequences ) {
            String sql = dialect.getAlterSequenceSQL( sequence, sequenceMap.get( sequence ) + 1 );
            Logs.extreme( ).info( sql );
            targetStatement.addBatch( sql );
          }
          targetStatement.executeBatch( );
        } catch ( SQLException sqle ) {
          LOG.error( sqle );
          Logs.extreme( ).error( sqle, sqle );
          throw sqle;
        } finally {
          try {
            targetStatement.close( );
          } catch ( Exception e ) {
            LOG.error( e );
          }
        }
      }
    }
    
    /**
     * @param <D>
     * @param context
     * @throws SQLException
     */
    public static <D> void synchronizeIdentityColumns( SynchronizationContext<D> context ) throws SQLException {
      
      Statement sourceStatement = null;
      Statement targetStatement = null;
      try {
        sourceStatement = context.getConnection( context.getSourceDatabase( ) ).createStatement( );
        targetStatement = context.getConnection( context.getTargetDatabase( ) ).createStatement( );
        Dialect dialect = context.getDialect( );
        for ( TableProperties table : context.getSourceDatabaseProperties( ).getTables( ) ) {
          Collection<String> columns = table.getIdentityColumns( );
          if ( !columns.isEmpty( ) ) {
            String selectSQL = MessageFormat.format( "SELECT max({0}) FROM {1}", Strings.join( columns, "), max(" ), table.getName( ) ); //$NON-NLS-1$ //$NON-NLS-2$
            Logs.extreme( ).info( selectSQL );
            Map<String, Long> map = new HashMap<String, Long>( );
            ResultSet resultSet = sourceStatement.executeQuery( selectSQL );
            if ( resultSet.next( ) ) {
              int i = 0;
              for ( String column : columns ) {
                map.put( column, resultSet.getLong( ++i ) );
              }
            }
            resultSet.close( );
            if ( !map.isEmpty( ) ) {
              for ( Map.Entry<String, Long> mapEntry : map.entrySet( ) ) {
                String alterSQL = dialect.getAlterIdentityColumnSQL( table, table.getColumnProperties( mapEntry.getKey( ) ), mapEntry.getValue( ) + 1 );
                if ( alterSQL != null ) {
                  Logs.extreme( ).info( alterSQL );
                  targetStatement.addBatch( alterSQL );
                }
              }
              targetStatement.executeBatch( );
            }
          }
        }
      } catch ( SQLException sqle ) {
        LOG.error( sqle );
        Logs.extreme( ).error( sqle, sqle );
        throw sqle;
      } finally {
        try {
          sourceStatement.close( );
        } catch ( Exception e1 ) {
          LOG.error( e1 );
        }
        try {
          targetStatement.close( );
        } catch ( Exception e2 ) {
          LOG.error( e2 );
        }
      }
    }
    
    /**
     * @param <D>
     * @param context
     * @throws SQLException
     */
    public static <D> void dropUniqueConstraints( SynchronizationContext<D> context ) throws SQLException {
      Dialect dialect = context.getDialect( );
      Connection connection = null;
      Statement statement = null;
      try {
        connection = context.getConnection( context.getTargetDatabase( ) );
        statement = connection.createStatement( );
        for ( TableProperties table : context.getTargetDatabaseProperties( ).getTables( ) ) {
          for ( UniqueConstraint constraint : table.getUniqueConstraints( ) ) {
            String sql = dialect.getDropUniqueConstraintSQL( constraint );
            Logs.extreme( ).info( sql );
            statement.addBatch( sql );
          }
        }
        statement.executeBatch( );
      } catch ( SQLException sqle ) {
        LOG.error( sqle );
        Logs.extreme( ).error( sqle, sqle );
        throw sqle;
      } finally {
        try {
          statement.close( );
        } catch ( Exception e ) {
          LOG.error( e );
        }
      }
    }
    
    /**
     * @param <D>
     * @param context
     * @throws SQLException
     */
    public static <D> void restoreUniqueConstraints( SynchronizationContext<D> context ) throws SQLException {
      Dialect dialect = context.getDialect( );
      Connection connection = null;
      Statement statement = null;
      try {
        connection = context.getConnection( context.getTargetDatabase( ) );
        statement = connection.createStatement( );
        for ( TableProperties table : context.getSourceDatabaseProperties( ).getTables( ) ) {
          // Drop unique constraints on the current table
          for ( UniqueConstraint constraint : table.getUniqueConstraints( ) ) {
            String sql = dialect.getCreateUniqueConstraintSQL( constraint );
            Logs.extreme( ).info( sql );
            statement.addBatch( sql );
          }
        }
        statement.executeBatch( );
      } catch ( SQLException sqle ) {
        LOG.error( sqle );
        Logs.extreme( ).error( sqle, sqle );
        throw sqle;
      } finally {
        try {
          statement.close( );
        } catch ( Exception e ) {
          LOG.error( e );
        }
      }
    }
    
    /**
     * @param connection
     */
    public static void rollback( Connection connection ) {
      try {
        connection.rollback( );
        connection.setAutoCommit( true );
      } catch ( SQLException e ) {
        LOG.warn( e.toString( ), e );
      }
    }
    
    /**
     * Helper method for {@link java.sql.ResultSet#getObject(int)} with special handling for large
     * objects.
     * * @param resultSet
     * 
     * @param index
     * @param type
     * @return the object of the specified type at the specified index from the specified result set
     * @throws SQLException
     */
    public static Object getObject( ResultSet resultSet, int index, int type ) throws SQLException {
      switch ( type ) {
        case Types.BLOB: {
          return resultSet.getBlob( index );
        }
        case Types.CLOB: {
          return resultSet.getClob( index );
        }
        default: {
          return resultSet.getObject( index );
        }
      }
    }
  }
  
  /**
   * Database-independent synchronization strategy that does full record transfer between two
   * databases.
   * This strategy is best used when there are <em>many</em> differences between the active database
   * and the inactive database (i.e. very much out of sync).
   * The following algorithm is used:
   * <ol>
   * <li>Drop the foreign keys on the inactive database (to avoid integrity constraint violations)</li>
   * <li>For each database table:
   * <ol>
   * <li>Delete all rows in the inactive database table</li>
   * <li>Query all rows on the active database table</li>
   * <li>For each row in active database table:
   * <ol>
   * <li>Insert new row into inactive database table</li>
   * </ol>
   * </li>
   * </ol>
   * </li>
   * <li>Re-create the foreign keys on the inactive database</li>
   * <li>Synchronize sequences</li>
   * </ol>
   * * @author Paul Ferraro
   */
  public static class FullSynchronizationStrategy implements SynchronizationStrategy {
    private int maxBatchSize = 100;
    private int fetchSize    = 0;
    
    @Override
    public <D> void synchronize( SynchronizationContext<D> context ) throws SQLException {
      Connection sourceConnection = context.getConnection( context.getSourceDatabase( ) );
      Connection targetConnection = context.getConnection( context.getTargetDatabase( ) );
      Dialect dialect = context.getDialect( );
      boolean autoCommit = targetConnection.getAutoCommit( );
      targetConnection.setAutoCommit( true );
      SynchronizationSupport.dropForeignKeys( context );
      targetConnection.setAutoCommit( false );
      try {
        for ( TableProperties table : context.getSourceDatabaseProperties( ).getTables( ) ) {
          String tableName = table.getName( );
          Collection<String> columns = table.getColumns( );
          String commaDelimitedColumns = Strings.join( columns, Strings.PADDED_COMMA );
          final String selectSQL = "SELECT " + commaDelimitedColumns + " FROM " + tableName; //$NON-NLS-1$ //$NON-NLS-2$
          final Statement selectStatement = sourceConnection.createStatement( );
          selectStatement.setFetchSize( this.fetchSize );
          String deleteSQL = dialect.getTruncateTableSQL( table );
          Logs.extreme( ).info( deleteSQL );
          Statement deleteStatement = targetConnection.createStatement( );
          int deletedRows = deleteStatement.executeUpdate( deleteSQL );
          LOG.info( Messages.getMessage( Messages.DELETE_COUNT, deletedRows, tableName ) );
          deleteStatement.close( );
          ResultSet resultSet = selectStatement.executeQuery( selectSQL );
          Logs.extreme( ).info( selectSQL );
          int statementCount = 0;
          while ( resultSet.next( ) ) {
            String insertSQL = "INSERT INTO " + tableName + " (" + commaDelimitedColumns + ") VALUES (" + Strings.join( Collections.nCopies( columns.size( ), Strings.QUESTION ), Strings.PADDED_COMMA ) + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            Logs.extreme( ).info( insertSQL );
            PreparedStatement insertStatement = targetConnection.prepareStatement( insertSQL );
            int index = 0;
            String selected = "SELECT * FROM " + tableName + ": " + resultSet.getRow( ) + " ";
            for ( String column : columns ) {
              index += 1;
              int type = dialect.getColumnType( table.getColumnProperties( column ) );
              Object object = SynchronizationSupport.getObject( resultSet, index, type );
              selected += "\n\t" + column + "=" + object + " ";
              if ( resultSet.wasNull( ) ) {
                insertStatement.setNull( index, type );
              } else {
                insertStatement.setObject( index, object, type );
              }
            }
            Logs.exhaust( ).trace( selected );
            insertStatement.addBatch( );
            insertStatement.executeBatch( );
            insertStatement.clearBatch( );
            insertStatement.clearParameters( );
            insertStatement.close( );
          }
          Logs.extreme( ).info( Messages.getMessage( Messages.INSERT_COUNT, statementCount, tableName ) );
          selectStatement.close( );
          targetConnection.commit( );
        }
      } catch ( SQLException e ) {
        SynchronizationSupport.rollback( targetConnection );
        throw e;
      } catch ( Exception e ) {
        SynchronizationSupport.rollback( targetConnection );
        throw new RuntimeException( e );
      }
      targetConnection.setAutoCommit( true );
      SynchronizationSupport.restoreForeignKeys( context );
      SynchronizationSupport.synchronizeIdentityColumns( context );
      SynchronizationSupport.synchronizeSequences( context );
      targetConnection.setAutoCommit( autoCommit );
    }
    
    public int getFetchSize( ) {
      return this.fetchSize;
    }
    
    public void setFetchSize( int fetchSize ) {
      this.fetchSize = fetchSize;
    }
    
    public int getMaxBatchSize( ) {
      return this.maxBatchSize;
    }
    
    public void setMaxBatchSize( int maxBatchSize ) {
      this.maxBatchSize = maxBatchSize;
    }
  }
  
  public static class PassiveSynchronizationStrategy implements SynchronizationStrategy {
    @Override
    public <D> void synchronize( SynchronizationContext<D> context ) {
      // Do nothing
    }
  }
  
}
