/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.entities;

import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.criterion.*;

import javax.persistence.*;
import java.sql.*;
import java.util.List;

public class EntityWrapper<TYPE> {

  private static Logger LOG = Logger.getLogger( EntityWrapper.class );

  private static EntityManagerFactory emf = getEntityManagerFactory();

  public static EntityManagerFactory getEntityManagerFactory()
  {
    synchronized ( EntityWrapper.class )
    {
      if ( emf == null )
      {
        emf = Persistence.createEntityManagerFactory( "eucalyptus" );
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        Session s = ( Session ) em.getDelegate();
        try
        {
          Connection conn = s.connection();
          Statement stmt = conn.createStatement();
          stmt.execute( "SET WRITE_DELAY 100 MILLIS" );
//:: TODO-1.4: work around known hsql bug to change LOG_SIZE? :://
//          stmt.execute( "SET LOG_SIZE 10" );
          conn.commit();
        }
        catch ( SQLException e )
        {
          LOG.error( e, e );
        }
        tx.commit();
        em.close();
      }
      return emf;
    }
  }

  private EntityManager em;
  private Session session;
  private EntityTransaction tx;

  public EntityWrapper()
  {
    this.em = EntityWrapper.getEntityManagerFactory().createEntityManager();
    this.session = ( Session ) em.getDelegate();
    this.tx = em.getTransaction();
    tx.begin();
  }

  public List<TYPE> query( TYPE example )
  {
    Example qbe = Example.create( example ).enableLike( MatchMode.EXACT );
    List<TYPE> resultList = ( List<TYPE> ) session.createCriteria( example.getClass() ).add( qbe ).list();
    return resultList;
  }

  public TYPE getUnique( TYPE example ) throws EucalyptusCloudException
  {
    List<TYPE> res = this.query( example );
    if ( res.size() != 1 )
      throw new EucalyptusCloudException( "Error locating information for " + example.toString() );
    return res.get( 0 );
  }

  public void add( TYPE newObject )
  {
    em.persist( newObject );
  }

  public void merge( TYPE newObject )
  {
    em.merge( newObject );
  }


  public void delete( TYPE deleteObject )
  {
    em.remove( deleteObject );
  }

  public void rollback()
  {
    this.tx.rollback();
    this.em.close();
  }

  public void commit()
  {
    this.em.flush();
    this.tx.commit();
    this.em.close();
  }

  public <NEWTYPE> EntityWrapper<NEWTYPE> recast( Class<NEWTYPE> c ) {
    return ( EntityWrapper<NEWTYPE>) this;
  }

  public EntityManager getEntityManager()
  {
    return em;
  }

  public Session getSession()
  {
    return session;
  }

  public static void close()
  {
    if ( EntityWrapper.getEntityManagerFactory().isOpen() )
      EntityWrapper.getEntityManagerFactory().close();
  }
}
