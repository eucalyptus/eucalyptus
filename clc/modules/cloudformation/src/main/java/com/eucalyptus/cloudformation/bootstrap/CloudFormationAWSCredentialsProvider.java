package com.eucalyptus.cloudformation.bootstrap;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.tokens.SecurityToken;
import com.eucalyptus.auth.tokens.SecurityTokenManager;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import java.util.concurrent.TimeUnit;

/**
 * Created by ethomas on 8/5/14.
 */
public class CloudFormationAWSCredentialsProvider implements AWSCredentialsProvider {

  private static final int EXPIRATION_SECS = 900;
  private static final int PRE_EXIPRY = 60;

  private volatile Supplier<AWSCredentials> credentialsSupplier;

  public CloudFormationAWSCredentialsProvider( ) {
    refresh( );
  }

  @Override
  public synchronized AWSCredentials getCredentials( ) {
    return credentialsSupplier.get( );
  }

  @Override
  public synchronized void refresh( ) {
    credentialsSupplier = refreshCredentialsSupplier( );
  }

  private Supplier<AWSCredentials> refreshCredentialsSupplier() {
    return Suppliers.memoizeWithExpiration( new Supplier<AWSCredentials>( ) {
      @Override
      public AWSCredentials get( ) {
        try {
          final Account account = Accounts.lookupAccountByName( Account.CLOUDFORMATION_SYSTEM_ACCOUNT );
          final SecurityToken securityToken =
              SecurityTokenManager.issueSecurityToken( account.lookupAdmin( ), EXPIRATION_SECS );
          return new BasicSessionCredentials(
              securityToken.getAccessKeyId( ),
              securityToken.getSecretKey( ),
              securityToken.getToken( ) );
        } catch ( final AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    }, EXPIRATION_SECS - PRE_EXIPRY, TimeUnit.SECONDS );
  }
}
