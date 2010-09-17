package com.eucalyptus.util.async;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class RequestInitializationException extends RequestException {

  public RequestInitializationException( BaseMessage msg ) {
    super( msg );
  }

  public RequestInitializationException( String message, BaseMessage msg ) {
    super( message, msg );
  }

  public RequestInitializationException( String message, Throwable ex, BaseMessage msg ) {
    super( message, ex, msg );
  }

  public RequestInitializationException( Throwable ex, BaseMessage msg ) {
    super( ex, msg );
  }

}
