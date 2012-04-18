package com.eucalyptus.webui.client.service;

import java.io.Serializable;

public class Session implements Serializable {
  
  private static final long serialVersionUID = 1L;

  private String id;
  
  public Session( ) {
  }
  
  public Session( String id ) {
    this.id = id;
  }
  
  public String getId( ) {
    return this.id;
  }
  
  public void setId( String id ) {
    this.id = id;
  }
  
}
