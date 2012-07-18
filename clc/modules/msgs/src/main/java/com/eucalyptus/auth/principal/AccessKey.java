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
 ************************************************************************/

package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.util.Date;
import com.eucalyptus.auth.AuthException;

public interface AccessKey extends /*HasId, */Serializable {

  public Boolean isActive( );
  public void setActive( Boolean active ) throws AuthException;
  
  public String getAccessKey( );
  public String getSecretKey( );
  
  /*
   * Returns the SECRET key -- also available via longer named {@link#getSecretKey()}
   * @deprecated {@link #getSecretKey()}
   * @see {@link com.eucalyptus.auth.Accounts}
   */
//  public String getKey( );
//  public void setKey( String key ) throws AuthException; TODO:YE:  AccessKey should be immutable.  Is there a reason to allow mutator access?
  
  public Date getCreateDate( );
  public void setCreateDate( Date createDate ) throws AuthException;
  
  public User getUser( ) throws AuthException;
  
}
