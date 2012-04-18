package com.eucalyptus.util.async;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class FailedRequestException extends RequestException {

  public FailedRequestException( BaseMessage msg ) {
    super( msg );
  }

  public FailedRequestException( String message, BaseMessage msg ) {
    super( message, msg );
  }

  public FailedRequestException( String message, Throwable ex, BaseMessage msg ) {
    super( message, ex, msg );
  }

  public FailedRequestException( Throwable ex, BaseMessage msg ) {
    super( ex, msg );
  }
}
