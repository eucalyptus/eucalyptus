package com.eucalyptus.ws.handlers.wssecurity;

import com.eucalyptus.ws.util.CredentialProxy;
import com.eucalyptus.ws.util.EucaKeyStore;
import com.eucalyptus.ws.util.EucalyptusProperties;
import com.google.common.collect.Lists;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPConstants;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;

import java.util.Collection;

public class InternalWsSecHandler extends WsSecHandler {

  public InternalWsSecHandler( ) {
    // super( new CredentialProxy( ServiceKeyStore.getInstance(),
    // EucalyptusProperties.NAME, EucalyptusProperties.NAME ) );
    super( new CredentialProxy( EucaKeyStore.getInstance( ), EucalyptusProperties.NAME, EucalyptusProperties.NAME ) );
  }

  @Override
  public Collection<WSEncryptionPart> getSignatureParts( ) {
    return Lists.newArrayList( new WSEncryptionPart( WSConstants.TIMESTAMP_TOKEN_LN, WSConstants.WSU_NS, "Content" ), new WSEncryptionPart( SOAPConstants.BODY_LOCAL_NAME, SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI, "Content" ) );
  }

  @Override
  public boolean shouldTimeStamp( ) {
    return true;
  }
}
