package com.eucalyptus.event;



public interface EventListener<T extends Event> {
  public void fireEvent( T event );
}
