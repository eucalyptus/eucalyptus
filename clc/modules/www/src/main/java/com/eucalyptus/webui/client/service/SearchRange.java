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
