/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

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
