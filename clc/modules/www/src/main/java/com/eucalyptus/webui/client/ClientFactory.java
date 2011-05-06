package com.eucalyptus.webui.client;

import com.eucalyptus.webui.client.service.EucalyptusServiceAsync;
import com.eucalyptus.webui.client.util.LocalSession;
import com.eucalyptus.webui.client.view.LoadingAnimationView;
import com.eucalyptus.webui.client.view.LoadingProgressView;
import com.eucalyptus.webui.client.view.LoginView;
import com.eucalyptus.webui.client.view.ServiceView;
import com.eucalyptus.webui.client.view.ShellView;
import com.eucalyptus.webui.client.view.StartView;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;

public interface ClientFactory {

  /**
   * @return the event bus for the main activities.
   */
	EventBus getMainEventBus( );
	/**
	 * @return the place controller for the main activities.
	 */
	PlaceController getMainPlaceController( );
	/**
	 * @return the event bus for the lifecycle activities.
	 */
	EventBus getLifecycleEventBus( );
	/**
	 * @return the place controller for the lifecycle activities.
	 */
	PlaceController getLifecyclePlaceController( );
	
	/**
	 * @return the impl. of local session record, essentially the session ID.
	 */
	LocalSession getLocalSession( );
	
	/**
	 * @return the impl. of Euare service.
	 */
	EucalyptusServiceAsync getBackendService( );

	/**
	 * @return the impl. of LoginView
	 */
	LoginView getLoginView( );
	
	/**
	 * @return the impl. of LoadingProgressingView
	 */
	LoadingProgressView getLoadingProgressView( );
	
	/**
	 * @return the impl. of ShellView
	 */
  ShellView getShellView( );
  
  /**
   * @return the impl. of StartView
   */
  StartView getStartView( );
  
  /**
   * @return the impl. of ServiceView
   */
  ServiceView getServiceView( );
	
  /**
   * @return the impl. of LoadingAnimationView
   */
  LoadingAnimationView getLoadingAnimationView( );
  
}
