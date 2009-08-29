package com.eucalyptus.event;


public interface EventListener {
  public void advertiseEvent( Event event );
  public void fireEvent( Event event );
}
