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
