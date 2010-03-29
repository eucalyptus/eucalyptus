package com.eucalyptus.auth;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.List;
import com.eucalyptus.auth.crypto.CryptoProviders;

/**
 * Static facade for accessing the system configured credential provider.
 * 
 * @author decker
 * @see UserCredentialProvider
 */
public final class CredentialProvider {
  
  /**
   * TODO: edu.ucsb.eucalyptus.admin.server.X509Download.getX509Zip(String, String):161
   */
  @Deprecated
  public static User addCertificate( String userName, String alias, X509Certificate cert ) throws GeneralSecurityException, UnsupportedOperationException {
    return CredentialProviders.getUserProvider( ).addCertificate( userName, alias, cert );
  }
  
  public static User addUser( String userName, Boolean admin, Boolean enabled ) throws UserExistsException, UnsupportedOperationException {
    return CredentialProviders.getUserProvider( ).addUser( userName, admin, enabled );
  }
  
  public static User deleteUser( String userName ) throws NoSuchUserException, UnsupportedOperationException {
    return CredentialProviders.getUserProvider( ).deleteUser( userName );
  }
  
  public static User updateUser( String userName, Boolean admin, Boolean enabled ) throws NoSuchUserException, UnsupportedOperationException {
    return CredentialProviders.getUserProvider( ).updateUser( userName, admin, enabled );
  }
  
  public static User resetUser( String userName, Boolean admin, Boolean enabled ) throws UnsupportedOperationException, NoSuchUserException {
    return CredentialProviders.getUserProvider( ).resetUser( userName, admin, enabled );
  }
  
  /**
   * TODO: edu.ucsb.eucalyptus.cloud.ws.WalrusImageManager.decryptImage(String, String, String, boolean):171
   */
  @Deprecated
  public static List<String> getAliases( ) {
    return CredentialProviders.getUserProvider( ).getAliases( );
  }
  
  public static List<User> getAllUsers( ) {
    return CredentialProviders.getUserProvider( ).getAllUsers( );
  }
  
  @Deprecated
  public static X509Certificate getCertificate( String alias ) throws GeneralSecurityException {
    return CredentialProviders.getUserProvider( ).getCertificate( alias );
  }
  
  @Deprecated
  public static String getCertificateAlias( String certPem ) throws GeneralSecurityException {
    return CredentialProviders.getUserProvider( ).getCertificateAlias( certPem );
  }
  
  @Deprecated
  public static String getCertificateAlias( X509Certificate certPem ) throws GeneralSecurityException {
    return CredentialProviders.getUserProvider( ).getCertificateAlias( certPem );
  }

  public static String getDName( String userName ) {
    return CredentialProviders.getUserProvider( ).getDName( userName );
  }
  
  public static List<User> getEnabledUsers( ) {
    return CredentialProviders.getUserProvider( ).getEnabledUsers( );
  }
  
  @Deprecated
  public static String getQueryId( String userName ) throws GeneralSecurityException {
    return CredentialProviders.getUserProvider( ).getQueryId( userName );
  }
  
  @Deprecated
  public static String getSecretKey( String queryId ) throws GeneralSecurityException {
    return CredentialProviders.getUserProvider( ).getSecretKey( queryId );
  }
  
  public static User getUser( String userName ) throws NoSuchUserException {
    return CredentialProviders.getUserProvider( ).getUser( userName );
  }

  public static Group getGroup( String groupName ) throws NoSuchGroupException {
    return CredentialProviders.getUserProvider( ).getGroup( groupName );
  }
  
  public static List<Group> getUserGroups( User user ) throws NoSuchUserException {
    return CredentialProviders.getUserProvider( ).getUserGroups( user );
  }

  public static User getUser( X509Certificate cert ) throws GeneralSecurityException {
    return CredentialProviders.getUserProvider( ).getUser( cert );
  }
  
  @Deprecated
  public static String getUserName( String queryId ) throws GeneralSecurityException {
    return CredentialProviders.getUserProvider( ).getUserName( queryId );
  }
  
  @Deprecated
  public static String getUserName( X509Certificate cert ) throws GeneralSecurityException {
    return CredentialProviders.getUserProvider( ).getUserName( cert );
  }
  
  public static String getUserNumber( String userName ) {
    return CredentialProviders.getUserProvider( ).getUserNumber( userName );
  }
  
  @Deprecated
  public static boolean hasCertificate( String alias ) {
    return CredentialProviders.getUserProvider( ).hasCertificate( alias );
  }
  
}
