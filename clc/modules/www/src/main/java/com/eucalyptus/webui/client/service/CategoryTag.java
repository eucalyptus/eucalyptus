package com.eucalyptus.webui.client.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CategoryTag implements Serializable {

  private static final long serialVersionUID = 1L;

  private String name;
  private List<CategoryItem> items;
  
  public CategoryTag( ) {
  }
  
  public CategoryTag( String name, List<CategoryItem> items ) {
    this.setName( name );
    this.setItems( items );
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getName( ) {
    return name;
  }

  public void setItems( List<CategoryItem> items ) {
    this.items = items;
  }

  public List<CategoryItem> getItems( ) {
    return items;
  }
  
}
