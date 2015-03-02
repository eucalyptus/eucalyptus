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
package com.eucalyptus.auth.euare.identity;

import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.InvalidAccessKeyAuthException;
import com.eucalyptus.auth.euare.EuareException;
import com.eucalyptus.auth.euare.common.identity.DescribePrincipalResponseType;
import com.eucalyptus.auth.euare.common.identity.DescribePrincipalResult;
import com.eucalyptus.auth.euare.common.identity.DescribePrincipalType;
import com.eucalyptus.auth.euare.common.identity.Principal;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Lists;

/**
 *
 */
@ComponentNamed
public class IdentityService {

  private static final Logger logger = Logger.getLogger( IdentityService.class );

  public DescribePrincipalResponseType describePrincipal( final DescribePrincipalType request ) throws EuareException {
    final DescribePrincipalResponseType response = request.getReply( );
    final DescribePrincipalResult result = new DescribePrincipalResult( );

    try {
      final String accessKeyId = request.getAccessKeyId( );
      if ( accessKeyId != null ) {
        final AccessKey accessKey = Accounts.lookupAccessKeyById( accessKeyId );
        final User user = accessKey.getUser( );
        final Principal principal = new Principal( );
        principal.setArn( Accounts.getUserArn( user ) );
        principal.setUserId( user.getUserId( ) );
        principal.setAccountAlias( user.getAccount( ).getName( ) );
        if ( accessKey.isActive( ) ) {
          final com.eucalyptus.auth.euare.common.identity.AccessKey key =
              new com.eucalyptus.auth.euare.common.identity.AccessKey( );
          key.setAccessKeyId( accessKey.getAccessKey( ) );
          key.setSecretAccessKey( accessKey.getSecretKey( ) );
          principal.setAccessKeys( Lists.newArrayList( key ) );
        }

        //TODO:STEVE: ensure policy permits no access if a user is disabled

        result.setPrincipal( principal );
      }
    } catch ( InvalidAccessKeyAuthException e ) {
      // not found, so empty response
    } catch ( AuthException e ) {
      throw handleException( e );
    }

    response.setDescribePrincipalResult( result );
    return response;
  }

  /**
   * Method always throws, signature allows use of "throw handleException ..."
   */
  private EuareException handleException( final Exception e ) throws EuareException {
    final EuareException cause = Exceptions.findCause( e, EuareException.class );
    if ( cause != null ) {
      throw cause;
    }

    logger.error( e, e );

    final EuareException exception =
        new EuareException( HttpResponseStatus.INTERNAL_SERVER_ERROR, "InternalError", String.valueOf(e.getMessage( )) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges( ) ) {
      exception.initCause( e );
    }
    throw exception;
  }

}
