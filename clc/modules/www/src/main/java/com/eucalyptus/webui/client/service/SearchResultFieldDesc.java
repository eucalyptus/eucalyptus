package com.eucalyptus.webui.client.service;

import java.io.Serializable;
import java.util.List;

public class SearchResultFieldDesc implements Serializable {

  private static final long serialVersionUID = 1L;
  
  public static enum TableDisplay {
    MANDATORY,
    OPTIONAL,
    NONE,
  }
  
  public static enum Type {
    TEXT,       // single line text string
    ARTICLE,    // multi-line text
    HIDDEN,     // password like text
    REVEALING,  // text revealing itself when mouseover (for security related stuff, like secret key)
    BOOLEAN,    // boolean
    DATE,       // date in long
    ENUM,       // enum value
    KEYVAL,     // dynamic key value (like single line text but can be removed)
    NEWKEYVAL,  // empty key value (for adding new)
    LINK,       // URL link
    ACTION      // custom action, usually causing a popup
  }
  
  private String name;                // ID of the field, also used as the key of a KEYVAL
  private String title;               // title for display
  private Boolean sortable;           // if sortable in table display
  private String width;               // width of column for table display
  private TableDisplay tableDisplay;  // table display type
  private Type type;                  // value type
  private Boolean editable;           // if this field is editable
  private Boolean hidden;             // if this field should be hidden in properties panel
  private List<String> enumValues;    // the list of enum values for an ENUM

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

  public void setEnumValues( List<String> enumValues ) {
    this.enumValues = enumValues;
  }

  public List<String> getEnumValues( ) {
    return enumValues;
  }
  
}
