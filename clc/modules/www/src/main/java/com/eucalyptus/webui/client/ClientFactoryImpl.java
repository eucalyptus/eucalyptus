package com.eucalyptus.webui.client;

import com.eucalyptus.webui.client.service.EucalyptusService;
import com.eucalyptus.webui.client.service.EucalyptusServiceAsync;
import com.eucalyptus.webui.client.util.LocalSession;
import com.eucalyptus.webui.client.util.LocalSessionImpl;
import com.eucalyptus.webui.client.view.LoadingAnimationView;
import com.eucalyptus.webui.client.view.LoadingAnimationViewImpl;
import com.eucalyptus.webui.client.view.LoadingProgressView;
import com.eucalyptus.webui.client.view.LoadingProgressViewImpl;
import com.eucalyptus.webui.client.view.LoginView;
import com.eucalyptus.webui.client.view.LoginViewImpl;
import com.eucalyptus.webui.client.view.ServiceView;
import com.eucalyptus.webui.client.view.ServiceViewImpl;
import com.eucalyptus.webui.client.view.ShellView;
import com.eucalyptus.webui.client.view.ShellViewImpl;
import com.eucalyptus.webui.client.view.StartView;
import com.eucalyptus.webui.client.view.StartViewImpl;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.ResettableEventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.place.shared.PlaceController;

public class ClientFactoryImpl implements ClientFactory
{
	private static final EventBus mainEventBus = new ResettableEventBus( new SimpleEventBus( ) );
	private static final PlaceController mainPlaceController = new PlaceController( mainEventBus );
	private static final EventBus lifecycleEventBus = new SimpleEventBus( );
	private static final PlaceController lifecyclePlaceController = new PlaceController( lifecycleEventBus );
	
	private static final LocalSession localSession = new LocalSessionImpl( );
	
	private static final EucalyptusServiceAsync euareService = GWT.create( EucalyptusService.class );

	private static LoginView loginView;
	private static LoadingProgressView loadingProgressView;
	private static ShellView shellView;
	private static StartView startView;
	private static ServiceView serviceView;
	private static LoadingAnimationView loadingAnimationView;

  @Override
  public LocalSession getLocalSession( ) {
    return localSession;
  }

  @Override
  public EucalyptusServiceAsync getBackendService( ) {
    return euareService;
  }

  @Override
  public EventBus getMainEventBus( ) {
    return mainEventBus;
  }

  @Override
  public PlaceController getMainPlaceController( ) {
    return mainPlaceController;
  }

  @Override
  public EventBus getLifecycleEventBus( ) {
    return lifecycleEventBus;
  }

  @Override
  public PlaceController getLifecyclePlaceController( ) {
    return lifecyclePlaceController;
  }
  
  @Override
  public LoginView getLoginView( ) {
    if ( loginView == null ) {
      loginView = new LoginViewImpl( );
    }
    return loginView;
  }
  
  @Override
  public ShellView getShellView( ) {
    if ( shellView == null ) {
      shellView = new ShellViewImpl( );
    }
    return shellView;
  }

  @Override
  public StartView getStartView( ) {
    if ( startView == null ) {
      startView = new StartViewImpl( );
    }
    return startView;
  }
  
  @Override
  public LoadingProgressView getLoadingProgressView( ) {
    if ( loadingProgressView == null ) {
      loadingProgressView = new LoadingProgressViewImpl( );
    }
    return loadingProgressView;
  }

  @Override
  public ServiceView getServiceView( ) {
    if ( serviceView == null ) {
      serviceView = new ServiceViewImpl( );
    }
    return serviceView;
  }  
  
  @Override
  public LoadingAnimationView getLoadingAnimationView( ) {
    if ( loadingAnimationView == null ) {
      loadingAnimationView = new LoadingAnimationViewImpl( );
    }
    return loadingAnimationView;
  }
  
}
