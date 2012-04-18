package com.eucalyptus.webui.client.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SearchResult implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private int totalSize;
  private SearchRange range;
  private ArrayList<SearchResultFieldDesc> descs = new ArrayList<SearchResultFieldDesc>( );
  private ArrayList<SearchResultRow> rows = new ArrayList<SearchResultRow>( );
  
  public SearchResult( ) {
  }
  
  public SearchResult( int totalSize, SearchRange range ) {
    this.totalSize = totalSize;
    this.range = range;
  }

  public void setDescs( List<SearchResultFieldDesc> descs ) {
    this.descs.clear( );
    this.descs.addAll( descs );
  }

  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "totalSize=" ).append( totalSize ).append( "\n" );
    sb.append( "range=[" ).append( range ).append( "]\n" );
    sb.append( "descs=[" ).append( descs ).append( "]\n" );
    sb.append( "rows=[" ).append( rows ).append( "]\n" );
    return sb.toString( );
  }
  
  public ArrayList<SearchResultFieldDesc> getDescs( ) {
    return descs;
  }

  public void setRows( List<SearchResultRow> rows ) {
    this.rows.clear( );
    this.rows.addAll( rows );
  }
  
  public void addRows( List<SearchResultRow> rows ) {
    this.rows.addAll( rows );
  }
  
  public void addRow( SearchResultRow row ) {
    this.rows.add( row );
  }

  public ArrayList<SearchResultRow> getRows( ) {
    return rows;
  }

  public void setTotalSize( int totalSize ) {
    this.totalSize = totalSize;
  }

  public int getTotalSize( ) {
    return totalSize;
  }

  public void setRange( SearchRange range ) {
    this.range = range;
  }

  public SearchRange getRange( ) {
    return range;
  }

  /**
   * @return the actual length.
   */
  public int length( ) {
    return this.rows.size( );
  }
  
}
