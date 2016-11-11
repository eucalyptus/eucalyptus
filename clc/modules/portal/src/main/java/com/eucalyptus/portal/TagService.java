/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
