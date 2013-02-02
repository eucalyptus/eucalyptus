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

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.ejb.EntityManagerFactoryImpl;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.HasNaturalId;
import com.eucalyptus.util.LogUtil;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@ConfigurableClass( root = "bootstrap.tx",
                    description = "Parameters controlling transaction behaviour." )
public class Entities {
  @ConfigurableField( description = "Maximum number of times a transaction may be retried before giving up.",
                      initial = "5" )
  public static Integer                                          CONCURRENT_UPDATE_RETRIES = 10;
  private static ConcurrentMap<String, String>                   txLog                     = new MapMaker( ).softKeys( ).softValues( ).makeMap( );
  private static Logger                                          LOG                       = Logger.getLogger( Entities.class );
  private static ThreadLocal<String>                             txRootThreadLocal         = new ThreadLocal<String>( );
  private static ThreadLocal<ConcurrentMap<String, CascadingTx>> txStateThreadLocal        = new ThreadLocal<ConcurrentMap<String, CascadingTx>>( ) {
                                                                                             
                                                                                             @Override
                                                                                             protected ConcurrentMap<String, CascadingTx> initialValue( ) {
                                                                                               return Maps.newConcurrentMap( );
                                                                                             }
                                                                                             
                                                                                           };
  
  private static CascadingTx lookup( final Object obj ) {
    final String ctx = lookatPersistenceContext( obj );
    return txStateThreadLocal.get( ).get( ctx );
  }
  
  static String lookatPersistenceContext( final Object obj ) throws RuntimeException {
    final Class type = Classes.typeOf( obj );
    final Ats ats = Ats.inClassHierarchy( type );
    PersistenceContext persistenceContext = null;
    if ( !ats.has( PersistenceContext.class ) ) {
      throw new RuntimeException( "Attempting to create an entity wrapper instance for non persistent type: " + type
                                  + ".  Class hierarchy contains: \n" + ats.toString( ) );
    } else {
      persistenceContext = ats.get( PersistenceContext.class );
    }
    return persistenceContext.name( );
  }
  
  private static boolean hasTransaction( ) {
    return !txStateThreadLocal.get( ).isEmpty( );
  }
  
  public static boolean hasTransaction( final Object obj ) {
    final String ctx = lookatPersistenceContext( obj );
    final CascadingTx tx = txStateThreadLocal.get( ).get( ctx );
    if ( tx == null ) {
      return false;
    } else if ( tx.isActive( ) ) {
      return true;
    } else {
      cleanStrandedTx( tx );
      return false;
    }
  }
  
  private static CascadingTx getTransaction( final Object obj ) {
    if ( hasTransaction( obj ) ) {
      return txStateThreadLocal.get( ).get( lookatPersistenceContext( obj ) );
    } else {
      throw new NoSuchElementException( "Failed to find active transaction for persistence context: " + lookatPersistenceContext( obj ) + " and object: " + obj );
    }
  }
  
  public static void removeTransaction( final CascadingTx tx ) {
    final String txId = makeTxRootName( tx );
    txLog.remove( txStateThreadLocal.toString( ) + tx.getRecord( ).getPersistenceContext( ) );
    txStateThreadLocal.get( ).remove( tx.getRecord( ).getPersistenceContext( ) );
    if ( txId.equals( txStateThreadLocal.get( ) ) ) {
      for ( final Entry<String, CascadingTx> e : txStateThreadLocal.get( ).entrySet( ) ) {
        cleanStrandedTx( e.getValue( ) );
      }
      txStateThreadLocal.get( ).clear( );
      txStateThreadLocal.remove( );
    }
  }
  
  private static void cleanStrandedTx( final CascadingTx txValue ) {
    LOG.error( "Found stranded transaction: " + txValue.getRecord( ).getPersistenceContext( ) + " started at: " + txValue.getRecord( ).getStack( ) );
    try {
      txValue.rollback( );
    } catch ( final Exception ex ) {
      LOG.trace( ex, ex );
    }
  }
  
  private static String makeTxRootName( final CascadingTx tx ) {
    return txStateThreadLocal.toString( ) + tx.getRecord( ).getPersistenceContext( );
  }
  
