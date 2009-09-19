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
package com.eucalyptus.util;

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.exception.JDBCConnectionException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mchange.v2.resourcepool.CannotAcquireResourceException;
import com.mchange.v2.resourcepool.TimeoutException;

public class EntityWrapper<TYPE> {

  static Logger    LOG     = Logger.getLogger( EntityWrapper.class );
  private TxHandle tx;

  public EntityWrapper( ) {
    this( "eucalyptus" );
  }

  @SuppressWarnings( "unchecked" )
  public EntityWrapper( String persistenceContext ) {
    try {
      DbEvent.CREATE.log( );
      this.tx = new TxHandle( persistenceContext );
    } catch ( Throwable e ) {
      this.exceptionCaught( e );
      throw (RuntimeException) e ;
    }
  }

  @SuppressWarnings( "unchecked" )
  public List<TYPE> query( TYPE example ) {
    DbEvent.QUERY.log( );
    Example qbe = Example.create( example ).enableLike( MatchMode.EXACT );
    List<TYPE> resultList = ( List<TYPE> ) this.getSession( ).createCriteria( example.getClass( ) ).add( qbe ).list( );
    return Lists.newArrayList( Sets.newHashSet( resultList ) );
  }

  public TYPE getUnique( TYPE example ) throws EucalyptusCloudException {
    DbEvent.UNIQUE.log( );
    List<TYPE> res = this.query( example );
    if ( res.size( ) != 1 ) {
      String msg = null;
      msg = example.toString( );
      if ( msg != null && msg.startsWith( example.getClass( ).getCanonicalName( ) ) ) {
        msg = LogUtil.dumpObject( example );
      }
      throw new EucalyptusCloudException( "Error locating information for " + msg );
    }
    return res.get( 0 );
  }

  @SuppressWarnings( "unchecked" )
  private void exceptionCaught( Throwable e ) {
    Throwable cause = DebugUtil.checkForCauseOfInterest( e, JDBCConnectionException.class, CannotAcquireResourceException.class, TimeoutException.class );
    if ( !( cause instanceof ExceptionNotRelatedException ) ) {
      LOG.error( cause, cause );
      DatabaseUtil.handleConnectionError( cause );
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
    try {
      this.tx.rollback( );
      DbEvent.ROLLBACK.log( );
    } catch ( Exception e ) {
      DbEvent.ROLLBACK.logError( );
      this.exceptionCaught( e );
    }
  }

  public void commit( ) {
    try {
      this.tx.commit( );
      DbEvent.COMMIT.log( );
    } catch ( Throwable e ) {
      DbEvent.COMMIT.logError( );
      this.exceptionCaught( e );
      throw (RuntimeException) e ;
    }
  }

  public Session getSession( ) {
    return tx.getSession( );
  }

  public EntityManager getEntityManager( ) {
    return tx.getEntityManager( );
  }

  @SuppressWarnings( "unchecked" )
  public <NEWTYPE> EntityWrapper<NEWTYPE> recast( Class<NEWTYPE> c ) {
    return ( EntityWrapper<NEWTYPE> ) this;
  }

  enum DbEvent {
    CREATE,
    COMMIT {
      public String toString( ) {
        return "CLOSE " + this.name( );
      }
    },
    ROLLBACK {
      public String toString( ) {
        return "CLOSE " + this.name( );
      }
    },
    UNIQUE,
    QUERY;
    String getMessage( ) {
      return this.toString( ) + " " + DebugUtil.getMyStackTraceElement( );
    }

    public void logError( ) {
      LOG.error( LogUtil.header( "ERROR during: " + this.getMessage( ) ) );
    }

    public void log( ) {
      LOG.debug( this.getMessage( ) );
    }
  }

}
