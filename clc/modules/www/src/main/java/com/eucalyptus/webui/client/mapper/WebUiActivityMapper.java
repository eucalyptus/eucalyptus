package com.eucalyptus.webui.client.mapper;

import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.activity.LoginActivity;
import com.eucalyptus.webui.client.activity.StartActivity;
import com.eucalyptus.webui.client.place.LoginPlace;
import com.eucalyptus.webui.client.place.StartPlace;
import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;

public class WebUiActivityMapper implements ActivityMapper {
  
  private ClientFactory clientFactory;
  
  public WebUiActivityMapper( ClientFactory clientFactory ) {
    super( );
    this.clientFactory = clientFactory;
  }
  
  @Override
  public Activity getActivity( Place place ) {
    if ( place instanceof LoginPlace ) {
      return new LoginActivity( ( LoginPlace )place, this.clientFactory );
    } else if ( place instanceof StartPlace ) {
      return new StartActivity( ( StartPlace )place, this.clientFactory );
    }
    return null;
  }
  
}
