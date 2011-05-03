package com.eucalyptus.webui.client.activity;

import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.event.AuthenticationDoneEvent;
import com.eucalyptus.webui.client.util.LocalSession;
import com.eucalyptus.webui.client.view.LoginView;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

/*
 * Authenticate current users. Prompt signing in if necessary using LoginView.
 * This is a gatekeeper for the entrance to all activities.
 */
public class Authenticator extends AbstractActivity implements LoginView.Presenter {

  private ClientFactory clientFactory;
  
  public Authenticator( ClientFactory clientFactory ) {
    this.clientFactory = clientFactory;
  }
  
  private void showLoginView( AcceptsOneWidget container ) {
    LoginView loginView = clientFactory.getLoginView( );
    loginView.setPresenter( this );
    container.setWidget( loginView );
  }

  @Override
  public void login( String username, String password, boolean staySignedIn ) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    String session = clientFactory.getLocalSession( ).loadSessionId( );
    if ( session == null ) {
      showLoginView( container );
    } else {
      clientFactory.getEventBus( ).fireEvent( new AuthenticationDoneEvent( session ) );
    }
  }

}