  private static CascadingTx createTransaction( final Object obj ) throws RecoverablePersistenceException, RuntimeException {
    final String ctx = lookatPersistenceContext( obj );
    final CascadingTx ret = new CascadingTx( ctx );
    try {
      ret.begin( );
      if ( txRootThreadLocal.get( ) == null ) {
        final String txId = makeTxRootName( ret );
        LOG.trace( "Creating root entry for transaction tree: " + txId + " at: \n" + Threads.currentStackString( ) );
        txRootThreadLocal.set( txId );
      }
      txStateThreadLocal.get( ).put( ctx, ret );
      return ret;
    } catch ( RuntimeException ex ) {
      try {
        ret.rollback( );
      } catch ( RuntimeException ex1 ) {
        throw ex1;
      }
      throw ex;
    }
  }
  
  public static EntityTransaction get( final Object obj ) {
    if ( hasTransaction( obj ) ) {
      final CascadingTx tx = getTransaction( obj );
      final EntityTransaction etx = tx.join( );
      return etx;
    } else {
      return createTransaction( obj );
    }
  }
  
  public static <T> void flush( final T object ) {
    getTransaction( object ).txState.getEntityManager( ).flush( );
  }
  
  public static <T> List<T> query( final T example ) {
    return query( example, false );
  }
  
