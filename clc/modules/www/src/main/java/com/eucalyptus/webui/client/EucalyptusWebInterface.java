package com.eucalyptus.webui.client;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.RootPanel;

public class EucalyptusWebInterface implements EntryPoint {

  private static Logger LOG = Logger.getLogger( "EucalyptusWebInterface" );
  
  private final EuareServiceAsync euareService = GWT.create( EuareService.class );
  
  @Override
  public void onModuleLoad( ) {
    Button button = new Button( "Click" );
    button.addClickHandler( new ClickHandler( ) {

      @Override
      public void onClick( ClickEvent arg0 ) {
        callEuareService( "Hello" );
      }
      
    });
    
    RootPanel.get( ).add( button );
  }

  private void callEuareService( String message ) {
    euareService.test( message, new AsyncCallback<String>() {

      @Override
      public void onFailure( Throwable e ) {
        LOG.log( Level.WARNING, "Failed to call the service: " + e.getMessage( ) );
      }

      @Override
      public void onSuccess( String result ) {
        LOG.log( Level.INFO, "Got from server: " + result );
      }
      
    });
  }
}
