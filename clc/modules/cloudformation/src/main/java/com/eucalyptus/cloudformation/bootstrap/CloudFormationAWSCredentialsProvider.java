package com.eucalyptus.cloudformation.bootstrap;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.tokens.SecurityTokenAWSCredentialsProvider;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Supplier;

/**
 * Created by ethomas on 8/5/14.
 */
public class CloudFormationAWSCredentialsProvider extends SecurityTokenAWSCredentialsProvider {

  public CloudFormationAWSCredentialsProvider( ) {
    super( CloudFormationUserSupplier.INSTANCE );
  }

  public enum CloudFormationUserSupplier implements Supplier<User> {
    INSTANCE;

    @Override
    public User get( ) {
      final Account account;
      try {
        account = Accounts.lookupAccountByName( Account.CLOUDFORMATION_SYSTEM_ACCOUNT );
        return account.lookupAdmin( );
      } catch ( AuthException e ) {
        throw Exceptions.toUndeclared( e );
      }
    }
  }
}
