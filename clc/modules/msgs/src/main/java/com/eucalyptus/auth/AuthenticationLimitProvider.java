/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth;

import java.util.ServiceLoader;
import javax.annotation.Nonnull;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.NonNullFunction;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 *
 */
public interface AuthenticationLimitProvider {

  int getAccessKeyLimitSpi( );

  int getSigningCertificateLimitSpi( );

  static class Values {
    static int getAccessKeyLimit( ) {
      return getValue( AuthenticationLimit.ACCESS_KEY );
    }

    static int getSigningCertificateLimit( ) {
      return getValue( AuthenticationLimit.SIGNING_CERTIFICATE );
    }

    static int getValue( final NonNullFunction<AuthenticationLimitProvider, Integer> valueFunction ) {
      return CollectionUtils.reduce(
          Lists.newArrayList( Iterators.transform(
              ServiceLoader.load( AuthenticationLimitProvider.class ).iterator(),
              valueFunction
          ) ),
          Integer.MAX_VALUE,
          CollectionUtils.min( )
      );
    }
  }

  enum AuthenticationLimit implements NonNullFunction<AuthenticationLimitProvider, Integer> {
    ACCESS_KEY {
      @Nonnull
      @Override
      public Integer apply( final AuthenticationLimitProvider authenticationLimitProvider ) {
        return authenticationLimitProvider.getAccessKeyLimitSpi( );
      }
    },
    SIGNING_CERTIFICATE {
      @Nonnull
      @Override
      public Integer apply( final AuthenticationLimitProvider authenticationLimitProvider ) {
        return authenticationLimitProvider.getSigningCertificateLimitSpi( );
      }
    },
  }
}
