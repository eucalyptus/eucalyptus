package com.eucalyptus.auth.login;

public class WalrusInternalWrappedCredentials extends WrappedCredentials<String> {
  
  private final String signature;
  private final String certString;
  
  public WalrusInternalWrappedCredentials( String correlationId, String verb, String addr, String date, String signature, String certString ) {
    super( correlationId, verb + "\n" + date + "\n" + addr );
    this.signature = signature;
    this.certString = certString;
  }
  
  public String getSignature( ) {
    return this.signature;
  }
  
  public String getCertString( ) {
    return this.certString;
  }
  
}
