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
package com.eucalyptus.auth;

import java.util.ServiceLoader;
import javax.annotation.Nonnull;
import com.eucalyptus.util.NonNullFunction;

/**
 *
 */
public interface AuthenticationLimitProvider {

  long getDefaultPasswordExpirySpi( );

  int getAccessKeyLimitSpi( );

  int getSigningCertificateLimitSpi( );

  int getPolicyAttachmentLimitSpi( );

  int getPolicySizeLimitSpi( );

  boolean getUseValidatingPolicyParserSpi( );

  class Values {
    public static long getDefaultPasswordExpiry( ) {
      return getLongValue( AuthenticationLongProperties.DEFAULT_PASSWORD_EXPIRY );
    }

    public static int getAccessKeyLimit( ) {
      return getIntValue( AuthenticationLimit.ACCESS_KEY );
    }

    public static int getSigningCertificateLimit( ) {
      return getIntValue( AuthenticationLimit.SIGNING_CERTIFICATE );
    }

    public static int getPolicySizeLimit( ) {
      return getIntValue( AuthenticationLimit.POLICY_SIZE );
    }

    public static int getPolicyAttachmentLimit( ) {
      return getIntValue( AuthenticationLimit.POLICY_ATTACHMENT );
    }

    public static int getOpenIdConnectProviderClientIdLimit( ) { return 100; }

    public static int getOpenIdConnectProviderThumprintLimit( ) { return 5; }

    public static boolean getUseValidatingPolicyParser( ) {
      return getBooleanValue( AuthenticationBooleanProperties.USE_VALIDATING );
    }

    static int getIntValue( final NonNullFunction<AuthenticationLimitProvider, Integer> valueFunction ) {
      return getValue( valueFunction );
    }

    static long getLongValue( final NonNullFunction<AuthenticationLimitProvider, Long> valueFunction ) {
      return getValue( valueFunction );
    }

    static boolean getBooleanValue( final NonNullFunction<AuthenticationLimitProvider, Boolean> valueFunction ) {
      return getValue( valueFunction );
    }

    static <VT> VT getValue(
        final NonNullFunction<AuthenticationLimitProvider, VT> valueFunction
    ) {
      return valueFunction.apply( ServiceLoader.load( AuthenticationLimitProvider.class ).iterator( ).next( ) );
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
    POLICY_ATTACHMENT {
      @Nonnull
      @Override
      public Integer apply( final AuthenticationLimitProvider authenticationLimitProvider ) {
        return authenticationLimitProvider.getPolicyAttachmentLimitSpi( );
      }
    },
    POLICY_SIZE {
      @Nonnull
      @Override
      public Integer apply( final AuthenticationLimitProvider authenticationLimitProvider ) {
        return authenticationLimitProvider.getPolicySizeLimitSpi( );
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

  enum AuthenticationLongProperties implements NonNullFunction<AuthenticationLimitProvider, Long> {
    DEFAULT_PASSWORD_EXPIRY {
      @Nonnull
      @Override
      public Long apply( final AuthenticationLimitProvider authenticationLimitProvider ) {
        return authenticationLimitProvider.getDefaultPasswordExpirySpi();
      }
    },
  }

  enum AuthenticationBooleanProperties implements NonNullFunction<AuthenticationLimitProvider, Boolean> {
    USE_VALIDATING {
      @Nonnull
      @Override
      public Boolean apply( final AuthenticationLimitProvider authenticationLimitProvider ) {
        return authenticationLimitProvider.getUseValidatingPolicyParserSpi();
      }
    }
  }


}


