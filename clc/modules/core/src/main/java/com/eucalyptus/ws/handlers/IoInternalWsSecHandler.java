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
