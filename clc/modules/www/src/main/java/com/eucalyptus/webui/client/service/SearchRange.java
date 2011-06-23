package com.eucalyptus.webui.client.service;

import java.io.Serializable;

public class SearchRange implements Serializable {

  private static final long serialVersionUID = 1L;

  private int start;
  private int length;
  private int sortField;
  private boolean ascending;
  
  public SearchRange( ) {
    this.setStart( 0 );
    this.setLength( 15 );
    this.setSortField( 0 );
    this.setAscending( true );
  }

  public SearchRange( int sortField ) {
    this.setStart( 0 );
    this.setLength( 15 );
    this.setSortField( sortField );
    this.setAscending( true );
  }
  
  public SearchRange( int start, int length ) {
    this.setStart( start );
    this.setLength( length );
    this.setSortField( 0 );
    this.setAscending( true );
  }
  
  public SearchRange( int start, int length, int sortField, boolean ascending ) {
    this.setStart( start );
    this.setLength( length );
    this.setSortField( sortField );
    this.setAscending( ascending );
  }
  
  @Override
  public String toString( ) {
    return "start=" + start + ", length=" + length + ", sortField=" + sortField + ", ascending=" + ascending;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if (!( obj instanceof SearchRange ) ) {
      return false;
    }
    SearchRange that = ( SearchRange ) obj;
    if ( this == that ) {
      return true;
    }
    if ( ( this.start == that.start ) && 
         ( this.length == that.length ) &&
         ( this.sortField == that.sortField ) &&
         ( this.ascending == that.ascending ) ) {
      return true;
    }
    return false;
  }
  
  public boolean isSameSort( SearchRange that ) {
    if ( that != null && this.sortField == that.sortField && this.ascending == that.ascending ) {
      return true;
    }
    return false;
  }
  
  public void setLength( int length ) {
    this.length = length;
  }

  public int getLength( ) {
    return length;
  }

  public void setSortField( int sortField ) {
    this.sortField = sortField;
  }

  public int getSortField( ) {
    return sortField;
  }

  public void setAscending( boolean ascending ) {
    this.ascending = ascending;
  }

  public boolean isAscending( ) {
    return ascending;
  }

  public void setStart( int start ) {
    this.start = start;
  }

  public int getStart( ) {
    return start;
  }
  
}
