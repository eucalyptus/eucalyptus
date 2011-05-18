package com.eucalyptus.webui.client.service;

import java.io.Serializable;
import java.util.ArrayList;

public class CategoryTag implements Serializable {

  private static final long serialVersionUID = 1L;

  private String name;
  private ArrayList<CategoryItem> items;
  
  public CategoryTag( ) {
  }
  
  public CategoryTag( String name, ArrayList<CategoryItem> items ) {
    this.setName( name );
    this.setItems( items );
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getName( ) {
    return name;
  }

  public void setItems( ArrayList<CategoryItem> items ) {
    this.items = items;
  }

  public ArrayList<CategoryItem> getItems( ) {
    return items;
  }
  
}
