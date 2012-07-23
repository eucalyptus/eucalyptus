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
import java.util.List;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyParseException;

public interface Group extends /*HasId, */BasePrincipal, Serializable {

  public String getGroupId( );
  public void setName( String name ) throws AuthException;
  
  public String getPath( );
  public void setPath( String path ) throws AuthException;
  
  public Boolean isUserGroup( );
  public void setUserGroup( Boolean userGroup ) throws AuthException;
  
  public Account getAccount( ) throws AuthException;
  
  public List<User> getUsers( ) throws AuthException;
  public boolean hasUser( String userName ) throws AuthException;
  public void addUserByName( String userName ) throws AuthException;
  public void removeUserByName( String userName ) throws AuthException; 
  
  public List<Policy> getPolicies( ) throws AuthException;
  public Policy addPolicy( String name, String policy ) throws AuthException, PolicyParseException;
  public void removePolicy( String name ) throws AuthException;
  
}
