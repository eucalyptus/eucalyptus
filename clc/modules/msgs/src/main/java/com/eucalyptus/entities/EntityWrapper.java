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
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
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
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.exception.JDBCConnectionException;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.system.LogLevels;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LogUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;

public class EntityWrapper<TYPE> {

  static Logger    LOG     = Logger.getLogger( EntityWrapper.class );
  private TxHandle tx;
  private static final boolean TRACE = "TRACE".equals( System.getProperty( "euca.log.exhaustive.db" ) );
  public EntityWrapper( ) {
    this( "eucalyptus_general" );
  }
  
  public static <T> EntityWrapper<T> get( Class<T> type ) {
    if( !type.isAnnotationPresent( PersistenceContext.class ) ) {
      throw new RuntimeException( "Attempting to create an entity wrapper instance for non persistent type: " + type.getCanonicalName( ) );
    }
    return new EntityWrapper<T>( type.getAnnotation( PersistenceContext.class ).name( ) );    
  }
  public static <T> EntityWrapper<T> get( T obj ) {
    return get( (Class<T>) obj.getClass( ) );
  }

  @SuppressWarnings( "unchecked" )
  public EntityWrapper( String persistenceContext ) {
    try {
      if( TRACE ) EventRecord.here( EntityWrapper.class, EventType.PERSISTENCE, DbEvent.CREATE.begin( ) ).trace( );
      this.tx = new TxHandle( persistenceContext );
    } catch ( Throwable e ) {
      if( TRACE ) EventRecord.here( EntityWrapper.class, EventType.PERSISTENCE, DbEvent.CREATE.fail( ),e.getMessage( ) ).trace( );
      this.exceptionCaught( e );
      throw (RuntimeException) e ;
    }
    if( TRACE ) EventRecord.here( EntityWrapper.class, EventType.PERSISTENCE, DbEvent.CREATE.end( ), Long.toString( tx.splitOperation() ), tx.getTxUuid( ) ).trace( );
  }

  @SuppressWarnings( "unchecked" )
  public List<TYPE> query( TYPE example ) {
    if( TRACE ) EventRecord.here( EntityWrapper.class, EventType.PERSISTENCE, DbEvent.QUERY.begin( ), tx.getTxUuid( ) ).trace( );
    Example qbe = Example.create( example ).enableLike( MatchMode.EXACT );
    List<TYPE> resultList = ( List<TYPE> ) this.getSession( ).createCriteria( example.getClass( ) ).setCacheable( true ).add( qbe ).list( );
    if( TRACE ) EventRecord.here( EntityWrapper.class, EventType.PERSISTENCE, DbEvent.QUERY.end( ), Long.toString( tx.splitOperation( ) ), tx.getTxUuid( ) ).trace( );
    return Lists.newArrayList( Sets.newHashSet( resultList ) );
  }

  public TYPE getUnique( TYPE example ) throws EucalyptusCloudException {
    if( TRACE ) EventRecord.here( EntityWrapper.class, EventType.PERSISTENCE, DbEvent.UNIQUE.begin( ), tx.getTxUuid( ) ).trace( );
    List<TYPE> res = this.query( example );
    if ( res.size( ) != 1 ) {
      String msg = null;
      try {
        msg = LogUtil.dumpObject( example );
      } catch ( Exception e ) {
        msg = example.toString( );
      }
      if( TRACE ) EventRecord.here( EntityWrapper.class, EventType.PERSISTENCE, DbEvent.QUERY.fail( ), Long.toString( tx.splitOperation( ) ), tx.getTxUuid( ) ).trace( );
      throw new EucalyptusCloudException( "Error locating information for " + msg );
    }
    if( TRACE ) EventRecord.here( EntityWrapper.class, EventType.PERSISTENCE, DbEvent.QUERY.end( ), Long.toString( tx.splitOperation( ) ), tx.getTxUuid( ) ).trace( );
    return res.get( 0 );
  }

  @SuppressWarnings( "unchecked" )
  private void exceptionCaught( Throwable e ) {
    if( e instanceof JDBCConnectionException || e instanceof IllegalStateException ) {
      LOG.error( e, e );
      PersistenceContexts.handleConnectionError( e );
    }
  }

  public void add( TYPE newObject ) {
    this.getEntityManager( ).persist( newObject );
  }

  public void merge( TYPE newObject ) {
    this.getEntityManager( ).merge( newObject );
  }

  public void mergeAndCommit( TYPE newObject ) {
    this.getEntityManager( ).merge( newObject );
    this.commit( );
  }

  public void delete( TYPE deleteObject ) {
    this.getEntityManager( ).remove( deleteObject );
  }

  public void rollback( ) {
    if( TRACE ) EventRecord.here( EntityWrapper.class, EventType.PERSISTENCE, DbEvent.ROLLBACK.begin( ), tx.getTxUuid( ) ).trace( );
    try {
      this.tx.rollback( );
    } catch ( Throwable e ) {
      if( TRACE ) EventRecord.here( EntityWrapper.class, EventType.PERSISTENCE, DbEvent.ROLLBACK.fail( ), Long.toString( tx.splitOperation() ), tx.getTxUuid( ) ).trace( );
      this.exceptionCaught( e );
    }
    if( TRACE ) EventRecord.here( EntityWrapper.class, EventType.PERSISTENCE, DbEvent.ROLLBACK.end( ), Long.toString( tx.splitOperation() ), tx.getTxUuid( ) ).trace( );
  }

  public void commit( ) {
    if( TRACE ) EventRecord.here( EntityWrapper.class, EventType.PERSISTENCE, DbEvent.COMMIT.begin( ), tx.getTxUuid( ) ).trace( );
    try {
      this.tx.commit( );
    } catch ( Throwable e ) {
      if( TRACE ) EventRecord.here( EntityWrapper.class, EventType.PERSISTENCE, DbEvent.COMMIT.fail( ), Long.toString( tx.splitOperation( ) ), tx.getTxUuid( ) ).trace( );
      this.exceptionCaught( e );
      throw (RuntimeException) e ;
    }
    if( TRACE ) EventRecord.here( EntityWrapper.class, EventType.PERSISTENCE, DbEvent.COMMIT.end( ), Long.toString( tx.splitOperation( ) ), tx.getTxUuid( ) ).trace( );
  }

  public Session getSession( ) {
    return tx.getSession( );
  }

  public EntityManager getEntityManager( ) {
    return tx.getEntityManager( );
  }

  @SuppressWarnings( "unchecked" )
  public <NEWTYPE> EntityWrapper<NEWTYPE> recast( Class<NEWTYPE> c ) {
    return (com.eucalyptus.entities.EntityWrapper<NEWTYPE> ) this;
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
    public String fail() {
      return this.name( ) + ":FAIL";
    }
    public String begin() {
      return this.name( ) + ":BEGIN";
    }
    public String end() {
      return this.name( ) + ":END";
    }
    public String getMessage( ) {
      if( LogLevels.TRACE ) {
        return EntityWrapper.getMyStackTraceElement( ).toString( );
      } else {
        return "n.a";
      }
    }
  }

}
