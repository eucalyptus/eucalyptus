package com.eucalyptus.webui.client;

import com.eucalyptus.webui.client.activity.LoginActivity;
import com.eucalyptus.webui.client.activity.ShellActivity;
import com.eucalyptus.webui.client.place.LoginPlace;
import com.eucalyptus.webui.client.place.ShellPlace;
import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;

/**
 * Mapping the lifecycle activities and places.
 * 
 * @author Ye Wen (wenye@eucalyptus.com)
 *
 */
public class LifecycleActivityMapper implements ActivityMapper {
  
  private ClientFactory clientFactory;
  
  public LifecycleActivityMapper( ClientFactory clientFactory ) {
    this.clientFactory = clientFactory;
  }
  
  @Override
  public Activity getActivity( Place place ) {
    if ( place instanceof LoginPlace ) {
      return new LoginActivity( ( LoginPlace ) place, this.clientFactory );
    } else if ( place instanceof ShellPlace ) {
      return new ShellActivity( ( ShellPlace ) place, this.clientFactory );
    }
    return null;
  }
  
}
