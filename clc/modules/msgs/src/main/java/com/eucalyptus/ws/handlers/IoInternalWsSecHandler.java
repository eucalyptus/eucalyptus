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
package com.eucalyptus.ws.handlers;

import java.security.cert.X509Certificate;
import java.util.Collection;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPConstants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.log4j.Logger;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.util.WSSecurity;
import com.eucalyptus.ws.IoMessage;
import com.eucalyptus.ws.WebServicesException;
import com.eucalyptus.ws.util.CredentialProxy;
import com.google.common.collect.Lists;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 *
 */
@ChannelHandler.Sharable
public class IoInternalWsSecHandler extends IoWsSecHandler {

  private static Logger LOG = Logger.getLogger( IoInternalWsSecHandler.class );
  public IoInternalWsSecHandler( ) {
    super( new CredentialProxy( Eucalyptus.class ) );
  }

  @Override
  public Collection<WSEncryptionPart> getSignatureParts( ) {
    return Lists.newArrayList( new WSEncryptionPart( WSConstants.TIMESTAMP_TOKEN_LN, WSConstants.WSU_NS, "Content" ),
        new WSEncryptionPart( SOAPConstants.BODY_LOCAL_NAME, SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI, "Content" ) );
  }

  @Override
  public boolean shouldTimeStamp( ) {
    return true;
  }

  @Override
  public void channelRead( final ChannelHandlerContext ctx, final Object msg ) throws Exception {
    final Object o = msg;
    if ( o instanceof IoMessage && ((IoMessage)o).isRequest( )) {
      final IoMessage ioMessage = ( IoMessage ) o;
      final SOAPEnvelope envelope = ioMessage.getSoapEnvelope( );

      X509Certificate cert = WSSecurity.verifyWSSec( envelope );
      if ( cert == null || !cert.equals( SystemCredentials.lookup( Eucalyptus.class ).getCertificate( ) ) ) {
        throw new WebServicesException( "Authentication failed: The following certificate is not trusted:\n " + cert );
      }

      Contexts.lookup( ioMessage.getCorrelationId( ) ).setUser( Principals.systemUser() );
    }
    super.channelRead( ctx, msg );
  }

  @Override
  public void write( final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise ) throws Exception {
    final Object o = msg;
    if ( o instanceof IoMessage && ((IoMessage)o).isRequest( ) ) {
      super.write( ctx, msg, promise );
    } else {
      ctx.write( msg, promise );
    }
  }
}
