package com.eucalyptus.webui.client.activity;

import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.LoginPlace;
import com.eucalyptus.webui.client.place.LogoutPlace;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

/**
 * The sole purpose of this activity is to clear up the main event bus
 * (via {@link com.google.gwt.activity.shared.ActivityManager}, when it
 * switches activities, since LogoutActivity registers nothing on the
 * main event bus), and do the real logout. And because LogoutPlace is
 * not registered for MainPlaceHistoryMapper, the URL will also be cleared
 * (see {@link ExPlaceHistoryHandler} impl.).
 * 
 * @author Ye Wen (wenye@eucalyptus.com)
 *
 */
public class LogoutActivity extends AbstractActivity {
  
  private LogoutPlace place;
  private ClientFactory clientFactory;
  
  public LogoutActivity( LogoutPlace place, ClientFactory clientFactory ) {
    this.place = place;
    this.clientFactory = clientFactory;
  }
  
  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    this.clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.DEFAULT_PROMPT ) );
  }
  
}
