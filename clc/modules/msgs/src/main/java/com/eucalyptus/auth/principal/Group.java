package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.util.List;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyParseException;

/**
 * 
 * @author wenye
 *
 */
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
