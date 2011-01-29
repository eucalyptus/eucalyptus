package com.eucalyptus.event;


public interface EventListener {
  public <T extends Event> void fireEvent( T event );
}
