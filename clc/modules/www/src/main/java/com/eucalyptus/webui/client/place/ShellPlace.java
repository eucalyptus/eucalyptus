package com.eucalyptus.webui.client.place;

import com.eucalyptus.webui.client.service.Session;
import com.google.gwt.place.shared.Place;

public class ShellPlace extends Place {
  
  private Session session;
  
  public ShellPlace( Session session ) {
    this.session = session;
  }
  
  public Session getSession( ) {
    return this.session;
  }

}
