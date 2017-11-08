/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

import java.util.Collection;
import java.util.List;
import javax.xml.crypto.dsig.Reference;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.log4j.Logger;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecSignature;
import org.apache.ws.security.message.WSSecTimestamp;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.crypto.util.WSSecurity;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.ws.util.CredentialProxy;
import com.google.common.collect.Lists;

@ChannelHandler.Sharable
public abstract class WsSecHandler extends MessageStackHandler {
  private static Logger         LOG    = Logger.getLogger( WsSecHandler.class );
  private final CredentialProxy credentials;

  static {
    WSSecurity.init();
  }

  public WsSecHandler( final CredentialProxy credentials ) {
    this.credentials = credentials;
  }

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) {
    final Object o = event.getMessage( );
    if ( o instanceof MappingHttpMessage ) {
      try {
        final MappingHttpMessage httpRequest = ( MappingHttpMessage ) o;
        OMElement elem = null;
        Document doc = null;
        SOAPEnvelope env = httpRequest.getSoapEnvelope( );
        HoldMe.canHas.lock( );
        try {
          final StAXOMBuilder doomBuilder = HoldMe.getStAXOMBuilder( HoldMe.getDOOMFactory( ), env.getXMLStreamReader( ) );
          elem = doomBuilder.getDocumentElement( );
          elem.build( );
          doc = ( ( Element ) elem ).getOwnerDocument( );
        } finally {
          HoldMe.canHas.unlock( );
        }

        final List<WSEncryptionPart> partsToSign = Lists.newArrayList();
        final WSSecHeader wsheader = new WSSecHeader( "", false );
        try {
          wsheader.insertSecurityHeader( doc );
        } catch ( WSSecurityException e ) {
          LOG.error( e, e );
          Channels.fireExceptionCaught( ctx, e );
        }

        final WSSecSignature signer = new WSSecSignature( );
        final WSSConfig config = WSSConfig.getNewInstance( );
        config.setWsiBSPCompliant( false );
        signer.setWsConfig( config );
        signer.setKeyIdentifierType( WSConstants.BST_DIRECT_REFERENCE );
        signer.setSigCanonicalization( WSConstants.C14N_EXCL_OMIT_COMMENTS );
        try {
          signer.prepare( doc, this.credentials, wsheader );
        } catch ( WSSecurityException e ) {
          LOG.error( doc );
          LOG.error( e, e );
          Channels.fireExceptionCaught( ctx, e );
        }

        if ( this.shouldTimeStamp( ) ) {
          final WSSecTimestamp ts = new WSSecTimestamp( );
          ts.setTimeToLive( 300 );
          ts.prepare( doc );
          ts.prependToHeader( wsheader );
        }
        partsToSign.addAll( this.getSignatureParts() );
        signer.appendBSTElementToHeader( wsheader );
        List<Reference> references = null;
        try {
          references = signer.addReferencesToSign( partsToSign, wsheader );
        } catch ( WSSecurityException e ) {
          LOG.error( doc );
          LOG.error( e, e );
          Channels.fireExceptionCaught( ctx, e );
        }

        try {
          signer.computeSignature( references, false, null );
        } catch ( WSSecurityException e ) {
          LOG.error( doc );
          LOG.error( e, e );
          Channels.fireExceptionCaught( ctx, e );
        }
        SOAPEnvelope envelope = null;
        HoldMe.canHas.lock( );
        try {
          final StAXSOAPModelBuilder stAXSOAPModelBuilder = new StAXSOAPModelBuilder( elem.getXMLStreamReader( ), HoldMe.getOMSOAP11Factory( ), null );
          envelope = stAXSOAPModelBuilder.getSOAPEnvelope( );
          if(envelope != null)
            envelope.build( );
        } finally {
          HoldMe.canHas.unlock( );
        }

        httpRequest.setSoapEnvelope( envelope );
      } catch ( OMException e ) {
        LOG.error( e, e );
        Channels.fireExceptionCaught( ctx, e );
      }
    }
  }

  public abstract Collection<WSEncryptionPart> getSignatureParts( );

  public abstract boolean shouldTimeStamp( );

}
