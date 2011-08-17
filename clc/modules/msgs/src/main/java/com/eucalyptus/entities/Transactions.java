package com.eucalyptus.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.HasNaturalId;
import com.eucalyptus.util.Logs;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

public class Transactions {
  private static Logger                           LOG      = Logger.getLogger( Transactions.class );
  private static ThreadLocal<AtomicInteger>       depth    = new ThreadLocal<AtomicInteger>( ) {
                                                             
                                                             @Override
                                                             protected AtomicInteger initialValue( ) {
                                                               return new AtomicInteger( 0 );
                                                             }
                                                             
                                                           };
  private static ThreadLocal<List<EntityWrapper>> wrappers = new ThreadLocal<List<EntityWrapper>>( ) {
                                                             
                                                             @Override
                                                             protected List<EntityWrapper> initialValue( ) {
                                                               return Lists.newArrayList( );
                                                             }
                                                             
                                                           };
  
  public static <T> EntityWrapper<T> get( T obj ) {
    depth.get( ).incrementAndGet( );
    EntityWrapper<T> db = Entities.get( obj );
    wrappers.get( ).add( db );
    return db;
  }
  
  private static void checkTransaction( ) {
    if ( Entities.hasTransaction( ) && depth.get( ).get( ) == 0 ) {
      depth.get( ).incrementAndGet( );
    }
  }
  
  private static void pop( ) {
    Integer nextLevel = depth.get( ).decrementAndGet( );
    if ( nextLevel <= 0 ) {
      for ( EntityWrapper db : wrappers.get( ) ) {
        if ( db.isActive( ) ) {
          db.commit( );
        }
      }
      wrappers.remove( );
      depth.remove( );
    }
  }
  
  private static TransactionException transformException( Throwable t ) {
    Logs.extreme( ).error( t, t );
    PersistenceExceptions.throwFiltered( t );
    if ( t instanceof InterruptedException ) {
      Thread.currentThread( ).interrupt( );
      return new TransactionExecutionException( t.getCause( ).getMessage( ), t.getCause( ) );
    } else if ( t instanceof EucalyptusCloudException ) {
      return new TransactionExecutionException( t.getMessage( ), t );
    } else if ( t instanceof UndeclaredThrowableException ) {
      return new TransactionCallbackException( t.getCause( ).getMessage( ), t.getCause( ) );
    } else {
      return new TransactionInternalException( t.getMessage( ), t );
    }
    
  }
  
  public static <T> List<T> findAll( T search ) throws TransactionException {
    return each( search, new Callback<T>( ) {
      
      @Override
      public void fire( T t ) {}
    } );
  }
  
  public static <T> List<T> each( T search, Callback<T> c ) throws TransactionException {
    checkTransaction( );
    assertThat( search, notNullValue( ) );
    assertThat( c, notNullValue( ) );
    EntityWrapper<T> db = Transactions.get( search );
    try {
      List<T> res = db.query( search );
      for ( T t : res ) {
        try {
          c.fire( t );
        } catch ( Exception ex ) {
          throw new TransactionCallbackException( ex );
        }
      }
      
      return res;
    } catch ( TransactionCallbackException e ) {
      db.rollback( );
      Logs.extreme( ).error( e, e );
      throw e;
    } catch ( UndeclaredThrowableException e ) {
      db.rollback( );
      Logs.extreme( ).error( e, e );
      throw e;
    } catch ( Throwable e ) {
      db.rollback( );
      Logs.extreme( ).error( e, e );
      throw new TransactionInternalException( e );
    } finally {
      pop( );
    }
  }
  
  public static <T> T find( T search ) throws TransactionException {
    return one( search, new Callback<T>( ) {
      
      @Override
      public void fire( T t ) {}
    } );
  }
  
  public static <T> T one( T search, final Callback<T> c ) throws TransactionException {
    return one( search, new Function<T, T>( ) {
      
      @Override
      public T apply( T input ) {
        try {
          c.fire( input );
        } catch ( Exception ex ) {
          throw new UndeclaredThrowableException( new TransactionCallbackException( ex ) );
        }
        return input;
      }
    } );
  }
  
