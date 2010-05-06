package com.eucalyptus.auth.principal;

import com.eucalyptus.auth.principal.domain.UserDomain;
import com.google.common.collect.ImmutableList;

/**
 * @author decker
 *
 */
public interface Group extends java.security.acl.Group, Cloneable, UserDomain {
  /**
   * @see java.security.Principal#getName()
   * @return
   */
  public abstract String getName( );
  
  /**
   * TODO: DOCUMENT Group.java
   * @return
   */
  public ImmutableList<User> getMembers( );
  
  /**
   * TODO: DOCUMENT Group.java
   * @return
   */
  public ImmutableList<Authorization> getAuthorizations( );
  
  /**
   * TODO: DOCUMENT Group.java
   * @param auth
   */
  public boolean addAuthorization( Authorization auth );
  
  /**
   * TODO: DOCUMENT Group.java
   * @param auth
   */
  public boolean removeAuthorization( Authorization auth );
  
}
