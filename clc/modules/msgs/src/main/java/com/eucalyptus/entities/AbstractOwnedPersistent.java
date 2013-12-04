/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 ************************************************************************/
package com.eucalyptus.entities;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.Transient;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedType;

/**
 *
 */
@MappedSuperclass
public class AbstractOwnedPersistent extends AbstractPersistent implements RestrictedType.AccountRestrictedType, RestrictedType.UserRestrictedType {
  private static final long serialVersionUID = 1L;
  
  @Column( name = "metadata_display_name" )
  protected String          displayName;

  @Column( name = "metadata_account_id" )
  private String          ownerAccountNumber;
  @Column( name = "metadata_unique_name", unique = true, nullable = false, length = 600)
  private String          uniqueName;
  @Transient
  protected OwnerFullName ownerFullNameCached = null;

  @Column( name = "metadata_user_id" )
  protected String          ownerUserId;
  @Column( name = "metadata_user_name" )
  protected String          ownerUserName;

  protected AbstractOwnedPersistent() {    
  }

  protected AbstractOwnedPersistent( @Nullable final OwnerFullName owner ) {
    setOwner( owner );
  }

  protected AbstractOwnedPersistent( final OwnerFullName owner,
                                     final String displayName ) {
    this( owner );
    setDisplayName( displayName );
  }

  public void clearUserIdentity() {
    this.ownerUserId = null;
    this.ownerUserName = null;
  }
  
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName( final String displayName ) {
    this.displayName = displayName;
  }

  @Override
  public String getOwnerAccountNumber() {
    return ownerAccountNumber;
  }

  public void setOwnerAccountNumber( final String ownerAccountNumber ) {
    this.ownerAccountNumber = ownerAccountNumber;
  }

  @Override
  public String getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId( final String ownerUserId ) {
    this.ownerUserId = ownerUserId;
  }

  @Override
  public String getOwnerUserName() {
    return ownerUserName;
  }

  public void setOwnerUserName( final String ownerUserName ) {
    this.ownerUserName = ownerUserName;
  }

  public OwnerFullName getOwner( ) {
    if ( this.ownerFullNameCached != null ) {
      return this.ownerFullNameCached;
    } else if ( this.getOwnerUserId() != null ) {
      final OwnerFullName tempOwner;
      if ( Principals.nobodyFullName( ).getUserId().equals( this.getOwnerUserId() ) ) {
        tempOwner = Principals.nobodyFullName( );
      } else if ( Principals.systemFullName( ).getUserId().equals( this.getOwnerUserId() ) ) {
        tempOwner = Principals.systemFullName( );
      } else {
        tempOwner = UserFullName.getInstance( this.getOwnerUserId() );
      }
      return ( this.ownerFullNameCached = tempOwner );
    } else if ( this.getOwnerAccountNumber( ) != null ) {
      final OwnerFullName tempOwner;
      if ( Principals.nobodyFullName().getAccountNumber( ).equals( this.getOwnerAccountNumber( ) ) ) {
        tempOwner = Principals.nobodyFullName( );
      } else if ( Principals.systemFullName( ).getAccountNumber( ).equals( this.getOwnerAccountNumber( ) ) ) {
        tempOwner = Principals.systemFullName( );
      } else {
        tempOwner = AccountFullName.getInstance( this.getOwnerAccountNumber() );
      }
      return ( this.ownerFullNameCached = tempOwner );
    } else {
      throw new RuntimeException( "Failed to identify resource owner" );
    }
  }

  public void setOwner( @Nullable OwnerFullName owner ) {
    this.ownerFullNameCached = null;
    this.setOwnerAccountNumber( owner != null
        ? owner.getAccountNumber( )
        : null );
    this.setOwnerUserId( owner != null
        ? owner.getUserId( )
        : null );
    this.setOwnerUserName( owner != null
        ? owner.getUserName( )
        : null );
  }

  protected String getUniqueName( ) {
    return this.uniqueName;
  }

  protected void setUniqueName( String uniqueName ) {
    this.uniqueName = uniqueName;
  }

  @PrePersist
  private void generateOnCommit( ) {
    this.uniqueName = createUniqueName( );
  }

  protected String createUniqueName( ) {
    return ( this.ownerAccountNumber != null && this.getDisplayName( ) != null )
        ? this.ownerAccountNumber + ":" + this.getDisplayName( )
        : null;
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 0;
    result = prime * result + ( ( this.uniqueName == null )
        ? 0
        : this.uniqueName.hashCode( ) );
    return result;
  }

  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( getClass( ) != obj.getClass( ) ) {
      return false;
    }
    AbstractOwnedPersistent other = ( AbstractOwnedPersistent ) obj;
    if ( this.uniqueName == null ) {
      if ( other.uniqueName != null ) {
        return false;
      }
    } else if ( !this.uniqueName.equals( other.uniqueName ) ) {
      return false;
    }
    return true;
  }  
}
