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

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.crypto.util.WSSecurity;
import com.eucalyptus.ws.IoMessage;
import com.eucalyptus.ws.util.CredentialProxy;
import com.google.common.collect.Lists;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 *
 */
public abstract class IoWsSecHandler extends ChannelDuplexHandler {

  private static Logger LOG    = Logger.getLogger( IoWsSecHandler.class );
  private final Function<ChannelHandlerContext,CredentialProxy> credentialLookup;

  static {
    WSSecurity.init( );
  }

  public IoWsSecHandler( final CredentialProxy credentials ) {
    this( ctx -> credentials );
  }

  public IoWsSecHandler( final Function<ChannelHandlerContext, CredentialProxy> credentialLookup ) {
    this.credentialLookup = credentialLookup;
  }

  @Override
  public void write( final io.netty.channel.ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise ) throws Exception {
    final Object o = msg;
    if ( o instanceof IoMessage ) {
      try {
        final IoMessage ioMessage = (IoMessage) o;
        OMElement elem = null;
        Document doc = null;
        SOAPEnvelope env = ioMessage.getSoapEnvelope( );
        HoldMe.canHas.lock( );
        try {
          final StAXOMBuilder doomBuilder = HoldMe.getStAXOMBuilder( HoldMe.getDOOMFactory( ), env.getXMLStreamReader( ) );
          elem = doomBuilder.getDocumentElement( );
          elem.build( );
          doc = ( (Element) elem ).getOwnerDocument( );
        } finally {
          HoldMe.canHas.unlock( );
        }

        final List<WSEncryptionPart> partsToSign = Lists.newArrayList( );
        final WSSecHeader wsheader = new WSSecHeader( "", false );
        try {
          wsheader.insertSecurityHeader( doc );
        } catch ( WSSecurityException e ) {
          LOG.error( e, e );
          ctx.fireExceptionCaught( e );
        }

        final WSSecSignature signer = new WSSecSignature( );
        final WSSConfig config = WSSConfig.getNewInstance( );
        config.setWsiBSPCompliant( false );
        signer.setWsConfig( config );
        signer.setKeyIdentifierType( WSConstants.BST_DIRECT_REFERENCE );
        signer.setSigCanonicalization( WSConstants.C14N_EXCL_OMIT_COMMENTS );
        try {
          signer.prepare( doc, this.credentialLookup.apply( ctx ), wsheader );
        } catch ( WSSecurityException e ) {
          LOG.error( doc );
          LOG.error( e, e );
          ctx.fireExceptionCaught( e );
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
          ctx.fireExceptionCaught( e );
        }

        try {
          signer.computeSignature( references, false, null );
        } catch ( WSSecurityException e ) {
          LOG.error( doc );
          LOG.error( e, e );
          ctx.fireExceptionCaught( e );
        }
        SOAPEnvelope envelope = null;
        HoldMe.canHas.lock( );
        try {
          final StAXSOAPModelBuilder stAXSOAPModelBuilder = new StAXSOAPModelBuilder( elem.getXMLStreamReader( ), HoldMe.getOMSOAP11Factory( ), null );
          envelope = stAXSOAPModelBuilder.getSOAPEnvelope( );
          if (envelope != null)
            envelope.build( );
        } finally {
          HoldMe.canHas.unlock( );
        }

        ioMessage.setSoapEnvelope( envelope );
      } catch ( OMException e ) {
        LOG.error( e, e );
        ctx.fireExceptionCaught( e );
      }
    }
    super.write( ctx, msg, promise );
  }

  public abstract Collection<WSEncryptionPart> getSignatureParts( );

  public abstract boolean shouldTimeStamp( );
}
