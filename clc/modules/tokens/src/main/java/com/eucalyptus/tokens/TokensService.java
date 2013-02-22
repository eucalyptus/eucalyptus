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
package com.eucalyptus.tokens;

import static com.eucalyptus.auth.login.AccountUsernamePasswordCredentials.AccountUsername;
import static com.eucalyptus.auth.login.HmacCredentials.QueryIdCredential;
import java.util.Set;
import javax.security.auth.Subject;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.tokens.SecurityToken;
import com.eucalyptus.auth.tokens.SecurityTokenManager;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Iterables;

/**
 * Service component for temporary access tokens
 */
public class TokensService {

  public GetSessionTokenResponseType getSessionToken( final GetSessionTokenType request ) throws EucalyptusCloudException {
    final GetSessionTokenResponseType reply = request.getReply();
    reply.getResponseMetadata().setRequestId( reply.getCorrelationId( ) );
    final Context ctx = Contexts.lookup();
    final Subject subject = ctx.getSubject();
    final User requestUser = ctx.getUser( );

    AccessKey accessKey = null;
    final Set<QueryIdCredential> queryIdCreds = subject.getPublicCredentials( QueryIdCredential.class );
    if ( !queryIdCreds.isEmpty() ) {
      try {
        accessKey = Accounts.lookupAccessKeyById( Iterables.getOnlyElement( queryIdCreds ).getQueryId() );
      } catch ( final AuthException e ) {
        throw new EucalyptusCloudException( "Error finding access key", e );
      }
    }

    String accessToken = null;
    final AccountUsername accountUsername =
        Iterables.getFirst( subject.getPublicCredentials( AccountUsername.class ), null );
    if ( accountUsername != null ) {
      try {
        final Account account = Accounts.lookupAccountByName( accountUsername.getAccount() );
        final User user = account.lookupUserByName( accountUsername.getUsername() );
        accessToken = user.getToken();
      } catch ( AuthException e ) {
        throw new EucalyptusCloudException();
      }
    }

    try {
      final SecurityToken token = SecurityTokenManager.issueSecurityToken(requestUser, accessKey, accessToken, request.getDurationSeconds());
      reply.setResult( GetSessionTokenResultType.forCredentials(
          token.getAccessKeyId(),
          token.getSecretKey(),
          token.getToken(),
          token.getExpires()
      ) );
    } catch ( final AuthException e ) {
      throw new EucalyptusCloudException( e.getMessage(), e );
    }

    return reply;
  }

}
