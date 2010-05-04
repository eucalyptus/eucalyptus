package com.eucalyptus.auth.principal;

import java.util.List;
import com.eucalyptus.auth.principal.domain.UserDomain;

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
  public List<User> getUsers( );
  
  /**
   * TODO: DOCUMENT Group.java
   * @return
   */
  public List<Authorization> getAuthorizations( );
  
  /**
   * TODO: DOCUMENT Group.java
   * @param auth
   */
  public void addAuthorization( Authorization auth );
  
  /**
   * TODO: DOCUMENT Group.java
   * @param auth
   */
  public void removeAuthorization( Authorization auth );
  
}
