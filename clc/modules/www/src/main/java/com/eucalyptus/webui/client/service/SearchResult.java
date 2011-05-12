package com.eucalyptus.webui.client.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SearchResult implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private int totalSize;
  private int start;
  private int length;
  private ArrayList<SearchResultFieldDesc> descs = new ArrayList<SearchResultFieldDesc>( );
  private ArrayList<SearchResultRow> rows = new ArrayList<SearchResultRow>( );
  
  public SearchResult( ) {
  }
  
  public SearchResult( int totalSize, int start, int length ) {
    this.totalSize = totalSize;
    this.start = start;
    this.length = length;
  }

  public void setDescs( List<SearchResultFieldDesc> descs ) {
    this.descs.clear( );
    this.descs.addAll( descs );
  }

  public ArrayList<SearchResultFieldDesc> getDescs( ) {
    return descs;
  }

  public void setRows( List<SearchResultRow> rows ) {
    this.rows.clear( );
    this.rows.addAll( rows );
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

  public void setStart( int start ) {
    this.start = start;
  }

  public int getStart( ) {
    return start;
  }

  public void setLength( int length ) {
    this.length = length;
  }

  public int getLength( ) {
    return length;
  }
  
}
