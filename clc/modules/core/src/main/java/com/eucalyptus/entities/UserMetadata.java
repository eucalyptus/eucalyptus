/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.entities;

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.type.RestrictedType.UserRestrictedType;

@MappedSuperclass
public abstract class UserMetadata<STATE extends Enum<STATE>> extends AccountMetadata<STATE> implements UserRestrictedType {
  @Transient
  private static final long serialVersionUID = 1L;

  @Column( name = "metadata_user_id" )
  protected String          ownerUserId;
  @Column( name = "metadata_user_name" )
  protected String          ownerUserName;

  /**
   * GRZE:NOTE: Should only /ever/ be used by sub classes.
   */
  protected UserMetadata( ) {}

  /**
   * GRZE:NOTE: Should only /ever/ be used by sub classes.
   */
  protected UserMetadata( final OwnerFullName owner ) {
    super( owner );
    this.setOwner( owner );
  }

  /**
   * GRZE:NOTE: Should only /ever/ be used by sub classes.
   */
  protected UserMetadata( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
    this.setOwner( owner );
  }

  @Override
  public void setOwner( final OwnerFullName owner ) {
    super.ownerFullNameCached = null;
    this.setOwnerUserId( owner != null
      ? owner.getUserId( )
      : null );
    this.setOwnerUserName( owner != null
      ? owner.getUserName( )
      : null );
    super.setOwner( owner );
  }

  @Override
  public OwnerFullName getOwner( ) {
    if ( super.ownerFullNameCached != null ) {
      return super.ownerFullNameCached;
    } else if ( this.getOwnerUserId( ) != null ) {
      OwnerFullName tempOwner = null;
      if ( Principals.nobodyFullName( ).getUserId( ).equals( this.getOwnerUserId( ) ) ) {
        tempOwner = Principals.nobodyFullName( );
      } else if ( Principals.systemFullName( ).getUserId( ).equals( this.getOwnerUserId( ) ) ) {
        tempOwner = Principals.systemFullName( );
      } else {
        tempOwner = UserFullName.getInstanceForAccount( this.getOwnerAccountNumber( ), this.getOwnerUserId( ) );
      }
      return ( super.ownerFullNameCached = tempOwner );
    } else {
      return super.getOwner( );
    }
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = ( prime * result ) + ( ( this.ownerUserId == null )
      ? 0
      : this.ownerUserId.hashCode( ) );
    return result;
  }

  @Override
  public String getOwnerUserId( ) {
    return this.ownerUserId;
  }

  public void setOwnerUserId( final String ownerUserId ) {
    this.ownerUserId = ownerUserId;
  }

  @Override
  public String getOwnerUserName( ) {
    return this.ownerUserName;
  }

  public void setOwnerUserName( final String ownerUserName ) {
    this.ownerUserName = ownerUserName;
  }

  @PrePersist
  @PreUpdate
  public void verifyComplete( ) {
    checkParam( this.ownerUserId, notNullValue() );
    checkParam( this.ownerUserName, notNullValue() );
  }
}
