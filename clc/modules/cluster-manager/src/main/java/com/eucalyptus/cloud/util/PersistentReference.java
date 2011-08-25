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

package com.eucalyptus.cloud.util;

import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;
import javax.persistence.MappedSuperclass;
import org.apache.log4j.Logger;
import com.eucalyptus.cloud.UserMetadata;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.HasNaturalId;
import com.eucalyptus.util.OwnerFullName;

@MappedSuperclass
public abstract class PersistentReference<T extends PersistentReference<T, R>, R extends HasNaturalId> extends UserMetadata<Resource.State> implements Resource<T, R> {
  private static final long serialVersionUID = 1L;
  private static Logger     LOG              = Logger.getLogger( PersistentReference.class );
  
  protected PersistentReference( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }
  
  @SuppressWarnings( "unchecked" )
  private T get( ) {
    return ( T ) this;
  }
  
  /**
   * Referer may be null!
   * 
   * @param referer
   */
  protected abstract void setReference( @Nullable R referer );
  
  /**
   * Clear the reference. This implies deleting {@code this}!
   * 
   * @return {@code reference} which points to {@code this} object.
   */
  protected abstract R clearReference( );
  
  protected abstract R getReference( );
  
  /**
   * {@inheritDoc ResourceAlllocation#allocate()}
   * 
   * @see Resource#allocate()
   * @return
   */
  @Override
  public final SetReference<T, R> allocate( ) throws ResourceAllocationException {
    this.doSetReferer( null, Resource.State.FREE, Resource.State.PENDING );
    return this.doCreateSetReference( );
  }
  
  /**
   * {@inheritDoc ResourceAlllocation#release()}
   * 
   * @see Resource#release()
   * @return
   * @throws ResourceAllocationException
   */
  @Override
  public ClearReference<T> release( ) throws ResourceAllocationException {
    this.doSetReferer( this.getReference( ), Resource.State.EXTANT, Resource.State.RELEASING );
    return this.doCreateClearReferer( );
  }
  
  /**
   * {@inheritDoc ResourceAlllocation#teardown()}
   * @throws ResourceAllocationException 
   * 
   * @see Resource#teardown()
   */
  @Override
  public final void teardown( ) throws ResourceAllocationException {
    final T ret = this.doSetReferer( null, null, Resource.State.FREE );
  }
  
  /**
   * {@inheritDoc ResourceAlllocation#reclaim()}
   * 
   * @see Resource#reclaim(com.eucalyptus.util.HasNaturalId)
   * @param referer
   * @return
   * @throws ResourceAllocationException 
   */
  @Override
  public final T reclaim( final R referer ) throws ResourceAllocationException {
    final T ret = PersistentReference.this.doSetReferer( referer, Resource.State.FREE, Resource.State.EXTANT );
    return ret;
  }
  
  @SuppressWarnings( "unchecked" )
  T doSetReferer( final R referer, final Resource.State preconditionState, final Resource.State finalState ) throws ResourceAllocationException {
    final EntityTransaction db = Entities.get( this.getClass( ) );
    try {
      final PersistentReference<T, R> thisEntity = Entities.merge( this );
      if ( ( thisEntity.getState( ) != null ) && ( preconditionState != null ) && !preconditionState.equals( thisEntity.getState( ) ) ) {
        throw new RuntimeException( "Error allocating resource " + PersistentReference.this.getClass( ).getSimpleName( ) + " with id "
                                    + this.getDisplayName( ) + " as the state is not " + preconditionState.name( ) + " (currently "
                                    + this.getState( ) + ")." );
      } else {
        if ( referer != null && !Resource.State.PENDING.equals( finalState ) ) {
          final R refererEntity = Entities.merge( referer );
          this.setReference( refererEntity );
          this.setState( finalState );
        } else {
          this.setReference( null );
          this.setState( finalState );
        }
      }
      db.commit( );
      return thisEntity.get( );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      LOG.error( ex );
      db.rollback( );
      throw new ResourceAllocationException( ex );
    }
  }
  
  private SetReference<T, R> doCreateSetReference( ) {
    final SetReference<T, R> ref = new SetReference<T, R>( ) {
      private volatile boolean finished = false;
      
      @Override
      public T set( final R referer ) throws ResourceAllocationException {
        this.checkFinished( );
        final T ret = PersistentReference.this.doSetReferer( referer, Resource.State.PENDING, Resource.State.EXTANT );
        this.finished = true;
        return ret;
      }
      
      public T abort( ) throws ResourceAllocationException {
        this.checkFinished( );
        final T ret = PersistentReference.this.doSetReferer( null, null, Resource.State.FREE );
        this.finished = true;
        return ret;
      }
      
      private void checkFinished( ) throws ResourceAllocationException {
        if ( this.finished ) {
          throw new ResourceAllocationException( "Failed to set referer since this reference has already been set: "
                                                 + PersistentReference.this.getDisplayName( )
                                                 + " to "
                                                 + PersistentReference.this.getReference( ) + " and is currently in state "
                                                 + PersistentReference.this.getState( ) );
        }
      }
      
      @SuppressWarnings( "unchecked" )
      @Override
      public T get( ) {
        return Entities.merge( ( T ) PersistentReference.this );
      }
      
      public int compareTo( final T o ) {
        return PersistentReference.this.compareTo( o );
      }
      
      @Override
      public T clear( ) throws ResourceAllocationException {
        return PersistentReference.this.doSetReferer( null, null, Resource.State.FREE );
      }
      
    };
    return ref;
  }
  
  private ClearReference<T> doCreateClearReferer( ) {
    final R referer = this.getReference( );
    final ClearReference<T> ref = new ClearReference<T>( ) {
      private volatile boolean finished = false;
      
      @Override
      public T clear( ) throws ResourceAllocationException {
        this.checkFinished( );
        final T ret = PersistentReference.this.doSetReferer( null, Resource.State.RELEASING, Resource.State.FREE );
        this.finished = true;
        return ret;
      }
      
      @Override
      public T abort( ) throws ResourceAllocationException {
        this.checkFinished( );
        final T ret = PersistentReference.this.doSetReferer( referer, Resource.State.RELEASING, Resource.State.EXTANT );
        this.finished = true;
        return ret;
      }
      
      private void checkFinished( ) throws ResourceAllocationException {
        if ( this.finished ) {
          throw new ResourceAllocationException( "Failed to set referer since this reference has already been set: "
                                                 + PersistentReference.this.getDisplayName( )
                                                 + " to "
                                                 + PersistentReference.this.getReference( ) + " and is currently in state "
                                                 + PersistentReference.this.getState( ) );
        }
      }
      
    };
    return ref;
  }
  
}
