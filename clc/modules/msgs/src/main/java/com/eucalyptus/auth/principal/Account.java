package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.eucalyptus.auth.AuthException;

/**
 * The interface for the user account.
 * 
 * @author wenye
 *
 */
public interface Account extends HasId, BasePrincipal, Serializable {
  
  public static final String NOBODY_ACCOUNT = "nobody";
  public static final String SYSTEM_ACCOUNT = "eucalyptus";//NOTE: for now this has to be the name as it is has to be the same as Eucalyptus.name()

  public void setName( String name ) throws AuthException;
  
  public List<User> getUsers( ) throws AuthException;
  
  public List<Group> getGroups( ) throws AuthException;
  
  public User addUser( String userName, String path, boolean skipRegistration, boolean enabled, Map<String, String> info ) throws AuthException;
  public void deleteUser( String userName, boolean forceDeleteAdmin, boolean recursive ) throws AuthException;
  
  public Group addGroup( String groupName, String path ) throws AuthException;
  public void deleteGroup( String groupName, boolean recursive ) throws AuthException;
  
  public Group lookupGroupByName( String groupName ) throws AuthException;
  
  public User lookupUserByName( String userName ) throws AuthException;
  
  public List<Authorization> lookupAccountGlobalAuthorizations( String resourceType ) throws AuthException;
  public List<Authorization> lookupAccountGlobalQuotas( String resourceType ) throws AuthException;
  
}
