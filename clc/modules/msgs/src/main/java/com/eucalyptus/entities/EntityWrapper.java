/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 */

/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.entities;

import java.util.List;
import java.util.NoSuchElementException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.LogLevels;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LogUtil;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class EntityWrapper<TYPE> {
  
  static Logger                LOG   = Logger.getLogger( EntityWrapper.class );
  private final TxHandle             tx;
  private static final boolean TRACE = "TRACE".equals( System.getProperty( "euca.log.exhaustive.db" ) );
  private static Class determineEntityClass( Class type ) {
    for ( Class c = type; c != Object.class; c = c.getSuperclass( ) ) {
      if ( c.isAnnotationPresent( PersistenceContext.class ) ) {
        return c;
      }
    }
    return type;
  }
  
  public static <T> EntityWrapper<T> get( Class<T> type ) {
    for ( Class c = type; c != Object.class; c = c.getSuperclass( ) ) {
      if ( c.isAnnotationPresent( PersistenceContext.class ) ) {
        return new EntityWrapper<T>( ( ( PersistenceContext ) c.getAnnotation( PersistenceContext.class ) ).name( ), true );
      }
    }
    throw new RuntimeException( "Attempting to create an entity wrapper instance for non persistent type: " + type.getCanonicalName( ) );
  }
  
  public static <T> EntityWrapper<T> get( T obj ) {
    return get( ( Class<T> ) obj.getClass( ) );
  }
  
  /**
   * Private for a reason.
   * @see {@link EntityWrapper#get(Class)}
   * @param persistenceContext
   */
  @Deprecated
  @SuppressWarnings( "unchecked" )
  private EntityWrapper( String persistenceContext, boolean ignored ) {
    try {
      if ( LogLevels.EXTREME ) LOG.debug( Joiner.on(":").join(  EntityWrapper.class, EventType.PERSISTENCE, DbEvent.CREATE.begin( ) ) );
      this.tx = new TxHandle( persistenceContext );
    } catch ( Throwable e ) {
      if ( LogLevels.EXTREME ) LOG.debug( Joiner.on(":").join(  EntityWrapper.class, EventType.PERSISTENCE, DbEvent.CREATE.fail( ), e.getMessage( ) ) );
      PersistenceErrorFilter.exceptionCaught( e );
      throw ( RuntimeException ) e;
    }
    if ( LogLevels.EXTREME ) LOG.debug( Joiner.on(":").join(  EntityWrapper.class, EventType.PERSISTENCE, DbEvent.CREATE.end( ), Long.toString( this.tx.splitOperation( ) ),
                                   this.tx.getTxUuid( ) ) );
  }
  
  @SuppressWarnings( "unchecked" )
  public List<TYPE> query( TYPE example ) {
    Example qbe = Example.create( example ).enableLike( MatchMode.EXACT );
    List<TYPE> resultList = ( List<TYPE> ) this.getSession( ).createCriteria( example.getClass( ) ).setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY ).setCacheable( true ).add( qbe ).list( );
    return Lists.newArrayList( Sets.newHashSet( resultList ) );
  }
  
  public TYPE lookupAndClose( TYPE example ) throws NoSuchElementException {
    TYPE ret = null;
    try {
      ret = this.getUnique( example );
      this.commit( );
    } catch ( EucalyptusCloudException ex ) {
      this.rollback( );
      throw new NoSuchElementException( ex.getMessage( ) );
    }
    return ret;
  }  
  
  public TYPE getUnique( TYPE example ) throws EucalyptusCloudException {
    if ( LogLevels.EXTREME ) LOG.debug( Joiner.on(":").join(  EventType.PERSISTENCE, DbEvent.UNIQUE.begin( ), this.tx.getTxUuid( ) ) );
    Object id = null;
    try {
      id = this.getEntityManager( ).getEntityManagerFactory( ).getPersistenceUnitUtil( ).getIdentifier( example );
    } catch ( Exception ex ) {
    }
    if( id != null ) {
      TYPE res = ( TYPE ) this.getEntityManager( ).find( example.getClass( ), id );
      if( res == null ) {
        throw new EucalyptusCloudException( "Get unique failed (returning 0 results for " + LogUtil.dumpObject( example ) );
      } else {
        return res;
      }
    } else {
      List<TYPE> res = this.query( example );
      if ( res.size( ) != 1 ) {
        throw new EucalyptusCloudException( "Get unique failed (returning " + res.size( ) + " results for " + LogUtil.dumpObject( example ) );
      }
    return res.get( 0 );
    }
  }
  
  
  
  /**
   * Invokes underlying persist implementation per jsr-220
   * 
   * @see http://opensource.atlassian.com/projects/hibernate/browse/HHH-1273
   * @param newObject
   */
  public void persist( TYPE newObject ) {
    try {
      this.getEntityManager( ).persist( newObject );
    } catch ( RuntimeException ex ) {
      PersistenceErrorFilter.exceptionCaught( ex );
    }
  }
  
  /**
   * Performs a save directly on the session with the distinguishing feature that generated IDs are
   * not forcibly generated (e.g., INSERTS are not performed)
   * 
   * @see http://opensource.atlassian.com/projects/hibernate/browse/HHH-1273
   * @param e
   */
  public void save( TYPE e ) {
    this.getSession( ).save( e );
  }
  
  /**
   * Calls {@link #persist(Object)}; here for legacy, and is deprecated in favor of persist
   * 
   * @see http://opensource.atlassian.com/projects/hibernate/browse/HHH-1273
   * @param newObject
   */
  public void add( TYPE newObject ) {
    this.persist( newObject );
  }
  
  /**
   * TODO: not use this please.
   * 
   * @param string
   * @return
   */
  public Query createQuery( String string ) {
    return this.getSession( ).createQuery( string );
  }
  
  /**
   * TODO: not use this.
   * 
   * @param class1
   * @param uuid
   * @return
   */
  public Object get( Class<TYPE> class1, String uuid ) {
    return this.getSession( ).get( class1, uuid );
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
  public TYPE merge( TYPE newObject ) {
    try {
      return this.getEntityManager( ).merge( newObject );
    } catch ( RuntimeException ex ) {
      PersistenceErrorFilter.exceptionCaught( ex );
      throw ex;
    }
  }
  
  /**
   * @see EntityWrapper#merge(Object)
   * @param newObject
   * @throws PersistenceException
   */
  public TYPE mergeAndCommit( TYPE newObject ) throws RecoverablePersistenceException {
    try {
      newObject = this.getEntityManager( ).merge( newObject );
      this.commit( );
      return newObject;
    } catch ( RuntimeException ex ) {
      PersistenceErrorFilter.exceptionCaught( ex );
      this.rollback( );
      throw ex;
    } catch ( Throwable ex ) {
      LOG.error( ex, ex );
      this.rollback( );
      throw new RecoverablePersistenceException( ex );
    }
  }
  
  public void delete( Object deleteObject ) {
    this.getEntityManager( ).remove( deleteObject );
  }
  
  public void rollback( ) {
    if ( LogLevels.EXTREME ) LOG.debug( Joiner.on(":").join(  EventType.PERSISTENCE, DbEvent.ROLLBACK.begin( ), this.tx.getTxUuid( ) ) );
    try {
      this.tx.rollback( );
    } catch ( Throwable e ) {
      if ( LogLevels.EXTREME ) LOG.debug( Joiner.on(":").join(  EventType.PERSISTENCE, DbEvent.ROLLBACK.fail( ), Long.toString( this.tx.splitOperation( ) ),
                                     this.tx.getTxUuid( ) ) );
      PersistenceErrorFilter.exceptionCaught( e );
    }
    if ( LogLevels.EXTREME ) LOG.debug( Joiner.on(":").join(  EventType.PERSISTENCE, DbEvent.ROLLBACK.end( ), Long.toString( this.tx.splitOperation( ) ),
                                   this.tx.getTxUuid( ) ) );
  }
  
  public void commit( ) {
    if ( LogLevels.EXTREME ) LOG.debug( Joiner.on(":").join(  EventType.PERSISTENCE, DbEvent.COMMIT.begin( ), this.tx.getTxUuid( ) ) );
    try {
      this.tx.commit( );
    } catch ( RuntimeException e ) {
      if ( LogLevels.EXTREME ) LOG.debug( Joiner.on(":").join(  EventType.PERSISTENCE, DbEvent.COMMIT.fail( ), Long.toString( this.tx.splitOperation( ) ),
                                     this.tx.getTxUuid( ) ) );
      PersistenceErrorFilter.exceptionCaught( e );
      throw e;
    } catch ( Throwable e ) {
      if ( LogLevels.EXTREME ) LOG.debug( Joiner.on(":").join(  EventType.PERSISTENCE, DbEvent.COMMIT.fail( ), Long.toString( this.tx.splitOperation( ) ),
                                     this.tx.getTxUuid( ) ) );
      PersistenceErrorFilter.exceptionCaught( e );
      throw new RuntimeException( e );
    }
    if ( LogLevels.EXTREME ) LOG.debug( Joiner.on(":").join(  EventType.PERSISTENCE, DbEvent.COMMIT.end( ), Long.toString( this.tx.splitOperation( ) ),
                                   this.tx.getTxUuid( ) ) );
  }
  
  public Criteria createCriteria( Class class1 ) {
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
  public <NEWTYPE> EntityWrapper<NEWTYPE> recast( Class<NEWTYPE> c ) {
    return ( com.eucalyptus.entities.EntityWrapper<NEWTYPE> ) this;
  }
  
  public static StackTraceElement getMyStackTraceElement( ) {
    int i = 0;
    for ( StackTraceElement ste : Thread.currentThread( ).getStackTrace( ) ) {
      if ( i++ < 2 || ste.getClassName( ).matches( ".*EntityWrapper.*" )
           || ste.getClassName( ).matches( ".*TxHandle.*" )
           || ste.getMethodName( ).equals( "getEntityWrapper" ) ) {
        continue;
      } else {
        return ste;
      }
    }
    throw new RuntimeException( "BUG: Reached bottom of stack trace without finding any relevent frames." );
  }
  
  enum DbEvent {
    CREATE,
    COMMIT,
    ROLLBACK,
    UNIQUE,
    QUERY;
    public String fail( ) {
      return this.name( ) + ":FAIL";
    }
    
    public String begin( ) {
      return this.name( ) + ":BEGIN";
    }
    
    public String end( ) {
      return this.name( ) + ":END";
    }
    
    public String getMessage( ) {
      if ( LogLevels.TRACE ) {
        return EntityWrapper.getMyStackTraceElement( ).toString( );
      } else {
        return "n.a";
      }
    }
  }
  
  public Query createSQLQuery( String sqlQuery ) {
    return this.getSession( ).createSQLQuery( sqlQuery );
  }
  
}
