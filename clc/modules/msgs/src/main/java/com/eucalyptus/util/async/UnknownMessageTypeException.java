package com.eucalyptus.util.async;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class UnknownMessageTypeException extends RequestException {
  public final Object message;
  public UnknownMessageTypeException( Object message, BaseMessage msg ) {
    super( msg );
    this.message = message;
  }

  public UnknownMessageTypeException( String descr, BaseMessage msg, Object message ) {
    super( descr, msg );
    this.message = message;
  }

  public UnknownMessageTypeException( String descr, Throwable ex, BaseMessage msg, Object message ) {
    super( descr, ex, msg );
    this.message = message;
  }

  public UnknownMessageTypeException( Throwable ex, BaseMessage msg, Object message ) {
    super( ex, msg );
    this.message = message;
  }
  
  public Object getUnknownMessage( ) {
    return this.message;
  }

}
