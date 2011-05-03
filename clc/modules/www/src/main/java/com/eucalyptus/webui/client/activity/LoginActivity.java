package com.eucalyptus.webui.client.activity;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.LoginPlace;
import com.eucalyptus.webui.client.place.StartPlace;
import com.eucalyptus.webui.client.view.LoginView;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.RootLayoutPanel;

public class LoginActivity extends AbstractActivity implements LoginView.Presenter {
  
  private static Logger LOG = Logger.getLogger( "LoginActivity" );
  
  private ClientFactory clientFactory;
  
  public LoginActivity( LoginPlace place, ClientFactory clientFactory ) {
    this.clientFactory = clientFactory;
  }
  
  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    LoginView loginView = clientFactory.getLoginView( );
    loginView.setPresenter( this );
    container.setWidget( loginView );
  }
  
  @Override
  public void login( String username, String password, boolean staySignedIn ) {
    LOG.log( Level.INFO, "login:" + username + " password:" + password + " staySignedIn:" + staySignedIn );
    try {
      clientFactory.getPlaceController( ).goTo( new StartPlace( ) );
    } catch ( Exception e ) {
      LOG.log( Level.WARNING, "Exception: " + e.getCause( ) );
    }
    //RootLayoutPanel.get( ).clear( );
    //RootLayoutPanel.get( ).add( clientFactory.getStartView( ) );
  }
  
}
