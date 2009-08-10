package com.eucalyptus.ws.handlers.wssecurity;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Collection;

import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPConstants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.auth.Credentials;
import com.eucalyptus.auth.User;
import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.ws.MappingHttpMessage;
import com.eucalyptus.ws.util.CredentialProxy;
import com.eucalyptus.ws.util.WSSecurity;
import com.google.common.collect.Lists;

public class InternalWsSecHandler extends WsSecHandler {

  public InternalWsSecHandler( ) throws GeneralSecurityException {
    super( new CredentialProxy( EucaKeyStore.getInstance().getCertificate( EucalyptusProperties.NAME ), EucaKeyStore.getInstance().getKeyPair( EucalyptusProperties.NAME, EucalyptusProperties.NAME ).getPrivate( ) ) );
  }

  @Override
  public Collection<WSEncryptionPart> getSignatureParts( ) {
    return Lists.newArrayList( new WSEncryptionPart( WSConstants.TIMESTAMP_TOKEN_LN, WSConstants.WSU_NS, "Content" ), new WSEncryptionPart( SOAPConstants.BODY_LOCAL_NAME, SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI, "Content" ) );
  }

  @Override
  public boolean shouldTimeStamp( ) {
    return true;
  }

  @Override
  public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
    final Object o = event.getMessage( );
    if ( o instanceof MappingHttpMessage ) {
      final MappingHttpMessage httpRequest = ( MappingHttpMessage ) o;
      SOAPEnvelope envelope = httpRequest.getSoapEnvelope( );
      X509Certificate cert = WSSecurity.getVerifiedCertificate( envelope );
      String userName = Credentials.Users.getUserName( cert );
      User user = Credentials.getUser( userName );
      httpRequest.setUser( user );
    }
  }
}
