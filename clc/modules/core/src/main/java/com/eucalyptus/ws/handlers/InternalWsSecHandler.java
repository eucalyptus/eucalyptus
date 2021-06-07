/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
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
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.util.WSSecurity;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.WebServicesException;
import com.eucalyptus.ws.util.CredentialProxy;
import com.google.common.collect.Lists;

@ChannelHandler.Sharable
public class InternalWsSecHandler extends WsSecHandler {
  
  private static Logger LOG = Logger.getLogger( InternalWsSecHandler.class );
  public InternalWsSecHandler( ) {
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
  public void incomingMessage( MessageEvent event ) throws Exception {
    final Object o = event.getMessage( );
    if ( o instanceof MappingHttpRequest ) {
      final MappingHttpMessage httpRequest = ( MappingHttpMessage ) o;
      final SOAPEnvelope envelope = httpRequest.getSoapEnvelope( );
      
      X509Certificate cert = WSSecurity.verifyWSSec( envelope );
      if ( cert == null || !cert.equals( SystemCredentials.lookup( Eucalyptus.class ).getCertificate( ) ) ) {
    	  throw new WebServicesException( "Authentication failed: The following certificate is not trusted:\n " + cert );
      }
      
      Contexts.lookup( ( ( MappingHttpMessage ) o ).getCorrelationId( ) ).setUser( Principals.systemUser() );
    }
  }

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) {
    final Object o = event.getMessage( );
    if ( o instanceof MappingHttpRequest ) {
      super.outgoingMessage( ctx, event );
    }
  }
}
