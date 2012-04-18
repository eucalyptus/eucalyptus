package com.eucalyptus.webui.client.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class QuickLinkTag implements Serializable {

  private static final long serialVersionUID = 1L;

  private String name;
  private ArrayList<QuickLink> items;
  
  public QuickLinkTag( ) {
  }
  
  public QuickLinkTag( String name, ArrayList<QuickLink> items ) {
    this.setName( name );
    this.setItems( items );
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getName( ) {
    return name;
  }

  public void setItems( ArrayList<QuickLink> items ) {
    this.items = items;
  }

  public List<QuickLink> getItems( ) {
    return items;
  }
  
}
