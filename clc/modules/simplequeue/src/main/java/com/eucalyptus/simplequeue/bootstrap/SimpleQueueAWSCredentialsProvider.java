/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.simplequeue.bootstrap;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.tokens.SecurityTokenAWSCredentialsProvider;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Supplier;

/**
 * Created by ethomas on 8/5/14.
 */
public class SimpleQueueAWSCredentialsProvider extends SecurityTokenAWSCredentialsProvider {

  public SimpleQueueAWSCredentialsProvider() {
    super( SimpleQueueUserSupplier.INSTANCE );
  }

  public SimpleQueueAWSCredentialsProvider(final int expirationSecs) {
    super( SimpleQueueUserSupplier.INSTANCE, expirationSecs );
  }

  public enum SimpleQueueUserSupplier implements Supplier<User> {
    INSTANCE;

    @Override
    public User get( ) {
      try {
        final String accountNumber = Accounts.lookupAccountIdByAlias( AccountIdentifiers.SIMPLEQUEUE_SYSTEM_ACCOUNT);
        return Accounts.lookupPrincipalByAccountNumber( accountNumber );
      } catch ( AuthException e ) {
        throw Exceptions.toUndeclared( e );
      }
    }
  }
}
