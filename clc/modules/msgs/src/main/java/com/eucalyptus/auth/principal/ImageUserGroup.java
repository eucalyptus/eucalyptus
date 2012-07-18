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

import java.util.ArrayList;
import java.util.List;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyParseException;

/**
 * Dummy class for image user group. Only "all" is supported in AWS.
 */
public class ImageUserGroup implements Group {
  
  public static final ImageUserGroup ALL = new ImageUserGroup( "all" );
  
  private String name;
  
  public ImageUserGroup( String name ) {
    this.name = name;
  }

  @Override
  public String getGroupId( ) {
    return null;
  }

  @Override
  public String getName( ) {
    return this.name;
  }

  @Override
  public void setName( String name ) throws AuthException {
    this.name = name;
  }

  @Override
  public String getPath( ) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setPath( String path ) throws AuthException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Boolean isUserGroup( ) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setUserGroup( Boolean userGroup ) throws AuthException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Account getAccount( ) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<User> getUsers( ) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void addUserByName( String userName ) throws AuthException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void removeUserByName( String userName ) throws AuthException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public List<Policy> getPolicies( ) {
    // TODO Auto-generated method stub
    return new ArrayList<Policy>( );
  }

  @Override
  public void removePolicy( String policyId ) throws AuthException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean hasUser( String userName ) throws AuthException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Policy addPolicy( String name, String policy ) throws AuthException, PolicyParseException {
    // TODO Auto-generated method stub
    return null;
  }
  
}
