/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

package com.eucalyptus.bootstrap;

import groovy.sql.Sql;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
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

import org.apache.log4j.Logger;
import org.logicalcobwebs.proxool.ProxoolFacade;

import com.eucalyptus.bootstrap.Hosts.DbFilter;
import com.eucalyptus.bootstrap.Hosts.SyncedDbFilter;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.Database;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.records.Logs;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.scripting.ScriptExecutionFailedException;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.Mbeans;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.async.Futures;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Databases {
  
  private static final Logger                 LOG                       = Logger.getLogger( Databases.class );
  private static final int                    MAX_DEACTIVATION_RETRIES  = 10;
  private static final int                    MAX_TX_START_SYNC_RETRIES = 120;
  private static final AtomicInteger          counter                   = new AtomicInteger( 500 );
  private static final Predicate<Host>        FILTER_SYNCING_DBS        = Predicates.and( DbFilter.INSTANCE, Predicates.not( SyncedDbFilter.INSTANCE ) );
  private static final ScriptedDbBootstrapper singleton                 = new ScriptedDbBootstrapper( );
  private static final String                 jdbcJmxDomain             = "net.sf.hajdbc";
  private static final ExecutorService        dbSyncExecutors           = Executors.newCachedThreadPool( );                                              //NOTE:GRZE:special case thread handling.
  private static final ReentrantReadWriteLock canHas                    = new ReentrantReadWriteLock( );

  private static Supplier<Set<String>>        activeHosts               = Suppliers.memoizeWithExpiration( ActiveHostSet.ACTIVATED, 2, TimeUnit.SECONDS );
  private static Supplier<Set<String>>        hostDatabases             = Suppliers.memoizeWithExpiration( ActiveHostSet.DBHOSTS, 1, TimeUnit.SECONDS );

  private static Predicate<StackTraceElement> notStackFilterYouAreLookingFor = Predicates.or(
      Threads.filterStackByQualifiedName( "com\\.eucalyptus\\.entities\\..*" ),
      Threads.filterStackByQualifiedName( "java\\.lang\\.Thread.*" ),
      Threads.filterStackByQualifiedName( "com\\.eucalyptus\\.system\\.Threads.*" ),
      Threads.filterStackByQualifiedName( "com\\.eucalyptus\\.bootstrap\\.Databases.*" ) );
  private static Predicate<StackTraceElement> stackFilter                    = Predicates.not( notStackFilterYouAreLookingFor );

  private static final int DATABASE_WEIGHT_PRIMARY = 100;
  private static final int DATABASE_WEIGHT_SECONDARY = 1;

  public static class DatabaseStateException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    public DatabaseStateException( String string ) {
      super( string );
    }

  }

  public enum Locks {
    DISABLED {
      @Override
      void isLocked() {
        File dbLockFile = this.getLockFile();
        if ( dbLockFile.exists() && Hosts.isCoordinator() ) {
          this.failStop();
        }
      }

      @Override
      public void failStop() {
        Faults.forComponent( Eucalyptus.class ).havingId( 1010 ).withVar( DB_LOCK_FILE, this.getLockFile().getAbsolutePath() ).log();
        LOG.error( "WARNING : DISABLED CLC STARTED OUT OF ORDER, REMOVE THE " + this.getLockName() + "FILE TO PROCEED WITH RISK" );
        System.exit( 1 );
      }

    },
    PARTITIONED {
      @Override
      void isLocked() {
        if ( this.getLockFile().exists() ) {
          failStop();
        }
      }

      @Override
      public void failStop() {
        Faults.forComponent( Eucalyptus.class ).havingId( 1011 ).withVar( DB_LOCK_FILE, this.getLockFile().getAbsolutePath() ).log();
        LOG.error( "PARTITION DETECTED -- FAIL-STOP TO AVOID POSSIBLE INCONSISTENCY." );
        LOG.error( "PARTITION DETECTED -- Shutting down CLC after experiencing a possible split-brain partition." );
        LOG.error( "PARTITION DETECTED -- See cloud-fault.log for guidance." );
        System.exit( 1 );
      }

      @Override
      public void create() {
        super.create();
        Faults.forComponent( Eucalyptus.class ).havingId( 1011 ).withVar( DB_LOCK_FILE, this.getLockFile().getAbsolutePath() ).log();
      }
    };
    public static final String DB_LOCK_FILE = "DB_LOCK_FILE";

    public void delete( ) {
      this.getLockFile( ).delete();
      LOG.debug( "The " + this.getLockFile( ).getAbsolutePath( ) + " file was deleted" );
    }

    protected String getLockName() {
      return this.name().toLowerCase() + ".lock";
    }

    abstract void isLocked( );

    public abstract void failStop( );

    protected File getLockFile( ) {
      return SubDirectory.DB.getChildFile( "data", this.getLockName() );
    }

    public void create( String reason ) {
      LOG.error( this.getLockName( ) + ": Caused by: " + reason );
      this.create( );
    }

    public void create( ) {
      try {
        if ( getLockFile( ).createNewFile( ) ) {
          LOG.debug( this.getLockName( ) + ": The " + this.getLockFile( ).getAbsolutePath( ) + " file was created." );
        }
      } catch ( IOException e ) {
        LOG.debug("Unable to create the " + this.getLockFile( ).getAbsolutePath( ) + " file: " + e.getMessage());
      }
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
    private static final AtomicReference<SyncState> syncState = new AtomicReference<>( SyncState.NOTSYNCED );
    
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

  public static Iterable<String> databases( ) {
    return Sets.newTreeSet( Iterables.transform( PersistenceContexts.list( ), PersistenceContexts.toDatabaseName( ) ) );
  }
  
  public static Iterable<String> remoteDatabases( ) {
    return Sets.newTreeSet( PersistenceContexts.listRemotable() );
  }

  @Provides( Empyrean.class )
  @RunDuring( Bootstrap.Stage.PoolInit )
  public static class DatabasePoolBootstrapper extends Bootstrapper.Simple {
    private static final int INITIAL_DB_SYNC_RETRY_WAIT = 5;
    
    @Override
    public boolean load( ) throws Exception {
      Hosts.awaitDatabases( );
      Locks.DISABLED.isLocked( );
      Locks.PARTITIONED.isLocked( );

      Groovyness.run( "setup_dbpool.groovy" );
      OrderedShutdown.registerShutdownHook( Empyrean.class, new Runnable( ) {
        
        @Override
        public void run( ) {
          try {
            for ( final String database : databases( ) ) {
              try {
                DatabaseClusterMBean db = Databases.lookup( database, TimeUnit.SECONDS.toMillis( 5 ) );
                for ( String host : db.getinactiveDatabases() ) {
                  Databases.disable( host );
                }
                for ( String host : db.getactiveDatabases() ) {
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

        Locks.DISABLED.create( );

        Hosts.UpdateEntry.INSTANCE.apply( Hosts.localHost( ) );
        LOG.info( LogUtil.subheader( "Database synchronization complete: " + Hosts.localHost( ) ) );
      }
      return true;
    }
    
    @Override
    public boolean check( ) throws Exception {
      if ( Bootstrap.isOperational( ) &&
           !Hosts.localHost( ).hasDatabase( ) ) {  // This check is redundant on hosts with DBs
        final List<Host.DBStatus> statusList = Hosts.localHost().getDatabaseStatus( );
        for ( final Host.DBStatus status : statusList ) {
          for ( String error : status.getError( ).asSet( ) ) {
            LOG.error( error );
          }
        }
      }
      return super.check( );
    }
  }

  @Provides( Empyrean.class )
  @RunDuring( Bootstrap.Stage.RemoteDbPoolInit )
  public static class DatabaseRemotePoolBootstrapper extends Bootstrapper.Simple {
    @Override
    public boolean load( ) throws Exception {
      try{
        Groovyness.run( "setup_dbpool_remote.groovy" );
      }catch(final Exception ex){
        ;
      }
      return true;
    }
  }
    
  private static void runDbStateChange( Function<String, Runnable> runnableFunction ) {
    Logs.extreme( ).info( "DB STATE CHANGE: " + runnableFunction );
    try {
      Logs.extreme( ).info( "Attempting to acquire db state lock: " + runnableFunction );
      if ( canHas.writeLock( ).tryLock( 5, TimeUnit.MINUTES ) ) {

        try {
          Logs.extreme( ).info( "Acquired db state lock: " + runnableFunction );
          Map<Runnable, Future<Runnable>> runnables = Maps.newHashMap( );
          for ( final String database : listDatabases( ) ) {
            Runnable run = runnableFunction.apply( database );
            runnables.put( run, ExecuteRunnable.INSTANCE.apply( run ) );
          }
          Map<Runnable, Future<Runnable>> succeeded = Futures.waitAll( runnables );
          MapDifference<Runnable, Future<Runnable>> failed = Maps.difference( runnables, succeeded );
          StringBuilder builder = new StringBuilder( );
          builder.append( Joiner.on( "\nSUCCESS: " ).join( succeeded.keySet() ) );
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
  
  enum DeactivateHostFunction implements Function<String, Function<String, Runnable>> {
    INSTANCE;
    /**
     * @see Function#apply(Object)
     */
    @Override
    public Function<String, Runnable> apply( final String hostName ) {
      return new Function<String, Runnable>( ) {
        @Override
        public Runnable apply( final String contextName ) {
          return new Runnable( ) {
            @Override
            public void run( ) {
              if ( Internets.testLocal( hostName ) ) {
                return;
              }
              try {
                final boolean wasPrimary;
                try {
                  wasPrimary = lookupDatabase( contextName, hostName ).getweight() == DATABASE_WEIGHT_PRIMARY;
                } catch ( Exception ex1 ) {
                  return;
                }
                
                LOG.info( "Tearing down database connections for: " + hostName + " to context: " + contextName );
                final DatabaseClusterMBean cluster = lookup( contextName, TimeUnit.SECONDS.toMillis( 5 ) );

                // deactivate database
                for ( int i = 0; 
                      i < MAX_DEACTIVATION_RETRIES && cluster.getactiveDatabases().contains( hostName ); 
                      i++ ) try {
                  LOG.debug( "Deactivating database connections for: " + hostName + " to context: " + contextName );
                  cluster.deactivate( hostName );
                  LOG.debug( "Deactived database connections for: " + hostName + " to context: " + contextName );
                  break;
                } catch ( Exception ex ) {
                  LOG.error( ex );
                  Logs.extreme( ).error( ex, ex );
                }
                  
                // remove database
                for ( int i = 0; 
                  i < MAX_DEACTIVATION_RETRIES && cluster.getinactiveDatabases().contains( hostName ) && !Hosts.contains( hostName ); 
                  i++ ) try {
                  LOG.debug( "Removing database registration for: " + hostName + " to context: " + contextName );
                  cluster.remove( hostName );
                  LOG.debug( "Removed database registration for: " + hostName + " to context: " + contextName );
                  break;
                } catch ( Exception ex ) {
                  LOG.error( ex );
                  Logs.extreme( ).error( ex, ex );
                }

                // promote other
                final Host coordinator = Hosts.getCoordinator( );
                if ( wasPrimary && coordinator != null ) {
                  try {
                    lookupDatabase( contextName, coordinator.getDisplayName( ) ).setweight( DATABASE_WEIGHT_PRIMARY );
                  } catch ( NoSuchElementException e ) {
                    // Weight will be set on activation
                  } catch ( Exception e ) {
                    LOG.error( "Error setting primary weight for host " + coordinator.getDisplayName( ) + " context " + contextName, e );
                  }
                }

                // refresh pooled connections to ensure closed if removed
                for ( int i = 0;
                      i < MAX_DEACTIVATION_RETRIES && 
                          !cluster.getactiveDatabases().contains( hostName ) &&
                          !cluster.getinactiveDatabases().contains( hostName );
                      i++ ) try {
                  // Release any open connections
                  LOG.debug( "Refreshing idle pooled connections for context: " + contextName );
                  ProxoolFacade.killAllConnections( contextName, "Database deregistered", true );
                  LOG.debug( "Refreshed idle pooled connections for context: " + contextName );
                  break;
                } catch ( Exception ex ) {
                  LOG.error( ex );
                  Logs.extreme( ).error( ex, ex );
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
      final String dbUrl = "jdbc:" + ServiceUris.remote( Database.class, host.getBindAddress( ), contextName );
      final String hostName = host.getDisplayName();
      final DriverDatabaseMBean database = Databases.lookupDatabase( contextName, hostName );
      database.setuser( getUserName() );
      database.setpassword( getPassword() );
      database.setweight( Hosts.isCoordinator( host )
          ? DATABASE_WEIGHT_PRIMARY
          : DATABASE_WEIGHT_SECONDARY );
      database.setlocal( host.isLocalHost() );
      database.setlocation( dbUrl );
    }
    
    @Override
    public Function<String, Runnable> apply( final Host host ) {
      return new Function<String, Runnable>( ) {
        @Override
        public Runnable apply( final String database ) {
          final String hostName = host.getBindAddress( ).getHostAddress( );
          return new Runnable( ) {
            @Override
            public void run( ) {
              try {
                final boolean fullSync = !Hosts.isCoordinator( ) && host.isLocalHost( ) && BootstrapArgs.isCloudController( ) && !Databases.isSynchronized( );
                final boolean passiveSync = !fullSync && host.hasSynced( );
                if ( !fullSync && !passiveSync ) {
                  throw Exceptions.toUndeclared( "Host is not ready to be activated: " + host );
                } else {
                  final DatabaseClusterMBean cluster = lookup( database, TimeUnit.SECONDS.toMillis( 30 ) );
                  final boolean activated = cluster.getactiveDatabases().contains( hostName );
                  final boolean deactivated = cluster.getinactiveDatabases().contains( hostName );
                  final String passiveStrategy = cluster.getdefaultSynchronizationStrategy();
                  final String fullStrategy = Iterables.getFirst( Sets.difference( 
                      cluster.getsynchronizationStrategies(), 
                      Collections.singleton( passiveStrategy ) ), "full" );
                  final String syncStrategy = fullSync ? fullStrategy : passiveStrategy;
                  if ( activated ) {
                    resetDatabaseWeights( database );
                    return;
                  } else if ( deactivated ) {
                    ActivateHostFunction.prepareConnections( host, database );
                  } else {
                    LOG.info( "Creating database " + database + " connections for: " + host );
                    try {
                      lookupDatabase( database, hostName );
                    } catch ( NoSuchElementException e ) {
                      try {
                        cluster.add( hostName );
                        Logs.extreme( ).debug( "Added database " + database + " connections for host: " + hostName );
                      } catch ( IllegalArgumentException ex ) {
                        Logs.extreme( ).debug( "Skipping addition of database " + database + " connections for host which already exists: " + hostName );
                      } catch ( IllegalStateException ex ) {
                        if ( Exceptions.isCausedBy( ex, InstanceAlreadyExistsException.class ) ) {
                          ManagementFactory.getPlatformMBeanServer( ).unregisterMBean( new ObjectName( 
                              jdbcJmxDomain, 
                              new Hashtable<>( ImmutableMap.of( "cluster", database, "type", "Database", "database", hostName ) ) ) );
                          cluster.add( hostName );
                        } else {
                          throw ex;
                        }
                      }
                    }
                    ActivateHostFunction.prepareConnections( host, database );
                  }

                  // demote others
                  if ( Hosts.isCoordinator( host ) ) {
                    for ( Host secondaryHost : Hosts.listActiveDatabases( ) ) if ( !secondaryHost.equals( host ) ) try {
                      lookupDatabase( database, secondaryHost.getDisplayName( ) ).setweight( DATABASE_WEIGHT_SECONDARY );
                    } catch ( NoSuchElementException e ) {
                      // Weight will be set on activation
                    } catch ( Exception e ) {
                      LOG.error( "Error setting primary weight for host " + secondaryHost.getDisplayName( ) + " context " + database, e );
                    }
                  }

                  try {
                    if ( fullSync ) {
                      LOG.info( "Full sync of database " + database + " on: " + host + " using: " + fullStrategy );
                    } else {
                      LOG.info( "Passive activation of database " + database + " connections to: " + host );
                    }
                    cluster.activate( hostName, syncStrategy );
                    if ( fullSync ) {
                      LOG.info( "Full sync of database " + database + " on: " + host + " using " + cluster.getactiveDatabases() );
                    } else {
                      LOG.info( "Passive activation of database " + database + " on: " + host + " using " + cluster.getactiveDatabases() );
                    }
                  } catch ( Exception ex ) {
                    throw Exceptions.toUndeclared( ex );
                  }

                  // refresh pooled connections
                  try {
                    // Release any open connections
                    LOG.debug( "Refreshing idle pooled connections for context: " + database );
                    ProxoolFacade.killAllConnections( database, "Database registered", true );
                    LOG.debug( "Refreshed idle pooled connections for context: " + database );
                  } catch ( Exception ex ) {
                    LOG.error( "Error refreshing connections on activation of context: " + database, ex );
                  }
                }
              } catch ( final NoSuchElementException | IllegalStateException ex1 ) {
                LOG.error( ex1 );
                Logs.extreme( ).error( ex1, ex1 );
                return;
              } catch ( final Exception ex1 ) {
                LOG.error( ex1 );
                Logs.extreme( ).error( ex1, ex1 );
                throw Exceptions.toUndeclared( "Failed to activate database " + database + " host " + host + " because of: " + ex1.getMessage( ), ex1 );
              }
            }
            
            @Override
            public String toString( ) {
              return "Databases.enable(): " + host.getDisplayName( ) + " " + database;
            }
          };
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

  private static void resetDatabaseWeights( final String contextName ) {
    for ( final Host host : Hosts.listActiveDatabases( ) ) try {
      lookupDatabase( contextName, host.getDisplayName( ) ).setweight(
          Hosts.isCoordinator( host ) ?
              DATABASE_WEIGHT_PRIMARY :
              DATABASE_WEIGHT_SECONDARY );
    } catch ( NoSuchElementException e ) {
      // Weight will be set on activation
    } catch ( Exception e ) {
      LOG.error( "Error resetting weight for host " + host.getDisplayName( ) + " context " + contextName, e );
    }
  }

  private static DatabaseClusterMBean lookup( final String database, final long timeout ) throws NoSuchElementException {
    return lookupMBean(
        ImmutableMap.of( "cluster", database, "type", "DatabaseCluster" ),
        DatabaseClusterMBean.class,
        new Predicate<DatabaseClusterMBean>() {
          @Override
          public boolean apply( final DatabaseClusterMBean cluster ) {
            cluster.getinactiveDatabases( );
            return true;
          }
        },
        timeout
    );
  }

  private static DriverDatabaseMBean lookupDatabase( final String database,
                                                     final String hostName 
  ) throws NoSuchElementException {
    return lookupMBean(
        ImmutableMap.of( "cluster", database, "type", "Database", "database", hostName ),
        DriverDatabaseMBean.class,
        new Predicate<DriverDatabaseMBean>() {
          @Override
          public boolean apply( final DriverDatabaseMBean database ) {
            database.getid( );
            return true;
          }
        },
        0
    );
  }

  private static <T> T lookupMBean( final Map<String,String> props, 
                                    final Class<T> type,
                                    final Predicate<T> tester,
                                    final long timeout ) {
    long until = System.currentTimeMillis() + timeout;
    do try {
      final T bean = Mbeans.lookup( jdbcJmxDomain, props, type );
      tester.apply( bean );
      return bean;
    } catch ( UndeclaredThrowableException e ) {
      if ( Exceptions.isCausedBy( e, InstanceNotFoundException.class ) ) {
        if ( System.currentTimeMillis() < until ) {
          try {
            TimeUnit.SECONDS.sleep( 5 );
          } catch ( InterruptedException e1 ) {
            Thread.interrupted( );
            break;
          }
          LOG.debug( "Waiting for MBean " + type.getSimpleName( ) + "/" + props );
          continue;
        }
        throw new NoSuchElementException( type.getSimpleName() + " " + props.toString() );
      } else {
        throw Exceptions.toUndeclared( e );
      }
    } while ( System.currentTimeMillis() < until );

    throw new NoSuchElementException( type.getSimpleName() + " " + props.toString() );
  }
  
  static boolean disable( final String hostName ) {
    if ( !Bootstrap.isFinished( ) ) {
      return false;
    } else if ( Internets.testLocal( hostName ) ) {
      return true;
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
    if ( !host.hasDatabase( ) || Bootstrap.isShuttingDown( ) ) {
      return false;
    } else if ( !Hosts.contains(host.getGroupsId())  ) {
      Hosts.remove( host.getGroupsId( ) );
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

  static Set<String> listPrimaryActiveDatabases( final String cluster ) {
    return listDatabases( cluster, true, Optional.of( DATABASE_WEIGHT_PRIMARY ) );
  }

  static Set<String> listSecondaryActiveDatabases( final String cluster ) {
    return listDatabases( cluster, true, Optional.of( DATABASE_WEIGHT_SECONDARY ) );
  }

  static Set<String> listInactiveDatabases( final String cluster ) {
    return listDatabases( cluster, false, Optional.<Integer>absent( ) );
  }

  private static Set<String> listDatabases( final String clusterName,
                                            final boolean active,
                                            final Optional<Integer> weight ) {
    final Set<String> databases = Sets.newLinkedHashSet( );
    try {
      final DatabaseClusterMBean cluster = lookup( clusterName, 0 );
      final Iterable<String> databaseIds = active ?
          cluster.getactiveDatabases( ) :
          cluster.getinactiveDatabases( );
      for ( final String databaseId : databaseIds ) try {
        final DriverDatabaseMBean database = lookupDatabase( clusterName, databaseId );
        if ( !weight.isPresent( ) || database.getweight( ) == weight.get( ) ) {
          databases.add( databaseId );
        }
      } catch ( NoSuchElementException e ) {
        // ignore database
      }
    } catch ( NoSuchElementException e ) {
      // no databases
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    return databases;
  }

  static Set<String> listRegisteredDatabases( ) {
    return Mbeans.listPropertyValues( jdbcJmxDomain, "cluster", ImmutableMap.of( "type", "DatabaseCluster" ) );
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
        for ( final String database : databases( ) ) {
          try {
            Set<String> activeDatabases = Databases.lookup( database, 0 ).getactiveDatabases();
            if ( BootstrapArgs.isCloudController( ) ) {
              activeDatabases.add( Internets.localHostIdentifier( ) );//GRZE: use Internets.localHostIdentifier() which is static, rather than the Hosts reference as it is stateful
            }
            union.addAll( activeDatabases );
            intersection.retainAll( activeDatabases );
          } catch ( Exception ex ) {}
        }
        Logs.extreme( ).debug( "ActiveHostSet: union of activated db connections: " + union );
        Logs.extreme( ).debug( "ActiveHostSet: intersection of db hosts and activated db connections: " + intersection );
        boolean dbVolatile = !hosts.equals( intersection );
        String msg = String.format( "ActiveHostSet: %-14.14s %s%s%s", dbVolatile
          ? "volatile"
          : "synchronized", hosts, dbVolatile
          ? "!="
          : "=", intersection );
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

  public static void check( ) {
    for ( final String database : databases( ) ) {
      try {
        DatabaseClusterMBean db = lookup( database, TimeUnit.SECONDS.toMillis( 5 ) );
        for ( String host : db.getactiveDatabases() ) {
          Host hostEntry = Hosts.lookup( host );
          if ( hostEntry == null ) {
            disable( host );
          } else if ( !Hosts.contains( hostEntry.getGroupsId() ) ) {
            Hosts.remove( host );//GRZE: this will clean up group state and de-activate db.
          }
        }
      } catch ( NoSuchElementException ex ) {
        LOG.error( ex, ex );
      }
    }
  }

  /**
   * HA-JDBC Dynamic MBean proxy interface
   */
  private interface DatabaseClusterMBean {

    Set<String> getinactiveDatabases();

    Set<String> getactiveDatabases();

    void add( String hostName );

    void remove( String hostName );

    void activate( String hostName, String syncStrategy );

    void deactivate( String hostName );
    
    Set<String> getsynchronizationStrategies();
    
    String getdefaultSynchronizationStrategy();
  }

  /**
   * HA-JDBC Dynamic MBean proxy interface
   */
  private interface DriverDatabaseMBean {

    boolean isActive( );
    
    String getid( );
    
    void setuser( String userName );

    void setpassword( String password );

    int getweight( );

    void setweight( int i );

    void setlocal( boolean localHost );

    void setlocation( String url );
  }
}
