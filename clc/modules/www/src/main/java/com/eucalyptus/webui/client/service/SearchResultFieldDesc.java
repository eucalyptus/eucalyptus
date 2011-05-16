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
    FILE,
    HIDDEN,
    BOOLEAN,
    LIST,
    ACTION,
  }
  
  private String name;
  private String title;
  private Boolean sortable;
  private String width;
  private TableDisplay tableDisplay;
  private Type type;
  private Boolean editable;
  private Boolean hidden;

  public SearchResultFieldDesc( ) {
  }
  
  public SearchResultFieldDesc( String title, Boolean sortable, String width ) {
    this.name = title;
    this.title = title;
    this.sortable = sortable;
    this.width = width;
    this.tableDisplay = TableDisplay.MANDATORY;
    this.type = Type.TEXT;
    this.setEditable( true );
    this.setHidden( false );
  }

  public SearchResultFieldDesc( String title, Boolean sortable, String width, TableDisplay tableDisplay, Type type, Boolean editable, Boolean hidden ) {
    this.name = title;
    this.title = title;
    this.sortable = sortable;
    this.width = width;
    this.tableDisplay = tableDisplay;
    this.type = type;
    this.editable = editable;
    this.hidden = hidden;
  }
  
  public SearchResultFieldDesc( String name, String title, Boolean sortable, String width, TableDisplay tableDisplay, Type type, Boolean editable, Boolean hidden ) {
    this.name = name;
    this.title = title;
    this.sortable = sortable;
    this.width = width;
    this.tableDisplay = tableDisplay;
    this.type = type;
    this.editable = editable;
    this.hidden = hidden;
  }
  
  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "(" );
    sb.append( "name=" ).append( name ).append( "," );
    sb.append( "title=" ).append( title ).append( "," );
    sb.append( "sortable=" ).append( sortable ).append( "," );
    sb.append( "width=" ).append( width ).append( "," );
    sb.append( "tableDisplay=" ).append( tableDisplay ).append( "," );
    sb.append( "type=" ).append( type ).append( "," );
    sb.append( "editable=" ).append( editable ).append( "," );
    sb.append( "hidden=" ).append( hidden );
    sb.append( ")" );
    return sb.toString( );
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

  public void setHidden( Boolean hidden ) {
    this.hidden = hidden;
  }

  public Boolean getHidden( ) {
    return hidden;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getName( ) {
    return name;
  }
  
}
