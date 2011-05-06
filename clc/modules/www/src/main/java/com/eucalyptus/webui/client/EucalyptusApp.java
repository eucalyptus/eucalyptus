package com.eucalyptus.webui.client;

import com.eucalyptus.webui.client.place.LoginPlace;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

/**
 * Top level UI app. It sets up the top-level lifecycle activity manager,
 * which controls the state transition between main UI and login UI.
 * 
 *        success
 * Login ---------> Shell
 *        failure
 * Login <--------- Shell
 *         logout
 * Login <--------- Shell
 * 
 * @author: Ye Wen (wenye@eucalyptus.com)
 * 
 */
public class EucalyptusApp {
  
  private final ClientFactory clientFactory;
  
  private ActivityManager lifecycleActivityManager;

  public EucalyptusApp( ClientFactory clientFactory ) {
    this.clientFactory = clientFactory;
  }
  
  public void start( final AcceptsOneWidget container ) {
    ActivityMapper activityMapper = new LifecycleActivityMapper( this.clientFactory );
    lifecycleActivityManager = new ActivityManager( activityMapper, this.clientFactory.getLifecycleEventBus( ) );
    lifecycleActivityManager.setDisplay( container );
    // Always login first 
    this.clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.DEFAULT_PROMPT ) );
  }
  
}
