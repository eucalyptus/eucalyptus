/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.cloud.util;

import javax.annotation.Nullable;
import javax.persistence.MappedSuperclass;
import org.apache.log4j.Logger;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.util.HasNaturalId;
import com.eucalyptus.util.OwnerFullName;

@MappedSuperclass
public abstract class PersistentReference<T extends PersistentReference<T, R>, R extends HasNaturalId> extends UserMetadata<Reference.State> implements Reference<T, R> {
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
  
  protected abstract R getReference( );
  
  /**
   * {@inheritDoc ResourceAlllocation#allocate()}
   * 
   * @see Reference#allocate()
   * @return
   */
  @Override
  public final T allocate( ) throws ResourceAllocationException {
    this.doSetReferer( null, Reference.State.FREE, Reference.State.PENDING );
    return ( T ) this;
  }
  
  /**
   * {@inheritDoc ResourceAlllocation#release()}
   * 
   * @see Reference#release()
   * @return
   * @throws ResourceAllocationException
   */
  @Override
  public T release( ) throws ResourceAllocationException {
    final T ret = this.doSetReferer( null, null, Reference.State.FREE );
    return ( T ) this;
  }
  
  /**
   * {@inheritDoc ResourceAlllocation#teardown()}
   * 
   * @throws ResourceAllocationException
   * 
   * @see Reference#teardown()
   */
  @Override
  public final boolean teardown( ) throws ResourceAllocationException {
    Entities.delete( this );
    return true;
  }
  
  /**
   * {@inheritDoc ResourceAlllocation#reclaim()}
   * 
   * @see Reference#reclaim(com.eucalyptus.util.HasNaturalId)
   * @param referer
   * @return
   * @throws ResourceAllocationException
   */
  @Override
  public final T reclaim( final R referer ) throws ResourceAllocationException {
    final T ret = PersistentReference.this.doSetReferer( referer, null, Reference.State.EXTANT );
    return ret;
  }
  
  @SuppressWarnings( "unchecked" )
  T doSetReferer( final R referer, final Reference.State preconditionState, final Reference.State finalState ) throws ResourceAllocationException {
    this.checkPreconditions( referer, preconditionState, finalState );
    if ( ( referer != null ) && !Reference.State.PENDING.equals( finalState ) ) {
      final R refererEntity = referer;
      this.setReference( refererEntity );
      this.setState( finalState );
    } else {
      this.setReference( null );
      this.setState( finalState );
    }
    return ( T ) this;
  }
  
  private void checkPreconditions( R referer, final Reference.State preconditionState, Reference.State finalState ) throws RuntimeException {
    if ( ( !Entities.hasTransaction( this ) ) ) {
      throw new RuntimeException( "Error allocating resource " + PersistentReference.this.getClass( ).getSimpleName( ) + " with id "
                                  + this.getDisplayName( ) + " as there is no ongoing transaction." );
    }
    State currentState = this.getState( );
    boolean matchPrecondition = preconditionState == null || ( currentState != null && preconditionState.equals( currentState ) );
    boolean matchFinal = ( finalState == null && currentState == null ) || ( finalState != null && currentState != null && finalState.equals( currentState ) );
    boolean matchReferer = ( this.getReference( ) == null ) || ( referer != null && this.getReference( ) != null && referer.equals( this.getReference( ) ) );
    if ( ( matchFinal && matchReferer ) || matchPrecondition ) {
      return;
    } else {
      throw new RuntimeException( "Error allocating resource " + PersistentReference.this.getClass( ).getSimpleName( )
        + " with id "
        + this.getDisplayName( )
        + " as the state is not either the precondition "
        + preconditionState.name( )
        + " or the final state "
        + finalState.name( )
        + " (currently "
        + currentState.name( )
        + ", referer "
        + this.getReference( )
        + ", passed referer "
        + referer
        + ")" );
    }
  }
  
  @Override
  public T set( final R referer ) throws ResourceAllocationException {
    final T ret = PersistentReference.this.doSetReferer( referer, Reference.State.PENDING, Reference.State.EXTANT );
    return ret;
  }
}
