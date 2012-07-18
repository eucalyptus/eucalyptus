/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 ************************************************************************/

package com.eucalyptus.entities;

import java.util.List;
import java.util.SortedSet;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.LockTimeoutException;
import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.persistence.PessimisticLockException;
import javax.persistence.RollbackException;
import javax.persistence.TransactionRequiredException;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.InstantiationException;
import org.hibernate.LazyInitializationException;
import org.hibernate.MappingException;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.PersistentObjectException;
import org.hibernate.PropertyAccessException;
import org.hibernate.QueryException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.SessionException;
import org.hibernate.StaleStateException;
import org.hibernate.TransactionException;
import org.hibernate.TransientObjectException;
import org.hibernate.TypeMismatchException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.WrongClassException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.jdbc.TooManyRowsAffectedException;
import org.hibernate.loader.MultipleBagFetchException;
import org.hibernate.type.SerializationException;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class PersistenceExceptions {
  private static Logger LOG = Logger.getLogger( PersistenceExceptions.class );
  
  public enum ErrorCategory {
    BUG {
      @Override
      public RuntimeException handleException( final RuntimeException e ) {
        LOG.fatal( e, e );
        throw super.handleException( e );
      }
    },
    APPLICATION, CONSTRAINT, RUNTIME,
    CONNECTION {
      @Override
      public RuntimeException handleException( final RuntimeException e ) {
        Databases.check( );
        Logs.extreme( ).error( e, e );
        return super.handleException( e );
      }
      
      @Override
      public boolean isRecoverable( ) {
        return true;
      }
    };
    
    public boolean isRecoverable( ) {
      return false;
    }
    
    public RuntimeException handleException( final RuntimeException e ) {
      final String msg = new StringBuilder( ).append( "[" ).append( this.name( ) ).append( "] Persistence error occurred because of: " + e.getMessage( ) ).toString( );
      Logs.exhaust( ).error( msg, e );
      return e;
    }
  };
  
  private static final Multimap<ErrorCategory, Class<? extends Exception>> errorCategorization = buildErrorMap( );
  
  @SuppressWarnings( "unchecked" )
  private static Multimap<ErrorCategory, Class<? extends Exception>> buildErrorMap( ) {
    final Multimap<ErrorCategory, Class<? extends Exception>> map = ArrayListMultimap.create( );
    map.get( ErrorCategory.BUG ).addAll( Lists.newArrayList( LazyInitializationException.class, InstantiationException.class, MappingException.class,
                                                             MultipleBagFetchException.class, NonUniqueObjectException.class, QueryException.class,
                                                             PersistentObjectException.class, PropertyAccessException.class, SerializationException.class,
                                                             SessionException.class, TooManyRowsAffectedException.class, TransientObjectException.class,
                                                             StaleStateException.class, TypeMismatchException.class, UnresolvableObjectException.class,
                                                             WrongClassException.class, SQLGrammarException.class, TransactionRequiredException.class,
                                                             HibernateException.class ) );
    map.get( ErrorCategory.CONSTRAINT ).addAll( Lists.newArrayList( ConstraintViolationException.class, NonUniqueResultException.class,
                                                                    NoResultException.class, NonUniqueResultException.class ) );
    map.get( ErrorCategory.RUNTIME ).addAll( Lists.newArrayList( TransactionException.class, IllegalStateException.class, RollbackException.class,
                                                                 PessimisticLockException.class, OptimisticLockException.class, EntityNotFoundException.class,
                                                                 EntityExistsException.class ) );
    map.get( ErrorCategory.CONNECTION ).addAll( Lists.newArrayList( JDBCConnectionException.class, QueryTimeoutException.class, LockTimeoutException.class ) );
    return map;
  }
  
  enum ClassifySingleException implements Function<Throwable, ErrorCategory> {
    INSTANCE;
    @Override
    public ErrorCategory apply( final Throwable input ) {
      final SortedSet<ErrorCategory> results = Sets.newTreeSet( );
      for ( final Class<?> p : Classes.classAncestors( input ) ) {
        for ( final ErrorCategory category : ErrorCategory.values( ) ) {
          if ( errorCategorization.get( category ).contains( p ) ) {
            if ( category.isRecoverable( ) ) {
              return category;
            } else {
              results.add( category );
            }
          }
        }
      }
      if ( results.isEmpty( ) ) {
        return ErrorCategory.APPLICATION;
      } else {
        return results.first( );
      }
    }
    
  }
  
  public static ErrorCategory classify( final Throwable e ) {
    final List<ErrorCategory> res = Lists.transform( Exceptions.causes( e ), ClassifySingleException.INSTANCE );
    if ( res.isEmpty( ) ) {
      return ErrorCategory.APPLICATION;
    } else {
      return Sets.newTreeSet( res ).last( );
    }
  }
  
  /**
   * Filters and classifies exceptions -- all JPA/hibernate exceptions are runtime exceptions. Those
   * which can be handled by the application, or which give feedback about the underlying cause of
   * the failure (e.g., constraints violation).
   * 
   * @param e
   * @see {@link http://docs.jboss.org/hibernate/core/3.5/api/org/hibernate/HibernateException.html}
   */
  @SuppressWarnings( "unchecked" )
  public static RecoverablePersistenceException throwFiltered( final Throwable e ) {
    Logs.extreme( ).trace( e, e );
    ConstraintViolationException constraintVolationCause = Exceptions.findCause( e, ConstraintViolationException.class );
    if ( constraintVolationCause != null ) {
      throw constraintVolationCause;
    } else {
      PersistentObjectException detachedObjectCause = Exceptions.findCause( e, PersistentObjectException.class );
      if ( detachedObjectCause != null ) {
        LOG.error( detachedObjectCause );
      }
      if ( e instanceof RuntimeException ) {
        final ErrorCategory category = PersistenceExceptions.classify( e );
        final RuntimeException up = category.handleException( ( RuntimeException ) e );
        if ( !category.isRecoverable( ) ) {
          throw up;
        } else {
          return new RecoverablePersistenceException( "Error during transaction: " + Joiner.on( '\n' ).join( Exceptions.causes( e ) ), e );
        }
      } else {
        throw ErrorCategory.APPLICATION.handleException( new PersistenceException( "Error during transaction: " + e.getMessage( ), e ) );
      }
    }
  }
  
  public static Exception transform( final Throwable e ) {
    try {
      return throwFiltered( e );
    } catch ( Exception ex ) {
      return ex;
    }
  }
}
