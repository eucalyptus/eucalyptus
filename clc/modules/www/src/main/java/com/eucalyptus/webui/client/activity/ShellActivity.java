package com.eucalyptus.webui.client.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.AppWidget;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.MainActivityMapper;
import com.eucalyptus.webui.client.MainPlaceHistoryMapper;
import com.eucalyptus.webui.client.place.LoginPlace;
import com.eucalyptus.webui.client.place.ShellPlace;
import com.eucalyptus.webui.client.place.StartPlace;
import com.eucalyptus.webui.client.service.CategoryTag;
import com.eucalyptus.webui.client.service.LoginUserProfile;
import com.eucalyptus.webui.client.view.FooterView;
import com.eucalyptus.webui.client.view.LoadingProgressView;
import com.eucalyptus.webui.client.view.ShellView;
import com.eucalyptus.webui.client.view.UserSettingView;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.ResettableEventBus;
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
public class ShellActivity extends AbstractActivity implements FooterView.Presenter, UserSettingView.Presenter {
  
  private static final Logger LOG = Logger.getLogger( ShellActivity.class.getName( ) );
  
  private static final Place DEFAULT_PLACE = new StartPlace( );
  
  private ClientFactory clientFactory;
  private ShellPlace place;
  
  private AcceptsOneWidget container;
  
  private ActivityManager mainActivityManager;
  private PlaceHistoryHandler mainPlaceHistoryHandler;
  
  private LoginUserProfile user;
  private HashMap<String, String> props;
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
  
  @Override
  public void onStop( ) {
    ( ( ResettableEventBus ) ( this.clientFactory.getMainEventBus( ) ) ).removeHandlers( );
  }
  
  private void startMain( ) {
    loadShellView( this.container );

    // This is the root container of all main activities
    AppWidget appWidget = new AppWidget( this.clientFactory.getShellView( ).getContentView( ).getContentContainer( ) );    
    ActivityMapper activityMapper = new MainActivityMapper( this.clientFactory );
    this.mainActivityManager = new ActivityManager( activityMapper, this.clientFactory.getMainEventBus( ) );
    this.mainActivityManager.setDisplay(appWidget);

    MainPlaceHistoryMapper historyMapper= GWT.create( MainPlaceHistoryMapper.class );
    this.mainPlaceHistoryHandler = new PlaceHistoryHandler( historyMapper );
    this.mainPlaceHistoryHandler.register( this.clientFactory.getMainPlaceController( ),
                                           this.clientFactory.getMainEventBus( ),
                                           DEFAULT_PLACE );
    this.mainPlaceHistoryHandler.handleCurrentHistory( );
  }
  
  private void loadLoadingProgressView( AcceptsOneWidget container ) {
    LoadingProgressView loadingProgressView = this.clientFactory.getLoadingProgressView( );
    loadingProgressView.setProgress( 1 );
    container.setWidget( loadingProgressView );
  }
  
  private void loadShellView( AcceptsOneWidget container ) {
    ShellView shellView = this.clientFactory.getShellView( );
    shellView.getDirectoryView( ).buildTree( this.category );
    shellView.getFooterView( ).setPresenter( this );
    shellView.getHeaderView( ).setUser( this.user.toString( ) );
    shellView.getHeaderView( ).getUserSetting( ).setUser( this.user.toString( ) );
    shellView.getHeaderView( ).getUserSetting( ).setPresenter( this );
    container.setWidget( shellView );
  }
  
  private void getLoginUserProfile( ) {
    this.clientFactory.getBackendService( ).getLoginUserProfile( this.clientFactory.getLocalSession( ).getSession( ),
                                                               new AsyncCallback<LoginUserProfile>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        LOG.log( Level.WARNING, "Cannot get login user profile. Maybe session is invalid: " + caught );
        clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.LOADING_FAILURE_PROMPT ) );
      }
      
      @Override
      public void onSuccess( LoginUserProfile result ) {
        user = result;
        clientFactory.getLoadingProgressView( ).setProgress( 33 );
        getSystemProperties( );
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
        props = result;
        clientFactory.getLoadingProgressView( ).setProgress( 67 );
        getCategory( );
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
        category = result;
        clientFactory.getLoadingProgressView( ).setProgress( 100 );
        startMain( );
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
    this.clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.DEFAULT_PROMPT ) );
  }
  
}
