/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.entities;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.async.Callbacks;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;
import javax.annotation.Nonnull;

public class Transactions {

  private static <T> TransactionResource get( T obj ) {
    return Entities.transactionFor( obj );
  }
  
  private static TransactionException transformException( Throwable t ) {
    Logs.exhaust( ).error( t, t );
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

  /**
   * List all entities matching the given restriction.
   *
   * @param restriction The entity restriction
   * @param <T> The entity type
   * @return The list of matching entities
   * @throws TransactionException If an error occurs
   */
  @Nonnull
  public static <T> List<T> findAll( @Nonnull EntityRestriction<T> restriction ) throws TransactionException {
    return each( restriction, Callbacks.<T>noop( ) );
  }

  /**
   * List all entities matching the given restriction.
   *
   * <p>The given callback is invoked within the transaction for each item.</p>
   *
   * @param restriction The entity restriction
   * @param callback The callback to use
   * @param <T> The entity type
   * @return The list of matching entities
   * @throws TransactionException If an error occurs
   */
  public static <T> List<T> each(
      @Nonnull EntityRestriction<T> restriction,
      @Nonnull Callback<T> callback
  ) throws TransactionException {
    checkParam( restriction, notNullValue() );
    checkParam( callback, notNullValue() );
    try ( final TransactionResource db = Transactions.get( restriction.getEntityClass( ) ) ) {
      final List<T> res = Entities.criteriaQuery( restriction ).list( );
      for ( T t : res ) {
        try {
          callback.fire( t );
        } catch ( Exception ex ) {
          throw new TransactionCallbackException( ex );
        }
      }
      db.commit( );
      return res;
    } catch ( TransactionCallbackException | UndeclaredThrowableException e ) {
      Logs.extreme( ).error( e, e );
      throw e;
    } catch ( Exception e ) {
      Logs.extreme( ).error( e, e );
      throw new TransactionInternalException( e );
    }
  }

  /**
   * Get the unique entity matching the given restriction.
   *
   * @param restriction The entity restriction
   * @param <T> The entity type
   * @return The entity
   * @throws TransactionException If an error occurs or a unique matching entity is not found
   */
  public static <T> T one( @Nonnull EntityRestriction<T> restriction ) throws TransactionException {
    return one( restriction, Functions.<T>identity( ) );
  }

  /**
   * Get the unique entity matching the given restriction.
   *
   * <p>The given callback is invoked with the transaction for the entity</p>
   *
   * @param restriction The entity restriction
   * @param callback The callback to use
   * @param <T> The entity type
   * @return The entity
   * @throws TransactionException If an error occurs or a unique matching entity is not found
   */
  public static <T> T one(
      @Nonnull EntityRestriction<T> restriction,
      @Nonnull final Callback<T> callback
  ) throws TransactionException {
    return one( restriction, FUtils.function( callback ) );
  }

  /**
   * Get the transformed unique entity matching the given restriction.
   *
   * <p>The given transform is invoked within the transaction for the entity</p>
   *
   * @param restriction The entity restriction
   * @param function The entity transformation function
   * @param <S> The result type
   * @param <T> The entity type
   * @return The transformed entity or null if the function returns null
   * @throws TransactionException If an error occurs
   */
  public static <S, T> S one(
      @Nonnull EntityRestriction<T> restriction,
      @Nonnull Function<T, S> function
  ) throws TransactionException {
    return one( restriction, Predicates.alwaysTrue( ), function );
  }

  /**
   * Get the transformed unique entity matching the given restriction and predicate.
   *
   * <p>The given transform is invoked within the transaction for the entity</p>
   *
   * @param restriction The entity restriction
   * @param predicate The predicate for entity matching
   * @param function The entity transformation function
   * @param <S> The result type
   * @param <T> The entity type
   * @return The transformed entity or null if the function returns null
   * @throws TransactionException If an error occurs
   */
  public static <S, T> S one(
      @Nonnull final EntityRestriction<T> restriction,
      @Nonnull final Predicate<? super T> predicate,
      @Nonnull final Function<? super T, S> function
  ) throws TransactionException {
    return one( restriction.getEntityClass( ), new Callable<T>( ) {
      @Override
      public T call( ) throws TransactionException {
        return Entities.criteriaQuery( restriction ).uniqueResult( );
      }
    }, predicate, function );
  }

  /**
   * Filtered listing of entities for the given restriction and condition.
   *
   * @param restriction The entity restriction
   * @param predicate The predicate for entity matching
   * @param <T> The entity type
   * @return The list of matching entities
   * @throws TransactionException If an error occurs
   */
  public static <T> List<T> filter(
      @Nonnull final EntityRestriction<T> restriction,
      @Nonnull final Predicate<? super T> predicate
  ) throws TransactionException {
    final Function<T, T> function = Functions.identity( );
    return filteredTransform( restriction, predicate, function );
  }

  /**
   * Transformed listing of entities for the given restriction.

   * <p>The given transform is invoked within the transaction for the entity</p>
   *
   * @param restriction The entity restriction
   * @param function The entity transformation function
   * @param <T> The entity type
   * @return The list of matching entities
   * @throws TransactionException If an error occurs
   */
  public static <S, T> List<S> transform(
      @Nonnull final EntityRestriction<T> restriction,
      @Nonnull final Function<T, S> function
  ) throws TransactionException {
    Predicate<T> p = Predicates.alwaysTrue( );
    return filteredTransform( restriction, p, function );
  }

  /**
   * Transformed filtered listing of entities for the given restriction.

   * <p>The given transform is invoked within the transaction for the entity</p>
   *
   * @param restriction The entity restriction
   * @param predicate The predicate for entity matching
   * @param function The entity transformation function
   * @param <T> The entity type
   * @param <O> The result type
   * @return The list of matching transformed entities
   * @throws TransactionException If an error occurs
   */
  public static <T, O> List<O> filteredTransform(
      @Nonnull final EntityRestriction<T> restriction,
      @Nonnull final Predicate<? super T> predicate,
      @Nonnull final Function<? super T, O> function
  ) throws TransactionException {
    checkParam( restriction, notNullValue() );
    final Supplier<List<T>> resultsSupplier = new Supplier<List<T>>() {
      @Override
      public List<T> get( ) {
        return Entities.criteriaQuery( restriction ).list( );
      }
    };
    return filteredTransform( restriction.getEntityClass( ), resultsSupplier, predicate, function );
  }

  /**
   * Delete the unique entity matching the given restriction.
   *
   * @param restriction The entity restriction
   * @param <T> The entity type
   * @return true if an entity was deleted
   * @throws TransactionException If an error occurs
   */
  public static <T> boolean delete( @Nonnull final EntityRestriction<T> restriction ) throws TransactionException {
    return delete( restriction, Predicates.<T>alwaysTrue() );
  }

  /**
   * Delete the unique entity matching the given restriction and precondition.
   *
   * @param restriction The entity restriction
   * @param precondition The predicate to match
   * @param <T> The entity type
   * @return True if an entity was deleted
   * @throws TransactionException If an error occurs
   */
  public static <T> boolean delete(
      @Nonnull final EntityRestriction<T> restriction,
      @Nonnull Predicate<? super T> precondition
  ) throws TransactionException {
    checkParam( restriction, notNullValue() );
    checkParam( precondition, notNullValue() );
    try ( final TransactionResource db = Transactions.get( restriction.getEntityClass( ) ) ) {
      T entity = Entities.criteriaQuery( restriction ).uniqueResult( );
      try {
        if ( precondition.apply( entity ) ) {
          Entities.delete( entity );
          db.commit( );
          return true;
        } else {
          return false;
        }
      } catch ( Exception ex ) {
        throw new TransactionCallbackException( ex );
      }
    } catch ( TransactionCallbackException e ) {
      Logs.extreme( ).error( e, e );
      throw e;
    } catch ( Exception t ) {
      throw Transactions.transformException( t );
    }
  }

  /**
   * Deletes all queried entities, based on the search entity, that match the precondition.
   * Returns true if all succeeded, false if otherwise. Does not stop on first failure. Attempts
   * all regardless of failure
   */
  public static <T> boolean deleteAll(
      @Nonnull final EntityRestriction<T> restriction,
      @Nonnull Predicate<? super T> precondition
  ) throws TransactionException {
    checkParam( restriction, notNullValue() );
    checkParam( precondition, notNullValue() );
    try ( final TransactionResource db = Transactions.get( restriction.getEntityClass( ) ) ) {
      List<T> entities = Entities.criteriaQuery( restriction ).list( );
      boolean failed = false;
      for(T entity : entities) {
        try {
          if ( precondition.apply( entity ) ) {
            Entities.delete( entity );
          } else {
            failed = true;
          }
        } catch ( Exception ex ) {
          throw new TransactionCallbackException( ex );
        }
      }
      db.commit( );
      return !failed;
    } catch ( TransactionCallbackException e ) {
      Logs.extreme( ).error( e, e );
      throw e;
    } catch ( Exception t ) {
      throw Transactions.transformException( t );
    }
  }

  /**
   * Bulk delete all matching entities, based on the restriction..
   *
   * @param restriction The entity restriction
   * @return The count of deleted entities.
   */
  public static <T> int deleteAll( @Nonnull final EntityRestriction<T> restriction ) throws TransactionException {
    checkParam( restriction, notNullValue() );
    try ( final TransactionResource db = Transactions.get( restriction.getEntityClass( ) ) ) {
      final int count = Entities.delete( restriction ).delete( );
      db.commit( );
      return count;
    } catch ( Exception t ) {
      throw Transactions.transformException( t );
    }
  }

  @Deprecated
  public static <T> List<T> findAll( T search ) throws TransactionException {
    return each( search, Callbacks.<T>noop( ) );
  }

  @Deprecated
  public static <T> List<T> each( T search, Callback<T> c ) throws TransactionException {
    return each( search, Restrictions.conjunction(), Collections.<String,String>emptyMap(), c );
  }

  @Deprecated
  public static <T> List<T> each( final T search,
                                  final Criterion criterion,
                                  final Map<String,String> aliases,
                                  final Callback<T> c ) throws TransactionException {
    checkParam( search, notNullValue() );
    checkParam( c, notNullValue() );
    try ( final TransactionResource db = Transactions.get( search ) ) {
      List<T> res = Entities.query( search, false, criterion, aliases );
      for ( T t : res ) {
        try {
          c.fire( t );
        } catch ( Exception ex ) {
          throw new TransactionCallbackException( ex );
        }
      }

      db.commit( );
      return res;
    } catch ( TransactionCallbackException | UndeclaredThrowableException e ) {
      Logs.extreme( ).error( e, e );
      throw e;
    } catch ( Exception e ) {
      Logs.extreme( ).error( e, e );
      throw new TransactionInternalException( e );
    }
  }

  @Deprecated
  public static <T> T find( T search ) throws TransactionException {
    return one( search, Functions.<T>identity() );
  }

  @Deprecated
  public static <T> T one( T search, final Callback<T> c ) throws TransactionException {
    return one( search, new Function<T, T>( ) {
      
      @Override
      public T apply( T input ) {
        try {
          c.fire( input );
        } catch ( Exception ex ) {
          throw Exceptions.toUndeclared( new TransactionCallbackException( ex ) );
        }
        return input;
      }
    } );
  }

  @Deprecated
  public static <S, T> S one( T search, Function<T, S> f ) throws TransactionException {
    return one( search, Predicates.alwaysTrue(), f );
  }

  @Deprecated
  public static <S, T> S one( final T search,
                              final Predicate<? super T> predicate,
                              final Function<? super T, S> f ) throws TransactionException {
    return one( search, new Callable<T>( ){
      @Override
      public T call( ) throws TransactionException {
        return Entities.uniqueResult( search );
      }
    }, predicate, f );
  }

  @Deprecated
  public static <S, T> S one( final T search,
                              final Criterion criterion,
                              final Map<String,String> aliases,
                              final Predicate<? super T> predicate,
                              final Function<? super T, S> f ) throws TransactionException {
    return one( search, new Callable<T>( ){
      @Override
      public T call( ) throws TransactionException {
        final List<T> ts = Entities.query( search, false, criterion, aliases );
        if ( ts.size( ) != 1 ) throw new NoSuchElementException( );
        return ts.get( 0 );
      }
    }, predicate, f );
  }

  private static <S, T> S one( final EntityRestriction<T> type,
                               final Callable<T> lookup,
                               final Predicate<? super T> predicate,
                               final Function<? super T, S> f ) throws TransactionException {
    checkParam( type, notNullValue() );
    return one( type.getEntityClass( ), lookup, predicate, f );
  }

  private static <S, T> S one( final T type,
                               final Callable<T> lookup,
                               final Predicate<? super T> predicate,
                               final Function<? super T, S> f ) throws TransactionException {
    checkParam( type, notNullValue() );
    return one( type.getClass( ), lookup, predicate, f );
  }

  private static <S, T> S one( final Class<?> type,
                               final Callable<T> lookup,
                               final Predicate<? super T> predicate,
                               final Function<? super T, S> f ) throws TransactionException {
    checkParam( f, notNullValue() );
    try ( final TransactionResource db = Transactions.get( type ) ) {
      T entity = lookup.call();
      if ( !predicate.apply( entity ) ) {
        throw new NoSuchElementException();
      }
      try {
        S res = f.apply( entity );
        db.commit( );
        return res;
      } catch ( Exception ex ) {
        throw new TransactionCallbackException( ex );
      }
    } catch ( TransactionCallbackException e ) {
      Logs.extreme( ).error( e, e );
      throw e;
    } catch ( UndeclaredThrowableException e ) {
      Logs.extreme( ).error( e, e );
      if ( e.getCause( ) instanceof TransactionException ) {
        throw e;
      } else {
        throw new TransactionCallbackException( e.getCause( ) );
      }
    } catch ( Exception t ) {
      throw Transactions.transformException( t );
    }
  }

  @Deprecated
  public static <T> List<T> filter( T search, Predicate<? super T> condition ) throws TransactionException {
    Function<T, T> f = Functions.identity( );
    return filteredTransform( search, condition, f );
  }

  @Deprecated
  public static <T> List<T> filter( final T search,
                                    final Predicate<? super T> condition,
                                    final Criterion criterion,
                                    final Map<String,String> aliases ) throws TransactionException {
    Function<T, T> f = Functions.identity( );
    return filteredTransform( search, criterion, aliases, condition, f );
  }

  @Deprecated
  public static <S, T> List<S> transform( T search, Function<T, S> f ) throws TransactionException {
    Predicate<T> p = Predicates.alwaysTrue( );
    return filteredTransform( search, p, f );
  }

  @Deprecated
  public static <T, O> List<O> filteredTransform( final T search,
                                                  final Predicate<? super T> condition,
                                                  final Function<? super T, O> transform ) throws TransactionException {
    checkParam( search, notNullValue() );
    final Supplier<List<T>> resultsSupplier = new Supplier<List<T>>() {
      @Override
      public List<T> get() {
        return Entities.query( search );
      }
    };

    return filteredTransform( search.getClass(), resultsSupplier, condition, transform );
  }

  @Deprecated
  public static <T, O> List<O> filteredTransform( final T search,
                                                  final Criterion criterion,
                                                  final Map<String,String> aliases,
                                                  final Predicate<? super T> condition,
                                                  final Function<? super T, O> transform ) throws TransactionException {
    checkParam( search, notNullValue() );
    final Supplier<List<T>> resultsSupplier = new Supplier<List<T>>() {
      @Override
      public List<T> get() {
        return Entities.query( search, false, criterion, aliases );
      }
    };

    return filteredTransform( search.getClass(), resultsSupplier, condition, transform );
  }

  private static <T, O> List<O> filteredTransform( Class<?> searchClass,
                                                   Supplier<List<T>> searchResultSupplier,
                                                   Predicate<? super T> condition,
                                                   Function<? super T, O> transform ) throws TransactionException {
    checkParam( searchResultSupplier, notNullValue() );
    checkParam( condition, notNullValue() );
    checkParam( transform, notNullValue() );
    List<O> res = Lists.newArrayList( );
    try ( final TransactionResource db = Transactions.get( searchClass ) ) {
      List<T> queryResults = searchResultSupplier.get();
      for ( T t : queryResults ) {
        if ( condition.apply( t ) ) {
          try {
            res.add( transform.apply( t ) );
          } catch ( Exception ex ) {
            throw new TransactionCallbackException( ex );
          }
        }
      }
      db.commit( );
      return res;
    } catch ( TransactionCallbackException e ) {
      Logs.extreme( ).error( e, e );
      throw e;
    } catch ( Exception e ) {
      throw Transactions.transformException( e );
    }
  }

  public static <T> T save( T saveMe ) throws TransactionException {
    return save( saveMe, new Callback<T>( ) {
      
      @Override
      public void fire( T t ) {}
    } );
  }

  public static <T> T save( T saveMe, Callback<T> c ) throws TransactionException {
    checkParam( saveMe, notNullValue() );
    checkParam( c, notNullValue() );
    try ( final TransactionResource db = Transactions.get( saveMe ) ) {
      T entity = Entities.merge( saveMe );
      try {
        c.fire( entity );
      } catch ( Exception ex ) {
        throw new TransactionCallbackException( ex );
      }
      db.commit( );
      return entity;
    } catch ( TransactionCallbackException e ) {
      Logs.extreme( ).error( e, e );
      throw e;
    } catch ( Exception t ) {
      throw Transactions.transformException( t );
    }
  }

  /**
   * Save a new entity.
   * 
   * <p>Unlike {@link #save} this method will never update an existing entity.</p>
   * 
   * @param saveMe The new entity to save.
   * @param <T> The entity type.
   * @return The persistent entity
   * @throws TransactionException If an error occurs
   */
  public static <T> T saveDirect( T saveMe ) throws TransactionException {
    checkParam( saveMe, notNullValue() );
    try ( final TransactionResource db = Transactions.get( saveMe ) ) {
      final T entity = Entities.persist( saveMe );
      db.commit( );
      return entity;
    } catch ( Exception t ) {
      throw Transactions.transformException( t );
    }
  }

  @Deprecated
  public static <T> boolean delete( T search ) throws TransactionException {
    return delete( search, Predicates.alwaysTrue() );
  }

  @Deprecated
  public static <T> boolean delete( T search, Predicate<? super T> precondition ) throws TransactionException {
    checkParam( search, notNullValue() );
    checkParam( precondition, notNullValue() );
    try ( final TransactionResource db = Transactions.get( search ) ) {
      T entity = Entities.uniqueResult( search );
      try {
        if ( precondition.apply( entity ) ) {
          Entities.delete( entity );
          db.commit( );
          return true;
        } else {
          return false;
        }
      } catch ( Exception ex ) {
        throw new TransactionCallbackException( ex );
      }
    } catch ( TransactionCallbackException e ) {
      Logs.extreme( ).error( e, e );
      throw e;
    } catch ( Exception t ) {
      throw Transactions.transformException( t );
    }
  }

  /**
   * Deletes all queried entities, based on the search entity, that match the precondition.
   * Returns true if all succeeded, false if otherwise. Does not stop on first failure. Attempts
   * all regardless of failure
   * @param search
   * @param precondition
   * @return
   * @throws TransactionException
   */
  @Deprecated
  public static <T> boolean deleteAll( T search, Predicate<? super T> precondition ) throws TransactionException {
	    checkParam( search, notNullValue() );
	    checkParam( precondition, notNullValue() );
	    try ( final TransactionResource db = Transactions.get( search ) ) {
	      List<T> entities = Entities.query(search);
	      boolean failed = false;
	      for(T entity : entities) {
	    	  try {
	    		  if ( precondition.apply( entity ) ) {
	    			  Entities.delete( entity );
	    		  } else {
	    			  failed = true;
	    		  }
	    	  } catch ( Exception ex ) {
	    		  throw new TransactionCallbackException( ex );
	    	  }
	      }
	      db.commit( );
	      return !failed;
	    } catch ( TransactionCallbackException e ) {
	      Logs.extreme( ).error( e, e );
	      throw e;
	    } catch ( Exception t ) {
	      throw Transactions.transformException( t );
	    }
	  }

}
