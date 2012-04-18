package com.eucalyptus.webui.client.place;

import com.google.gwt.place.shared.Place;

public class ApplyPlace extends Place {

  public static enum ApplyType {
    ACCOUNT,
    USER,
    PASSWORD_RESET
  }
  
  private ApplyType type;
  
  public ApplyPlace( ApplyType type ) {
    this.setType( type );
  }

  public void setType( ApplyType type ) {
    this.type = type;
  }

  public ApplyType getType( ) {
    return type;
  }
  
}
