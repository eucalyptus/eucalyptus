package com.eucalyptus.webui.client.activity;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.StartPlace;
import com.eucalyptus.webui.client.view.StartView;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class StartActivity extends AbstractActivity {
  
  private static Logger LOG = Logger.getLogger( "StartActivity" );
  
  private ClientFactory clientFactory;
  
  public StartActivity( StartPlace place, ClientFactory clientFactory ) {
    this.clientFactory = clientFactory;
    LOG.log( Level.INFO, "Creating StartActivity" );
  }
  
  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    LOG.log( Level.INFO, "Start StartActivity" );
    StartView startView = clientFactory.getStartView( );
    container.setWidget( startView );    
  }
  
}
