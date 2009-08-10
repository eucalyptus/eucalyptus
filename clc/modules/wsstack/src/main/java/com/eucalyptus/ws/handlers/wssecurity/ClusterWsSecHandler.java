package com.eucalyptus.ws.handlers.wssecurity;

import com.eucalyptus.ws.MappingHttpMessage;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.util.CredentialProxy;
import com.eucalyptus.ws.util.EucalyptusProperties;
import com.eucalyptus.ws.util.ServiceKeyStore;
import com.google.common.collect.Lists;

import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.ws.security.WSEncryptionPart;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

import java.security.GeneralSecurityException;
import java.util.Collection;

public class ClusterWsSecHandler extends WsSecHandler {
  private static final String WSA_NAMESPACE = "http://www.w3.org/2005/08/addressing";

  public ClusterWsSecHandler( ) throws GeneralSecurityException {
    super( new CredentialProxy( ServiceKeyStore.getInstance().getCertificate( EucalyptusProperties.NAME ), ServiceKeyStore.getInstance().getKeyPair( EucalyptusProperties.NAME, EucalyptusProperties.NAME ).getPrivate( ) ) );
  }

  @Override
  public Collection<WSEncryptionPart> getSignatureParts() {
    return Lists.newArrayList( new WSEncryptionPart( "To", WSA_NAMESPACE, "Content" ),new WSEncryptionPart( "MessageID", WSA_NAMESPACE, "Content" ), new WSEncryptionPart( "Action", WSA_NAMESPACE, "Content" ) );
  }
  
  

  @Override
  public boolean shouldTimeStamp() {
    return false;
  }

}
