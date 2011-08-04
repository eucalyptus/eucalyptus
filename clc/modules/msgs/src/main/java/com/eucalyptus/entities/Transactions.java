package com.eucalyptus.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Logs;
import com.eucalyptus.util.TransactionCallbackException;
import com.eucalyptus.util.TransactionException;
import com.eucalyptus.util.TransactionExecutionException;
import com.eucalyptus.util.TransactionInternalException;
import com.eucalyptus.util.Tx;
import com.eucalyptus.util.async.Callback;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

public class Transactions {
  private static Logger                        LOG  = Logger.getLogger( Transactions.class );
  private static ThreadLocal<EntityWrapper<?>> dbtl = new ThreadLocal<EntityWrapper<?>>( );
  
  private static TransactionException transformException( Throwable t ) {
    Logs.extreme( ).error( t, t );
    if ( t instanceof InterruptedException ) {
      Thread.currentThread( ).interrupt( );
      return new TransactionExecutionException( t.getCause( ).getMessage( ), t.getCause( ) );
    } else if ( t instanceof EucalyptusCloudException ) {
      return new TransactionExecutionException( t.getCause( ).getMessage( ), t.getCause( ) );
    } else if ( t instanceof UndeclaredThrowableException ) {
      return new TransactionCallbackException( t.getCause( ).getMessage( ), t.getCause( ) );
    } else {
      return new TransactionInternalException( t.getMessage( ), t );
    }
    
  }
  
//  
//  public static <T> EntityWrapper<T> join( ) {
//    return ( EntityWrapper<T> ) dbtl.get( );
//  }
  
  /**
   * TODO:GRZE: make this friendly wrt multiple types
   * 
   * @param <T>
   * @param search
   * @return
   */
  private static <T> EntityWrapper<T> joinOrCreate( T search ) {
    EntityWrapper<T> db;
//    if ( dbtl.get( ) != null ) {
//      try {
//        db = ( EntityWrapper<T> ) dbtl.get( );
//      } catch ( Exception ex ) {
//        db = dbtl.get( ).recast( ( Class<T> ) search.getClass( ) );
//      }
//    } else {
    db = EntityWrapper.get( search );
    dbtl.set( db );
//    }
    return db;
  }
  
  @Deprecated
  public static <T> T one( T search, final Tx<T> c ) throws TransactionException {
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
  
  public static <T> T find( T search ) throws TransactionException {
    return one( search, new Callback<T>( ) {
      
      @Override
      public void fire( T t ) {}
    } );
  }
  
  public static <T> List<T> findAll( T search ) throws TransactionException {
    return each( search, new Callback<T>( ) {
      
      @Override
      public void fire( T t ) {}
    } );
  }
  
  public static <T> boolean delete( T search ) throws TransactionException {
    return delete( search, new Predicate<T>( ) {
      
      @Override
      public boolean apply( T input ) {
        return false;
      }
    } );
  }
  
  public static <T> T save( T saveMe ) throws TransactionException {
    return save( saveMe, new Callback<T>( ) {
      
      @Override
      public void fire( T t ) {}
    } );
  }
  
  public static <T> List<T> filter( T search, Predicate<T> condition ) throws TransactionException {
    Function<T, T> f = Functions.identity( );
    return filteredTransform( search, condition, f );
  }
  
  public static <S, T> List<S> transform( T search, Function<T, S> f ) throws TransactionException {
    Predicate<T> p = Predicates.alwaysTrue( );
    return filteredTransform( search, p, f );
  }
  
  public static <T> List<T> each( T search, Callback<T> c ) {
    assertThat( search, notNullValue( ) );
    assertThat( c, notNullValue( ) );
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
  
  public static <T> boolean delete( T search, Predicate<T> precondition ) throws TransactionException {
    assertThat( search, notNullValue( ) );
    assertThat( precondition, notNullValue( ) );
    EntityWrapper<T> db = Transactions.joinOrCreate( search );
    try {
      T entity = db.getUnique( search );
      if ( precondition.apply( entity ) ) {
        db.delete( entity );
        db.commit( );
        return true;
      } else {
        db.commit( );
        return false;
      }
    } catch ( Throwable t ) {
      db.rollback( );
      throw Transactions.transformException( t );
    } finally {
      dbtl.remove( );
    }
  }
  
  public static <T> T one( T search, Callback<T> c ) throws TransactionException {
    assertThat( search, notNullValue( ) );
    assertThat( c, notNullValue( ) );
    EntityWrapper<T> db = Transactions.joinOrCreate( search );
    try {
      T entity = db.getUnique( search );
      c.fire( entity );
      db.commit( );
      return entity;
    } catch ( Throwable t ) {
      db.rollback( );
      throw Transactions.transformException( t );
    } finally {
      dbtl.remove( );
    }
  }
  
  public static <S, T> S transformOne( T search, Function<T, S> f ) throws TransactionException {
    assertThat( search, notNullValue( ) );
    assertThat( f, notNullValue( ) );
    EntityWrapper<T> db = Transactions.joinOrCreate( search );
    try {
      T entity = db.getUnique( search );
      S res = f.apply( entity );
      db.commit( );
      return res;
    } catch ( Throwable t ) {
      db.rollback( );
      throw Transactions.transformException( t );
    } finally {
      dbtl.remove( );
    }
  }
  
  public static <T> T save( T saveMe, Callback<T> c ) throws TransactionException {
    assertThat( saveMe, notNullValue( ) );
    assertThat( c, notNullValue( ) );
    EntityWrapper<T> db = Transactions.joinOrCreate( saveMe );
    try {
      T entity = db.merge( saveMe );
      c.fire( entity );
      db.commit( );
      return entity;
    } catch ( Throwable t ) {
      db.rollback( );
      throw Transactions.transformException( t );
    } finally {
      dbtl.remove( );
    }
  }
  
  public static <T, O> List<O> filteredTransform( T search, Predicate<T> condition, Function<T, O> transform ) throws TransactionException {
    assertThat( search, notNullValue( ) );
    assertThat( condition, notNullValue( ) );
    assertThat( transform, notNullValue( ) );
    List<O> res = Lists.newArrayList( );
    EntityWrapper<T> db = Transactions.joinOrCreate( search );
    try {
      List<T> queryResults = db.query( search );
      for ( T t : queryResults ) {
        if ( condition.apply( t ) ) {
          res.add( transform.apply( t ) );
        }
      }
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      throw Transactions.transformException( e );
    } finally {
      dbtl.remove( );
    }
    return res;
  }
  
}
