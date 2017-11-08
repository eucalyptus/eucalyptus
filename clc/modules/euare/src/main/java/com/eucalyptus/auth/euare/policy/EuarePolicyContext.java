/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth.euare.policy;

import javax.annotation.Nullable;
import com.eucalyptus.auth.euare.principal.EuareAccount;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;

/**
 *
 */
public class EuarePolicyContext {
  private final static ThreadLocal<EuarePolicyContextResource> resourceLocal = new ThreadLocal<>();

  static void clearContext( ) {
    resourceLocal.set( null );
  }

  static void setEuarePolicyContextResource( @Nullable final EuarePolicyContextResource resource ) {
    resourceLocal.set( resource );
  }

  @Nullable
  public static Boolean isSystemAccount( ) {
    final EuarePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.isSystemAccount();
  }

  public static interface EuarePolicyContextResource {
    @Nullable
    Boolean isSystemAccount( );
  }

  @TypeMapper
  public enum AccountEuarePolicyContextTransform implements Function<EuareAccount,EuarePolicyContextResource> {
    INSTANCE;

    @Override
    public EuarePolicyContextResource apply( final EuareAccount account ) {
      return new EuarePolicyContextResource( ) {
        @Nullable
        @Override
        public Boolean isSystemAccount( ) {
          return com.eucalyptus.auth.euare.Accounts.isSystemAccount( account );
        }
      };
    }
  }
}