  @SuppressWarnings( { "unchecked", "cast" } )
  public static <T> List<T> query( final T example, final boolean readOnly ) {
    final Example qbe = Example.create( example ).enableLike( MatchMode.EXACT );
    final List<T> resultList = ( List<T> ) getTransaction( example ).getTxState( ).getSession( )
                                                                    .createCriteria( example.getClass( ) )
                                                                    .setReadOnly( readOnly )
                                                                    .setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY )
                                                                    .setCacheable( true )
                                                                    .add( qbe )
                                                                    .list( );
    return Lists.newArrayList( Sets.newHashSet( resultList ) );
  }

  /**
   * Query items matching the given example restricted by the given criterion.
   *
   * <P>The caller must have an active transaction for the entity.</P>
   *
   * @param example The example object
   * @param readOnly Use True if the results will not be modified
   * @param criterion Additional restrictions for the query
   * @param aliases Any aliases necessary for the given criterion
   * @param <T> The entity type
   * @return The result list
   */
  @SuppressWarnings( { "unchecked", "cast" } )
  public static <T> List<T> query( final T example, 
                                   final boolean readOnly, 
                                   final org.hibernate.criterion.Criterion criterion,
                                   final Map<String,String> aliases ) {
    final Example qbe = Example.create( example ).enableLike( MatchMode.EXACT );
    final Criteria criteria = getTransaction( example ).getTxState( ).getSession( )
        .createCriteria( example.getClass( ) )
        .setReadOnly( readOnly )
        .setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY )
        .setCacheable( true )
        .add( qbe )
        .add( criterion );
    for ( final Map.Entry<String,String> aliasEntry : aliases.entrySet() ) {
      criteria.createAlias( aliasEntry.getKey(), aliasEntry.getValue() ); // inner join by default
    }
    final List<T> resultList = ( List<T> ) criteria.list( );
    return Lists.newArrayList( Sets.newHashSet( resultList ) );
  }
  
  @SuppressWarnings( { "unchecked", "cast" } )
  public static <T> List<T> query( final T example, final boolean readOnly, final int maxResults ) {
    final Example qbe = Example.create( example ).enableLike( MatchMode.EXACT );
    final List<T> resultList = ( List<T> ) getTransaction( example ).getTxState( ).getSession( )
                                                                    .createCriteria( example.getClass( ) )
                                                                    .setReadOnly( readOnly )
                                                                    .setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY )
                                                                    .setCacheable( true )
                                                                    .add( qbe )
                                                                    .setMaxResults( maxResults )
                                                                    .setFetchSize( maxResults )
                                                                    .list( );
    return Lists.newArrayList( Sets.newHashSet( resultList ) );
  }
  
  public static <T> T uniqueResult( final T example ) throws TransactionException, NoSuchElementException {
    try {
      final Object pk = resolvePrimaryKey( example );
      final String natId = resolveNaturalId( example );
      if ( pk != null ) {
        return maybePrimaryKey( example );
      } else if ( natId != null ) {
        return maybeNaturalId( example );
      } else {
        return maybeDefinitelyExample( example );
      }
    } catch ( final NoSuchElementException ex ) {
      throw ex;
    } catch ( final RuntimeException ex ) {
      Logs.extreme( ).trace( ex, ex );
      final Exception newEx = PersistenceExceptions.throwFiltered( ex );
      throw new TransactionInternalException( newEx.getMessage( ), newEx );
    }
  }

  public static void evict( final Object obj  ) {
    getTransaction( obj ).getTxState( ).getSession( ).evict( obj );
  }

  private static <T> String resolveNaturalId( final T example ) {
    if ( ( example instanceof HasNaturalId ) && ( ( ( HasNaturalId ) example ).getNaturalId( ) != null ) ) {
      return ( ( HasNaturalId ) example ).getNaturalId( );
    } else {
      return null;
    }
  }

  private static <T> T maybeDefinitelyExample( final T example ) throws HibernateException, NoSuchElementException {
    @SuppressWarnings( "unchecked" )
    final T ret = ( T ) Entities.getTransaction( example )
                                .getTxState( )
                                .getSession( )
                                .createCriteria( example.getClass( ) )
                                .add( Example.create( example ).enableLike( MatchMode.EXACT ) )
                                .setCacheable( true )
                                .setMaxResults( 1 )
                                .setFetchSize( 1 )
                                .setFirstResult( 0 )
                                .uniqueResult( );
    if ( ret == null ) {
      throw new NoSuchElementException( "example: " + LogUtil.dumpObject( example ) );
    }
    return ret;
  }
  
  private static <T> T maybeNaturalId( final T example ) throws HibernateException, NoSuchElementException {
    final String natId = ( ( HasNaturalId ) example ).getNaturalId( );
    @SuppressWarnings( "unchecked" )
    final T ret = ( T ) Entities.getTransaction( example ).getTxState( )
                                .getSession( )
                                .createCriteria( example.getClass( ) )
                                .add( Restrictions.naturalId( ).set( "naturalId", natId ) )
                                .setCacheable( true )
                                .setMaxResults( 1 )
                                .setFetchSize( 1 )
                                .setFirstResult( 0 )
                                .uniqueResult( );
    if ( ret == null ) {
      throw new NoSuchElementException( "@NaturalId: " + natId );
    }
    return ret;
  }
  
  private static <T> T maybePrimaryKey( final T example ) throws NoSuchElementException {
    final Object id = resolvePrimaryKey( example );
    if ( id == null ) {
      return null;
    } else {
      final T res = ( T ) Entities.getTransaction( example ).getTxState( ).getEntityManager( ).find( example.getClass( ), id );
      if ( res == null ) {
        throw new NoSuchElementException( "@Id: " + id );
      } else {
        return res;
      }
    }
  }
  
  private static <T> Object resolvePrimaryKey( final T example ) {
    return Entities.getTransaction( example ).getTxState( ).getEntityManager( ).getEntityManagerFactory( ).getPersistenceUnitUtil( ).getIdentifier( example );
  }
  
  public static Criteria createCriteria( final Class class1 ) {
    return getTransaction( class1 ).getTxState( ).getSession( ).createCriteria( class1 );
  }
  
  public static Criteria createCriteriaUnique( final Class class1 ) {
    return getTransaction( class1 ).getTxState( ).getSession( ).createCriteria( class1 ).setCacheable( true ).setFetchSize( 1 ).setMaxResults( 1 ).setFirstResult( 0 );
  }
  
  /**
   * Invokes underlying persist implementation per jsr-220
   * 
   * @throws ConstraintViolationException
   * @see http://opensource.atlassian.com/projects/hibernate/browse/HHH-1273
   * @param newObject
   * @return
   */
  public static <T> T persist( final T newObject ) throws ConstraintViolationException {
    try {
      getTransaction( newObject ).getTxState( ).getEntityManager( ).persist( newObject );
      return newObject;
    } catch ( final RuntimeException ex ) {
      PersistenceExceptions.throwFiltered( ex );
      throw ex;
    }
  }
  
  /**
   * <table>
   * <tbody>
   * <tr valign="top">
   * <th>Scenario</th>
   * <th><tt>EntityManager.persist</tt></th>
   * <th><tt>EntityManager.merge</tt></th>
   * <th><tt>SessionManager.saveOrUpdate</tt></th>
   * </tr>
   * <tr valign="top">
   * <th>Object passed was never persisted</th>
   * 
   * <td>1. Object added to persistence context as new entity<br>
   * 2. New entity inserted into database at flush/commit</td>
   * <td>1. State copied to new entity.<br>
   * 2. New entity added to persistence context<br>
   * 3. New entity inserted into database at flush/commit<br>
   * 4. New entity returned</td>
   * <td>1. Object added to persistence context as new entity<br>
   * 2. New entity inserted into database at flush/commit</td>
   * </tr>
   * <tr valign="top">
   * <th>Object was previously persisted, but not loaded in this persistence context</th>
   * <td>1. <tt>EntityExistsException</tt> thrown (or a <tt>PersistenceException</tt> at
   * flush/commit)</td>
   * 
   * <td>2. Existing entity loaded.<br>
   * 2. State copied from object to loaded entity<br>
   * 3. Loaded entity updated in database at flush/commit<br>
   * 4. Loaded entity returned</td>
   * <td>1. Object added to persistence context<br>
   * 2. Loaded entity updated in database at flush/commit</td>
   * </tr>
   * <tr valign="top">
   * <th>Object was previously persisted and already loaded in this persistence context</th>
   * <td>1. <tt>EntityExistsException</tt> thrown (or a <tt>PersistenceException</tt> at flush or
   * commit time)</td>
   * 
   * <td>1. State from object copied to loaded entity<br>
   * 2. Loaded entity updated in database at flush/commit<br>
   * 3. Loaded entity returned</td>
   * <td>1. <tt>NonUniqueObjectException</tt> thrown</td>
   * </tr>
   * </tbody>
   * </table>
   * 
   * @throws ConstraintViolationException
   * @param newObject
   * @throws NoSuchElementException
   * @throws TransactionException
   */
  public static <T> T merge( final T newObject ) throws ConstraintViolationException {
    if ( !isPersistent( newObject ) ) {
      try {
        return uniqueResult( newObject );
      } catch ( final Exception ex ) {
        return persist( newObject );
      }
    } else {
      try {
        final T persistedObject = getTransaction( newObject ).getTxState( ).getEntityManager( ).merge( newObject );
        return persistedObject == newObject
                                           ? newObject
                                           : persistedObject;
      } catch ( final RuntimeException ex ) {
        
        PersistenceExceptions.throwFiltered( ex );
        throw ex;
      }
    }
  }

  /**
   * Invokes underlying merge implementation per jsr-220
   *
   * @param object The object to merge
   * @param <T> The return type
   * @return The persistent instance for the object
   */
  public static <T> T mergeDirect( final T object ) {
    try {
      return getTransaction( object ).getTxState( ).getEntityManager( ).merge( object );
    } catch ( final RuntimeException ex ) {
      PersistenceExceptions.throwFiltered( ex );
      throw ex;
    }
  }

  public <T> T lookupAndClose( final T example ) throws NoSuchElementException {
    EntityTransaction db;
    T ret = null;
    if ( !hasTransaction( example ) ) {
      db = get( example );
    } else {
      db = getTransaction( example ).join( );
    }
    try {
      ret = uniqueResult( example );
      db.commit( );
    } catch ( final TransactionException ex ) {
      db.rollback( );
      throw new NoSuchElementException( ex.getMessage( ) );
    }
    return ret;
  }
  
  public static <T> Function<T, T> merge( ) {
    return new Function<T, T>( ) {
      
      @Override
      public T apply( final T arg0 ) {
        return Entities.merge( arg0 );
      }
    };
  }
  
  public static <T> void refresh( final T newObject ) throws ConstraintViolationException {
    try {
      getTransaction( newObject ).getTxState( ).getEntityManager( ).refresh( newObject, ( LockModeType ) null );
    } catch ( final RuntimeException ex ) {
      PersistenceExceptions.throwFiltered( ex );
      throw ex;
    }
  }
  
  public static <T> void refresh( final T newObject, final LockModeType lockMode ) throws ConstraintViolationException {
    try {
      getTransaction( newObject ).getTxState( ).getEntityManager( ).refresh( newObject, lockMode );
    } catch ( final RuntimeException ex ) {
      PersistenceExceptions.throwFiltered( ex );
      throw ex;
    }
  }
  
  /**
   * {@inheritDoc Session}
   * 
   * @param obj
   * @return
   */
  public static boolean isPersistent( final Object obj ) {
    if ( !hasTransaction( ) ) {
      return false;
    } else {
      return getTransaction( obj ).getTxState( ).getSession( ).contains( obj );
    }
  }
  
  /**
   * @param <T>
   * @param deleteObject
   */
  public static <T> void delete( final T deleteObject ) {
    getTransaction( deleteObject ).getTxState( ).getEntityManager( ).remove( deleteObject );
  }

  /**
   * Delete all entities for the given class.
   *
   * @param <T> The entity type
   * @param deleteClass The entity class
   */
  public static <T> int deleteAll( final Class<T> deleteClass ) {
    return deleteAllMatching( deleteClass, null, Collections.<String,Object>emptyMap() );
  }

  /**
   * Delete all matching entities for the given class.
   *
   * @param <T> The entity type
   * @param deleteClass The entity class
   * @param condition The condition to match
   */
  public static <T> int deleteAllMatching( final Class<T> deleteClass,
                                           final String condition,
                                           final Map<String,?> parameters ) {
    try {
      final Query query = getTransaction( deleteClass ).getTxState().getEntityManager()
          .createQuery( "DELETE FROM " + deleteClass.getName() + " " + Strings.nullToEmpty(condition));
      for ( final Entry<String,?> entry : parameters.entrySet() ) {
        query.setParameter( entry.getKey(), entry.getValue() );
      }
      return query.executeUpdate();
    } catch ( Exception e ) {
      LOG.error( deleteClass, e );
      throw Exceptions.toUndeclared( e );
    }
  }

  /**
   * Count the matching entities for the given example.
   * 
   * @param example The example entity
   * @return The number of matching entities
   */
  public static long count( final Object example ) {
    final Example qbe = Example.create( example );
    final Number count = (Number) getTransaction( example ).getTxState( ).getSession( )
        .createCriteria( example.getClass( ) )
        .setReadOnly( true )
        .setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY )
        .setCacheable( false )
        .add( qbe )
        .setProjection( Projections.rowCount() )
        .uniqueResult( );
    return count.longValue(); 
  }

  private static class TxStateThreadLocal extends ThreadLocal<ConcurrentMap<String, CascadingTx>> {
    TxStateThreadLocal( ) {}
    
    @Override
    protected ConcurrentMap<String, CascadingTx> initialValue( ) {
      return Maps.newConcurrentMap( );
    }
    
    private boolean clearStale( final PersistenceContext persistenceContext ) {
      final CascadingTx tx = this.get( ).get( persistenceContext.name( ) );
      if ( !tx.isActive( ) ) {
        try {
          tx.getTxState( ).doCleanup( );
        } catch ( final Exception ex ) {
          LOG.error( ex, ex );
        } finally {
          this.get( ).remove( persistenceContext.name( ) );
        }
        return true;
      } else {
        return false;
      }
    }
    
  }
  
  /**
   * Private for a reason.
   */
  private static class CascadingTx implements EntityTransaction {
    private final TxRecord record;
    private TxState        txState;
    
    /**
     * Private for a reason.
     * 
     * @see {@link CascadingTx#get(Class)}
     * @param persistenceContext
     * @throws RecoverablePersistenceException
     */
    @SuppressWarnings( "unchecked" )
    CascadingTx( final String ctx ) throws RecoverablePersistenceException {
      final StackTraceElement ste = Threads.currentStackFrame( 4 );
      final String uuid = UUID.randomUUID( ).toString( );
      this.record = new TxRecord( ctx, uuid, ste );
      try {
        this.txState = new TxState( ctx );
      } catch ( final RuntimeException ex ) {
        Logs.extreme( ).error( ex, ex );
        this.rollback( );
        throw PersistenceExceptions.throwFiltered( ex );
      }
    }
    
    /**
     * @delegate Do not change semantics here.
     * @see javax.persistence.EntityTransaction#getRollbackOnly()
     * @return
     */
    @Override
    public boolean getRollbackOnly( ) throws RecoverablePersistenceException {
      return this.txState == null
                                 ? false
                                 : this.txState.getRollbackOnly( );
    }
    
    /**
     * @delegate Do not change semantics here.
     * @see javax.persistence.EntityTransaction#setRollbackOnly()
     */
    @Override
    public void setRollbackOnly( ) throws RecoverablePersistenceException {
      if ( this.txState != null ) {
        this.txState.setRollbackOnly( );
      }
    }
    
    /**
     * @delegate Do not change semantics here.
     * @see javax.persistence.EntityTransaction#isActive()
     * @return
     */
    @Override
    public boolean isActive( ) throws RecoverablePersistenceException {
      return this.txState == null
                                 ? false
                                 : this.txState.isActive( );
    }
    
    /**
     * @delegate Do not change semantics here.
     * @see javax.persistence.EntityTransaction#begin()
     */
    @Override
    public void begin( ) throws RecoverablePersistenceException {
      try {
        this.txState.begin( );
      } catch ( final RecoverablePersistenceException ex ) {
        PersistenceExceptions.throwFiltered( ex );
        removeTransaction( this );
      } catch ( final RuntimeException ex ) {
        PersistenceExceptions.throwFiltered( ex );
        removeTransaction( this );
        throw ex;
      }
    }
    
    /**
     * @delegate Do not change semantics here.
     * @see javax.persistence.EntityTransaction#rollback()
     */
    @Override
    public void rollback( ) throws RecoverablePersistenceException {
      removeTransaction( this );
      if ( ( this.txState != null ) && this.txState.isActive( ) ) {
        try {
          this.txState.rollback( );
          this.txState = null;
        } catch ( final RuntimeException ex ) {
          Logs.extreme( ).error( ex );
//          throw PersistenceExceptions.throwFiltered( ex );
        }
      } else {
        Logs.extreme( ).debug( "Duplicate call to rollback( )" );
      }
    }
    
    /**
     * @delegate Do not change semantics here.
     * @see javax.persistence.EntityTransaction#commit()
     */
    @Override
    public void commit( ) throws RecoverablePersistenceException {
      removeTransaction( this );
      if ( ( this.txState != null ) && this.txState.isActive( ) ) {
        try {
          this.txState.commit( );
        } catch ( final RuntimeException ex ) {
          throw PersistenceExceptions.throwFiltered( ex );
        }
      } else {
        Logs.extreme( ).error( "Duplicate call to commit( ): " + Threads.currentStackString( ) );
      }
    }
    
    TxState getTxState( ) {
      return this.txState;
    }
    
    public EntityTransaction join( ) {
      return new EntityTransaction( ) {
        private final String uuid = UUID.randomUUID( ).toString( );
        
        @Override
        public void setRollbackOnly( ) {}
        
        @Override
        public void rollback( ) {
//          Logs.extreme( ).trace( "Child call to rollback() is ignored: " + Threads.currentStackRange( 2, 8 ) );
        }
        
        @Override
        public boolean isActive( ) {
          return CascadingTx.this.isActive( );
        }
        
        @Override
        public boolean getRollbackOnly( ) {
          return CascadingTx.this.getRollbackOnly( );
        }
        
        @Override
        public void commit( ) {
          try {
//            CascadingTx.this.getTxState( ).getEntityManager( ).flush( );
//          Logs.extreme( ).trace( "Child call to commit() is ignored: " + Threads.currentStackRange( 2, 8 ) );
          } catch ( final HibernateException ex ) {
            LOG.error( ex, ex );
          }
        }
        
        @Override
        public void begin( ) {}
      };
    }
    
    TxRecord getRecord( ) {
      return this.record;
    }
    
    private class TxState implements EntityTransaction {
      private EntityManager                em;
      private EntityTransaction            transaction;
      private final WeakReference<Session> sessionRef;
      
      public TxState( final String ctx ) {
        try {
          final EntityManagerFactory anemf = ( EntityManagerFactoryImpl ) PersistenceContexts.getEntityManagerFactory( ctx );
          checkParam( anemf, notNullValue() );
          this.em = anemf.createEntityManager( );
          checkParam( this.em, notNullValue() );
          this.transaction = this.em.getTransaction( );
          checkParam( this.transaction, notNullValue() );
          this.sessionRef = new WeakReference<Session>( ( Session ) this.em.getDelegate( ) );
        } catch ( final RuntimeException ex ) {
          this.doCleanup( );
          throw ex;
        }
      }
      
      private boolean isOpen( ) {
        final boolean hasEm = ( this.em != null ) && this.em.isOpen( );
        final boolean hasSession = ( this.sessionRef.get( ) != null ) && this.sessionRef.get( ).isOpen( );
        final boolean hasTx = ( this.transaction != null ) && this.transaction.isActive( );
        if ( hasEm && hasSession && hasTx ) {
          return true;
        } else {
          this.doCleanup( );
          return false;
        }
      }
      
      private void doRollback( ) {
        try {
          if ( ( this.transaction != null ) && this.transaction.isActive( ) ) {
            this.transaction.rollback( );
          }
        } catch ( final Throwable e ) {
          PersistenceExceptions.throwFiltered( e );
        } finally {
          this.doCleanup( );
        }
      }
      
      private void doCleanup( ) {
        if ( ( this.transaction != null ) && this.transaction.isActive( ) ) {
          this.transaction.rollback( );
        }
        this.transaction = null;
        if ( ( this.sessionRef != null ) && ( this.sessionRef.get( ) != null ) ) {
          this.sessionRef.clear( );
        }
        if ( ( this.em != null ) && this.em.isOpen( ) ) {
          this.em.close( );
        }
        this.em = null;
      }
      
      EntityManager getEntityManager( ) {
        return this.em;
      }
      
      Session getSession( ) {
        return this.sessionRef.get( );
      }
      
      /**
       * @delegate Do not change semantics here.
       * @see javax.persistence.EntityTransaction#begin()
       */
      @Override
      public void begin( ) {
        try {
          this.transaction.begin( );
        } catch ( final RuntimeException ex ) {
          LOG.warn( ex );
          Logs.extreme( ).warn( ex, ex );
          throw ex;
        }
      }
      
      /**
       * @delegate Do not change semantics here.
       * @see javax.persistence.EntityTransaction#commit()
       */
      @Override
      public void commit( ) {
        try {
          this.transaction.commit( );
        } catch ( final RuntimeException ex ) {
          LOG.trace( ex, ex );
          Logs.extreme( ).warn( ex, ex );
          throw ex;
        }
      }
      
      /**
       * @delegate Do not change semantics here.
       * @see javax.persistence.EntityTransaction#getRollbackOnly()
       * @return
       */
      @Override
      public boolean getRollbackOnly( ) {
        return this.transaction.getRollbackOnly( );
      }
      
      /**
       * @delegate Do not change semantics here.
       * @see javax.persistence.EntityTransaction#isActive()
       * @return
       */
      @Override
      public boolean isActive( ) {
        return this.transaction.isActive( );
      }
      
      /**
       * @delegate Do not change semantics here.
       * @see javax.persistence.EntityTransaction#rollback()
       */
      @Override
      public void rollback( ) {
        try {
          this.transaction.rollback( );
        } catch ( final RuntimeException ex ) {
          LOG.error( ex, ex );
          throw ex;
        }
      }
      
      /**
       * @delegate Do not change semantics here.
       * @see javax.persistence.EntityTransaction#setRollbackOnly()
       */
      @Override
      public void setRollbackOnly( ) {
        this.transaction.setRollbackOnly( );
      }
      
    }
  }
  
  public static class TxRecord {
    private final String            persistenceContext;
    private final String            uuid;
    private final Long              startTime;
    private final StackTraceElement ste;
    private final String            stack;
    
    TxRecord( final String persistenceContext, final String uuid, final StackTraceElement ste ) {
      this.persistenceContext = persistenceContext;
      this.uuid = uuid;
      this.ste = ste;
      this.stack = Threads.currentStackString( );
      this.startTime = System.currentTimeMillis( );
    }
    
    public Long getStartTime( ) {
      return this.startTime;
    }
    
    public String getPersistenceContext( ) {
      return this.persistenceContext;
    }
    
    public String getUuid( ) {
      return this.uuid;
    }
    
    public StackTraceElement getSte( ) {
      return this.ste;
    }
    
    String getStack( ) {
      return this.stack;
    }
    
  }
  
  private static class TransactionalFunction<E, D, R> implements Function<D, R> {
    private Class<E>       entityType;
    private Function<D, R> function;
    private Integer        retries = CONCURRENT_UPDATE_RETRIES;
    
    TransactionalFunction( Class<E> entityType, Function<D, R> function, Integer retries ) {
      this.entityType = entityType;
      this.function = function;
      this.retries = retries;
    }
    
    @Override
    public R apply( final D input ) {
      RuntimeException rootCause = null;
      for ( int i = 0; i < retries; i++ ) {
        EntityTransaction db = Entities.get( this.entityType );
        try {
          R ret = this.function.apply( input );
          db.commit( );
          return ret;
        } catch ( RuntimeException ex ) {
          db.rollback( );
          if ( Exceptions.isCausedBy( ex, OptimisticLockException.class ) ) {
            rootCause = Exceptions.findCause( ex, OptimisticLockException.class );
          } else if ( Exceptions.isCausedBy( ex, LockAcquisitionException.class ) ) {
            rootCause = Exceptions.findCause( ex, LockAcquisitionException.class );
          } else {
            rootCause = ex;
            Logs.extreme( ).error( ex, ex );
            throw ex;
          }
          try {
            TimeUnit.MILLISECONDS.sleep( 20 );
          } catch ( InterruptedException ex1 ) {
            Exceptions.maybeInterrupted( ex1 );
          }
          continue;
        }
      }
      throw ( rootCause != null
                               ? rootCause
                               : new NullPointerException( "BUG: Transaction retry failed but root cause exception is unknown!" ) );
    }
    
  }
  
  public static <E, T> Predicate<T> asTransaction( final Predicate<T> predicate ) {
    final List<Class> generics = Classes.genericsToClasses( predicate );
    for ( final Class<?> type : generics ) {
      if ( PersistenceContexts.isPersistentClass( type ) ) {
        return asTransaction( type, predicate );
      }
    }
    throw new IllegalArgumentException( "Failed to find generics for provided predicate, cannot make into transaction: " + Threads.currentStackString( ) );
  }

  public static <E, T> Predicate<T> asTransaction( final Class<E> type, final Predicate<T> predicate ) {
    return asTransaction( type, predicate, CONCURRENT_UPDATE_RETRIES );
  }
  
  public static <E, T> Predicate<T> asTransaction( final Class<E> type, final Predicate<T> predicate, final Integer retries ) {
    final Function<T, Boolean> funcionalized = Functions.forPredicate( predicate );
    final Function<T, Boolean> transactionalized = Entities.asTransaction( type, funcionalized, retries );
    return new Predicate<T>( ) {
      
      @Override
      public boolean apply( T input ) {
        return transactionalized.apply( input );
      }
      
    };
  }
  
  public static <T, R> Function<T, R> asTransaction( final Function<T, R> function ) {
    if ( function instanceof TransactionalFunction ) {
      return function;
    } else {
      final List<Class> generics = Classes.genericsToClasses( function );
      for ( final Class<?> type : generics ) {
        if ( PersistenceContexts.isPersistentClass( type ) ) {
          return asTransaction( type, function );
        }
      }
      throw new IllegalArgumentException( "Failed to find generics for provided function, cannot make into transaction: " + Threads.currentStackString( ) );
    }
  }
  
  public static <E, T, R> Function<T, R> asTransaction( final Class<E> type, final Function<T, R> function ) {
    if ( function instanceof TransactionalFunction ) {
      return function;
    } else {
      return asTransaction( type, function, CONCURRENT_UPDATE_RETRIES );
    }
  }
  
  public static <E, T, R> Function<T, R> asTransaction( final Class<E> type, final Function<T, R> function, final int retries ) {
    if ( function instanceof TransactionalFunction ) {
      return function;
    } else {
      return new TransactionalFunction<E, T, R>( type, function, retries );
    }
  }
  
  public static void commit( EntityTransaction tx ) {
    if ( tx.getRollbackOnly( ) ) {
      tx.rollback( );
    } else if ( Databases.isVolatile( ) ) {
      tx.rollback( );
    } else {
      tx.commit( );
    }
  }
  
}
