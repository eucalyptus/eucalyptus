/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.handlers.wssecurity;

import java.util.Collection;
import java.util.Vector;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.log4j.Logger;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecSignature;
import org.apache.ws.security.message.WSSecTimestamp;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.eucalyptus.ws.util.CredentialProxy;

@ChannelPipelineCoverage( "all" )
public abstract class WsSecHandler extends MessageStackHandler {
  private static Logger         LOG    = Logger.getLogger( WsSecHandler.class );
  private final CredentialProxy credentials;

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

        final Vector v = new Vector( );
        final WSSecHeader wsheader = new WSSecHeader( "", false );
        wsheader.insertSecurityHeader( doc );

        final WSSecSignature signer = new WSSecSignature( );
        signer.setKeyIdentifierType( WSConstants.BST_DIRECT_REFERENCE );
        signer.setSigCanonicalization( WSConstants.C14N_EXCL_OMIT_COMMENTS );
        try {
          signer.prepare( doc, this.credentials, wsheader );
        } catch ( WSSecurityException e ) {
          LOG.error( e, e );
          Channels.fireExceptionCaught( ctx, e );
        }

        if ( this.shouldTimeStamp( ) ) {
          final WSSecTimestamp ts = new WSSecTimestamp( );
          ts.setTimeToLive( 300 );
          ts.prepare( doc );
          ts.prependToHeader( wsheader );
        }
        v.addAll( this.getSignatureParts( ) );
        signer.appendBSTElementToHeader( wsheader );
        signer.appendToHeader( wsheader );
        try {
          signer.addReferencesToSign( v, wsheader );
        } catch ( WSSecurityException e ) {
          LOG.error( e, e );
          Channels.fireExceptionCaught( ctx, e );
        }

        try {
          signer.computeSignature( );
        } catch ( WSSecurityException e ) {
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

  @Override
  public void incomingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
  }

}
