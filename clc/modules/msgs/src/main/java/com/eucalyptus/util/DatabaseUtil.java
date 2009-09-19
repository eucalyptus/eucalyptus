package com.eucalyptus.util;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.hibernate.ejb.EntityManagerFactoryImpl;

import com.eucalyptus.bootstrap.SystemBootstrapper;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.util.EntityWrapper.DbEvent;
import com.google.common.collect.Maps;
import com.mchange.v2.c3p0.C3P0Registry;
import com.mchange.v2.c3p0.PooledDataSource;

public class DatabaseUtil implements EventListener {
  private static Logger LOG = Logger.getLogger( DatabaseUtil.class );
  public static int MAX_FAIL = 5;
  public static long MAX_OPEN_TIME = 30*1000l;
  private static int failCount = 0;
  private static Map<String, EntityManagerFactoryImpl> emf   = new ConcurrentSkipListMap<String, EntityManagerFactoryImpl>( );
  
  private static Map<String, String>                   pools = Maps.newHashMap( );
  public static void printConnectionPoolStatus( ) {
    for ( PooledDataSource ds : ( Set<PooledDataSource> ) C3P0Registry.getPooledDataSources( ) ) {
      try {
        LOG.debug( "Datasource: " + pools.get( ds.getDataSourceName( ) ) + " -- " + ds.getDataSourceName( ) );
        LOG.debug( String.format( "-> Threads: size=%d active=%d idle=%d pending=%d helpers=%d waiting=%d", 
            ds.getThreadPoolSize( ), ds.getThreadPoolNumActiveThreads( ), ds.getThreadPoolNumIdleThreads( ), 
            ds.getThreadPoolNumTasksPending( ), ds.getNumHelperThreads( ), ds.getNumThreadsAwaitingCheckoutDefaultUser( ) ) );
        LOG.debug( String.format( "-> Statement Cache: size=%d conn-w/-stmts=%d checked-out=%d", 
            ds.getStatementCacheNumStatementsAllUsers( ), ds.getStatementCacheNumConnectionsWithCachedStatementsAllUsers( ), ds.getStatementCacheNumCheckedOutStatementsAllUsers( ) ) );
        LOG.debug( String.format( "-> Connections: size=%d busy=%d idle=%d orphan=%d failed-checkin=%d failed-checkout=%d failed-idle=%d", 
            ds.getNumConnectionsAllUsers( ), ds.getNumBusyConnectionsAllUsers( ), ds.getNumIdleConnectionsAllUsers( ), 
            ds.getNumUnclosedOrphanedConnectionsAllUsers( ), ds
            .getNumFailedCheckinsDefaultUser( ), ds.getNumFailedCheckoutsDefaultUser( ), ds.getNumFailedIdleTestsDefaultUser( ) ) );
      } catch ( SQLException e1 ) {
        LOG.debug( e1, e1 );
      }
    }
  }
  public static EntityManagerFactoryImpl getEntityManagerFactory( ) {
    return getEntityManagerFactory( "eucalyptus" );
  }
  @SuppressWarnings( "deprecation" )
  public static EntityManagerFactoryImpl getEntityManagerFactory( final String persistenceContext ) {
    synchronized ( EntityWrapper.class ) {
      if ( !emf.containsKey( persistenceContext ) ) {
        LOG.info( "-> Setting up persistence context for : " + persistenceContext );
        LOG.info( "-> database host: " + System.getProperty( "euca.db.host" ) );
        LOG.info( "-> database port: " + System.getProperty( "euca.db.port" ) );
        emf.put( persistenceContext, ( EntityManagerFactoryImpl ) Persistence.createEntityManagerFactory( persistenceContext ) );
        for ( PooledDataSource ds : ( Set<PooledDataSource> ) C3P0Registry.getPooledDataSources( ) ) {
          if ( pools.containsValue( persistenceContext ) ) break;
          if ( !pools.containsKey( ds.getDataSourceName( ) ) ) {
            pools.put( ds.getDataSourceName( ), persistenceContext );
          }
        }
      }
      return emf.get( persistenceContext );
    }
  }
  public static Set<String> getPersistenceContexts( ) {
    return emf.keySet( );
  }
  public static void handleConnectionError( Throwable cause ) {
    debug( );
    LOG.warn( "Caught exception in database path: resetting connection pools.", cause );
    for ( PooledDataSource ds : ( Set<PooledDataSource> ) C3P0Registry.getPooledDataSources( ) ) {
      try {
        LOG.warn( "-> Resetting: " + ds.getDataSourceName( ) );
        ds.softResetAllUsers( );
      } catch ( Throwable e1 ) {}
    }
    touchDatabase( );
  }
  private static void touchDatabase( ) {
    if( !SystemBootstrapper.getDatabaseBootstrapper( ).isRunning( ) ) {
      LOG.fatal( LogUtil.header( "Database is not running.  Attempting to recover by reloading." ) );
      System.exit( 123 );//reload.
    } else {
      if( MAX_FAIL > failCount ) {
        LOG.warn( LogUtil.subheader( "Error using or obtaining a database connection, will try till " + (MAX_FAIL-failCount++) + ">" + MAX_FAIL + " more times before reloading." ) );
      } else {
        System.exit( 123 );//reload.
      }
    }
  }
  public static void debug( ) {
    if( DebugUtil.DEBUG ) {
      for( String persistenceContext : getPersistenceContexts( ) ) {
        EntityManagerFactoryImpl anemf = ( EntityManagerFactoryImpl ) getEntityManagerFactory( persistenceContext );
        LOG.debug( LogUtil.subheader( persistenceContext + " hibernate statistics: " + anemf.getSessionFactory( ).getStatistics( ) ) );
      }
      TxHandle.printTxStatus( );
      DatabaseUtil.printConnectionPoolStatus( );
    }
  }
  @Override
  public void advertiseEvent( Event event ) {}

  @Override
  public void fireEvent( Event event ) {
    if( event instanceof ClockTick && !((ClockTick)event).isBackEdge( ) ) {
      debug( );
    }
  }

}
