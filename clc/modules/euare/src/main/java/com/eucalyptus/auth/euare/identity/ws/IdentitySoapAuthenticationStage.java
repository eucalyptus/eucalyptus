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

import java.security.cert.X509Certificate;
import java.util.Collection;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPConstants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
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
  public int compareTo( UnrollableStage o ) {
    return this.getName( ).compareTo( o.getName( ) );
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
        final SOAPEnvelope envelope = httpRequest.getSoapEnvelope( );
        final X509Certificate cert = WSSecurity.verifyWSSec( envelope );
        if ( cert == null || !regionConfigurationManager.isRegionCertificate( cert ) ) {
          throw new WebServicesException( "Authentication failed: The following certificate is not trusted:\n " + cert );
        }
        Contexts.lookup( ( (MappingHttpMessage) o ).getCorrelationId() ).setUser( Principals.systemUser() );
      }
    }
  }
}
