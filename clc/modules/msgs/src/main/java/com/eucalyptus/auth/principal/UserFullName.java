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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;

public class UserFullName implements OwnerFullName {
  private static final long serialVersionUID = 1L;
  private static ConcurrentMap<String, UserFullName> userIdMap = new MapMaker( ).softValues( ).makeMap( );
  private static Logger                              LOG       = Logger.getLogger( UserFullName.class );
  private static final String                        VENDOR    = "euare";
  private final String                               userId;
  private final String                               userName;
  private final String                               accountNumber;
  private final String                               accountName;
  private final String                                     authority;
  private final String                                     relativeId;
  String                                             qName;
  
  private UserFullName( final User user ) throws AuthException {
    this.userId = user.getUserId( );
    assertThat( this.userId, notNullValue( ) );
    this.userName = user.getName( );
    final Account account = user.getAccount( );
    this.accountNumber = account.getAccountNumber( );
    assertThat( this.accountNumber, notNullValue( ) );
    this.accountName = account.getName( );
    this.authority = new StringBuilder( ).append( FullName.PREFIX ).append( FullName.SEP ).append( VENDOR ).append( FullName.SEP ).append( FullName.SEP ).append( this.accountNumber ).append( FullName.SEP ).toString( );
    this.relativeId = FullName.ASSEMBLE_PATH_PARTS.apply( new String[] { "user", user.getName( ) } );
    this.qName = this.authority + this.relativeId;
  }
  
  public static UserFullName getInstance( final String userId, final String... relativePath ) {
    if ( userIdMap.containsKey( userId ) ) {
      return userIdMap.get( userId );
    } else {
      try {
        userIdMap.put( userId, getInstance( Accounts.lookupUserById( userId ), relativePath ) );
        return userIdMap.get( userId );
      } catch ( final AuthException ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }
  }
  
  public static UserFullName getInstance( final User user, final String... relativePath ) {
    try {
      if ( ( user != null ) && !Principals.isFakeIdentify( user.getUserId( ) ) ) {
        if ( !userIdMap.containsKey( user.getUserId( ) ) ) {
          userIdMap.put( user.getUserId( ), new UserFullName( user ) );
        }
        return userIdMap.get( user.getUserId( ) );
      } else if ( Principals.systemUser( ).equals( user ) ) {
        return new UserFullName( Principals.systemUser( ) );
      } else {
        return new UserFullName( Principals.nobodyUser( ) );
      }
    } catch ( final AuthException ex ) {
      LOG.error( ex.getMessage( ) );
      try {
        return new UserFullName( Principals.nobodyUser( ) );
      } catch ( final AuthException ex1 ) {
        LOG.error( ex1, ex1 );
        throw Exceptions.toUndeclared( ex );
      }
    } catch ( final Exception ex ) {
      throw Exceptions.toUndeclared( ex );
    }
  }
  
  @Override
  public String getUniqueId( ) {
    return this.userId;
  }
  
  @Override
  public String getUserId( ) {
    return this.userId;
  }
  
  @Override
  public String getUserName( ) {
    return this.userName;
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( this.userId == null )
      ? 0
      : this.userId.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( final Object obj ) {
    if ( this == obj ) return true;
    if ( !super.equals( obj ) ) return false;
    if ( this.getClass( ) != obj.getClass( ) ) return false;
    if ( obj instanceof UserFullName ) {
      final UserFullName other = ( UserFullName ) obj;
      if ( this.userId == null ) {
        if ( other.userId != null ) return false;
      } else if ( !this.userId.equals( other.userId ) ) return false;
    } else if ( obj instanceof OwnerFullName ) {
      final OwnerFullName that = ( OwnerFullName ) obj;
      if ( this.getAccountNumber( ) != null ) {
        if ( this.getUserId( ) != null ) {
          return this.getAccountNumber( ).equals( that.getAccountNumber( ) ) && this.getUserId( ).equals( that.getUserId( ) );
        } else if ( this.getUserName( ) != null ) {
          return this.getAccountNumber( ).equals( that.getAccountNumber( ) ) && this.getUserName( ).equals( that.getUserName( ) );
        }
      } else {
        if ( this.getUserId( ) != null ) {
          return this.getAccountNumber( ).equals( that.getAccountNumber( ) ) && this.getUserId( ).equals( that.getUserId( ) );
        } else if ( this.getUserName( ) != null ) {
          return this.getAccountNumber( ).equals( that.getAccountNumber( ) ) && this.getUserName( ).equals( that.getUserName( ) );
        }
      }
    }
    return true;
  }
  
  @Override
  public String getNamespace( ) {
    return this.accountNumber;
  }
  
  @Override
  public String toString( ) {
    return this.qName;
  }
  
  @Override
  public String getAccountNumber( ) {
    return this.accountNumber;
  }
  
  @Override
  public String getAccountName( ) {
    return this.accountName;
  }
  
  @Override
  public String getAuthority( ) {
    return this.authority;
  }
  
  @Override
  public final String getRelativeId( ) {
    return this.relativeId;
  }
  
  @Override
  public final String getPartition( ) {
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
  
  /**
   * @see com.eucalyptus.util.OwnerFullName#isOwner(java.lang.String)
   */
  @Override
  public boolean isOwner( final String ownerId ) {
    return this.userId.equals( ownerId ) || this.accountNumber.equals( ownerId );
  }
  
  /**
   * @see com.eucalyptus.util.OwnerFullName#isOwner(com.eucalyptus.util.OwnerFullName)
   */
  @Override
  public boolean isOwner( final OwnerFullName ownerFullName ) {
    return this.userId.equals( ownerFullName.getAccountNumber( ) ) || this.accountNumber.equals( ownerFullName.getAccountNumber( ) );
  }
  
  public AccountFullName asAccountFullName( ) {
    return AccountFullName.getInstance( this.getAccountNumber( ) );
  }
}
