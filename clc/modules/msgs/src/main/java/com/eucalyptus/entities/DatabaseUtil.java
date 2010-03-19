package com.eucalyptus.entities;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import org.apache.log4j.Logger;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.EntityManagerFactoryImpl;
import com.eucalyptus.bootstrap.SystemBootstrapper;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.util.LogUtil;
import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.Collections;

@SuppressWarnings( "unchecked" )
public class DatabaseUtil {
	static Logger                                        LOG           = Logger.getLogger( DatabaseUtil.class );
	public static int                                    MAX_FAIL      = 5;
	private static int                                   failCount     = 0;
	private static Map<String, EntityManagerFactoryImpl> emf           = new ConcurrentSkipListMap<String, EntityManagerFactoryImpl>( );
	private static List<Exception> illegalAccesses = Collections.synchronizedList( Lists.newArrayList( ) );

	public static EntityManagerFactoryImpl getEntityManagerFactory( ) {
		return getEntityManagerFactory( "eucalyptus_general" );
	}

	 public static EntityManagerFactoryImpl registerPersistenceContext( final String persistenceContext, final Ejb3Configuration config ) {
	    synchronized ( EntityWrapper.class ) {
	      if( illegalAccesses != null && !illegalAccesses.isEmpty( ) ) {
	        for( Exception e : illegalAccesses ) {
	          LOG.fatal( e, e );
	        }
	        LogUtil.header( "Illegal Access to Persistence Context.  Database not yet configured. This is always a BUG." );
	        System.exit( 1 );
	      } else if ( !emf.containsKey( persistenceContext ) ) {
          illegalAccesses = null;
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
			  illegalAccesses = illegalAccesses == null ? Collections.synchronizedList( Lists.newArrayList( ) ) : illegalAccesses;			  
			  illegalAccesses.add( e );
			  throw e;
			}
			return emf.get( persistenceContext );
		}

	public static Set<String> getPersistenceContexts( ) {
		return emf.keySet( );
	}

	public static void handleConnectionError( Throwable cause ) {
//		DebugUtil.debug( );
		touchDatabase( );
	}

	private static void touchDatabase( ) {
		if ( !SystemBootstrapper.getDatabaseBootstrapper( ).isRunning( ) ) {
			LOG.fatal( LogUtil.header( "Database is not running.  Attempting to recover by reloading." ) );
			System.exit( 123 );// reload.
		} else {
			if ( MAX_FAIL > failCount ) {
				LOG.warn( LogUtil.subheader( "Error using or obtaining a database connection, will try till " + ( MAX_FAIL - failCount++ ) + ">" + MAX_FAIL + " more times before reloading." ) );
        System.exit(123);       
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
