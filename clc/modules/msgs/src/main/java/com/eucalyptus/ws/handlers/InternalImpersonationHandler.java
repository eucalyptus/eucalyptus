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
package com.eucalyptus.ws.handlers;

import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * TODO:STEVE: Review approach to impersonation
 * TODO:GRZE: Review approach to impersonation
 */
@ChannelPipelineCoverage( ChannelPipelineCoverage.ALL )
public class InternalImpersonationHandler extends MessageStackHandler {
  @Override
  public void incomingMessage( final MessageEvent event ) throws Exception {
    final Object o = event.getMessage( );
    if ( o instanceof MappingHttpRequest ) {
      final MappingHttpMessage httpRequest = ( MappingHttpMessage ) o;
      if ( httpRequest.getMessage() instanceof BaseMessage ) {
        final BaseMessage baseMessage = (BaseMessage) httpRequest.getMessage();
        final String userId = baseMessage.getEffectiveUserId();
        try {
          final Context context = Contexts.lookup( httpRequest.getCorrelationId() );
          if ( userId != null &&
              !Principals.isFakeIdentify( userId ) &&
              context.hasAdministrativePrivileges() ) {
            final User user= Accounts.lookupUserById( userId );
            context.setUser( user );
          }
        } catch ( final NoSuchContextException e ) {
          // no impersonation for you
        } catch ( final AuthException e ) {
          throw new EucalyptusCloudException( "User not found: " + userId );
        }
      }
    } 
  }
}
