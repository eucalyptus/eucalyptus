package com.eucalyptus.auth;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.List;

public interface ImmutableUserCredentialProvider {
  /**
   * Check to see if the specified alias has a corresponding certificate. TODO: this should likely be the dn
   * 
   * @param alias
   * @return
   */
  public abstract boolean hasCertificate( final String alias );
  /**
   * Get the X509 certiticate corresponding to the alias. TODO: this should likely be the dn
   * 
   * @param alias
   * @return
   * @throws GeneralSecurityException
   */
  public abstract X509Certificate getCertificate( final String alias ) throws GeneralSecurityException;
  /**
   * Construct a DN given a userName.
   * 
   * @param userName
   * @return
   */
  public abstract String getDName( final String userName );
  /**
   * Get the alias corresponding to a PEM-string of an X509 certificate TODO: this should likely be the dn
   * 
   * @param certPem
   * @return
   * @throws GeneralSecurityException
   */
  public abstract String getCertificateAlias( final String certPem ) throws GeneralSecurityException;
  public abstract String getCertificateAlias( final X509Certificate cert ) throws GeneralSecurityException;
  /**
   * Get the user's query ID
   * 
   * @param userName
   * @return
   * @throws GeneralSecurityException
   */
  public abstract String getQueryId( String userName ) throws GeneralSecurityException;
  /**
   * Get the user's secret key
   * 
   * @param queryId
   * @return
   * @throws GeneralSecurityException
   */
  public abstract String getSecretKey( String queryId ) throws GeneralSecurityException;
  /**
   * Get the user name that corresponds to the given query ID
   * 
   * @param queryId
   * @return
   * @throws GeneralSecurityException
   */
  public abstract String getUserName( String queryId ) throws GeneralSecurityException;
  /**
   * Get the user name that corresponds to the given X509Certificate
   * 
   * @param cert
   * @return
   * @throws GeneralSecurityException
   */
  public abstract String getUserName( X509Certificate cert ) throws GeneralSecurityException;
  /**
   * Get the user object given an X509Certificate
   * 
   * @param cert
   * @return
   * @throws GeneralSecurityException
   */
  public abstract User getUser( X509Certificate cert ) throws GeneralSecurityException;
  /**
   * Get the user object given a user name
   * 
   * @param userName
   * @return
   * @throws NoSuchUserException
   */
  public abstract User getUser( String userName ) throws NoSuchUserException;
  /**
   * Get all certificate aliases.
   * 
   * @return
   */
  public abstract List<String> getAliases( );
  /**
   * Get the <i>user number</i> as used by some client tools. N.B. This value is generally ignored by the system but is needed by some client tools.
   * 
   * @param userName
   * @return
   */
  public abstract String getUserNumber( final String userName );
  /**
   * Get a list of all enabled users.
   * 
   * @return
   */
  public abstract List<User> getEnabledUsers( );
  /**
   * Get a list of all known users.
   * 
   * @return
   */
  public abstract List<User> getAllUsers( );
  public abstract List<Group> getUserGroups( User user );
  public abstract Group getGroup( String groupName ) throws NoSuchGroupException;
}
