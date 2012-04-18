package com.eucalyptus.webui.client;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.view.GlobalResources;
import com.eucalyptus.webui.client.view.ShellViewImpl;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.ui.RootLayoutPanel;

public class EucalyptusWebInterface implements EntryPoint {

  private static final Logger LOG = Logger.getLogger( EucalyptusWebInterface.class.getName( ) );
  
  private EucalyptusApp app;
  
  @Override
  public void onModuleLoad( ) {
    // Make sure we catch any uncaught exceptions, for debugging purpose.
    GWT.setUncaughtExceptionHandler( new GWT.UncaughtExceptionHandler( ) {
      public void onUncaughtException( Throwable e ) {
        if ( e != null ) {
          LOG.log( Level.SEVERE, e.getMessage( ), e );
        }
      }
    } );
    GWT.<GlobalResources>create( GlobalResources.class ).buttonCss( ).ensureInjected( );
    // Create ClientFactory using deferred binding so we can
    // replace with different implementations in gwt.xml
    ClientFactory clientFactory = GWT.create( ClientFactory.class );
    // Start
    app = new EucalyptusApp( clientFactory );
    app.start( new AppWidget( RootLayoutPanel.get( ) ) );
  }
  
}
