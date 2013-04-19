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

package com.eucalyptus.entities;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;

public class Transactions {
  private static Logger                           LOG      = Logger.getLogger( Transactions.class );
  private static ThreadLocal<AtomicInteger>       depth    = new ThreadLocal<AtomicInteger>( ) {
                                                             
                                                             @Override
                                                             protected AtomicInteger initialValue( ) {
                                                               return new AtomicInteger( 0 );
                                                             }
                                                             
                                                           };
  private static ThreadLocal<List<EntityTransaction>> wrappers = new ThreadLocal<List<EntityTransaction>>( ) {
                                                             
                                                             @Override
                                                             protected List<EntityTransaction> initialValue( ) {
                                                               return Lists.newArrayList( );
                                                             }
                                                             
                                                           };
  
  private static <T> EntityTransaction get( T obj ) {
    depth.get( ).incrementAndGet( );
    EntityTransaction db = Entities.get( obj );
    wrappers.get( ).add( db );
    return db;
  }
  
  private static void pop( ) {
    Integer nextLevel = depth.get( ).decrementAndGet( );
    if ( nextLevel <= 0 ) {
      for ( EntityTransaction db : wrappers.get( ) ) {
        if ( db.isActive( ) ) {
          db.commit( );
        }
      }
      wrappers.remove( );
      depth.remove( );
    }
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
  
  public static <T> List<T> findAll( T search ) throws TransactionException {
    return each( search, new Callback<T>( ) {
      
      @Override
      public void fire( T t ) {}
    } );
  }

  public static <T> List<T> each( T search, Callback<T> c ) throws TransactionException {
    return each( search, Restrictions.conjunction(), Collections.<String,String>emptyMap(), c );
  }

  public static <T> List<T> each( final T search,
                                  final Criterion criterion,
                                  final Map<String,String> aliases,
                                  final Callback<T> c ) throws TransactionException {
    checkParam( search, notNullValue() );
    checkParam( c, notNullValue() );
    EntityTransaction db = Transactions.get( search );
    try {
      List<T> res = Entities.query( search, false, criterion, aliases );
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
    } catch ( Exception e ) {
      db.rollback( );
      Logs.extreme( ).error( e, e );
      throw new TransactionInternalException( e );
    } finally {
      pop( );
    }
  }

  public static <T> T find( T search ) throws TransactionException {
    return one( search, Functions.<T>identity() );
  }
  
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

  public static <S, T> S one( T search, Function<T, S> f ) throws TransactionException {
    return one( search, Predicates.alwaysTrue(), f );
  }

  public static <S, T> S one( final T search,
                              final Predicate<? super T> predicate,
                              final Function<? super T, S> f ) throws TransactionException {
    checkParam( search, notNullValue() );
    checkParam( f, notNullValue() );
    EntityTransaction db = Transactions.get( search );
    try {
      T entity = Entities.uniqueResult( search );
      if ( !predicate.apply( entity ) ) {
        throw new NoSuchElementException();
      }
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
    } catch ( Exception t ) {
      db.rollback( );
      throw Transactions.transformException( t );
    } finally {
      pop( );
    }
  }
  
  public static <T> List<T> filter( T search, Predicate<? super T> condition ) throws TransactionException {
    Function<T, T> f = Functions.identity( );
    return filteredTransform( search, condition, f );
  }

  public static <T> List<T> filter( final T search,
                                    final Predicate<? super T> condition,
                                    final Criterion criterion,
                                    final Map<String,String> aliases ) throws TransactionException {
    Function<T, T> f = Functions.identity( );
    return filteredTransform( search, criterion, aliases, condition, f );
  }

  public static <S, T> List<S> transform( T search, Function<T, S> f ) throws TransactionException {
    Predicate<T> p = Predicates.alwaysTrue( );
    return filteredTransform( search, p, f );
  }
  
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
    EntityTransaction db = Transactions.get( searchClass );
    try {
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
      return res;
    } catch ( TransactionCallbackException e ) {
      db.rollback( );
      Logs.extreme( ).error( e, e );
      throw e;
    } catch ( Exception e ) {
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
    checkParam( saveMe, notNullValue() );
    checkParam( c, notNullValue() );
    EntityTransaction db = Transactions.get( saveMe );
    try {
      T entity = Entities.merge( saveMe );
      try {
        c.fire( entity );
      } catch ( Exception ex ) {
        throw new TransactionCallbackException( ex );
      }
      db.commit( );
      return entity;
    } catch ( TransactionCallbackException e ) {
      db.rollback( );
      Logs.extreme( ).error( e, e );
      throw e;
    } catch ( Exception t ) {
      db.rollback( );
      throw Transactions.transformException( t );
    } finally {
      pop( );
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
    final EntityTransaction db = Transactions.get( saveMe );
    try {
      final T entity = Entities.persist( saveMe );
      db.commit( );
      return entity;
    } catch ( Exception t ) {
      throw Transactions.transformException( t );
    } finally {
      if ( db.isActive() ) db.rollback();
      pop( );
    }
  }

  public static <T> boolean delete( T search ) throws TransactionException {
    return delete( search, Predicates.alwaysTrue() );
  }
  
  public static <T> boolean delete( T search, Predicate<? super T> precondition ) throws TransactionException {
    checkParam( search, notNullValue() );
    checkParam( precondition, notNullValue() );
    EntityTransaction db = Transactions.get( search );
    try {
      T entity = Entities.uniqueResult( search );
      try {
        if ( precondition.apply( entity ) ) {
          Entities.delete( entity );
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
    } catch ( Exception t ) {
      db.rollback( );
      throw Transactions.transformException( t );
    } finally {
      pop( );
    }
  }
  
}
