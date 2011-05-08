package com.eucalyptus.webui.client.activity;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.LoginPlace;
import com.eucalyptus.webui.client.place.StartPlace;
import com.eucalyptus.webui.client.view.StartView;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

/**
 * Showing the start page, providing guides for first-time users,
 * and shortcuts for experienced users.
 * 
 * @author Ye Wen (wenye@eucalyptus.com)
 *
 */
public class StartActivity extends AbstractActivity {
  
  public static final String TITLE = "WELCOME";
  
  private static final Logger LOG = Logger.getLogger( StartActivity.class.getName( ) );
  
  private ClientFactory clientFactory;
  private StartPlace place;
  
  public StartActivity( StartPlace place, ClientFactory clientFactory ) {
    this.clientFactory = clientFactory;
    this.place = place;
  }
  
  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    LOG.log( Level.INFO, "Start StartActivity" );
    this.clientFactory.getShellView( ).getContentView( ).setContentTitle( TITLE );
    StartView startView = this.clientFactory.getStartView( );
    container.setWidget( startView );
  }
  
}
