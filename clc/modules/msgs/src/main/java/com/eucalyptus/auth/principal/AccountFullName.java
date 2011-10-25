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

package com.eucalyptus.auth.principal;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.OwnerFullName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class AccountFullName implements OwnerFullName {
  private static Logger       LOG    = Logger.getLogger( UserFullName.class );
  private static final String VENDOR = "euare";
  private final String        accountNumber;
  private final String        accountName;
  private final String        authority;
  private final String        relativeId;
  private final String        qName;
  
  protected AccountFullName( OwnerFullName ownerFn, String... relativePath ) {
    assertThat( ownerFn, notNullValue( ) );
    this.accountNumber = ownerFn.getAccountNumber( );
    assertThat( this.accountNumber, notNullValue( ) );
    this.accountName = ownerFn.getAccountName( );
    this.authority = ownerFn.getAuthority( );
    this.relativeId = FullName.ASSEMBLE_PATH_PARTS.apply( relativePath );
    this.qName = this.authority + this.relativeId;
  }
  
  protected AccountFullName( Account account, String... relativePath ) {
    this.accountNumber = account.getAccountNumber( );
    assertThat( this.accountNumber, notNullValue( ) );
    this.accountName = account.getName( );
    this.authority = new StringBuilder( ).append( FullName.PREFIX ).append( FullName.SEP ).append( VENDOR ).append( FullName.SEP ).append( FullName.SEP ).append( this.accountNumber ).append( FullName.SEP ).toString( );
    this.relativeId = FullName.ASSEMBLE_PATH_PARTS.apply( relativePath );
    this.qName = this.authority + this.relativeId;
  }
  
  @Override
  public String getAccountNumber( ) {
    return this.accountNumber;
  }
  
  @Override
  public final String getVendor( ) {
    return VENDOR;
  }
  
  @Override
  public final String getRegion( ) {
    return EMPTY;
  }
  
  @Override
  public final String getNamespace( ) {
    return this.accountNumber;
  }
  
  @Override
  public final String getRelativeId( ) {
    return this.relativeId;
  }
  
  @Override
  public String getAuthority( ) {
    return this.authority;
  }
  
  @Override
  public final String getPartition( ) {
    return this.accountNumber;
  }
  
  @Override
  public String toString( ) {
    return this.qName;
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.accountNumber == null )
      ? 0
      : this.accountNumber.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( obj == null ) {
      return false;
    }
    if ( getClass( ) != obj.getClass( ) ) {
      return false;
    }
    if ( obj instanceof OwnerFullName ) {
      OwnerFullName that = ( OwnerFullName ) obj;
      return this.getAccountNumber( ).equals( that.getAccountNumber( ) );
    } else {
      return false;
    }
  }
  
  @Override
  public String getUniqueId( ) {
    return this.accountNumber;
  }
  
  public static AccountFullName getInstance( String accountId, String... relativePath ) {
    Account account = null;
    try {
      account = Accounts.lookupAccountById( accountId );
    } catch ( AuthException ex ) {
      LOG.error( ex, ex );
    }
    return getInstance( account, relativePath );
  }
  
  public static AccountFullName getInstance( Account account, String... relativePath ) {
    if ( account == null ) {
      return new AccountFullName( Principals.nobodyAccount( ), relativePath );
    } else if ( account == Principals.systemUser( ) ) {
      return new AccountFullName( Principals.nobodyAccount( ), relativePath );
    } else {
      return new AccountFullName( account, relativePath );
    }
  }
  
  @Override
  public String getUserId( ) {
    return null;
  }
  
  @Override
  public String getUserName( ) {
    return null;
  }
  
  @Override
  public String getAccountName( ) {
    return this.accountName;
  }
  
  /**
   * @see com.eucalyptus.util.OwnerFullName#isOwner(java.lang.String)
   */
  @Override
  public boolean isOwner( String ownerId ) {
    return this.getAccountNumber( ) != null && this.getAccountNumber( ).equals( ownerId );
  }
  
  /**
   * @see com.eucalyptus.util.OwnerFullName#isOwner(com.eucalyptus.util.OwnerFullName)
   */
  @Override
  public boolean isOwner( OwnerFullName ownerFullName ) {
    return false;
  }
  
}
