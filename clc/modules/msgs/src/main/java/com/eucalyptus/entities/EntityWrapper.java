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
import static org.hamcrest.Matchers.*;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.NonUniqueResultException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionException;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.ejb.EntityManagerFactoryImpl;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.HasNaturalId;
import com.eucalyptus.util.LogUtil;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class EntityWrapper<TYPE> {
  private static Logger    LOG = Logger.getLogger( EntityWrapper.class );
  private TransactionState tx;
  private final String     txStart;
  
  enum TxEvent {
    CREATE,
    COMMIT,
    ROLLBACK,
    UNIQUE,
    QUERY;
    public String getMessage( ) {
      if ( Logs.isExtrrreeeme( ) ) {
        return Threads.currentStackString( );
      } else {
        return "n.a";
      }
    }
  }
  
  enum TxWatchdog implements EventListener {
    INSTANCE;
    @Override
    public void fireEvent( final Event event ) {
      if ( event instanceof ClockTick ) {
        //TODO:GRZE:tx monitoring here.
      }
    }
  }
  
  enum TxStep {
    BEGIN,
    END,
    FAIL;
    public String event( final TxEvent e ) {
      return e.name( ) + ":" + this.name( );
    }
  }
  
/**
   * @see {@link Entities#get(Class)
   * @see {@link CascadingTx}
   */
  @Deprecated
  public static <T> EntityWrapper<T> get( final Class<T> type ) {
    return new EntityWrapper( Entities.lookatPersistenceContext( type ) );
  }
  
/**
   * @see {@link Entities#get(Object)
   * @see {@link CascadingTx}
   */
  @SuppressWarnings( "unchecked" )
  @Deprecated
  public static <T> EntityWrapper<T> get( final T obj ) {
    return new EntityWrapper( Entities.lookatPersistenceContext( obj ) );
  }
  
  /**
   * Private for a reason.
   * 
   * @see {@link EntityWrapper#get(Class)}
   * @param persistenceContext
   */
  @SuppressWarnings( "unchecked" )
  private EntityWrapper( final String persistenceContext ) {
    this.tx = new TransactionState( persistenceContext );
    this.txStart = Threads.currentStackString( );
  }
  
  @SuppressWarnings( { "unchecked", "cast" } )
  public <T> List<T> query( final T example ) {
    final Example qbe = Example.create( example ).enableLike( MatchMode.EXACT );
    final List<T> resultList = this.getSession( )
                                   .createCriteria( example.getClass( ) )
                                   .setLockMode( LockMode.NONE )
                                   .setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY )
                                   .setCacheable( true )
                                   .add( qbe )
                                   .list( );
    return Lists.newArrayList( Sets.newHashSet( resultList ) );
  }

  // Fix for EUCA-3453
  /**
   * Returns a list of results from the database that exactly match <code>example</code>. This method does not use <i><code>enableLike</code></i> match while the 
   * {@link #query(Object)} does. <i><code>enableLike</code></i> criteria trips hibernate when special characters are involved. So it has been replaced by "=" (equals to)
   * 
   * @param example
   * @return
   */
  @SuppressWarnings( { "unchecked", "cast" } )
  public <T> List<T> queryEscape( final T example ) {
    final Example qbe = Example.create( example );
    final List<T> resultList = this.getSession( )
                                   .createCriteria( example.getClass( ) )
                                   .setLockMode( LockMode.NONE )
                                   .setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY )
                                   .setCacheable( true )
                                   .add( qbe )
                                   .list( );
    return Lists.newArrayList( Sets.newHashSet( resultList ) );
  }
  
  public <T> T lookupAndClose( final T example ) throws NoSuchElementException {
    T ret = null;
    try {
      ret = this.getUnique( example );
      this.commit( );
    } catch ( final EucalyptusCloudException ex ) {
      this.rollback( );
      throw new NoSuchElementException( ex.getMessage( ) );
    }
    return ret;
  }
  
  @SuppressWarnings( "unchecked" )
  public <T> T uniqueResult( final T example ) throws TransactionException {
    try {
      return this.recast( ( Class<T> ) example.getClass( ) ).getUnique( example );
    } catch ( final RuntimeException ex ) {
      throw new TransactionInternalException( ex.getMessage( ), ex );
    } catch ( final EucalyptusCloudException ex ) {
      throw new TransactionExecutionException( ex.getMessage( ), ex );
    }
  }
  
  
  //Fix for EUCA-3453
  /**
   * Returns the unique result from the database that exactly matches <code>example</code>. This method is same as {@link #uniqueResult(Object)}
   * but calls {@link #getUniqueEscape(Object)} instead of {@link #getUnique(Object)}
   * 
   * @param example
   * @return
   * @throws TransactionException
   */
  @SuppressWarnings( "unchecked" )
  public <T> T uniqueResultEscape( final T example ) throws TransactionException {
    try {
      return this.recast( ( Class<T> ) example.getClass( ) ).getUniqueEscape( example );
    } catch ( final RuntimeException ex ) {
      throw new TransactionInternalException( ex.getMessage( ), ex );
    } catch ( final EucalyptusCloudException ex ) {
      throw new TransactionExecutionException( ex.getMessage( ), ex );
    }
  }
  
  @SuppressWarnings( "unchecked" )
  public <T> T getUnique( final T example ) throws EucalyptusCloudException {
    try {
      Object id = null;
      try {
        id = this.getEntityManager( ).getEntityManagerFactory( ).getPersistenceUnitUtil( ).getIdentifier( example );
      } catch ( final Exception ex ) {}
      if ( id != null ) {
        final T res = ( T ) this.getEntityManager( ).find( example.getClass( ), id );
        if ( res == null ) {
          throw new NoSuchElementException( "@Id: " + id );
        } else {
          return res;
        }
      } else if ( ( example instanceof HasNaturalId ) && ( ( ( HasNaturalId ) example ).getNaturalId( ) != null ) ) {
        final String natId = ( ( HasNaturalId ) example ).getNaturalId( );
        final T ret = ( T ) this.createCriteria( example.getClass( ) )
                                .setLockMode( LockMode.NONE )
                                .setCacheable( true )
                                .setMaxResults( 1 )
                                .setFetchSize( 1 )
                                .setFirstResult( 0 )
                                .add( Restrictions.naturalId( ).set( "naturalId", natId ) )
                                .uniqueResult( );
        if ( ret == null ) {
          throw new NoSuchElementException( "@NaturalId: " + natId );
        }
        return ret;
      } else {
        final T ret = ( T ) this.createCriteria( example.getClass( ) )
                                .setLockMode( LockMode.NONE )
                                .setCacheable( true )
                                .setMaxResults( 1 )
                                .setFetchSize( 1 )
                                .setFirstResult( 0 )
                                .add( Example.create( example ).enableLike( MatchMode.EXACT ) )
                                .uniqueResult( );
        if ( ret == null ) {
          throw new NoSuchElementException( "example: " + LogUtil.dumpObject( example ) );
        }
        return ret;
      }
    } catch ( final NonUniqueResultException ex ) {
      throw new EucalyptusCloudException( "Get unique failed for " + example.getClass( ).getSimpleName( ) + " because " + ex.getMessage( ), ex );
    } catch ( final NoSuchElementException ex ) {
      throw new EucalyptusCloudException( "Get unique failed for " + example.getClass( ).getSimpleName( ) + " using " + ex.getMessage( ), ex );
    } catch ( final Exception ex ) {
      final Exception newEx = PersistenceExceptions.throwFiltered( ex );
      throw new EucalyptusCloudException( "Get unique failed for " + example.getClass( ).getSimpleName( ) + " because " + newEx.getMessage( ), newEx );
    }
  }
  
  //Fix for EUCA-3453
  /**
   * Returns the unique result from the database that exactly matches <code>example</code>. The differences between this method and {@link #getUnique(Object)} are:
   * <ol><li>{@link #getUnique(Object)} uses <i><code>enableLike</code></i> match and this method does not. <i><code>enableLike</code></i> criteria trips hibernate when 
   * special characters are involved. So it has been replaced by exact "=" (equals to)</li>  
   * <li>Unique result logic is correctly implemented in this method. If the query returns more than one result, this method correctly throws an exception 
   * wrapping <code>NonUniqueResultException</code>. {@link #getUnique(Object)} does not throw an exception in this case and returns a result as long as it finds
   * one or more matching results (because of the following properties set on the query: <code>setMaxResults(1)</code>, <code>setFetchSize(1)</code> and 
   * <code>setFirstResult(0)</code>)</li></ol>
   * 
   * @param example
   * @return
   * @throws EucalyptusCloudException
   */
  @SuppressWarnings( "unchecked" )
  public <T> T getUniqueEscape( final T example ) throws EucalyptusCloudException {
    try {
      Object id = null;
      try {
        id = this.getEntityManager( ).getEntityManagerFactory( ).getPersistenceUnitUtil( ).getIdentifier( example );
      } catch ( final Exception ex ) {}
      if ( id != null ) {
        final T res = ( T ) this.getEntityManager( ).find( example.getClass( ), id );
        if ( res == null ) {
          throw new NoSuchElementException( "@Id: " + id );
        } else {
          return res;
        }
      } else if ( ( example instanceof HasNaturalId ) && ( ( ( HasNaturalId ) example ).getNaturalId( ) != null ) ) {
        final String natId = ( ( HasNaturalId ) example ).getNaturalId( );
        final T ret = ( T ) this.createCriteria( example.getClass( ) )
                                .setLockMode( LockMode.NONE )
                                .setCacheable( true )
                                .add( Restrictions.naturalId( ).set( "naturalId", natId ) )
                                .uniqueResult( );
        if ( ret == null ) {
          throw new NoSuchElementException( "@NaturalId: " + natId );
        }
        return ret;
      } else {
        final T ret = ( T ) this.createCriteria( example.getClass( ) )
                                .setLockMode( LockMode.NONE )
                                .setCacheable( true )
                                .add( Example.create( example ) )
                                .uniqueResult( );
        if ( ret == null ) {
          throw new NoSuchElementException( "example: " + LogUtil.dumpObject( example ) );
        }
        return ret;
      }
    } catch ( final NonUniqueResultException ex ) {
      throw new EucalyptusCloudException( "Get unique failed for " + example.getClass( ).getSimpleName( ) + " because " + ex.getMessage( ), ex );
    } catch ( final NoSuchElementException ex ) {
      throw new EucalyptusCloudException( "Get unique failed for " + example.getClass( ).getSimpleName( ) + " using " + ex.getMessage( ), ex );
    } catch ( final Exception ex ) {
      final Exception newEx = PersistenceExceptions.throwFiltered( ex );
      throw new EucalyptusCloudException( "Get unique failed for " + example.getClass( ).getSimpleName( ) + " because " + newEx.getMessage( ), newEx );
    }
  }
  
  /**
   * Invokes underlying persist implementation per jsr-220
   * 
   * @see http://opensource.atlassian.com/projects/hibernate/browse/HHH-1273
   * @param newObject
   * @return
   */
  public <T> T persist( final T newObject ) {
    try {
      this.getEntityManager( ).persist( newObject );
      return newObject;
    } catch ( final RuntimeException ex ) {
      PersistenceExceptions.throwFiltered( ex );
      throw ex;
    }
  }
  
  /**
   * Calls {@link #persist(Object)}; here for legacy, and is deprecated in favor of persist
   * 
   * @see http://opensource.atlassian.com/projects/hibernate/browse/HHH-1273
   * @param newObject
   */
  @Deprecated
  public <T> T add( final T newObject ) {
    return this.persist( newObject );
  }
  
  /**
   * TODO: not use this please.
   * 
   * @param string
   * @return
   */
  public Query createQuery( final String string ) {
    return this.getSession( ).createQuery( string );
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
   * @param newObject
   */
  public <T> T merge( final T newObject ) {
    try {
      return this.getEntityManager( ).merge( newObject );
    } catch ( final RuntimeException ex ) {
      PersistenceExceptions.throwFiltered( ex );
      throw ex;
    }
  }
  
  /**
   * @see EntityWrapper#merge(Object)
   * @param newObject
   * @throws PersistenceException
   */
  public <T> T mergeAndCommit( T newObject ) {
    try {
      newObject = this.getEntityManager( ).merge( newObject );
      this.commit( );
      return newObject;
    } catch ( final RuntimeException ex ) {
      try {
        PersistenceExceptions.throwFiltered( ex );
        throw ex;
      } finally {
        this.rollback( );
      }
    }
  }
  
  public void delete( final Object deleteObject ) {
    this.getEntityManager( ).remove( deleteObject );
  }
  
  public void rollback( ) {
    if ( this.tx != null ) {
      this.tx.rollback( );
      this.tx = null;
    }
  }
  
  public void commit( ) throws ConstraintViolationException {
    if ( this.tx != null ) {
      this.tx.commit( );
      this.tx = null;
    } else {
      throw new SessionException( "Attempt to commit session which is already closed:  " + this.txStart );
    }
  }
  
  public Criteria createCriteria( final Class class1 ) {
    return this.getSession( ).createCriteria( class1 );
  }
  
  /** package default on purpose **/
  EntityManager getEntityManager( ) {
    return this.tx.getEntityManager( );
  }
  
  /** :| should also be package default **/
  Session getSession( ) {
    return this.tx.getSession( );
  }
  
  @SuppressWarnings( "unchecked" )
  public <N> EntityWrapper<N> recast( final Class<N> c ) {
    return ( com.eucalyptus.entities.EntityWrapper<N> ) this;
  }
  
  @SuppressWarnings( "unchecked" )
  public <N> EntityWrapper<N> recast( ) {
    return ( com.eucalyptus.entities.EntityWrapper<N> ) this;
  }
  
  public static StackTraceElement getMyStackTraceElement( ) {
    int i = 0;
    for ( final StackTraceElement ste : Thread.currentThread( ).getStackTrace( ) ) {
      if ( ( i++ < 2 ) || ste.getClassName( ).matches( ".*EntityWrapper.*" )
           || ste.getClassName( ).matches( ".*TxHandle.*" )
           || ste.getMethodName( ).equals( "getEntityWrapper" ) ) {
        continue;
      } else {
        return ste;
      }
    }
    throw new RuntimeException( "BUG: Reached bottom of stack trace without finding any relevent frames." );
  }
  
  public Query createSQLQuery( final String sqlQuery ) {
    return this.getSession( ).createSQLQuery( sqlQuery );
  }
  
  public boolean isActive( ) {
    return this.tx != null && this.tx.isActive( );
  }
  
  public static <TYPE> EntityWrapper<TYPE> create( final PersistenceContext persistence ) {
    return new EntityWrapper<TYPE>( persistence.name( ) );
  }
  
  protected void cleanUp( ) {
    try {
      LOG.error( "Cleaning up stray entity wrapper: " + this.tx );
      this.tx.rollback( );
    } catch ( final Exception ex ) {
      LOG.error( ex, ex );
    }
  }
  
  static class TransactionState implements Comparable<TransactionState>, EntityTransaction {
    private static ConcurrentNavigableMap<String, TransactionState> outstanding = new ConcurrentSkipListMap<String, TransactionState>( );
    
    private EntityManager                                           em;
    private final WeakReference<Session>                            session;
    private EntityTransaction                                       transaction;
    private final String                                            owner;
    private final Long                                              startTime;
    private final String                                            txUuid;
    private final StopWatch                                         stopWatch;
    private volatile long                                           splitTime   = 0l;
    
    TransactionState( final String ctx ) {
      this.startTime = System.currentTimeMillis( );
      this.txUuid = String.format( "%s:%s", ctx, UUID.randomUUID( ).toString( ) );
      this.stopWatch = new StopWatch( );
      this.stopWatch.start( );
      this.owner = Logs.isExtrrreeeme( )
        ? Threads.currentStackString( )
        : "n/a";
      try {
        this.eventLog( TxStep.BEGIN, TxEvent.CREATE );
        final EntityManagerFactory anemf = ( EntityManagerFactoryImpl ) PersistenceContexts.getEntityManagerFactory( ctx );
        checkParam( anemf, notNullValue() );
        this.em = anemf.createEntityManager( );
        checkParam( this.em, notNullValue() );
        this.transaction = this.em.getTransaction( );
        this.transaction.begin( );
        this.session = new WeakReference<Session>( ( Session ) this.em.getDelegate( ) );
        this.eventLog( TxStep.END, TxEvent.CREATE );
      } catch ( final Throwable ex ) {
        Logs.exhaust( ).error( ex, ex );
        this.eventLog( TxStep.FAIL, TxEvent.CREATE );
        this.rollback( );
        throw new RuntimeException( PersistenceExceptions.throwFiltered( ex ) );
      } finally {
        outstanding.put( this.txUuid, this );
      }
    }
    
    private boolean isExpired( ) {
      final long splitTime = this.split( );
      return ( splitTime - 30000 ) > this.startTime;
    }
    
    private long split( ) {
      this.stopWatch.split( );
      this.splitTime = this.stopWatch.getSplitTime( );
      this.stopWatch.unsplit( );
      return this.splitTime;
    }
    
    private final void eventLog( final TxStep txState, final TxEvent txAction ) {
      if ( Logs.isExtrrreeeme( ) ) {
        final long oldSplit = this.splitTime;
        this.stopWatch.split( );
        this.splitTime = this.stopWatch.getSplitTime( );
        this.stopWatch.unsplit( );
        final Long split = this.splitTime - oldSplit;
        Logs.exhaust( ).debug( Joiner.on( ":" ).join( EventType.PERSISTENCE, txState.event( txAction ), Long.toString( split ),
                                                      this.getTxUuid( ) ) );
      }
    }
    
    @Override
    public void rollback( ) {
      this.eventLog( TxStep.BEGIN, TxEvent.ROLLBACK );
      try {
        if ( ( this.transaction != null ) && this.transaction.isActive( ) ) {
          this.transaction.rollback( );
        }
        this.eventLog( TxStep.END, TxEvent.ROLLBACK );
      } catch ( final Throwable e ) {
        this.eventLog( TxStep.FAIL, TxEvent.ROLLBACK );
        PersistenceExceptions.throwFiltered( e );
      } finally {
        this.cleanup( );
      }
    }
    
    private void cleanup( ) {
      try {
        if ( ( this.transaction != null ) && this.transaction.isActive( ) ) {
          this.transaction.rollback( );
        }
        this.transaction = null;
        if ( ( this.session != null ) && ( this.session.get( ) != null ) ) {
          this.session.clear( );
        }
        if ( ( this.em != null ) && this.em.isOpen( ) ) {
          this.em.close( );
        }
        this.em = null;
      } finally {
        outstanding.remove( this.txUuid );
      }
    }
    
    @Override
    public void commit( ) {
      this.eventLog( TxStep.BEGIN, TxEvent.COMMIT );
      try {
        this.transaction.commit( );
        this.eventLog( TxStep.END, TxEvent.COMMIT );
      } catch ( final RuntimeException e ) {
        this.rollback( );
        this.eventLog( TxStep.FAIL, TxEvent.COMMIT );
        PersistenceExceptions.throwFiltered( e );
        throw e;
      } finally {
        this.cleanup( );
      }
    }
    
    public String getTxUuid( ) {
      this.split( );
      return this.txUuid;
    }
    
    @Override
    public boolean getRollbackOnly( ) {
      return this.transaction.getRollbackOnly( );
    }
    
    @Override
    public boolean isActive( ) {
      final boolean hasEm = ( this.em != null ) && this.em.isOpen( );
      final boolean hasSession = ( this.session.get( ) != null ) && this.session.get( ).isOpen( );
      final boolean hasTx = ( this.transaction != null ) && this.transaction.isActive( );
      if ( hasEm && hasSession && hasTx ) {
        return true;
      } else {
        this.cleanup( );
        return false;
      }
    }
    
    @Override
    public void setRollbackOnly( ) {
      this.transaction.setRollbackOnly( );
    }
    
    private Session getSession( ) {
      if ( this.isActive( ) ) {
        return this.session.get( );
      } else {
        throw new SessionException( "This session is no longer active: " + this.getTxUuid( ) );
      }
    }
    
    private EntityManager getEntityManager( ) {
      if ( this.isActive( ) ) {
        return this.em;
      } else {
        throw new SessionException( "This session is no longer active: " + this.getTxUuid( ) );
      }
    }
    
    @Override
    public void begin( ) {
      this.transaction.begin( );
    }
    
    @Override
    public int hashCode( ) {
      final int prime = 31;
      int result = 1;
      result = prime * result + ( ( this.owner == null )
        ? 0
        : this.owner.hashCode( ) );
      result = prime * result + ( ( this.startTime == null )
        ? 0
        : this.startTime.hashCode( ) );
      return result;
    }
    
    @Override
    public boolean equals( final Object obj ) {
      if ( this == obj ) return true;
      if ( obj == null ) return false;
      if ( this.getClass( ) != obj.getClass( ) ) return false;
      final TransactionState other = ( TransactionState ) obj;
      if ( this.owner == null ) {
        if ( other.owner != null ) return false;
      } else if ( !this.owner.equals( other.owner ) ) return false;
      if ( this.startTime == null ) {
        if ( other.startTime != null ) return false;
      } else if ( !this.startTime.equals( other.startTime ) ) return false;
      return true;
    }
    
    @Override
    public int compareTo( final TransactionState that ) {
      return this.txUuid.compareTo( that.txUuid );
    }
    
    @Override
    public String toString( ) {
      return String.format( "TxHandle:txUuid=%s:startTime=%s:splitTime=%s:owner=%s", this.txUuid, this.startTime, this.splitTime, Logs.isExtrrreeeme( )
        ? this.owner
        : "n/a" );
    }
    
    public static class TxWatchdog implements EventListener {
      
      @Override
      public void fireEvent( final Event event ) {
        if ( event instanceof ClockTick ) {
          for ( final TransactionState tx : TransactionState.outstanding.values( ) ) {
            if ( tx.isExpired( ) ) {
              tx.cleanup( );
              LOG.error( "Found expired TxHandle: " + tx );
              LOG.error( tx.owner );
            }
          }
        }
      }
    }
    
  }
  
}
