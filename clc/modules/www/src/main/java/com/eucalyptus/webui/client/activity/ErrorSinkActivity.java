package com.eucalyptus.webui.client.activity;

import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.ErrorSinkPlace;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class ErrorSinkActivity extends AbstractActivity {
  
  public static final String TITLE = "ERROR";
  
  private ClientFactory clientFactory;
  private ErrorSinkPlace place;
  
  public ErrorSinkActivity( ErrorSinkPlace place, ClientFactory clientFactory ) {
    this.place = place;
    this.clientFactory = clientFactory;
  }
  
  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    this.clientFactory.getShellView( ).getContentView( ).setContentTitle( TITLE );
    container.setWidget( this.clientFactory.getErrorSinkView( ) );
  }
  
}
