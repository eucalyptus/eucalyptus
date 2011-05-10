package com.eucalyptus.webui.client.service;

import java.io.Serializable;

public class DataFieldDesc implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private String title;
  private Boolean sortable;
  private String width;
  private String widthUnit;
  
  public DataFieldDesc( String title, Boolean sortable, String width, String widthUnit ) {
    this.title = title;
    this.sortable = sortable;
    this.width = width;
    this.widthUnit = widthUnit;
  }
  
  public void setTitle( String title ) {
    this.title = title;
  }
  public String getTitle( ) {
    return title;
  }
  public void setSortable( Boolean sortable ) {
    this.sortable = sortable;
  }
  public Boolean getSortable( ) {
    return sortable;
  }
  public void setWidth( String width ) {
    this.width = width;
  }
  public String getWidth( ) {
    return width;
  }
  public void setWidthUnit( String widthUnit ) {
    this.widthUnit = widthUnit;
  }
  public String getWidthUnit( ) {
    return widthUnit;
  }  
  
}
