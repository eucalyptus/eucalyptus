package com.eucalyptus.util.async;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

public abstract class SubjectMessageCallback<P, Q extends BaseMessage, R extends BaseMessage> extends MessageCallback<Q, R> {
  private P subject;
  
  protected SubjectMessageCallback( ) {
    super( );
  }

  /**
   * @return the subject
   */
  public P getSubject( ) {
    if ( this.subject == null ) {
      throw new IllegalStateException( "The subject assocaited with the callback has not been set." );
    }
    return this.subject;
  }
  
  /**
   * @param subject the subject to set
   */
  public void setSubject( P subject ) {
    this.subject = subject;
  }
  
}
