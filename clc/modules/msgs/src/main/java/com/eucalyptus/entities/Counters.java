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
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.entities;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.crypto.Crypto;
import com.eucalyptus.auth.crypto.Digest;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Transactions;
import com.eucalyptus.util.Tx;

@Entity
@PersistenceContext( name = "eucalyptus_general" )
@Table( name = "counters" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class Counters extends AbstractPersistent implements Serializable {
  private static Logger   LOG = Logger.getLogger( Counters.class );
  private static Counters singleton;
  
  public static long getIdBlock( int length ) {
    if ( singleton != null ) {
      return singleton.getBlock( length );
    } else {
      synchronized ( Counters.class ) {
        if ( singleton == null ) {
          EntityWrapper<Counters> db = new EntityWrapper<Counters>( "eucalyptus_general" );
          try {
            singleton = db.getUnique( new Counters() );
          } catch ( EucalyptusCloudException e ) {
            singleton = new Counters( 0l );
            try {
              db.add( singleton );
              db.commit( );
            } catch ( Exception e1 ) {
              LOG.fatal( e1, e1 );
              LOG.fatal( "Failed to initialize system counters.  These are important." );
              System.exit( -1 );
            }
          }
        }
      }
      return singleton.getBlock( length );
    }
  }
  
  public static String getNextId( ) {
    return Crypto.getDigestBase64( Long.toString( Counters.getIdBlock( 1 ) ), Digest.SHA512, false ).replaceAll( "\\.", "" );
  }
  
  @Transient
  private static Long             period   = 1000l;
  @Transient
  private static final AtomicLong tempId   = new AtomicLong( -1 );
  @Transient
  private static final AtomicLong lastSave = new AtomicLong( -1 );
  @Column( name = "msg_count" )
  private Long                    messageId;
  
  public Counters( ) {}
  public Counters( Long start ) { 
    this.messageId = start;
  }
  
  public static Counters uninitialized( ) {
    Counters c = new Counters( );
    c.setMessageId( null );
    return c;
  }
  
  Long getBlock( int length ) {
    final Long idStart;
    if ( tempId.compareAndSet( -1l, this.messageId ) ) {
      lastSave.set( tempId.addAndGet( Counters.period ) );
      idStart = tempId.addAndGet( length );
    } else {
      idStart = tempId.addAndGet( length );
      if ( ( idStart - lastSave.get( ) ) > 1000 ) {
        try {
          Transactions.one( Counters.uninitialized( ), new Tx<Counters>() {
            @Override
            public void fire( Counters t ) throws Throwable {
              t.setMessageId( idStart );
            }
          } );
        } catch ( EucalyptusCloudException e ) {
          LOG.debug( e, e );
        }
        lastSave.set( idStart );
      }
    }
    return idStart;
  }
  
  public Long getMessageId( ) {
    return messageId;
  }
  
  public void setMessageId( Long messageId ) {
    this.messageId = messageId;
  }
  
}
