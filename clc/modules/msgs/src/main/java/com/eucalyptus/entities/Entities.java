/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import groovy.lang.Delegate;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicStampedReference;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.NonUniqueResultException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionException;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.ejb.EntityManagerFactoryImpl;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.HasNaturalId;
import com.eucalyptus.util.LogUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Entities {
  private static Logger                                       LOG     = Logger.getLogger( Entities.class );
  private static ThreadLocal<ConcurrentMap<String, NestedTx>> txState = new ThreadLocal<ConcurrentMap<String, NestedTx>>( ) {
                                                                        
                                                                        @Override
                                                                        protected ConcurrentMap<String, NestedTx> initialValue( ) {
                                                                          return Maps.newConcurrentMap( );
                                                                        }
                                                                        
                                                                      };
  
  private static NestedTx lookup( final Object obj ) {
    String ctx = lookatPersistenceContext( obj );
    return txState.get( ).get( ctx );
  }
  
  private static String lookatPersistenceContext( final Object obj ) throws RuntimeException {
    Class type = Classes.typeOf( obj );
    final Ats ats = Ats.inClassHierarchy( type );
    PersistenceContext persistenceContext = null;
    if ( !ats.has( PersistenceContext.class ) ) {
      throw new RuntimeException( "Attempting to create an entity wrapper instance for non persistent type: " + type.getCanonicalName( )
                                  + ".  Class hierarchy contains: \n" + ats.toString( ) );
    } else {
      persistenceContext = ats.get( PersistenceContext.class );
    }
    return persistenceContext.name( );
  }
  
  private static boolean hasTransaction( ) {
    return !txState.get( ).isEmpty( );
  }
  
  private static boolean hasTransaction( Object obj ) {
    String ctx = lookatPersistenceContext( obj );
    return txState.get( ).containsKey( ctx );
  }
  
  private static NestedTx getTransaction( Object obj ) {
    String ctx = lookatPersistenceContext( obj );
    return txState.get( ).get( ctx );
  }
  
  public static EntityTransaction get( final Object obj ) {
    if ( hasTransaction( obj ) ) {
      return new EntityTransaction( ) {
      };
    } else {
      new NestedTx( lookatPersistenceContext( obj ) );
    }
  }
  
  @SuppressWarnings( "unchecked" )
  public <T> List<T> query( final T example ) {
    final Example qbe = Example.create( example ).enableLike( MatchMode.EXACT );
    final List<T> resultList = ( List<T> ) this.getSession( )
                                               .createCriteria( example.getClass( ) )
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
      return this.getUnique( example );
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
      } else {
        final T ret = ( T ) this.createCriteria( example.getClass( ) )
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
  
  public <T> void delete( final T deleteObject ) {
    txState.get( ).get( ).getEntityManager( ).remove( deleteObject );
  }
  
  private static class NestedTxThreadLocal extends ThreadLocal<ConcurrentMap<String, AtomicStampedReference<NestedTx>>> {
    NestedTxThreadLocal( ) {}
    
    @Override
    protected ConcurrentMap<String, AtomicStampedReference<NestedTx>> initialValue( ) {
      return Maps.newConcurrentMap( );
    }
    
    @SuppressWarnings( "unchecked" )
    <T> NestedTx lookup( final PersistenceContext persistenceContext ) {
      return ( NestedTx ) this.add( persistenceContext );
    }
    
    private NestedTx add( final PersistenceContext persistenceContext ) {
      AtomicStampedReference<NestedTx> ref = null;
      if ( this.get( ).containsKey( persistenceContext.name( ) ) ) {
        if ( this.clearStale( persistenceContext ) ) {
          ref = this.addNestedTx( persistenceContext );
        } else {
          ref = this.get( ).get( persistenceContext.name( ) );
        }
      } else {
        ref = this.addNestedTx( persistenceContext );
      }
      return ref.getReference( );
    }
    
    private boolean clearStale( final PersistenceContext persistenceContext ) {
      final AtomicStampedReference<NestedTx> stampedWrapper = this.get( ).get( persistenceContext.name( ) );
      if ( !stampedWrapper.getReference( ).isActive( ) || ( stampedWrapper.getStamp( ) == 0 ) ) {
        try {
          stampedWrapper.getReference( ).cleanUp( );
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
    
    private AtomicStampedReference<NestedTx> addNestedTx( final PersistenceContext persistenceContext ) {
      final NestedTx entityWrapper = new NestedTx( persistenceContext, new Predicate<TxUnroll>( ) {
        
        /**
         * Handles nesting of transactions and unrolling nested failures.
         * 
         * During normal operations {@link #apply(TxUnroll)} should be called with
         * {@link TxUnroll#ROLLBACK} as
         * the argument. The returned {@code boolean} value indicates whether or not it is safe to
         * modify the transaction state -- i.e. have we unrolled the whole stack.
         * 
         * In the case of a failure, {@link #apply(TxUnroll)} should be called with
         * {@link TxUnroll#SAFELY}.
         * This will force a rollback and clean up nested transactions state.
         * 
         * @param forceClose
         * @return {@code true} if it is safe to modify the state of any inflight transactions.
         *         {@code false} otherwise
         */
        @Override
        public boolean apply( final TxUnroll forceClose ) {
          if ( TxUnroll.ROLLBACK.equals( forceClose ) ) {
            final AtomicStampedReference<NestedTx> ref = txState.get( ).remove( persistenceContext.name( ) );
            if ( ( ref.getReference( ) != null ) && ref.getReference( ).isActive( ) ) {
              ref.getReference( ).doRollback( );
            }
            return false;
          } else {
            final AtomicStampedReference<NestedTx> ref = txState.get( ).get( persistenceContext.name( ) );
            final int stamp = ref.getStamp( );
            ref.set( ref.getReference( ), stamp - 1 );
            return ref.getStamp( ) <= 0;
          }
        }
        
      } );
      final AtomicStampedReference<NestedTx> ref = new AtomicStampedReference<NestedTx>( entityWrapper, 1 );
      this.get( ).put( persistenceContext.name( ), ref );
      return ref;
    }
    
  }
  
  public static class NestedTx implements EntityTransaction {
    private final String                 ctx;
    private final TxRecord               record;
    private EntityManager                em;
    private EntityTransaction            transaction;
    private final WeakReference<Session> sessionRef;
    
    /**
     * Private for a reason.
     * 
     * @see {@link NestedTx#get(Class)}
     * @param persistenceContext
     */
    @SuppressWarnings( "unchecked" )
    private NestedTx( String ctx ) {
      this.ctx = ctx;
      this.record = new TxRecord( ctx );
      final EntityManagerFactory anemf = ( EntityManagerFactoryImpl ) PersistenceContexts.getEntityManagerFactory( ctx );
      assertThat( anemf, notNullValue( ) );
      try {
        this.record.logEvent( TxState.BEGIN, TxEvent.CREATE );
        this.em = anemf.createEntityManager( );
        assertThat( this.em, notNullValue( ) );
        this.transaction = this.em.getTransaction( );
        assertThat( this.transaction, notNullValue( ) );
        this.transaction.begin( );
        this.sessionRef = new WeakReference<Session>( ( Session ) this.em.getDelegate( ) );
        this.record.logEvent( TxState.END, TxEvent.CREATE );
      } catch ( final Throwable ex ) {
        Logs.exhaust( ).error( ex, ex );
        this.record.logEvent( TxState.FAIL, TxEvent.CREATE );
        this.rollback( );
        throw new RuntimeException( PersistenceExceptions.throwFiltered( ex ) );
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
     * @see javax.persistence.EntityTransaction#setRollbackOnly()
     */
    @Override
    public void setRollbackOnly( ) {
      this.transaction.setRollbackOnly( );
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
     * @see javax.persistence.EntityTransaction#begin()
     */
    @Override
    public void begin( ) {
      this.transaction.begin( );
    }
    
    /**
     * @delegate Do not change semantics here.
     * @see javax.persistence.EntityTransaction#rollback()
     */
    @Override
    public void rollback( ) {
      try {
        if ( ( this.transaction != null ) && this.transaction.isActive( ) ) {
          this.transaction.rollback( );
        }
        this.record.logEvent( TxState.END, TxEvent.ROLLBACK );
      } catch ( final Throwable e ) {
        this.record.logEvent( TxState.FAIL, TxEvent.ROLLBACK );
        PersistenceExceptions.throwFiltered( e );
      } finally {
        this.cleanup( );
      }
    }
    
    /**
     * @delegate Do not change semantics here.
     * @see javax.persistence.EntityTransaction#commit()
     */
    @Override
    public void commit( ) {
      this.record.logEvent( TxState.BEGIN, TxEvent.COMMIT );
      try {
        this.transaction.commit( );
        this.record.logEvent( TxState.END, TxEvent.COMMIT );
      } catch ( final RuntimeException e ) {
        this.rollback( );
        this.record.logEvent( TxState.FAIL, TxEvent.COMMIT );
        PersistenceExceptions.throwFiltered( e );
        throw e;
      } finally {
        this.cleanup( );
      }
    }
    
    private boolean isOpen( ) {
      final boolean hasEm = ( this.em != null ) && this.em.isOpen( );
      final boolean hasSession = ( this.sessionRef.get( ) != null ) && this.sessionRef.get( ).isOpen( );
      final boolean hasTx = ( this.transaction != null ) && this.transaction.isActive( );
      if ( hasEm && hasSession && hasTx ) {
        return true;
      } else {
        this.cleanup( );
        return false;
      }
    }
    
    void doRollback( ) {
      try {
        if ( ( this.transaction != null ) && this.transaction.isActive( ) ) {
          this.transaction.rollback( );
        }
        this.record.logEvent( TxState.END, TxEvent.ROLLBACK );
      } catch ( final Throwable e ) {
        this.record.logEvent( TxState.FAIL, TxEvent.ROLLBACK );
        PersistenceExceptions.throwFiltered( e );
      } finally {
        this.cleanup( );
      }
    }
    
    private void cleanup( ) {
      if ( ( this.sessionRef != null ) && ( this.sessionRef.get( ) != null ) ) {
        this.sessionRef.clear( );
      }
      if ( ( this.transaction != null ) && this.transaction.isActive( ) ) {
        this.transaction.rollback( );
      }
      this.transaction = null;
      if ( ( this.em != null ) && this.em.isOpen( ) ) {
        this.em.close( );
      }
      this.em = null;
    }
    
    public Criteria createCriteria( final Class class1 ) {
      return this.getSession( ).createCriteria( class1 );
    }
    
    /** package default on purpose **/
    EntityManager getEntityManager( ) {
      return this.em;
    }
    
    /** :| should also be package default **/
    Session getSession( ) {
      return this.sessionRef.get( );
    }
    
  }
  
  static class TxRecord {
    private final String    persistenceContext;
    private final String    uuid;
    private final String    startingStackTrace;
    private final StopWatch stopWatch;
    private final Long      startTime;
    private volatile long   splitTime = 0l;
    
    TxRecord( String persistenceContext ) {
      this.persistenceContext = persistenceContext;
      this.uuid = String.format( "%s:%s", this.persistenceContext, UUID.randomUUID( ).toString( ) );
      this.startingStackTrace = Logs.isExtrrreeeme( )
        ? Threads.currentStackString( )
        : "n/a";
      this.startTime = System.currentTimeMillis( );
      this.stopWatch = new StopWatch( );
      this.stopWatch.start( );
    }
    
    private long split( ) {
      this.stopWatch.split( );
      this.splitTime = this.stopWatch.getSplitTime( );
      this.stopWatch.unsplit( );
      return this.splitTime;
    }
    
    private boolean isExpired( ) {
      final long splitTime = this.split( );
      return ( splitTime - 30000 ) > this.startTime;
    }
    
    final void logEvent( final TxState txState, final TxEvent txAction ) {
      if ( Logs.isExtrrreeeme( ) ) {
        final long oldSplit = this.splitTime;
        this.stopWatch.split( );
        this.splitTime = this.stopWatch.getSplitTime( );
        this.stopWatch.unsplit( );
        final Long split = this.splitTime - oldSplit;
        Logs.exhaust( ).debug( Joiner.on( ":" ).join( EventType.PERSISTENCE, txState.event( txAction ), Long.toString( split ), this.uuid ) );
      }
    }
    
    private String getStartingStackTrace( ) {
      return this.startingStackTrace;
    }
    
    private StopWatch getStopWatch( ) {
      return this.stopWatch;
    }
    
    private long getSplitTime( ) {
      return this.splitTime;
    }
    
    private Long getStartTime( ) {
      return this.startTime;
    }
    
    private String getPersistenceContext( ) {
      return this.persistenceContext;
    }
    
    private String getUuid( ) {
      return this.uuid;
    }
    
  }
  
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
  
  enum TxState {
    BEGIN,
    END,
    FAIL;
    public String event( final TxEvent e ) {
      return e.name( ) + ":" + this.name( );
    }
  }
  
  enum TxUnroll {
    SAFELY,
    ROLLBACK;
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
}
