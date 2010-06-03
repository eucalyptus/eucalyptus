package com.eucalyptus.util;

import com.eucalyptus.records.EventType;
import com.eucalyptus.records.EventRecord;

public abstract class SimpleTransition<O, T extends Comparable> extends Transition<O, T> {
  
  public SimpleTransition( String name, T oldState, T newState ) {
    super( name, oldState, newState );
    EventRecord.caller( this.getClass( ), EventType.TRANSITION_BEGIN, this.toString( ) );
  }

  /**
   * @see com.eucalyptus.util.Transition#post(java.lang.Object)
   * @param component
   * @throws Exception
   */
  @Override
  protected void post( O component ) throws Exception {}


  /**
   * @see com.eucalyptus.util.Transition#prepare(java.lang.Object)
   * @param component
   * @throws Exception
   */
  @Override
  protected void prepare( O component ) throws Exception {}


  /**
   * @see com.eucalyptus.util.Transition#rollback(java.lang.Object)
   * @param component
   */
  @Override
  protected void rollback( O component ) {}

}
