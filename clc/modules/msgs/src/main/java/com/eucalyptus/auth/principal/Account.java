package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.eucalyptus.auth.AuthException;
import com.google.common.collect.Lists;

/**
 * The interface for the user account.
 * 
 * @author wenye
 *
 */
public interface Account extends HasId, BasePrincipal, Serializable {
  
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
  
  public static final Account SYSTEM = new Account( ) {
    @Override public String getId( ) { return "0"; }
    @Override public String getName( ) { return SYSTEM_ACCOUNT; }
    @Override public void setName( String name ) throws AuthException {}
    @Override public List<User> getUsers( ) throws AuthException { return Lists.newArrayList( User.SYSTEM ); }
    @Override public List<Group> getGroups( ) throws AuthException { return Lists.newArrayList( ); };
    @Override public User addUser( String userName, String path, boolean skipRegistration, boolean enabled, Map<String, String> info ) throws AuthException { throw new AuthException( AuthException.SYSTEM_MODIFICATION ); }
    @Override public void deleteUser( String userName, boolean forceDeleteAdmin, boolean recursive ) throws AuthException {}
    @Override public Group addGroup( String groupName, String path ) throws AuthException { throw new AuthException( AuthException.SYSTEM_MODIFICATION ); }
    @Override public void deleteGroup( String groupName, boolean recursive ) throws AuthException {}
    @Override public Group lookupGroupByName( String groupName ) throws AuthException { throw new AuthException( AuthException.SYSTEM_MODIFICATION );/** TODO:GRZE:YE really what goes here? **/ }
    @Override public User lookupUserByName( String userName ) throws AuthException { if( User.SYSTEM.getName( ).equals( userName ) ) { return User.SYSTEM; } else { throw new AuthException( AuthException.SYSTEM_MODIFICATION ); } }
    @Override public List<Authorization> lookupAccountGlobalAuthorizations( String resourceType ) throws AuthException { /** TODO:GRZE:YE is there an allow all? **/ return null; }
    @Override public List<Authorization> lookupAccountGlobalQuotas( String resourceType ) throws AuthException { /** TODO:GRZE:YE is there an unlimited? **/ return null; }
  };
}
