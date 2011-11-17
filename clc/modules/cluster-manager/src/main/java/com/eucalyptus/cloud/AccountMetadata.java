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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.cloud;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.Transient;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.cloud.CloudMetadata.KeyPairMetadata;
import com.eucalyptus.entities.AbstractStatefulPersistent;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedType.AccountRestrictedType;

@MappedSuperclass
public abstract class AccountMetadata<STATE extends Enum<STATE>> extends AbstractStatefulPersistent<STATE> implements AccountRestrictedType, HasFullName<AccountMetadata> {
  @Column( name = "metadata_account_id" )
  private String          ownerAccountNumber;
  @Column( name = "metadata_account_name" )
  private String          ownerAccountName;
  @Column( name = "metadata_unique_name", unique = true, nullable = false )
  private String          uniqueName;
  @Transient
  protected OwnerFullName ownerFullNameCached = null;
  
  /**
   * GRZE:NOTE: Should only /ever/ be used by sub classes.
   */
  protected AccountMetadata( ) {}
  
  /**
   * GRZE:NOTE: Should only /ever/ be used by sub classes.
   */
  protected AccountMetadata( OwnerFullName owner ) {
    this.ownerAccountNumber = owner != null
      ? owner.getAccountNumber( )
      : null;
    this.ownerAccountName = owner != null
      ? owner.getAccountName( )
      : null;
  }
  
  public AccountMetadata( OwnerFullName owner, String displayName ) {
    super( displayName );
    this.ownerAccountNumber = owner != null
      ? owner.getAccountNumber( )
      : null;
    this.ownerAccountName = owner != null
      ? owner.getAccountName( )
      : null;
  }
  
  public OwnerFullName getOwner( ) {
    if ( this.ownerFullNameCached != null ) {
      return this.ownerFullNameCached;
    } else if ( this.getOwnerAccountNumber( ) != null ) {
      OwnerFullName tempOwner;
      if ( Principals.nobodyFullName( ).getAccountNumber( ).equals( this.getOwnerAccountNumber( ) ) ) {
        tempOwner = Principals.nobodyFullName( );
      } else if ( Principals.systemFullName( ).getAccountNumber( ).equals( this.getOwnerAccountNumber( ) ) ) {
        tempOwner = Principals.systemFullName( );
      } else {
        tempOwner = AccountFullName.getInstance( this.getOwnerAccountNumber( ) );
      }
      return ( this.ownerFullNameCached = tempOwner );
    } else {
      throw new RuntimeException( "Failed to identify user with id " + this.ownerAccountNumber + " something has gone seriously wrong." );
    }
  }
  
  @Override
  public String getOwnerAccountNumber( ) {
    return this.ownerAccountNumber;
  }
  
  protected void setOwnerAccountNumber( String ownerAccountId ) {
    this.ownerAccountNumber = ownerAccountId;
  }
  
  protected void setOwner( OwnerFullName owner ) {
    this.ownerFullNameCached = null;
    this.setOwnerAccountNumber( owner != null
      ? owner.getAccountNumber( )
      : null );
    this.setOwnerAccountName( owner != null
      ? owner.getAccountName( )
      : null );
  }
  
  public String getOwnerAccountName( ) {
    return this.ownerAccountName;
  }
  
  public void setOwnerAccountName( String ownerAccountName ) {
    this.ownerAccountName = ownerAccountName;
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
  
  private String createUniqueName( ) {
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
    AccountMetadata other = ( AccountMetadata ) obj;
    if ( this.uniqueName == null ) {
      if ( other.uniqueName != null ) {
        return false;
      }
    } else if ( !this.uniqueName.equals( other.uniqueName ) ) {
      return false;
    }
    return true;
  }
  
  @Override
  public int compareTo( AccountMetadata that ) {
    return this.getFullName( ).toString( ).compareTo( that.getFullName( ).toString( ) );
  }
  
}
