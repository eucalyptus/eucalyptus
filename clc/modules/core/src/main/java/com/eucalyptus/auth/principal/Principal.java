/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth.principal;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;

/**
 *
 */
public interface Principal {

  enum PrincipalType {
    AWS {
      @Override
      public String convertForUserMatching( final String principal ) {
        if ( "*".equals( principal ) ) {
          return principal;
        } else {
          final Matcher accountIdMatcher = accountIdPattern.matcher( principal );
          if ( accountIdMatcher.matches() ) {
            final String accountId = MoreObjects.firstNonNull( accountIdMatcher.group( 1 ), accountIdMatcher.group( 2 ) );
            try {
              return Accounts.getAccountArn( accountId );
            } catch ( final AuthException e ) { // account id is validated already, so not exception not expected
              throw Exceptions.toUndeclared( e );
            }
          } else {
            final Ern ern = Ern.parse( principal );
            if ( IAM_USER.equals( ern.getResourceType( ) ) ||
                IAM_ROLE.equals( ern.getResourceType( ) ) ||
                STS_ROLE.equals( ern.getResourceType( ) ) ) {
              return principal;
            }
            return null;
          }
        }
      }
    },
    Federated {
      @Override
      public String convertForUserMatching( final String principal ) {
        return principal;
      }
    },
    Service {
      @Override
      public String convertForUserMatching( final String principal ) {
        return principal;
      }
    },
    ;

    private static final String IAM_USER = PolicySpec.qualifiedName( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER );
    private static final String IAM_ROLE = PolicySpec.qualifiedName( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_ROLE );
    private static final String STS_ROLE = PolicySpec.qualifiedName( PolicySpec.VENDOR_STS, PolicySpec.STS_RESOURCE_ASSUMED_ROLE );
    private static final Pattern accountIdPattern = Pattern.compile( "([0-9]{12})|arn:aws:iam::([0-9]{12}):root" );

    public Set<String> convertForUserMatching( final Set<String> values ) {
      final Set<String> converted = Sets.newHashSet();
      for ( final String value : values ) {
        final String convertedValue = convertForUserMatching( value );
        if ( convertedValue != null ) {
          converted.add( convertedValue );
        }
      }
      return converted;
    }

    public abstract String convertForUserMatching( final String principal );
  }

  boolean isNotPrincipal();

  PrincipalType getType();

  Set<String> getValues();
}
