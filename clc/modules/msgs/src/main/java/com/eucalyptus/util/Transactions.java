package com.eucalyptus.util;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import javax.persistence.EntityManager;
import org.apache.log4j.Logger;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.async.Callback;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import java.util.concurrent.ExecutionException;

public class Transactions {
  private static Logger LOG = Logger.getLogger( Transactions.class );
  private static ThreadLocal<EntityWrapper<?>> dbtl = new ThreadLocal<EntityWrapper<?>>();
  public static <T> EntityWrapper<T> join( ) {
    return ( EntityWrapper<T> ) dbtl.get( );
  }
  
  public static <T> T one( T search, JoinTx<T>... txs ) throws TransactionException {
    if ( search == null ) {
      TransactionException ex = new TransactionException( "A search object must be supplied" );
      LOG.warn( ex.getMessage( ), ex );
      throw ex;
    }
    EntityWrapper<T> db = EntityWrapper.get( search );
    dbtl.set( db );
    try {
      T entity = db.getUnique( search );
      for ( JoinTx<T> c : txs ) {
        c.fire( db, entity );
      }
      db.commit( );
      return entity;
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new TransactionException( e.getMessage( ), e );
    } catch ( Throwable e ) {
      db.rollback( );
      LOG.error( e, e );
      throw new TransactionFireException( e.getMessage( ), e );
    } finally {
      dbtl.remove( );
    }
  }

  
  
  public static <T> T find( T search ) throws TransactionException {
    return one( search, new Tx<T>( ) {
      
      @Override
      public void fire( T t ) throws Throwable {}
    } );
  }
  
  public static <T> T one( T search, final Tx<T> c ) throws TransactionException {
    return one( search, new Callback<T>( ) {
      
      @Override
      public void fire( T t )  {
        try {
          c.fire( t );
        } catch ( Throwable ex ) {
          throw new UndeclaredThrowableException( ex );
        }
      }
    } );
  }
  
  public static <T> T one( T search, Callback<T> c ) throws TransactionException {//TODO:GRZE:adjust these to use callbacks
    if ( search == null ) {
      TransactionException ex = new TransactionException( "A search object must be supplied" );
      LOG.warn( ex.getMessage( ), ex );
      throw ex;
    }
    EntityWrapper<T> db = EntityWrapper.get( search );
    dbtl.set( db );
    try {
      T entity = db.getUnique( search );
      c.fire( entity );
      db.commit( );
      return entity;
    } catch ( UndeclaredThrowableException e ) {
      db.rollback( );
      throw new TransactionException( e.getCause( ).getMessage( ), e.getCause( ) );
    } catch ( Throwable e ) {
      db.rollback( );
      LOG.error( e, e );
      throw new TransactionFireException( e.getMessage( ), e );
    } finally {
      dbtl.remove( );
    }
  }
  
  public static <T> T save( T saveMe ) throws ExecutionException {
    return save( saveMe, null );
  }
  
  public static <T> T save( T saveMe, Callback<T> c ) throws ExecutionException {
    EntityWrapper<T> db = EntityWrapper.get( saveMe );
    dbtl.set( db );
    try {
      T entity = db.merge( saveMe );
      if ( c != null ) {
        c.fire( entity );
      }
      db.commit( );
      return entity;
    } catch ( UndeclaredThrowableException e ) {
      db.rollback( );
      throw new TransactionException( e.getCause( ).getMessage( ), e.getCause( ) );
    } catch ( Throwable e ) {
      db.rollback( );
      LOG.error( e, e );
      throw new TransactionFireException( e.getMessage( ), e );
    } finally {
      dbtl.remove( );
    }
  }
  
  public static <T> List<T> many( T search, Callback<T> c ) throws TransactionException {
    if ( search == null ) {
      TransactionException ex = new TransactionException( "A search object must be supplied" );
      LOG.warn( ex.getMessage( ), ex );
      throw ex;
    }
    EntityWrapper<T> db = EntityWrapper.get( search );
    dbtl.set( db );
    try {
      List<T> res = db.query( search );
      for ( T entity : res ) {
        c.fire( entity );
      }
      db.commit( );
      return res;
    } catch ( UndeclaredThrowableException e ) {
      db.rollback( );
      throw new TransactionException( e.getCause( ) );
    } catch ( Throwable e ) {
      LOG.error( e, e );
      throw new TransactionFireException( e.getMessage( ), e );
    } finally {
      dbtl.remove( );
    }
  }
  
  public static <T> List<T> filter( T search, Predicate<T> c ) throws TransactionException {
    if ( search == null ) {
      TransactionException ex = new TransactionException( "A search object must be supplied" );
      LOG.warn( ex.getMessage( ), ex );
      throw ex;
    }
    EntityWrapper<T> db = EntityWrapper.get( search );
    dbtl.set( db );
    try {
      List<T> res = Lists.newArrayList( );
      List<T> queryResults = db.query( search );
      for ( T t : queryResults ) {
        if ( c.apply( t ) ) {
          res.add( t );
        }
      }
      db.commit( );
      return res;
    } catch ( Throwable e ) {
      db.rollback( );
      LOG.error( e, e );
      throw new TransactionFireException( e.getMessage( ), e );
    } finally {
      dbtl.remove( );
    }
  }
  
  public static <T> List<T> list( T search, Callback<List<T>> c ) throws TransactionException {
    if ( search == null ) {
      TransactionException ex = new TransactionException( "A search object must be supplied" );
      LOG.warn( ex.getMessage( ), ex );
      throw ex;
    }
    EntityWrapper<T> db = EntityWrapper.get( search );
    dbtl.set( db );
   try {
      List<T> res = db.query( search );
      c.fire( res );
      db.commit( );
      return res;
    } catch ( UndeclaredThrowableException e ) {
      db.rollback( );
      throw new TransactionException( e.getCause( ) );
    } catch ( Throwable e ) {
      db.rollback( );
      LOG.error( e, e );
      throw new TransactionFireException( e.getMessage( ), e );
    } finally {
      dbtl.remove( );
    }
  }
  
//  public static <T> T one( Class<T> type, String id, Tx<T> c ) throws TransactionException {
//    if ( id == null ) {
//      TransactionException ex = new TransactionException( "An search ID must be supplied" );
//      LOG.warn( ex.getMessage( ), ex );
//      throw ex;
//    }
//    EntityWrapper<T> db = EntityWrapper.get( type );
//    EntityManager em = db.getEntityManager( );
//    try {
//      T entity = em.find( type, id );
//      c.fire( entity );
//      db.commit( );
//      return entity;
//    } catch ( EucalyptusCloudException e ) {
//      db.rollback( );
//      throw new TransactionException( e.getMessage( ), e );
//    } catch ( Throwable e ) {
//      db.rollback( );
//      LOG.error( e, e );
//      throw new TransactionFireException( e.getMessage( ), e );
//    }
//  }
  
}
