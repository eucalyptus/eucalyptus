package com.eucalyptus.util.async;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * TODO: DOCUMENT
 */
public class ConnectionException extends RequestException {

  public ConnectionException( BaseMessage msg ) {
    super( msg );
  }

  public ConnectionException( String message, BaseMessage msg ) {
    super( message, msg );
  }

  public ConnectionException( String message, Throwable ex, BaseMessage msg ) {
    super( message, ex, msg );
  }

  public ConnectionException( Throwable ex, BaseMessage msg ) {
    super( ex, msg );
  }

}
