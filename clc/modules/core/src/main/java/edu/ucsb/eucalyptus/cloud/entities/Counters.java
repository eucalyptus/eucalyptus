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
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.entities;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.auth.Hashes;
import com.eucalyptus.bootstrap.Component;
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
      EntityManager em = EntityWrapper.getEntityManagerFactory( Component.eucalyptus.name( ) ).createEntityManager(  );
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
      EntityManager em = EntityWrapper.getEntityManagerFactory( Component.eucalyptus.name( )).createEntityManager(  );
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
