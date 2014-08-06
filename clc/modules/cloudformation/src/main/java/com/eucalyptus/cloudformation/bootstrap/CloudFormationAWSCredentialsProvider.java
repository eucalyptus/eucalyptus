package com.eucalyptus.cloudformation.bootstrap;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.tokens.SecurityToken;
import com.eucalyptus.auth.tokens.SecurityTokenManager;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Created by ethomas on 8/5/14.
 */
public class CloudFormationAWSCredentialsProvider implements AWSCredentialsProvider {
  private static final Logger LOG = Logger.getLogger(CloudFormationAWSCredentialsProvider.class);
  private static final int EXPIRATION_SECS = 900;
  Supplier<AWSCredentials> credentialsSupplier;

  CloudFormationAWSCredentialsProvider() {
    credentialsSupplier = refreshCredentialsSupplier();
  }
  @Override
  public synchronized AWSCredentials getCredentials() {
    return credentialsSupplier.get();
  }

  @Override
  public synchronized void refresh() {
    credentialsSupplier = refreshCredentialsSupplier();
  }

  private Supplier<AWSCredentials> refreshCredentialsSupplier() {
    Supplier<AWSCredentials> basicSupplier = new Supplier<AWSCredentials> () {
      @Override
      public AWSCredentials get() {
        try {
          Account account = Accounts.lookupAccountByName(Account.CLOUDFORMATION_SYSTEM_ACCOUNT);
          User admin = account.lookupAdmin();
          SecurityToken securityToken = SecurityTokenManager.issueSecurityToken(admin, EXPIRATION_SECS);
          return new BasicSessionCredentials(securityToken.getAccessKeyId(), securityToken.getSecretKey(), securityToken.getToken());
        } catch (AuthException e) {
          LOG.error(e);
          return null; // TODO: how to best handle this?
        }
      }
    };
    return Suppliers.memoizeWithExpiration(basicSupplier, EXPIRATION_SECS, TimeUnit.SECONDS);
  }
}
