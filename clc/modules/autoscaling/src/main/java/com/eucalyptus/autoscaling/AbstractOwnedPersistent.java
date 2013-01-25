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
package com.eucalyptus.autoscaling;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.Transient;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.util.OwnerFullName;

/**
 *
 */
@MappedSuperclass
public class AbstractOwnedPersistent extends AbstractPersistent {
  private static final long serialVersionUID = 1L;
  
  @Column( name = "metadata_display_name" )
  protected String          displayName;

  @Column( name = "metadata_account_id" )
  private String          ownerAccountNumber;
  @Column( name = "metadata_unique_name", unique = true, nullable = false )
  private String          uniqueName;
  @Transient
  protected OwnerFullName ownerFullNameCached = null;

  @Column( name = "metadata_user_id" )
  protected String          ownerUserId;
  @Column( name = "metadata_user_name" )
  protected String          ownerUserName;

  protected AbstractOwnedPersistent() {    
  }

  protected AbstractOwnedPersistent( final OwnerFullName owner ) {
    setOwner( owner );
  }

  protected AbstractOwnedPersistent( final OwnerFullName owner,
                                     final String displayName ) {
    this( owner );
    setDisplayName( displayName );
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName( final String displayName ) {
    this.displayName = displayName;
  }

  public String getOwnerAccountNumber() {
    return ownerAccountNumber;
  }

  public void setOwnerAccountNumber( final String ownerAccountNumber ) {
    this.ownerAccountNumber = ownerAccountNumber;
  }

  public OwnerFullName getOwnerFullNameCached() {
    return ownerFullNameCached;
  }

  public void setOwnerFullNameCached( final OwnerFullName ownerFullNameCached ) {
    this.ownerFullNameCached = ownerFullNameCached;
  }

  public String getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId( final String ownerUserId ) {
    this.ownerUserId = ownerUserId;
  }

  public String getOwnerUserName() {
    return ownerUserName;
  }

  public void setOwnerUserName( final String ownerUserName ) {
    this.ownerUserName = ownerUserName;
  }

  public OwnerFullName getOwner( ) {
    if ( this.ownerFullNameCached != null ) {
      return this.ownerFullNameCached;
    } else if ( this.getOwnerAccountNumber( ) != null ) {
      OwnerFullName tempOwner;
      if ( Principals.nobodyFullName().getAccountNumber( ).equals( this.getOwnerAccountNumber( ) ) ) {
        tempOwner = Principals.nobodyFullName( );
      } else if ( Principals.systemFullName( ).getAccountNumber( ).equals( this.getOwnerAccountNumber( ) ) ) {
        tempOwner = Principals.systemFullName( );
      } else {
        tempOwner = AccountFullName.getInstance( this.getOwnerAccountNumber() );
      }
      return ( this.ownerFullNameCached = tempOwner );
    } else {
      throw new RuntimeException( "Failed to identify user with id " + this.ownerAccountNumber + " something has gone seriously wrong." );
    }
  }

  protected void setOwner( OwnerFullName owner ) {
    this.ownerFullNameCached = null;
    this.setOwnerAccountNumber( owner != null
        ? owner.getAccountNumber( )
        : null );
  }

  protected String getUniqueName( ) {
    if ( this.uniqueName == null ) {
      return this.uniqueName = this.createUniqueName( );
    } else {
      return this.uniqueName;
    }
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
