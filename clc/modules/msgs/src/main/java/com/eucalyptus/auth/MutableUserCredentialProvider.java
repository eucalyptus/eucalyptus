package com.eucalyptus.auth;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

public interface MutableUserCredentialProvider {
  /**
   * Add the given certificate, <tt>cert</tt> to the list for the user with named <tt>userName</tt> as <tt>alias</tt>
   * 
   * @param userName
   * @param alias
   * @param cert
   * @throws GeneralSecurityException
   * @throws UnsupportedOperationException
   *           if the operation is unsupported by the implementation
   */
  public abstract User addCertificate( final String userName, final String alias, final X509Certificate cert ) throws GeneralSecurityException, UnsupportedOperationException;
  /**
   * Add a user who is enabled. Generates the needed query ID, secret key, etc.
   * 
   * @param userName
   * @param admin
   * @param queryId
   * @param secretKey
   * @return
   * @throws UserExistsException
   * @throws UnsupportedOperationException
   */
  public abstract User addUser( String userName, Boolean admin, Boolean enabled ) throws UserExistsException, UnsupportedOperationException;
  /**
   * Add a user.
   * @param userName
   * @param isAdmin
   * @param isEnabled
   * @param secretKey
   * @param queryId
   * @return
   * @throws UserExistsException if the user exists.
   * User
   */
  public abstract User addUser( String userName, Boolean isAdmin, Boolean isEnabled, String secretKey, String queryId ) throws UserExistsException;
  /**
   * Delete the user with the given userName
   * 
   * @param userName
   * @throws NoSuchUserException
   * @throws UnsupportedOperationException
   */
  public abstract User deleteUser( String userName ) throws NoSuchUserException, UnsupportedOperationException;
  /**
   * Change the admin or enabled state of the user with the given userName
   * 
   * @param userName
   * @param enabled
   * @throws NoSuchUserException
   * @throws UnsupportedOperationException
   */
  public abstract User updateUser( String userName, Boolean admin, Boolean enabled ) throws NoSuchUserException, UnsupportedOperationException;
  /**
   * Change the admin or enabled state of the user with the given userName
   * 
   * @param userName
   * @param enabled
   * @throws NoSuchUserException
   * @throws UnsupportedOperationException
   */
  public abstract User resetUser( String userName, Boolean admin, Boolean enabled ) throws NoSuchUserException, UnsupportedOperationException;
  /**
   * Reset the query credentials for the user name <tt>userName</tt>
   * @param userName
   * @throws NoSuchUserException
   * @throws UnsupportedOperationException
   */
  public abstract User resetUserQueryKeys( String userName ) throws NoSuchUserException, UnsupportedOperationException;
  public abstract User revokeCertificate( String userName, String alias ) throws NoSuchUserException, NoSuchCertificateException;
}
