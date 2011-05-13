package com.eucalyptus.webui.client.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.AppWidget;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.ExPlaceHistoryHandler;
import com.eucalyptus.webui.client.MainActivityMapper;
import com.eucalyptus.webui.client.MainPlaceHistoryMapper;
import com.eucalyptus.webui.client.place.LoginPlace;
import com.eucalyptus.webui.client.place.LogoutPlace;
import com.eucalyptus.webui.client.place.ShellPlace;
import com.eucalyptus.webui.client.place.StartPlace;
import com.eucalyptus.webui.client.service.CategoryTag;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.client.service.LoginUserProfile;
import com.eucalyptus.webui.client.session.SessionData;
import com.eucalyptus.webui.client.view.DetailView;
import com.eucalyptus.webui.client.view.FooterView;
import com.eucalyptus.webui.client.view.LoadingProgressView;
import com.eucalyptus.webui.client.view.SearchHandler;
import com.eucalyptus.webui.client.view.ShellView;
import com.eucalyptus.webui.client.view.UserSettingView;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.ResettableEventBus;
import com.google.gwt.http.client.URL;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

/**
 * Bootstrapping activity that brings up the main UI. Then it sets up and launches
 * the main activity manager.
 * 
 * @author Ye Wen (wenye@eucalyptus.com)
 *
 */
public class ShellActivity extends AbstractActivity implements FooterView.Presenter, UserSettingView.Presenter, DetailView.Presenter, SearchHandler {
  
  private static final Logger LOG = Logger.getLogger( ShellActivity.class.getName( ) );
  
  private static final String DEFAULT_VERSION = "Eucalyptus unknown version";
  
  private ClientFactory clientFactory;
  private ShellPlace place;
  
  private AcceptsOneWidget container;
  
  private ArrayList<CategoryTag> category;
  
  public ShellActivity( ShellPlace place, ClientFactory clientFactory ) {
    this.place = place;
    this.clientFactory = clientFactory;
  }
  
  @Override
  public void start( final AcceptsOneWidget container, EventBus eventBus ) {
    this.container = container;
    loadLoadingProgressView( container );
    getLoginUserProfile( );
  }
  
  private void startMain( ) {
    loadShellView( this.container );

    // This is the root container of all main activities
    AppWidget appWidget = new AppWidget( this.clientFactory.getShellView( ).getContentView( ).getContentContainer( ) );    
    this.clientFactory.getMainActivityManager( ).setDisplay(appWidget);
    
    ExPlaceHistoryHandler placeHistoryHandler = ( ExPlaceHistoryHandler ) clientFactory.getMainPlaceHistoryHandler( );
    placeHistoryHandler.handleCurrentHistory( );
  }
  
  private void loadLoadingProgressView( AcceptsOneWidget container ) {
    LoadingProgressView loadingProgressView = this.clientFactory.getLoadingProgressView( );
    loadingProgressView.setProgress( 1 );
    container.setWidget( loadingProgressView );
  }
  
  private void loadShellView( AcceptsOneWidget container ) {
    ShellView shellView = this.clientFactory.getShellView( );
    
    shellView.getDirectoryView( ).buildTree( this.category );
    shellView.getDirectoryView( ).setSearchHandler( this );
    
    shellView.getFooterView( ).setPresenter( this );
    shellView.getFooterView( ).setVersion( clientFactory.getSessionData( ).getStringProperty( SessionData.VERSION, DEFAULT_VERSION ) );
    
    String user = clientFactory.getSessionData( ).getLoginUser( ).toString( );
    shellView.getHeaderView( ).setUser( user );
    shellView.getHeaderView( ).getUserSetting( ).setUser( user );
    shellView.getHeaderView( ).getUserSetting( ).setPresenter( this );
    shellView.getHeaderView( ).setSearchHandler( this );
    
    shellView.getDetailView( ).setPresenter( this );
    
    container.setWidget( shellView );
  }
  
  private void getLoginUserProfile( ) {
    this.clientFactory.getBackendService( ).getLoginUserProfile( this.clientFactory.getLocalSession( ).getSession( ),
                                                               new AsyncCallback<LoginUserProfile>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        LOG.log( Level.WARNING, "Cannot get login user profile. Maybe session is invalid: " + caught );
        if ( EucalyptusServiceException.INVALID_SESSION.equals( caught.getMessage( ) ) ) {
          clientFactory.getLocalSession( ).clearSession( );
        }
        clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.DEFAULT_PROMPT ) );
      }
      
      @Override
      public void onSuccess( LoginUserProfile result ) {
        if ( result == null ) {
          LOG.log( Level.WARNING, "Got empty user profile" );
          clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.LOADING_FAILURE_PROMPT ) );
        } else {
          clientFactory.getSessionData( ).setLoginUser( result );
          clientFactory.getLoadingProgressView( ).setProgress( 33 );
          getSystemProperties( );
        }
      }
      
    });
  }
  
  private void getSystemProperties( ) {
    this.clientFactory.getBackendService( ).getSystemProperties( this.clientFactory.getLocalSession( ).getSession( ),
                                                                 new AsyncCallback<HashMap<String, String>>( ) {
      
      @Override
      public void onFailure( Throwable caught ) {
        LOG.log( Level.WARNING, "Cannot get system properties: " + caught );
        clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.LOADING_FAILURE_PROMPT ) );
      }
      
      @Override
      public void onSuccess( HashMap<String, String> result ) {
        if ( result == null ) {
          LOG.log( Level.WARNING, "Got empty system properties" );
          clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.LOADING_FAILURE_PROMPT ) );          
        } else {
          clientFactory.getSessionData( ).setProperties( result );
          clientFactory.getLoadingProgressView( ).setProgress( 67 );
          getCategory( );
        }
      }
    } );
  }
  
  private void getCategory( ) {
    this.clientFactory.getBackendService( ).getCategory( this.clientFactory.getLocalSession( ).getSession( ),
                                                         new AsyncCallback<ArrayList<CategoryTag>>( ) {
      
      @Override
      public void onFailure( Throwable caught ) {
        LOG.log( Level.WARNING, "Cannot get category: " + caught );
        clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.LOADING_FAILURE_PROMPT ) );
      }
      
      @Override
      public void onSuccess( ArrayList<CategoryTag> result ) {
        if ( result == null ) {
          LOG.log( Level.WARNING, "Got empty category" );
          clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.LOADING_FAILURE_PROMPT ) );          
        } else {
          category = result;
          clientFactory.getLoadingProgressView( ).setProgress( 100 );
          startMain( );
        }
      }
    } );
  }

  @Override
  public void onShowLogConsole( ) {
    this.clientFactory.getShellView( ).showLogConsole( );
  }
  
  @Override
  public void onHideLogConsole( ) {
    this.clientFactory.getShellView( ).hideLogConsole( );
  }

  @Override
  public void logout( ) {
    this.clientFactory.getLocalSession( ).clearSession( );
    //this.clientFactory.getMainHistorian( ).newItem( "", false );
    //this.clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.DEFAULT_PROMPT ) );
    this.clientFactory.getMainPlaceController( ).goTo( new LogoutPlace( ) );
  }

  @Override
  public void search( String search ) {
    if ( search != null ) {
      LOG.log( Level.INFO, "New search: " + search );
      this.clientFactory.getMainHistorian( ).newItem( search, true/*issueEvent*/ );
    } else {
      LOG.log( Level.INFO, "Empty search!" );
    }
  }

  @Override
  public void hideDetail( ) {
    this.clientFactory.getShellView( ).hideDetail( );
  }
  
}
