package com.eucalyptus.ws.handlers.wssecurity;

import java.security.GeneralSecurityException;
import java.util.Collection;

import org.apache.ws.security.WSEncryptionPart;

import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.ws.util.CredentialProxy;
import com.google.common.collect.Lists;

public class ClusterWsSecHandler extends WsSecHandler {
  private static final String WSA_NAMESPACE = "http://www.w3.org/2005/08/addressing";

  public ClusterWsSecHandler( ) throws GeneralSecurityException {
    super( new CredentialProxy( SystemCredentialProvider.getCredentialProvider( Component.eucalyptus ).getCertificate( ), SystemCredentialProvider.getCredentialProvider( Component.eucalyptus ).getPrivateKey( ) ) );
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
