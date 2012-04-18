package com.eucalyptus.webui.client.activity;

import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.ErrorSinkPlace;
import com.eucalyptus.webui.client.view.ErrorSinkView;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class ErrorSinkActivity extends AbstractActivity {
  
  public static final String TITLE = "ERROR";
  
  public static final String ERROR_TARGET = "Error in URL or search target!";
  
  private ClientFactory clientFactory;
  private ErrorSinkPlace place;
  
  public ErrorSinkActivity( ErrorSinkPlace place, ClientFactory clientFactory ) {
    this.place = place;
    this.clientFactory = clientFactory;
  }
  
  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    this.clientFactory.getShellView( ).getContentView( ).setContentTitle( TITLE );
    ErrorSinkView view = this.clientFactory.getErrorSinkView( );
    view.setMessage( ERROR_TARGET );
    container.setWidget( view );
  }
  
}
