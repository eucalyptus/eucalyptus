package com.eucalyptus.webui.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.place.shared.PlaceChangeEvent.Handler;

public class AuthenticationDoneEvent extends GwtEvent<AuthenticationDoneEvent.Handler> {
  
  public interface Handler extends EventHandler {
    void onAuthenticationDone( AuthenticationDoneEvent event );
  }
  
  public static final Type<Handler> TYPE = new Type<Handler>();
  
  private final String sessionId;
  
  public AuthenticationDoneEvent( String sessionId ) {
    this.sessionId = sessionId;
  }

  public String getSessionId( ) {
    return this.sessionId;
  }
  
  @Override
  public Type<Handler> getAssociatedType( ) {
    return TYPE;
  }

  @Override
  protected void dispatch( Handler handler ) {
    handler.onAuthenticationDone( this );
  }
  
}
