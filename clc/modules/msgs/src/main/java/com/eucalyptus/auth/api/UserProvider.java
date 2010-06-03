package com.eucalyptus.auth.api;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.List;
import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.UserExistsException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.util.Tx;

/**
 * @author decker
 *
 */
public interface UserProvider {
  /**
   * Get the user object given an X509Certificate
   * 
   * @param cert
   * @return
   * @throws GeneralSecurityException
   */
  public abstract User lookupQueryId( String queryId ) throws NoSuchUserException;
  
  /**
   * Get the user object given an X509Certificate
   * 
   * @param cert
   * @return
   * @throws GeneralSecurityException
   */
  public abstract User lookupCertificate( X509Certificate cert ) throws NoSuchUserException;
  
  /**
   * Get the user object given a user name
   * 
   * @param userName
   * @return
   * @throws NoSuchUserException
   */
  public abstract User lookupUser( String userName ) throws NoSuchUserException;
  
  /**
   * Get a list of all enabled users.
   * 
   * @return
   */
  public abstract List<User> listEnabledUsers( );
  
  /**
   * Get a list of all known users.
   * 
   * @return
   */
  public abstract List<User> listAllUsers( );
  
  /**
   * Add a user with system generated query ID, secret key, session tokens, etc.
   * 
   * @param userName
   * @param admin
   * @param enabled
   * @return
   * @throws UserExistsException
   *           if the user exists. User
   * @throws UnsupportedOperationException
   */
  public abstract User addUser( String userName, Boolean admin, Boolean enabled ) throws UserExistsException, UnsupportedOperationException;
  
  /**
   * Delete the user with the given userName
   * 
   * @param userName
   * @throws NoSuchUserException
   * @throws UnsupportedOperationException
   */
  public abstract void deleteUser( String userName ) throws NoSuchUserException, UnsupportedOperationException;

  /**
   * TODO: DOCUMENT UserProvider.java
   * @param cert
   * @return
   * @throws NoSuchCertificateException
   */
  public abstract boolean checkRevokedCertificate( X509Certificate cert ) throws NoSuchCertificateException;
  
  /**
   * Update a user via an external callback.
   * 
   * @param userName
   * @param userTx
   * @throws NoSuchUserException
   */
  public abstract void updateUser( String userName, Tx<User> userTx ) throws NoSuchUserException;
}