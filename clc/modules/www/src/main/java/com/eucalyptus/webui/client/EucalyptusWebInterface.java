package com.eucalyptus.webui.client;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.mapper.WebUiActivityMapper;
import com.eucalyptus.webui.client.mapper.WebUiPlaceHistoryMapper;
import com.eucalyptus.webui.client.place.LoginPlace;
import com.eucalyptus.webui.client.view.ShellViewImpl;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;

public class EucalyptusWebInterface implements EntryPoint {

  private static Logger LOG = Logger.getLogger( "EucalyptusWebInterface" );
  
  public static ShellViewImpl shell;
  
  private Place defaultPlace = new LoginPlace( );
  private AppWidget appWidget = new AppWidget( );
  
  @Override
  public void onModuleLoad( ) {
    GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
      public void onUncaughtException(Throwable e) {
        LOG.log(Level.SEVERE, e.getMessage(), e);
      }
    });
    // Create ClientFactory using deferred binding so we can replace with different
    // impls in gwt.xml
    ClientFactory clientFactory = GWT.create( ClientFactory.class );
    EventBus eventBus = clientFactory.getEventBus();
    PlaceController placeController = clientFactory.getPlaceController( );

    // Start ActivityManager for the main widget with our ActivityMapper
    ActivityMapper activityMapper = new WebUiActivityMapper( clientFactory );
    ActivityManager activityManager = new ActivityManager( activityMapper, eventBus );
    activityManager.setDisplay( appWidget );

    // Start PlaceHistoryHandler with our PlaceHistoryMapper
    WebUiPlaceHistoryMapper historyMapper= GWT.create(WebUiPlaceHistoryMapper.class);
    PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(historyMapper);
    historyHandler.register(placeController, eventBus, defaultPlace);

    shell = ( ShellViewImpl ) clientFactory.getStartView( );
    //RootLayoutPanel.get().add(appWidget);
    // Goes to place represented on URL or default place
    historyHandler.handleCurrentHistory( );
    //RootLayoutPanel.get( ).add( shell = new ShellViewImpl( ) );
  }
  
}
