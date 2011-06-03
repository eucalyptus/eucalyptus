package com.eucalyptus.webui.client.service;

import java.io.Serializable;

public class GuideItem implements Serializable {

  private static final long serialVersionUID = 1L;

  private String title;
  private String link;
  private String icon;
  
  public GuideItem( ) {
  }
  
  public GuideItem( String title, String link, String icon ) {
    this.setTitle( title );
    this.setLink( link );
    this.setIcon( icon );
  }

  public void setTitle( String title ) {
    this.title = title;
  }

  public String getTitle( ) {
    return title;
  }

  public void setLink( String link ) {
    this.link = link;
  }

  public String getLink( ) {
    return link;
  }

  public void setIcon( String icon ) {
    this.icon = icon;
  }

  public String getIcon( ) {
    return icon;
  }
  
}
