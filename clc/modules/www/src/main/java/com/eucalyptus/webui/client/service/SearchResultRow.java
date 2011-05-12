package com.eucalyptus.webui.client.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.google.gwt.view.client.ProvidesKey;

public class SearchResultRow implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  public static final ProvidesKey<SearchResultRow> KEY_PROVIDER = new ProvidesKey<SearchResultRow>( ) {

    @Override
    public Object getKey( SearchResultRow item ) {
      if ( item != null ) {
        return item.getField( 0 );
      }
      return null;
    }
    
  };
  
  private ArrayList<String> row = new ArrayList<String>( );
  
  public SearchResultRow( ) {
  }
  
  public SearchResultRow( List<String> row ) {
    this.row.addAll( row );
  }
  
  public String getField( int i ) {
    return row.get( i );
  }
  
  public void setField( int i, String val ) {
    row.set( i, val );
  }
  
  @Override
  public String toString( ) {
    return row.toString( );
  }
  
  @Override
  public boolean equals( Object that ) {
    if ( this == that ) {
      return true;
    }
    if ( ! ( that instanceof SearchResultRow ) ) {
      return false;
    }
    SearchResultRow thatRow = ( SearchResultRow ) that;
    if ( this.row.size( ) != thatRow.row.size( ) ) {
      return false;
    }
    for ( int i = 0; i < this.row.size( ); i++ ) {
      if ( ! ( this.row.get( i ).equals( thatRow.row.get( i ) ) ) ) {
        return false;
      }
    }
    return true;
  }
}
