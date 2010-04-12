/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.auth.util;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.log4j.Logger;
import org.apache.ws.security.SOAPConstants;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.message.EnvelopeIdResolver;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.ws.security.message.token.Reference;
import org.apache.ws.security.message.token.SecurityTokenReference;
import org.apache.ws.security.message.token.X509Security;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.XMLSignatureException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.auth.login.SecurityContext;
import com.eucalyptus.binding.HoldMe;

public class WSSecurity {
  private static Logger             LOG = Logger.getLogger( WSSecurity.class );
  private static CertificateFactory factory;
  static {
    org.apache.xml.security.Init.init( );
    WSSConfig.getDefaultWSConfig( ).addJceProvider( "BC", BouncyCastleProvider.class.getCanonicalName( ) );
    WSSConfig.getDefaultWSConfig( ).setTimeStampStrict( true );
    WSSConfig.getDefaultWSConfig( ).setEnableSignatureConfirmation( true );
  }
  
  public static CertificateFactory getCertificateFactory( ) {
    if ( factory == null ) {
      try {
        factory = CertificateFactory.getInstance( "X.509"/*, "BC"*/);
      } catch ( CertificateException e ) {
        LOG.error( e, e );
      } /*catch ( NoSuchProviderException e ) {
        LOG.error( e, e );
        }*/
    }
    return factory;
  }
  
  private static boolean useBc = false;
  
  public static X509Certificate verifySignature( final Element securityNode, final XMLSignature sig ) throws WSSecurityException, XMLSignatureException {
    final SecurityTokenReference secRef = WSSecurity.getSecurityTokenReference( sig.getKeyInfo( ) );
    final Reference tokenRef = secRef.getReference( );
    Element bstDirect = WSSecurityUtil.getElementByWsuId( securityNode.getOwnerDocument( ), tokenRef.getURI( ) );
    if ( bstDirect == null ) {
      bstDirect = WSSecurityUtil.getElementByGenId( securityNode.getOwnerDocument( ), tokenRef.getURI( ) );
      if ( bstDirect == null ) {
        throw new WSSecurityException( WSSecurityException.INVALID_SECURITY, "noCert" );
      }
    }
    BinarySecurity token = new BinarySecurity( bstDirect );
    String type = token.getValueType( );
    X509Certificate cert = null;
    try {
      if ( useBc ) {
        Node node = bstDirect.getFirstChild( );
        String certStr = ( "-----BEGIN CERTIFICATE-----\n" + ( node == null || !( node instanceof Text ) ? null : ( ( Text ) node ).getData( ) ) + "\n-----END CERTIFICATE-----\n" );
        ByteArrayInputStream pemByteIn = new ByteArrayInputStream( certStr.getBytes( ) );
        PEMReader in = new PEMReader( new InputStreamReader( pemByteIn ) );
        try {
          cert = ( X509Certificate ) in.readObject( );
        } catch ( Throwable e ) {
          LOG.error( e, e );
        }
      } else {
        X509Security x509 = new X509Security( bstDirect );
        byte[] bstToken = x509.getToken( );
        CertificateFactory factory = getCertificateFactory( );
        cert = ( X509Certificate ) factory.generateCertificate( new ByteArrayInputStream( bstToken ) );
      }
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new WSSecurityException( WSSecurityException.UNSUPPORTED_SECURITY_TOKEN, "unsupportedBinaryTokenType", new Object[] { type } );
    }
    if ( !sig.checkSignatureValue( cert ) ) {
      throw new WSSecurityException( WSSecurityException.FAILED_CHECK );
    }
    return cert;
  }
  
  public static XMLSignature getXmlSignature( final Element signatureNode ) throws WSSecurityException {
    XMLSignature sig = null;
    try {
      sig = new XMLSignature( ( Element ) signatureNode, null );
    } catch ( XMLSecurityException e2 ) {
      throw new WSSecurityException( WSSecurityException.FAILED_CHECK, "noXMLSig", null, e2 );
    }
    sig.addResourceResolver( EnvelopeIdResolver.getInstance( ) );
    return sig;
  }
  
  public static SecurityTokenReference getSecurityTokenReference( KeyInfo info ) throws WSSecurityException {
    Node secTokenRef = WSSecurityUtil.getDirectChild( info.getElement( ), SecurityTokenReference.SECURITY_TOKEN_REFERENCE, WSConstants.WSSE_NS );
    if ( secTokenRef == null ) {
      throw new WSSecurityException( WSSecurityException.INVALID_SECURITY, "unsupportedKeyInfo" );
    }
    
    SecurityTokenReference secRef;
    try {
      secRef = new SecurityTokenReference( ( Element ) secTokenRef );
    } catch ( WSSecurityException e1 ) {
      LOG.error( e1, e1 );
      throw e1;
    }
    if ( !secRef.containsReference( ) ) throw new WSSecurityException( WSSecurityException.FAILED_CHECK );
    return secRef;
  }
  
  public static Element getSignatureElement( final Element securityNode ) {
    final Element signatureNode = ( Element ) WSSecurityUtil.getDirectChildElement( securityNode, WSConstants.SIG_LN, WSConstants.SIG_NS );
    return signatureNode;
  }
  
  private static Element getSecurityElement( final Element env ) {
    final SOAPConstants soapConstants = WSSecurityUtil.getSOAPConstants( env );
    final Element soapHeaderElement = ( Element ) WSSecurityUtil.getDirectChildElement( env, soapConstants.getHeaderQName( ).getLocalPart( ),
                                                                                        soapConstants.getEnvelopeURI( ) );
    if ( soapHeaderElement != null ) {
      final Element securityNode = ( Element ) WSSecurityUtil.getDirectChildElement( soapHeaderElement, WSConstants.WSSE_LN, WSConstants.WSSE_NS );
      return securityNode;
    }
    return null;
  }
  
  public static Element getSecurityElement( SOAPEnvelope envelope ) {
    final StAXOMBuilder doomBuilder = HoldMe.getStAXOMBuilder( HoldMe.getDOOMFactory( ), envelope.getXMLStreamReader( ) );
    final OMElement elem = doomBuilder.getDocumentElement( );
    elem.build( );
    final Element env = ( ( Element ) elem );
    final Element securityNode = getSecurityElement( env );
    return securityNode;
  }
  
  public static XMLSignature getXMLSignature( final Element securityNode ) throws WSSecurityException, XMLSignatureException {
    if ( securityNode != null ) {
      XMLSignature sig = checkSignature( securityNode );
      return sig;
    } else {
      throw new XMLSignatureException( "No BST Element." );
    }
  }

  private static XMLSignature checkSignature( final Element securityNode ) throws WSSecurityException, XMLSignatureException {
    final Element signatureNode = getSignatureElement( securityNode );
    final XMLSignature sig = getXmlSignature( signatureNode );
    if ( sig.getKeyInfo( ) == null ) throw new WSSecurityException( WSSecurityException.SECURITY_TOKEN_UNAVAILABLE );
    return sig;
  }
  
}
