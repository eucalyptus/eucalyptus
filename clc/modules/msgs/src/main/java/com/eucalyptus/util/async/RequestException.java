package com.eucalyptus.util.async;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class RequestException extends RuntimeException {
  final BaseMessage msg;

  public RequestException( BaseMessage msg ) {
    super( msg.toSimpleString( ) );
    this.msg = msg;
  }

  public RequestException( String message, Throwable ex, BaseMessage msg ) {
    super( message + ":" + msg.toSimpleString( ), ex );
    this.msg = msg;
  }

  public RequestException( String message, BaseMessage msg ) {
    super( message + ":" + msg.toSimpleString( ) );
    this.msg = msg;
  }

  public RequestException( Throwable ex, BaseMessage msg ) {
    super( ex.getMessage( ) + ":" + msg.toSimpleString( ),  ex );
    this.msg = msg;
  }

  public <T extends BaseMessage> T getRequest( ) {
    return ( T ) this.msg;
  }

}
