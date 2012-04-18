package com.eucalyptus.util.async;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class NoResponseException extends RequestException {

  public NoResponseException( BaseMessage msg ) {
    super( msg );
  }

  public NoResponseException( String message, BaseMessage msg ) {
    super( message, msg );
  }

  public NoResponseException( String message, Throwable ex, BaseMessage msg ) {
    super( message, ex, msg );
  }

  public NoResponseException( Throwable ex, BaseMessage msg ) {
    super( ex, msg );
  }

}
