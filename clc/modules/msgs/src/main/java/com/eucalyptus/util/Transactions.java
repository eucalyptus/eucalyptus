package com.eucalyptus.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.async.Callback;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

public class Transactions {
  private static Logger                        LOG  = Logger.getLogger( Transactions.class );
  private static ThreadLocal<EntityWrapper<?>> dbtl = new ThreadLocal<EntityWrapper<?>>( );
  
  public static <T> EntityWrapper<T> join( ) {
    return ( EntityWrapper<T> ) dbtl.get( );
  }

  /**
   * TODO:GRZE: make this friendly wrt multiple types
   * 
   * @param <T>
   * @param search
   * @return
   */
  private static <T> EntityWrapper<T> joinOrCreate( T search ) {
    EntityWrapper<T> db;
    if ( dbtl.get( ) != null ) {
      try {
        db = ( EntityWrapper<T> ) dbtl.get( );
      } catch ( Exception ex ) {
        db = dbtl.get( ).recast( ( Class<T> ) search.getClass( ) );
      }
    } else {
      db = EntityWrapper.get( search );
      dbtl.set( db );
    }
    return db;
  }
  
  public static <T> List<T> each( T search, Callback<T> c ) {
    assertThat( search, notNullValue( ) );
    EntityWrapper<T> db = Transactions.joinOrCreate( search );
    try {
      List<T> res = db.query( search );
      for ( T t : res ) {
        c.fire( t );
      }
      db.commit( );
      return res;
    } catch ( UndeclaredThrowableException e ) {
      db.rollback( );
      LOG.error( e, e );
    } catch ( Throwable e ) {
      db.rollback( );
      LOG.error( e, e );
    } finally {
      dbtl.remove( );
    }
    return Lists.newArrayList( );
  }
  
  public static <T> T one( T search, final Tx<T> c ) throws ExecutionException {
    return one( search, new Callback<T>( ) {
      
      @Override
      public void fire( T t ) {
        try {
          c.fire( t );
        } catch ( Throwable ex ) {
          throw new UndeclaredThrowableException( ex );
        }
      }
    } );
  }
  
  public static <T> T find( T search ) throws ExecutionException {
    return one( search, new Callback<T>( ) {
      
      @Override
      public void fire( T t ) {}
    } );
  }
  
  public static <T> T one( T search, Callback<T> c ) throws ExecutionException {//TODO:GRZE:adjust these to use callbacks
    assertThat( search, notNullValue( ) );
    EntityWrapper<T> db = Transactions.joinOrCreate( search );
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
  
  public static <S, T> S oneTransform( T search, Function<T, S> c ) throws ExecutionException {//TODO:GRZE:adjust these to use callbacks
    assertThat( search, notNullValue( ) );
    EntityWrapper<T> db = Transactions.joinOrCreate( search );
    try {
      T entity = db.getUnique( search );
      S res = c.apply( entity );
      db.commit( );
      return res;
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
  
  public static <S, T> S transform( T search, Function<T, S> c ) throws ExecutionException {//TODO:GRZE:adjust these to use callbacks
    assertThat( search, notNullValue( ) );
    EntityWrapper<T> db = Transactions.joinOrCreate( search );
    try {
      T entity = db.getUnique( search );
      S res = c.apply( entity );
      db.commit( );
      return res;
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
    EntityWrapper<T> db = Transactions.joinOrCreate( saveMe );
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
  
  public static <T> List<T> filter( T search, Predicate<T> c ) {
    assertThat( search, notNullValue( ) );
    List<T> res = Lists.newArrayList( );
    EntityWrapper<T> db = EntityWrapper.get( search );
    dbtl.set( db );
    try {
      List<T> queryResults = db.query( search );
      for ( T t : queryResults ) {
        if ( c.apply( t ) ) {
          res.add( t );
        }
      }
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      LOG.error( e, e );
    } finally {
      dbtl.remove( );
    }
    return res;
  }
  
}
