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


