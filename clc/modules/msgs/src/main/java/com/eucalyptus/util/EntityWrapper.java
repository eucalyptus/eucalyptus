/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 */

package com.eucalyptus.util;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.criterion.*;


import javax.persistence.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class EntityWrapper<TYPE> {

  private static Logger                            LOG = Logger.getLogger( EntityWrapper.class );

  private static Map<String, EntityManagerFactory> emf = new ConcurrentSkipListMap<String, EntityManagerFactory>( );

  public static EntityManagerFactory getEntityManagerFactory( ) {
    return EntityWrapper.getEntityManagerFactory( "eucalyptus" );
  }

  @SuppressWarnings( "deprecation" )
  public static EntityManagerFactory getEntityManagerFactory( final String persistenceContext ) {
    synchronized ( EntityWrapper.class ) {
      if ( !emf.containsKey( persistenceContext ) ) {
        LOG.info( "-> Setting up persistence context for : " + persistenceContext );
        LOG.info( "-> database host: " + System.getProperty("euca.db.host") );
        LOG.info( "-> database port: " + System.getProperty("euca.db.port") );
        //TODO: fix the way that persistence context is setup.
        emf.put( persistenceContext, Persistence.createEntityManagerFactory( persistenceContext ) );
        EntityManager em = emf.get( persistenceContext ).createEntityManager( );
        EntityTransaction tx = em.getTransaction( );
        tx.begin( );
        Session s = ( Session ) em.getDelegate( );
        try {
          Connection conn = s.connection( );
          Statement stmt = conn.createStatement( );
          stmt.execute( "SET WRITE_DELAY 100 MILLIS" );
          conn.commit( );
        } catch ( SQLException e ) {
          LOG.error( e, e );
        }
        tx.commit( );
        em.close( );
      }
      return emf.get( persistenceContext );
    }
  }

  private EntityManager     em;
  private Session           session;
  private EntityTransaction tx;

  public EntityWrapper( ) {
    this( "eucalyptus" );
  }

  public EntityWrapper( String persistenceContext ) {
    this.em = EntityWrapper.getEntityManagerFactory( persistenceContext ).createEntityManager( );
    this.session = ( Session ) em.getDelegate( );
    this.tx = em.getTransaction( );
    tx.begin( );
  }

  @SuppressWarnings( "unchecked" )
  public List<TYPE> query( TYPE example ) {
    Example qbe = Example.create( example ).enableLike( MatchMode.EXACT );
    List<TYPE> resultList = ( List<TYPE> ) session.createCriteria( example.getClass( ) ).add( qbe ).list( );
    return resultList;
  }

  public TYPE getUnique( TYPE example ) throws EucalyptusCloudException {
    List<TYPE> res = this.query( example );
    if ( res.size( ) != 1 ) throw new EucalyptusCloudException( "Error locating information for " + example.toString( ) );
    return res.get( 0 );
  }

  public void add( TYPE newObject ) {
    em.persist( newObject );
  }

  public void merge( TYPE newObject ) {
    em.merge( newObject );
  }

  public void delete( TYPE deleteObject ) {
    em.remove( deleteObject );
  }

  public void rollback( ) {
    if( this.tx.isActive( ) ) {
      this.tx.rollback( );      
    }
    this.em.close( );
  }

  public void commit( ) {
    this.em.flush( );
    this.tx.commit( );
    this.em.close( );
  }

  @SuppressWarnings( "unchecked" )
  public <NEWTYPE> EntityWrapper<NEWTYPE> recast( Class<NEWTYPE> c ) {
    return (EntityWrapper<NEWTYPE> ) this;
  }

  public EntityManager getEntityManager( ) {
    return em;
  }

  public Session getSession( ) {
    return session;
  }

}
