/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/

package com.eucalyptus.portal.awsusage;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.tokens.SecurityTokenAWSCredentialsProvider;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Supplier;

public class BillingAWSCredentialsProvider extends SecurityTokenAWSCredentialsProvider {

  public BillingAWSCredentialsProvider( ) {
    super( BillingUserSupplier.INSTANCE );
  }

  public BillingAWSCredentialsProvider( final int expirationSecs ) {
    super( BillingUserSupplier.INSTANCE, expirationSecs );
  }

  public enum BillingUserSupplier implements Supplier<User> {
    INSTANCE;

    @Override
    public User get( ) {
      try {
        final String accountNumber =
                Accounts.lookupAccountIdByAlias( AccountIdentifiers.BILLING_SYSTEM_ACCOUNT );
        return Accounts.lookupPrincipalByAccountNumber( accountNumber );
      } catch ( AuthException e ) {
        throw Exceptions.toUndeclared( e );
      }
    }
  }
}
