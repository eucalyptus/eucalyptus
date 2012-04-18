package com.eucalyptus.webui.client.service;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SearchResultCache {

  private static final Logger LOG = Logger.getLogger( SearchResultCache.class.getName( ) );
  
  private static final SearchRange NULL_SORT = new SearchRange( -1, 0, -999, true );
  
  private SearchRange sort = NULL_SORT;
  private int totalSize = 0;
  private ArrayList<SearchResultFieldDesc> descs = new ArrayList<SearchResultFieldDesc>( );
  private ArrayList<SearchResultRow> rows = new ArrayList<SearchResultRow>( );
  
  public SearchResultCache( ) {
  }
  
  public ArrayList<SearchResultFieldDesc> getDescs( ) {
    return descs;
  }
  
  public void update( SearchResult result ) {
    if ( !this.sort.isSameSort( result.getRange( ) ) ) {
      restart( result );
    }
    merge( result );
  }

  private void fillRows( int newSize ) {
    for ( int i = rows.size( ); i < newSize; i++ ) {
      this.rows.add( null );
    }
  }
  
  private void merge( SearchResult result ) {
    if ( result != null ) {
      int start = result.getRange( ).getStart( );
      ArrayList<SearchResultRow> newRows = result.getRows( );
      int newSize = start + newRows.size( );
      if ( rows.size( ) < newSize ) {
        fillRows( newSize );
      }
      for ( int i = 0; i < newRows.size( ); i++ ) {
        rows.set( i + start, newRows.get( i ) );
      }
    }
  }
  
  public SearchResult lookup( SearchRange range ) {
    if ( !this.sort.isSameSort( range ) ) {
      LOG.log( Level.INFO, "Not the same sort: " + this.sort + " vs. " + range );
      return null;
    }
    int realSize = Math.min( range.getLength( ), this.totalSize - range.getStart( ) );
    if ( range.getStart( ) + realSize > this.rows.size( ) ) {
      LOG.log( Level.INFO, "Outside of range: cache size=" + this.rows.size( ) );
      return null;
    }
    for ( int i = 0; i < realSize; i++ ) {
      if ( this.rows.get( i + range.getStart( ) ) == null ) {
        LOG.log( Level.INFO, "Hole at " + ( i + range.getStart( ) ) );
        return null;
      }
    }
    SearchResult result = new SearchResult( this.totalSize, range );
    result.setDescs( this.descs );
    result.addRows( this.rows.subList( range.getStart( ), range.getStart( ) + realSize ) );
    return result;
  }

  public void clear( ) {
    this.sort = NULL_SORT;
    this.totalSize = 0;
    this.descs.clear( );
    this.rows.clear( );    
  }
  
  public void restart( SearchResult newResult ) {
    clear( );
    if ( newResult != null ) {
      this.sort = newResult.getRange( );
      this.totalSize = newResult.getTotalSize( );
      this.descs.addAll( newResult.getDescs( ) );
      LOG.log( Level.INFO, "After restart: " + this.getDescs( ) );
    }
  }
  
}
