package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.security.Principal;

public interface BasePrincipal extends Principal, Serializable {
  /**
   * Returns the name of this principal.
   * 
   * @return the name of this principal.
   */
  public abstract String getName( );
  
  /**
   * Compares this principal to the specified object. Returns true if the object passed in matches the principal represented by the implementation of this
   * interface.
   * 
   * @param another
   *          principal to compare with.
   * @return true if the principal passed in is the same as that encapsulated by this principal, and false otherwise.
   */
  public abstract boolean equals( Object another );
  
  /**
   * Returns a string representation of this principal.
   * 
   * @return a string representation of this principal.
   */
  public abstract String toString( );
  
  /**
   * Returns a hashcode for this principal.
   * 
   * @return a hashcode for this principal.
   */
  public abstract int hashCode( );
}
