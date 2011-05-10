package com.eucalyptus.webui.client.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DataRow implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private final ArrayList<String> row = new ArrayList<String>( );
  
  public DataRow( List<String> row ) {
    this.row.addAll( row );
  }
  
  public String getCol( int i ) {
    return row.get( i );
  }
  
  public void setCol( int i, String val ) {
    row.set( i, val );
  }
  
}
