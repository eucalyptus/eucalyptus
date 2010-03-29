package com.eucalyptus.auth;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

public abstract class AbstractImmutableCredentialProvider implements UserCredentialProvider {

  /**
   * @see com.eucalyptus.auth.MutableUserCredentialProvider#addCertificate(java.lang.String, java.lang.String, java.security.cert.X509Certificate)
   */
  @Override
  public User addCertificate( String userName, String alias, X509Certificate cert ) throws GeneralSecurityException, UnsupportedOperationException {
    throw new UnsupportedOperationException( "Credential Provider is read only.  Operation is unsupported: " + Thread.currentThread( ).getStackTrace( )[0] );
  }

  /**
   * @see com.eucalyptus.auth.MutableUserCredentialProvider#addUser(java.lang.String, java.lang.Boolean, java.lang.Boolean, java.lang.String, java.lang.String)
   */
  @Override
  public User addUser( String userName, Boolean isAdmin, Boolean isEnabled, String secretKey, String queryId ) throws UserExistsException {
    throw new UnsupportedOperationException( "Credential Provider is read only.  Operation is unsupported: " + Thread.currentThread( ).getStackTrace( )[0] );
  }

  /**
   * @see com.eucalyptus.auth.MutableUserCredentialProvider#addUser(java.lang.String, java.lang.Boolean, java.lang.Boolean)
   */
  @Override
  public User addUser( String userName, Boolean admin, Boolean enabled ) throws UserExistsException, UnsupportedOperationException {
    throw new UnsupportedOperationException( "Credential Provider is read only.  Operation is unsupported: " + Thread.currentThread( ).getStackTrace( )[0] );
  }

  /**
   * @see com.eucalyptus.auth.MutableUserCredentialProvider#deleteUser(java.lang.String)
   */
  @Override
  public User deleteUser( String userName ) throws NoSuchUserException, UnsupportedOperationException {
    throw new UnsupportedOperationException( "Credential Provider is read only.  Operation is unsupported: " + Thread.currentThread( ).getStackTrace( )[0] );
  }

  /**
   * @see com.eucalyptus.auth.MutableUserCredentialProvider#resetUser(java.lang.String, java.lang.Boolean, java.lang.Boolean)
   */
  @Override
  public User resetUser( String userName, Boolean admin, Boolean enabled ) throws NoSuchUserException, UnsupportedOperationException {
    throw new UnsupportedOperationException( "Credential Provider is read only.  Operation is unsupported: " + Thread.currentThread( ).getStackTrace( )[0] );
  }

  /**
   * @see com.eucalyptus.auth.MutableUserCredentialProvider#resetUserQueryKeys(java.lang.String)
   */
  @Override
  public User resetUserQueryKeys( String userName ) throws NoSuchUserException, UnsupportedOperationException {
    throw new UnsupportedOperationException( "Credential Provider is read only.  Operation is unsupported: " + Thread.currentThread( ).getStackTrace( )[0] );
  }

  /**
   * @see com.eucalyptus.auth.MutableUserCredentialProvider#revokeCertificate(java.lang.String, java.lang.String)
   */
  @Override
  public User revokeCertificate( String userName, String alias ) throws NoSuchUserException, NoSuchCertificateException {
    throw new UnsupportedOperationException( "Credential Provider is read only.  Operation is unsupported: " + Thread.currentThread( ).getStackTrace( )[0] );
  }

  /**
   * @see com.eucalyptus.auth.MutableUserCredentialProvider#updateUser(java.lang.String, java.lang.Boolean, java.lang.Boolean)
   */
  @Override
  public User updateUser( String userName, Boolean admin, Boolean enabled ) throws NoSuchUserException, UnsupportedOperationException {
    throw new UnsupportedOperationException( "Credential Provider is read only.  Operation is unsupported: " + Thread.currentThread( ).getStackTrace( )[0] );
  }

}
