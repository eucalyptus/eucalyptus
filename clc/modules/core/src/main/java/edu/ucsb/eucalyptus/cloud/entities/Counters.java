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

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.auth.Hashes;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusProperties;

import javax.persistence.*;
import java.util.List;
import java.util.zip.Adler32;

@Entity
@Table( name = "counters" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class Counters {

  @Id
  @Column( name = "id" )
  private Long id = -1l;
  @Column( name = "msg_count" )
  private Long messageId;

  public Counters()
  {
    this.id = 0l;
    this.messageId = 0l;
  }

  public Long getMessageId()
  {
    return messageId;
  }

  public void setMessageId( Long messageId )
  {
    this.messageId = messageId;
  }

  @Transient
  private static long tempId = -1;
  @Transient
  private static Adler32 digest = new Adler32();
  public static synchronized long getIdBlock( int length )
  {
    ensurePersistence( );
    long oldTemp = tempId;
    tempId+=length;
    return oldTemp;
  }

  public synchronized static String getNextId()
  {
    ensurePersistence( );
    tempId++;
    return Hashes.getDigestBase64( Long.toString( tempId ), Hashes.Digest.SHA512, false ).replaceAll( "\\.","" );
  }

  private static void ensurePersistence( )
  {
    long modulus = 100l;
    if ( tempId < 0 )
    {
      Counters find = null;
      EntityManager em = EntityWrapper.getEntityManagerFactory( EucalyptusProperties.NAME).createEntityManager(  );
      Session session = (Session) em.getDelegate();
      List<Counters> found = ( List<Counters> ) session.createSQLQuery( "select * from counters" ).addEntity( Counters.class ).list();
      if( found.isEmpty() )
      {
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        Counters newCounters = new Counters();
        em.persist(newCounters);
        em.flush();
        tx.commit();
        find = newCounters;
      }
      else
        find = found.get(0);
      tempId = find.getMessageId() + modulus;
      em.close();
    }
    else if ( tempId % modulus == 0 )
    {
      EntityManager em = EntityWrapper.getEntityManagerFactory( EucalyptusProperties.NAME).createEntityManager(  );
      Session session = (Session) em.getDelegate();
      Transaction tx = session.beginTransaction();
      tx.begin();
      Counters find = ( Counters ) session.createSQLQuery( "select * from counters" ).addEntity( Counters.class ).list().get( 0 );
      tempId += modulus;
      find.setMessageId( tempId );
      session.save( find );
      tx.commit();
      em.close();
    }
  }
}
