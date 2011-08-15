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

import javax.persistence.MappedSuperclass;
import com.eucalyptus.cloud.UserMetadata;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.util.HasNaturalId;
import com.eucalyptus.util.Logs;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TransactionException;
import com.eucalyptus.util.async.Callback;

@MappedSuperclass
public abstract class PersistentResource<T extends PersistentResource<T, R>, R extends HasNaturalId> extends UserMetadata<ResourceAllocation.State> implements ResourceAllocation<T, R> {
  
  protected PersistentResource( OwnerFullName owner, String displayName ) {
    super( owner, displayName );
  }

  /**
   * {@inheritDoc ResourceAllocation#currentState()}
   * 
   * @see ResourceAllocation#currentState()
   * @return
   */
  @Override
  public final ResourceAllocation.State currentState( ) {
    return this.getState( );
  }
  
  protected abstract void setReferer( R referer );
  
  protected abstract R getReferer( );
  
  /**
   * {@inheritDoc ResourceAlllocation#allocate()}
   * 
   * @see ResourceAllocation#allocate()
   * @return
   */
  @Override
  public final SetReference<T, R> allocate( ) throws ResourceAllocationException {
    this.doSetReferer( null, ResourceAllocation.State.FREE, ResourceAllocation.State.PENDING );
    return this.doCreateSetReference( );
  }
  
  /**
   * {@inheritDoc ResourceAlllocation#release()}
   * 
   * @see ResourceAllocation#release()
   * @return
   * @throws ResourceAllocationException
   */
  @Override
  public ClearReference<T> release( ) throws ResourceAllocationException {
    this.doSetReferer( this.getReferer( ), ResourceAllocation.State.EXTANT, ResourceAllocation.State.RELEASING );
    return this.doCreateClearReferer( );
  }
  
  /**
   * {@inheritDoc ResourceAlllocation#teardown()}
   * 
   * @see ResourceAllocation#teardown()
   */
  @Override
  public final void teardown( ) {}
  
  /**
   * {@inheritDoc ResourceAlllocation#reclaim()}
   * 
   * @see ResourceAllocation#reclaim(com.eucalyptus.util.HasNaturalId)
   * @param referer
   * @return
   */
  @Override
  public final T reclaim( R referer ) {
    return null;
  }
  
  @SuppressWarnings( "unchecked" )
  T doSetReferer( final R referer, final ResourceAllocation.State preconditionState, final ResourceAllocation.State finalState ) throws ResourceAllocationException {
    try {
      return Transactions.naturalId( ( T ) this, new Callback<T>( ) {
        
        @Override
        public void fire( T input ) {
          if ( !preconditionState.equals( input.getState( ) ) ) {
            throw new RuntimeException( "Error allocating resource " + PersistentResource.this.getClass( ).getSimpleName( ) + " with id "
                                        + PersistentResource.this.getDisplayName( ) + " as the state is not " + preconditionState.name( ) + " (currently "
                                        + PersistentResource.this.getState( ) + ")." );
          } else {
            PersistentResource.this.setReferer( referer );
            PersistentResource.this.setState( finalState );
          }
        }
      } );
    } catch ( TransactionException ex ) {
      Logs.extreme( ).error( ex, ex );
      throw new ResourceAllocationException( ex );
    }
  }
  
  private SetReference<T, R> doCreateSetReference( ) {
    SetReference<T, R> ref = new SetReference<T, R>( ) {
      private volatile boolean finished = false;
      
      @Override
      public T set( R referer ) throws ResourceAllocationException {
        this.checkFinished( );
        T ret = PersistentResource.this.doSetReferer( referer, ResourceAllocation.State.PENDING, ResourceAllocation.State.EXTANT );
        this.finished = true;
        return ret;
      }
      
      @Override
      public T abort( ) throws ResourceAllocationException {
        this.checkFinished( );
        T ret = PersistentResource.this.doSetReferer( null, ResourceAllocation.State.PENDING, ResourceAllocation.State.FREE );
        this.finished = true;
        return ret;
      }
      
      private void checkFinished( ) throws ResourceAllocationException {
        if ( this.finished ) {
          throw new ResourceAllocationException( "Failed to set referer since this reference has already been set: " + PersistentResource.this.getDisplayName( )
                                                 + " to "
                                                 + PersistentResource.this.getReferer( ) + " and is currently in state " + PersistentResource.this.getState( ) );
        }
      }

      @Override
      public T get( ) throws TransactionException {
        return Transactions.naturalId( ( T ) PersistentResource.this );
      }
      
    };
    return ref;
  }
  
  private ClearReference<T> doCreateClearReferer( ) {
    final R referer = this.getReferer( );
    ClearReference<T> ref = new ClearReference<T>( ) {
      private volatile boolean finished = false;
      
      @Override
      public T clear( ) throws ResourceAllocationException {
        this.checkFinished( );
        T ret = PersistentResource.this.doSetReferer( null, ResourceAllocation.State.RELEASING, ResourceAllocation.State.FREE );
        this.finished = true;
        return ret;
      }
      
      @Override
      public T abort( ) throws ResourceAllocationException {
        this.checkFinished( );
        T ret = PersistentResource.this.doSetReferer( referer, ResourceAllocation.State.RELEASING, ResourceAllocation.State.EXTANT );
        this.finished = true;
        return ret;
      }
      
      private void checkFinished( ) throws ResourceAllocationException {
        if ( this.finished ) {
          throw new ResourceAllocationException( "Failed to set referer since this reference has already been set: " + PersistentResource.this.getDisplayName( )
                                                 + " to "
                                                 + PersistentResource.this.getReferer( ) + " and is currently in state " + PersistentResource.this.getState( ) );
        }
      }
      
    };
    return ref;
  }
  
}
