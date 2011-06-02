package com.eucalyptus.webui.client;

import com.eucalyptus.webui.client.activity.ActionUtil;
import com.eucalyptus.webui.client.activity.WebAction;
import com.eucalyptus.webui.client.place.ConfirmSignupPlace;
import com.eucalyptus.webui.client.place.LoginPlace;
import com.eucalyptus.webui.client.place.ResetPasswordPlace;
import com.eucalyptus.webui.shared.query.QueryType;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.user.client.History;
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
  
  private static final String CONFIRMATIONCODE = "confirmationcode";

  private final ClientFactory clientFactory;
  
  private ActivityManager lifecycleActivityManager;

  public EucalyptusApp( ClientFactory clientFactory ) {
    this.clientFactory = clientFactory;
  }
  
  public void start( final AcceptsOneWidget container ) {
    ActivityMapper activityMapper = new LifecycleActivityMapper( this.clientFactory );
    lifecycleActivityManager = new ActivityManager( activityMapper, this.clientFactory.getLifecycleEventBus( ) );
    lifecycleActivityManager.setDisplay( container );
    // First check special action activities
    checkAction( );
  }
  
  private void checkAction( ) {
    String token = History.getToken( );
    if ( token.startsWith( QueryType.confirm.name( ) + WebAction.ACTION_SEPARATOR ) ) {
      WebAction action = ActionUtil.parseAction( token );
      if ( action != null ) {
        this.clientFactory.getLifecyclePlaceController( ).goTo( new ConfirmSignupPlace( action.getValue( CONFIRMATIONCODE ) ) );
        return;
      }
    } else if ( token.startsWith( QueryType.reset.name( ) + WebAction.ACTION_SEPARATOR ) ) {
      WebAction action = ActionUtil.parseAction( token );
      if ( action != null ) {
        this.clientFactory.getLifecyclePlaceController( ).goTo( new ResetPasswordPlace( action.getValue( CONFIRMATIONCODE ) ) );
        return;
      }      
    }
    // Always login first 
    this.clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.DEFAULT_PROMPT ) );
  }
  
}
