/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.portal;

import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import java.util.Collections;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.portal.common.model.GetTagKeysResponseType;
import com.eucalyptus.portal.common.model.GetTagKeysType;
import com.eucalyptus.portal.common.policy.TagPolicySpec;
import com.eucalyptus.portal.provider.TagProviders;
import com.eucalyptus.util.Exceptions;

/**
 *
 */
@SuppressWarnings( "unused" )
@ComponentNamed
public class TagService {

  private static final Logger logger = Logger.getLogger( TagService.class );

  public GetTagKeysResponseType getTagKeys( final GetTagKeysType request ) throws TagServiceException {
    final GetTagKeysResponseType response = request.getReply( );
    final Context context = checkAuthorized( );
    try {
      response.getResult( ).getKeys( ).addAll( TagProviders.getTagKeys( context.getUser( ) ) );
      Collections.sort( response.getResult( ).getKeys( ), String.CASE_INSENSITIVE_ORDER );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return response;
  }

  private static Context checkAuthorized( ) throws TagServiceException {
    final Context ctx = Contexts.lookup( );
    final AuthContextSupplier requestUserSupplier = ctx.getAuthContext( );
    if ( !Permissions.isAuthorized(
        TagPolicySpec.VENDOR_TAG,
        "",
        "",
        null,
        getIamActionByMessageType( ),
        requestUserSupplier ) ) {
      throw new TagServiceUnauthorizedException(
          "UnauthorizedOperation",
          "You are not authorized to perform this operation." );
    }
    return ctx;
  }


  /**
   * Method always throws, signature allows use of "throw handleException ..."
   */
  private static TagServiceException handleException( final Exception e  ) throws TagServiceException {
    Exceptions.findAndRethrow( e, TagServiceException.class );

    logger.error( e, e );

    final TagServiceException exception = new TagServiceException( "InternalError", String.valueOf(e.getMessage()) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges() ) {
      exception.initCause( e );
    }
    throw exception;
  }
}
