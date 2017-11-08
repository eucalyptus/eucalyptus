/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cloudformation.bootstrap;

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
public class CloudFormationAWSCredentialsProvider extends SecurityTokenAWSCredentialsProvider {

  public CloudFormationAWSCredentialsProvider( ) {
    super( CloudFormationUserSupplier.INSTANCE );
  }

  public CloudFormationAWSCredentialsProvider( final int expirationSecs ) {
    super( CloudFormationUserSupplier.INSTANCE, expirationSecs );
  }

  public enum CloudFormationUserSupplier implements Supplier<User> {
    INSTANCE;

    @Override
    public User get( ) {
      try {
        final String accountNumber = Accounts.lookupAccountIdByAlias( AccountIdentifiers.CLOUDFORMATION_SYSTEM_ACCOUNT );
        return Accounts.lookupPrincipalByAccountNumber( accountNumber );
      } catch ( AuthException e ) {
        throw Exceptions.toUndeclared( e );
      }
    }
  }
}
