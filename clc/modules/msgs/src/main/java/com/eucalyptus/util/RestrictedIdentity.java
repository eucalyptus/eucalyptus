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
package com.eucalyptus.util;

import static com.eucalyptus.component.ComponentId.ComponentMessage;
import static com.eucalyptus.component.ComponentId.PolicyVendor;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicyAction;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.principal.Principal;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.system.Ats;
import com.google.common.base.Supplier;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 *
 */
public class RestrictedIdentity {

  private static Logger LOG = Logger.getLogger( RestrictedIdentity.class );

  /**
   * Enforce user authorization for resource access using the supplied policy.
   *
   * @param resourceAccount The account of the resource
   * @param resourcePolicy The policy attached to the resource
   * @param exceptionSupplier Supplier of exception to throw if not authorized
   * @param <T> The throwable type
   * @throws T If not authorized
   * @see com.eucalyptus.auth.policy.PolicyParser#getResourceInstance()
   */
  public static <T extends Throwable> void checkAuthorized( final Account resourceAccount,
                                                            final String resourceType,
                                                            final String resourceName,
                                                            final Policy resourcePolicy,
                                                            final Supplier<T> exceptionSupplier ) throws T {
    final Context ctx = Contexts.lookup();
    final Class<? extends BaseMessage> msgType = ctx.getRequest( ).getClass();
    final String vendor = findPolicyVendor( msgType );
    final String action = RestrictedTypes.getIamActionByMessageType();
    final User requestUser = ctx.getUser();
    try {
      final Principal.PrincipalType principalType;
      final String principalName;
      if ( Principals.isSameUser( requestUser, Principals.systemUser() ) ) {
        principalType = Principal.PrincipalType.Service;
        principalName = "ec2.amazon.com";
      } else {
        principalType = Principal.PrincipalType.AWS;
        principalName = Accounts.getUserArn( requestUser );
      }

      if ( !Permissions.isAuthorized(
          vendor,
          principalType,
          principalName,
          resourcePolicy,
          resourceType,
          resourceName,
          resourceAccount,
          action,
          requestUser ) ) {
        throw exceptionSupplier.get();
      }
    } catch ( AuthException e ) {
      LOG.error( e, e );
      throw exceptionSupplier.get();
    }
  }

  private static String findPolicyVendor( Class<? extends BaseMessage > msgType ) throws IllegalArgumentException {
    final Ats ats = Ats.inClassHierarchy( msgType );

    if ( ats.has( PolicyVendor.class ) ) {
      return ats.get( PolicyVendor.class ).value();
    }

    if ( ats.has( PolicyAction.class ) ) {
      return ats.get( PolicyAction.class ).vendor();
    }

    if ( ats.has( ComponentMessage.class ) ) {
      final Class<? extends ComponentId> componentIdClass =
          ats.get( ComponentMessage.class ).value();
      final Ats componentAts = Ats.inClassHierarchy( componentIdClass );
      if ( componentAts.has( PolicyVendor.class ) ) {
        return componentAts.get( PolicyVendor.class ).value();
      }
    }

    throw new IllegalArgumentException( "Failed to determine policy"
        + ": require @PolicyVendor, @PolicyAction or @ComponentMessage in request type hierarchy "
        + msgType.getCanonicalName( ) );
  }
}