  public static <S, T> S one( T search, Function<T, S> f ) throws TransactionException {
    checkTransaction( );
    assertThat( search, notNullValue( ) );
    assertThat( f, notNullValue( ) );
    EntityWrapper<T> db = Transactions.get( search );
    try {
      T entity = db.getUnique( search );
      try {
        S res = f.apply( entity );
        return res;
      } catch ( Exception ex ) {
        throw new TransactionCallbackException( ex );
      }
    } catch ( TransactionCallbackException e ) {
      db.rollback( );
      Logs.extreme( ).error( e, e );
      throw e;
    } catch ( UndeclaredThrowableException e ) {
      db.rollback( );
      Logs.extreme( ).error( e, e );
      if ( e.getCause( ) instanceof TransactionException ) {
        throw e;
      } else {
        throw new TransactionCallbackException( e.getCause( ) );
      }
    } catch ( Throwable t ) {
      db.rollback( );
      throw Transactions.transformException( t );
    } finally {
      pop( );
    }
  }
  
  public static <T> List<T> filter( T search, Predicate<T> condition ) throws TransactionException {
    Function<T, T> f = Functions.identity( );
    return filteredTransform( search, condition, f );
  }
  
  public static <S, T> List<S> transform( T search, Function<T, S> f ) throws TransactionException {
    Predicate<T> p = Predicates.alwaysTrue( );
    return filteredTransform( search, p, f );
  }
  
  public static <T, O> List<O> filteredTransform( T search, Predicate<T> condition, Function<T, O> transform ) throws TransactionException {
    checkTransaction( );
    assertThat( search, notNullValue( ) );
    assertThat( condition, notNullValue( ) );
    assertThat( transform, notNullValue( ) );
    List<O> res = Lists.newArrayList( );
    EntityWrapper<T> db = Transactions.get( search );
    try {
      List<T> queryResults = db.query( search );
      for ( T t : queryResults ) {
        if ( condition.apply( t ) ) {
          try {
            res.add( transform.apply( t ) );
          } catch ( Exception ex ) {
            throw new TransactionCallbackException( ex );
          }
        }
      }
      return res;
    } catch ( TransactionCallbackException e ) {
      db.rollback( );
      Logs.extreme( ).error( e, e );
      throw e;
    } catch ( Throwable e ) {
      db.rollback( );
      throw Transactions.transformException( e );
    } finally {
      pop( );
    }
  }
  
  public static <T> T save( T saveMe ) throws TransactionException {
    return save( saveMe, new Callback<T>( ) {
      
      @Override
      public void fire( T t ) {}
    } );
  }
  
  public static <T> T save( T saveMe, Callback<T> c ) throws TransactionException {
    checkTransaction( );
    assertThat( saveMe, notNullValue( ) );
    assertThat( c, notNullValue( ) );
    EntityWrapper<T> db = Transactions.get( saveMe );
    try {
      T entity = db.merge( saveMe );
      try {
        c.fire( entity );
      } catch ( Exception ex ) {
        throw new TransactionCallbackException( ex );
      }
      return entity;
    } catch ( TransactionCallbackException e ) {
      db.rollback( );
      Logs.extreme( ).error( e, e );
      throw e;
    } catch ( Throwable t ) {
      db.rollback( );
      throw Transactions.transformException( t );
    } finally {
      pop( );
    }
  }
  
  public static <T> boolean delete( T search ) throws TransactionException {
    return delete( search, new Predicate<T>( ) {
      
      @Override
      public boolean apply( T input ) {
        return false;
      }
    } );
  }
  
  public static <T> boolean delete( T search, Predicate<T> precondition ) throws TransactionException {
    checkTransaction( );
    assertThat( search, notNullValue( ) );
    assertThat( precondition, notNullValue( ) );
    EntityWrapper<T> db = Transactions.get( search );
    try {
      T entity = db.getUnique( search );
      try {
        if ( precondition.apply( entity ) ) {
          db.delete( entity );
          return true;
        } else {
          return false;
        }
      } catch ( Exception ex ) {
        throw new TransactionCallbackException( ex );
      }
    } catch ( TransactionCallbackException e ) {
      db.rollback( );
      Logs.extreme( ).error( e, e );
      throw e;
    } catch ( Throwable t ) {
      db.rollback( );
      throw Transactions.transformException( t );
    } finally {
      pop( );
    }
  }
  
}
