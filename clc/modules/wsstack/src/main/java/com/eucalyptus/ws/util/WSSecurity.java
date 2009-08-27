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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.util;

import java.io.ByteArrayInputStream;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.DOOMAbstractFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.log4j.Logger;
import org.apache.ws.security.SOAPConstants;
import org.apache.ws.security.WSConstants;
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
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.eucalyptus.auth.Credentials;

public class WSSecurity {
  private static Logger             LOG = Logger.getLogger( WSSecurity.class );
  private static CertificateFactory factory;
  static {
    Credentials.init( );
  }

  public static CertificateFactory getCertificateFactory( ) {
    if ( factory == null ) {
      try {
        factory = CertificateFactory.getInstance( "X.509", "BC" );
      } catch ( CertificateException e ) {
        LOG.error( e, e );
      } catch ( NoSuchProviderException e ) {
        LOG.error( e, e );
      }
    }
    return factory;
  }

  public static X509Certificate verifySignature( final Element securityNode, final XMLSignature sig ) throws WSSecurityException, XMLSignatureException {
    final SecurityTokenReference secRef = WSSecurity.getSecurityTokenReference( sig.getKeyInfo( ) );
    final Reference tokenRef = secRef.getReference( );
    Element bstDirect = WSSecurityUtil.getElementByWsuId( securityNode.getOwnerDocument( ), tokenRef.getURI( ) );
    if ( bstDirect == null ) {
      bstDirect = WSSecurityUtil.getElementByGenId( securityNode.getOwnerDocument( ), tokenRef.getURI( ) );
      if ( bstDirect == null ) { throw new WSSecurityException( WSSecurityException.INVALID_SECURITY, "noCert" ); }
    }
    BinarySecurity token = new BinarySecurity( bstDirect );
    String type = token.getValueType( );
    X509Security x509 = null;
    X509Certificate cert = null;
    try {
      x509 = new X509Security( bstDirect );
      byte[] bstToken = x509.getToken( );
      CertificateFactory factory = getCertificateFactory( );
      cert = ( X509Certificate ) factory.generateCertificate( new ByteArrayInputStream( bstToken ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new WSSecurityException( WSSecurityException.UNSUPPORTED_SECURITY_TOKEN, "unsupportedBinaryTokenType", new Object[] { type } );
    }
    if ( !sig.checkSignatureValue( cert ) ) { throw new WSSecurityException( WSSecurityException.FAILED_CHECK ); }
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
    if ( secTokenRef == null ) { throw new WSSecurityException( WSSecurityException.INVALID_SECURITY, "unsupportedKeyInfo" ); }

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

  public static Element getSecurityElement( final Element env ) {
    final SOAPConstants soapConstants = WSSecurityUtil.getSOAPConstants( env );
    final Element soapHeaderElement = ( Element ) WSSecurityUtil.getDirectChildElement( env, soapConstants.getHeaderQName( ).getLocalPart( ), soapConstants.getEnvelopeURI( ) );
    final Element securityNode = ( Element ) WSSecurityUtil.getDirectChildElement( soapHeaderElement, WSConstants.WSSE_LN, WSConstants.WSSE_NS );
    return securityNode;
  }

  public static X509Certificate getVerifiedCertificate( SOAPEnvelope envelope ) throws WSSecurityException, XMLSignatureException {
    final StAXOMBuilder doomBuilder = new StAXOMBuilder( DOOMAbstractFactory.getOMFactory( ), envelope.getXMLStreamReader( ) );
    final OMElement elem = doomBuilder.getDocumentElement( );
    elem.build( );
//    final Document doc = ( ( Element ) elem ).getOwnerDocument( );
    final Element env = ( ( Element ) elem );
    final Element securityNode = getSecurityElement( env );
    final Element signatureNode = getSignatureElement( securityNode );
    final XMLSignature sig = getXmlSignature( signatureNode );
    if ( sig.getKeyInfo( ) == null ) throw new WSSecurityException( WSSecurityException.SECURITY_TOKEN_UNAVAILABLE );
    X509Certificate cert = verifySignature( securityNode, sig );
    return cert;
  }

}
