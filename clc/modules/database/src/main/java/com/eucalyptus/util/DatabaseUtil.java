package com.eucalyptus.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.EntityManagerFactoryImpl;

import com.eucalyptus.bootstrap.SystemBootstrapper;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;

public class DatabaseUtil implements EventListener {
	static Logger                                        LOG           = Logger.getLogger( DatabaseUtil.class );
	public static int                                    MAX_FAIL      = 5;
	public static int                                    MAX_XTREME_FAIL      = 7;
	private static int                                   failCount     = 0;
	private static Map<String, EntityManagerFactoryImpl> emf           = new ConcurrentSkipListMap<String, EntityManagerFactoryImpl>( );


	public static EntityManagerFactoryImpl getEntityManagerFactory( ) {
		return getEntityManagerFactory( "eucalyptus_general" );
	}

	 public static EntityManagerFactoryImpl registerPersistenceContext( final String persistenceContext, final Ejb3Configuration config ) {
	    synchronized ( EntityWrapper.class ) {
	      if ( !emf.containsKey( persistenceContext ) ) {
	        EntityManagerFactoryImpl entityManagerFactory = (EntityManagerFactoryImpl) config.buildEntityManagerFactory( );//Persistence.createEntityManagerFactory( persistenceContext );
          LOG.info( "-> Setting up persistence context for : " + persistenceContext );
          LOG.info( LogUtil.subheader( LogUtil.dumpObject( config ) ) );
          emf.put( persistenceContext, entityManagerFactory );
	      }
	      return emf.get( persistenceContext );
	    }
	  }

	
	@SuppressWarnings( "deprecation" )
	public static EntityManagerFactoryImpl getEntityManagerFactory( final String persistenceContext ) {
			if ( !emf.containsKey( persistenceContext ) ) {
			  RuntimeException e = new RuntimeException ("Attempting to access an entity wrapper before the database has been configured." );
			  LOG.error( e, e );
			  throw e;
			}
			return emf.get( persistenceContext );
		}

	public static Set<String> getPersistenceContexts( ) {
		return emf.keySet( );
	}

	public static void handleConnectionError( Throwable cause ) {
		DebugUtil.debug( );
		touchDatabase( );
	}

	private static void touchDatabase( ) {
		if ( !SystemBootstrapper.getDatabaseBootstrapper( ).isRunning( ) ) {
			LOG.fatal( LogUtil.header( "Database is not running.  Attempting to recover by reloading." ) );
			System.exit( 123 );// reload.
		} else {
			if ( MAX_FAIL > failCount ) {
				LOG.warn( LogUtil.subheader( "Error using or obtaining a database connection, will try till " + ( MAX_FAIL - failCount++ ) + ">" + MAX_FAIL + " more times before reloading." ) );
			} else if( MAX_XTREME_FAIL > failCount++) {
				LOG.warn("-> Database performance severely degraded. Restarting.");
				System.exit(123);    	  
			}
		}
	}

	@Override
	public void advertiseEvent( Event event ) {}

	@Override
	public void fireEvent( Event event ) {
		if ( event instanceof ClockTick ) {
			ClockTick e = ( ClockTick ) event;
			if ( e.isBackEdge( ) ) {
				DebugUtil.Times.print( );
			} else {
				DebugUtil.Times.update( );
				DebugUtil.debug( );
			}
		}
	}

	public static void closeAllEMFs() {
		for(String key : emf.keySet()) {
			EntityManagerFactoryImpl factory = (EntityManagerFactoryImpl) emf.get(key);
			if(factory.isOpen())
				factory.close();
		}
	}
}
