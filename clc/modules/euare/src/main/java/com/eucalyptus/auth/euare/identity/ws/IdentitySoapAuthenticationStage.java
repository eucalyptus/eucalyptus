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

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPConstants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurationManager;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.util.WSSecurity;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.WebServicesException;
import com.eucalyptus.ws.handlers.WsSecHandler;
import com.eucalyptus.ws.stages.UnrollableStage;
import com.eucalyptus.ws.util.CredentialProxy;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 *
 */
public class IdentitySoapAuthenticationStage implements UnrollableStage {

  @Override
  public void unrollStage( final ChannelPipeline pipeline ) {
    pipeline.addLast( "identity-ws-security", new IdentityWsSecHandler( ) );
  }

  @Override
  public String getName( ) {
    return "identity-soap-authentication";
  }


  @ChannelHandler.Sharable
  public static class IdentityWsSecHandler extends WsSecHandler {

    private static RegionConfigurationManager regionConfigurationManager = new RegionConfigurationManager( );

    public IdentityWsSecHandler( ) {
      super( new CredentialProxy( Eucalyptus.class ) );
    }

    @Override
    public Collection<WSEncryptionPart> getSignatureParts( ) {
      return Lists.newArrayList(
          new WSEncryptionPart( WSConstants.TIMESTAMP_TOKEN_LN, WSConstants.WSU_NS, "Content" ),
          new WSEncryptionPart( SOAPConstants.BODY_LOCAL_NAME, SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI, "Content" ) );
    }

    @Override
    public boolean shouldTimeStamp( ) {
      return true;
    }

    @Override
    public void incomingMessage( final MessageEvent event ) throws Exception {
      final Object o = event.getMessage( );
      if ( o instanceof MappingHttpRequest ) {
        final MappingHttpMessage httpRequest = ( MappingHttpMessage ) o;
        final List<String> forwardedForValues = httpRequest.getHeaders( "X-Forwarded-For" );
        if ( forwardedForValues != null && !forwardedForValues.isEmpty( ) ) {
          final String clientIp = Iterables.get( Splitter.on(',').split( Iterables.get( forwardedForValues, 0 ) ), 0 );
          if ( !regionConfigurationManager.isValidForwardedForAddress( clientIp ) ) {
            throw new WebServicesException( "Forbidden", HttpResponseStatus.FORBIDDEN );
          }
        }
        final SOAPEnvelope envelope = httpRequest.getSoapEnvelope( );
        final X509Certificate cert = WSSecurity.verifyWSSec( envelope );
        if ( cert == null || !regionConfigurationManager.isRegionCertificate( cert ) ) {
          throw new WebServicesException(
              "Authentication failed: The following certificate is not trusted:\n " + cert,
              HttpResponseStatus.FORBIDDEN );
        }
        Contexts.lookup( ( (MappingHttpMessage) o ).getCorrelationId() ).setUser( Principals.systemUser() );
      }
    }
  }
}
