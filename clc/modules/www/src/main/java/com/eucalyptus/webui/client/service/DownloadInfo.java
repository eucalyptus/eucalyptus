package com.eucalyptus.webui.client.service;

import java.io.Serializable;

public class DownloadInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  private String url;
  private String name;
  private String description;
  
  public DownloadInfo( ) {
  }
  
  public DownloadInfo( String url, String name, String description ) {
    this.setUrl( url );
    this.setName( name );
    this.setDescription( description );
  }

  public void setUrl( String url ) {
    this.url = url;
  }

  public String getUrl( ) {
    return url;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getName( ) {
    return name;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public String getDescription( ) {
    return description;
  }
  
}
