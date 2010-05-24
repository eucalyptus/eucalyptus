package com.eucalyptus.entities;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceContext;
import org.apache.log4j.Logger;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.EntityManagerFactoryImpl;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.LogUtil;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import edu.emory.mathcs.backport.java.util.Collections;
import com.eucalyptus.records.EventRecord;

@SuppressWarnings( "unchecked" )
public class PersistenceContexts {
  public static int                                     MAX_FAIL        = 5;
  private static AtomicInteger                          failCount       = new AtomicInteger( 0 );
  private static Logger                                 LOG             = Logger.getLogger( PersistenceContexts.class );
  private static final ArrayListMultimap<String, Class> entities        = Multimaps.newArrayListMultimap( );
  private static final List<Class>                      sharedEntities  = Lists.newArrayList( );
  private static Map<String, EntityManagerFactoryImpl>  emf             = new ConcurrentSkipListMap<String, EntityManagerFactoryImpl>( );
  private static List<Exception>                        illegalAccesses = Collections.synchronizedList( Lists.newArrayList( ) );
  
  static void addEntity( Class entity ) {
    if ( !isDuplicate( entity ) ) {
      String ctxName = Ats.from( entity ).get( PersistenceContext.class ).name( );
      EventRecord.here( PersistenceContextDiscovery.class, EventType.PERSISTENCE_ENTITY_REGISTERED, ctxName, entity.getCanonicalName( ) ).info( );
      entities.put( ctxName, entity );
    }
  }
  
  static void addSharedEntity( Class entity ) {
    if ( !isDuplicate( entity ) ) {
      EventRecord.here( PersistenceContextDiscovery.class, EventType.PERSISTENCE_ENTITY_REGISTERED, "shared", entity.getCanonicalName( ) ).info( );
      sharedEntities.add( entity );
    }
  }
  
  private static boolean isDuplicate( Class entity ) {
    PersistenceContext ctx = Ats.from( entity ).get( PersistenceContext.class );
    if( Ats.from( entity ).has( MappedSuperclass.class ) ) {
      return false;
    } else if ( ctx == null || ctx.name( ) == null ) {
      RuntimeException ex = new RuntimeException( "Failed to register broken entity class: " + entity.getCanonicalName( ) + ".  Ensure that the class has a well-formed @PersistenceContext annotation.");
      LOG.error( ex, ex );
      return false;
    } else if ( sharedEntities.contains( entity ) ) {
      Class old = sharedEntities.get( sharedEntities.indexOf( entity ) );
      LOG.error( "Duplicate entity definition detected: " + entity.getCanonicalName( ) );
      LOG.error( "=> OLD: " + old.getProtectionDomain( ).getCodeSource( ).getLocation( ) );
      LOG.error( "=> NEW: " + entity.getProtectionDomain( ).getCodeSource( ).getLocation( ) );
      throw BootstrapException.throwFatal( "Duplicate entity definition in shared entities: " + entity.getCanonicalName( ) + ". See error logs for details." );
    } else if ( entities.get( ctx.name( ) ) != null && entities.get( ctx.name( ) ).contains( entity ) ) {
      List<Class> context = entities.get( ctx.name( ) );
      Class old = context.get( context.indexOf( entity ) );
      LOG.error( "Duplicate entity definition detected: " + entity.getCanonicalName( ) );
      LOG.error( "=> OLD: " + old.getProtectionDomain( ).getCodeSource( ).getLocation( ) );
      LOG.error( "=> NEW: " + entity.getProtectionDomain( ).getCodeSource( ).getLocation( ) );
      throw BootstrapException.throwFatal( "Duplicate entity definition in '" + ctx.name( ) + "': " + entity.getCanonicalName( ) + ". See error logs for details." );
    } else {
      return false;
    }
  }
  
  public static EntityManagerFactoryImpl registerPersistenceContext( final String persistenceContext, final Ejb3Configuration config ) {
    synchronized ( PersistenceContexts.class ) {
      if ( illegalAccesses != null && !illegalAccesses.isEmpty( ) ) {
        for ( Exception e : illegalAccesses ) {
          LOG.fatal( e, e );
        }
        LogUtil.header( "Illegal Access to Persistence Context.  Database not yet configured. This is always a BUG: " + persistenceContext );
        System.exit( 1 );
      } else if ( !emf.containsKey( persistenceContext ) ) {
        illegalAccesses = null;
        EntityManagerFactoryImpl entityManagerFactory = ( EntityManagerFactoryImpl ) config.buildEntityManagerFactory( );
        LOG.info( "-> Setting up persistence context for : " + persistenceContext );
        LOG.info( LogUtil.subheader( LogUtil.dumpObject( config ) ) );
        emf.put( persistenceContext, entityManagerFactory );
      }
      return emf.get( persistenceContext );
    }
  }
  
  public static List<String> list( ) {
    return Lists.newArrayList( entities.keySet( ) );
  }
  
  public static List<Class> listEntities( String persistenceContext ) {
    return entities.get( persistenceContext );
  }
  
  public static void handleConnectionError( Throwable cause ) {
    touchDatabase( );
  }
  
  private static void touchDatabase( ) {
    if ( MAX_FAIL > failCount.getAndIncrement( ) ) {
      LOG.fatal( LogUtil.header( "Database connection failure limit reached (" + MAX_FAIL + "):  HUPping the system." ) );
      System.exit( 123 );
    } else {
      LOG.warn( LogUtil.subheader( "Error using or obtaining a database connection, fail count is " + failCount.intValue( ) + " (max=" + MAX_FAIL
                                   + ") more times before reloading." ) );
    }
  }
  
  @SuppressWarnings( "deprecation" )
  public static EntityManagerFactoryImpl getEntityManagerFactory( final String persistenceContext ) {
    if ( !emf.containsKey( persistenceContext ) ) {
      RuntimeException e = new RuntimeException( "Attempting to access an entity wrapper before the database has been configured: " + persistenceContext + ".  The available contexts are: " + emf.keySet( ));
      illegalAccesses = illegalAccesses == null ? Collections.synchronizedList( Lists.newArrayList( ) ) : illegalAccesses;
      illegalAccesses.add( e );
      throw e;
    }
    return emf.get( persistenceContext );
  }

  public static void shutdown() {
    for( String ctx : emf.keySet( ) ) {
      EntityManagerFactoryImpl em = emf.get( ctx );
      if( em.isOpen( ) ) {
        LOG.info( "Closing persistence context: " + ctx );
        em.close( );
      } else {
        LOG.info( "Closing persistence context: " + ctx + " (found it closed already)" );
      }
    }
  }
  
}
