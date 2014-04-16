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
package com.eucalyptus.auth.euare.policy;

import javax.annotation.Nullable;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.Account;
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
  public enum AccountEuarePolicyContextTransform implements Function<Account,EuarePolicyContextResource> {
    INSTANCE;

    @Override
    public EuarePolicyContextResource apply( final Account account ) {
      return new EuarePolicyContextResource( ) {
        @Nullable
        @Override
        public Boolean isSystemAccount( ) {
          return Accounts.isSystemAccount( account );
        }
      };
    }
  }
}
