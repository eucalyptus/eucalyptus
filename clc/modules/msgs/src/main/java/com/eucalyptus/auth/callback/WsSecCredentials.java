package com.eucalyptus.auth.callback;

import org.apache.axiom.soap.SOAPEnvelope;

public class WsSecCredentials extends WrappedCredentials<SOAPEnvelope> {

  
  public WsSecCredentials( String correlationId, SOAPEnvelope loginData ) {
    super( correlationId, loginData );
  }
  
}
