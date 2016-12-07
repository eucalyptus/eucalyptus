/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
