package com.eucalyptus.webui.client.activity;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.ApplyPlace;
import com.eucalyptus.webui.client.place.ApplyPlace.ApplyType;
import com.eucalyptus.webui.client.place.LoginPlace;
import com.eucalyptus.webui.client.place.ShellPlace;
import com.eucalyptus.webui.client.service.Session;
import com.eucalyptus.webui.client.view.LoginView;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

/**
 * Login process. Always the first activity when the web app loads.
 * 
 * @author Ye Wen (wenye@eucalyptus.com)
 *
 */
public class LoginActivity extends AbstractActivity implements LoginView.Presenter {

  private static final Logger LOG = Logger.getLogger( LoginActivity.class.getName( ) );
                                                     
  private ClientFactory clientFactory;
  private LoginPlace place;
  
  public LoginActivity( LoginPlace place, ClientFactory clientFactory ) {
    this.place = place;
    this.clientFactory = clientFactory;
  }
  
  private void showLoginView( AcceptsOneWidget container ) {
    LoginView loginView = this.clientFactory.getLoginView( );
    loginView.setPresenter( this );
    loginView.setPrompt( place.getPrompt( ) );
    container.setWidget( loginView );
    loginView.clearPassword( );
    loginView.setFocus( );
  }

  /**
   * This is called when the user click on the submit button of the login screen.
   */
  @Override
  public void login( String accountName, String userName, String password, final boolean staySignedIn ) {
    this.clientFactory.getBackendService( ).login( accountName, userName, password, new AsyncCallback<Session>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        LOG.log( Level.WARNING, "Login failed: " + caught );
        clientFactory.getLoginView( ).setPrompt( LoginPlace.LOGIN_FAILURE_PROMPT );
      }

      @Override
      public void onSuccess( Session session ) {
        if ( session == null ) {
          LOG.log( Level.WARNING, "Login failed: empty session" );
          clientFactory.getLoginView( ).setPrompt( LoginPlace.LOGIN_FAILURE_PROMPT );
        } else {
          // Login success. Save the session (persistent if the user wants to stay signed in)
          clientFactory.getLocalSession( ).saveSession( session, staySignedIn );
          // Starting the shell
          clientFactory.getLifecyclePlaceController( ).goTo( new ShellPlace( session ) );
        }
      }
      
    } );
    
  }

  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    // Check stored session first. If empty, load login screen.
    Session session = this.clientFactory.getLocalSession( ).getSession( );
    if ( session == null ) {
      showLoginView( container );
    } else {
      this.clientFactory.getLifecyclePlaceController( ).goTo( new ShellPlace( session ) );
    }
  }

  @Override
  public void onAccountSignup( ) {
    this.clientFactory.getLifecyclePlaceController( ).goTo( new ApplyPlace( ApplyType.ACCOUNT ) );
  }

  @Override
  public void onUserSignup( ) {
    this.clientFactory.getLifecyclePlaceController( ).goTo( new ApplyPlace( ApplyType.USER ) );
  }

  @Override
  public void onRecoverPassword( ) {
    this.clientFactory.getLifecyclePlaceController( ).goTo( new ApplyPlace( ApplyType.PASSWORD_RESET ) );
  }

}
