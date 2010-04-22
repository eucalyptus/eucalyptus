package com.eucalyptus.auth.login;

import java.io.IOException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

public abstract class WrappedCredentials<TYPE> implements CallbackHandler {
  private TYPE loginData;
  private String correlationId;
  public WrappedCredentials( String correlationId, TYPE loginData ) {
    super( );
    this.loginData = loginData;
    this.correlationId = correlationId;
  }

  @Override
  public void handle( Callback[] callbacks ) throws IOException, UnsupportedCallbackException {}

  public TYPE getLoginData( ) {
    return this.loginData;
  }

  public String getCorrelationId( ) {
    return this.correlationId;
  }
  
}