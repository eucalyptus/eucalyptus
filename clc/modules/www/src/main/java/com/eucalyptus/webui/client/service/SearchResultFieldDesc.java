package com.eucalyptus.webui.client.service;

import java.io.Serializable;

public class SearchResultFieldDesc implements Serializable {

  private static final long serialVersionUID = 1L;
  
  public static enum TableDisplay {
    MANDATORY,
    OPTIONAL,
    NONE,
  }
  
  public static enum Type {
    TEXT,
    HIDDEN,
    BOOLEAN,
    LIST,
  }
  
  private String title;
  private Boolean sortable;
  private String width;
  private TableDisplay tableDisplay;
  private Type type;
  private Boolean editable;

  public SearchResultFieldDesc( ) {
  }
  
  public SearchResultFieldDesc( String title, Boolean sortable, String width ) {
    this.title = title;
    this.sortable = sortable;
    this.width = width;
    this.tableDisplay = TableDisplay.MANDATORY;
    this.type = Type.TEXT;
    this.setEditable( true );
  }

  public SearchResultFieldDesc( String title, Boolean sortable, String width, TableDisplay tableDisplay, Type type, Boolean editable ) {
    this.title = title;
    this.sortable = sortable;
    this.width = width;
    this.tableDisplay = tableDisplay;
    this.type = type;
    this.setEditable( editable );
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

  public void setTableDisplay( TableDisplay tableDisplay ) {
    this.tableDisplay = tableDisplay;
  }

  public TableDisplay getTableDisplay( ) {
    return tableDisplay;
  }

  public void setType( Type type ) {
    this.type = type;
  }

  public Type getType( ) {
    return type;
  }

  public void setEditable( Boolean editable ) {
    this.editable = editable;
  }

  public Boolean getEditable( ) {
    return editable;
  }
  
}
