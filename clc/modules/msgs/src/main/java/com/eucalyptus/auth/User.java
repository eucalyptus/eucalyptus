package com.eucalyptus.auth;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.List;

public abstract interface User extends Principal, java.io.Serializable {
  public abstract String getUserName( );
  public abstract Boolean getIsAdministrator( );
  public abstract void setIsAdministrator( Boolean admin );
  public abstract Boolean getIsEnabled( );
  public abstract void setIsEnabled( Boolean enabled );
  public abstract List<X509Certificate> getX509Certificates( );
  public abstract List<String> getCertificateAliases( );
  public abstract String getQueryId( );
  public abstract String getSecretKey( );
  /**
   * Compares this principal to the specified object.  Returns true
   * if the object passed in matches the principal represented by
   * the implementation of this interface.
   *
   * @param another principal to compare with.
   *
   * @return true if the principal passed in is the same as that
   * encapsulated by this principal, and false otherwise.

   */
  public abstract boolean equals(Object another);

  /**
   * Returns a string representation of this principal.
   *
   * @return a string representation of this principal.
   */
  public abstract String toString();

  /**
   * Returns a hashcode for this principal.
   *
   * @return a hashcode for this principal.
   */
  public abstract int hashCode();

  /**
   * Returns the name of this principal.
   *
   * @return the name of this principal.
   */
  public abstract String getName();
  
}
