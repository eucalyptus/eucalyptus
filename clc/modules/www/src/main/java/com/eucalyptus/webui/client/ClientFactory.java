package com.eucalyptus.webui.client;

import com.eucalyptus.webui.client.service.EucalyptusServiceAsync;
import com.eucalyptus.webui.client.session.LocalSession;
import com.eucalyptus.webui.client.session.SessionData;
import com.eucalyptus.webui.client.view.AccountView;
import com.eucalyptus.webui.client.view.DetailView;
import com.eucalyptus.webui.client.view.ErrorSinkView;
import com.eucalyptus.webui.client.view.LoadingAnimationView;
import com.eucalyptus.webui.client.view.LoadingProgressView;
import com.eucalyptus.webui.client.view.LoginView;
import com.eucalyptus.webui.client.view.ConfigView;
import com.eucalyptus.webui.client.view.ShellView;
import com.eucalyptus.webui.client.view.StartView;
import com.eucalyptus.webui.client.view.VmTypeView;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.place.shared.PlaceHistoryHandler.Historian;

public interface ClientFactory {

  /**
   * @return the default place.
   */
  Place getDefaultPlace( );
  /**
   * @return the place for the error page.
   */
  Place getErrorPlace( );
  
  /**
   * @return the event bus for the main activities.
   */
	EventBus getMainEventBus( );
	/**
	 * @return the place controller for the main activities.
	 */
	PlaceController getMainPlaceController( );
	/**
	 * @return the activity manager for the main activities.
	 */
	ActivityManager getMainActivityManager( );
	/**
	 * @return the place history handler for the main activities.
	 */
	PlaceHistoryHandler getMainPlaceHistoryHandler( );
	/**
	 * @return the Historian for the main activities.
	 */
	Historian getMainHistorian( );
	
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
	 * @return the local session data.
	 */
	SessionData getSessionData( );
	
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
  ConfigView getConfigView( );
	
  /**
   * @return the impl. of LoadingAnimationView
   */
  LoadingAnimationView getLoadingAnimationView( );
  
  /**
   * @return the impl. of ErrorSinkView
   */
  ErrorSinkView getErrorSinkView( );
  
  /**
   * @return the impl. of AccountView
   */
  AccountView getAccountView( );
  
  /**
   * @return the impl. of VmTypeView
   */
  VmTypeView getVmTypeView( );
  
}
