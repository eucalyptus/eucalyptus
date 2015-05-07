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
package com.eucalyptus.auth.euare.identity.ws;

import java.net.InetSocketAddress;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.auth.euare.common.identity.Identity;
import com.eucalyptus.auth.euare.identity.config.IdentityConfiguration;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurationManager;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.ws.WebServicesException;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.eucalyptus.ws.server.SoapPipeline;
import com.eucalyptus.ws.stages.UnrollableStage;

/**
 *
 */
@ComponentPart( Identity.class )
public class IdentitySoapPipeline extends SoapPipeline {

  private static RegionConfigurationManager regionConfigurationManager = new RegionConfigurationManager( );

  private final UnrollableStage auth = new IdentitySoapAuthenticationStage( );

  public IdentitySoapPipeline( ) {
    super(
        "identity-soap",
        Identity.class,
        IdentityConfiguration.SERVICE_PATH,
        "http://www.eucalyptus.com/ns/identity/2015-03-01/",
        "http://www.eucalyptus.com/ns/identity/\\d\\d\\d\\d-\\d\\d-\\d\\d/" );
  }

  @Override
  public ChannelPipeline addHandlers( final ChannelPipeline pipeline ) {
    pipeline.addLast( "source-ip-restriction-soap-fault", new MessageStackHandler( ){
      @Override
      public void incomingMessage( final MessageEvent event ) throws Exception {
        final InetSocketAddress remoteAddress = (InetSocketAddress) event.getChannel( ).getRemoteAddress( );
        if ( !regionConfigurationManager.isValidRemoteAddress( remoteAddress.getAddress( ) ) ) {
          throw new WebServicesException( "Forbidden", HttpResponseStatus.FORBIDDEN );
        }
      }
    } );
    return super.addHandlers( pipeline );
  }

  @Override
  protected UnrollableStage getAuthenticationStage( ) {
    return auth;
  }
}
