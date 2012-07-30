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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

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
  // Row specific extra field descriptions
  private ArrayList<SearchResultFieldDesc> extraFields = new ArrayList<SearchResultFieldDesc>( );
  
  public SearchResultRow( ) {
  }
  
  public SearchResultRow( List<String> row ) {
    this( row, null );
  }
  
  public SearchResultRow( List<String> row, List<SearchResultFieldDesc> extra ) {
    if ( row != null ) {
      this.row.addAll( row );
    }
    if ( extra != null ) {
      this.extraFields.addAll( extra );
    }
  }
  
  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "(" );
    sb.append( "extraFields=" ).append( extraFields ).append( "," );
    sb.append( "rows=" ).append( row );
    sb.append( ")" );
    return sb.toString( );
  }
  
  public String getField( int i ) {
    if ( i < row.size( ) ) {
      return row.get( i );
    }
    return null;
  }
  
  public void setField( int i, String val ) {
    if ( val == null ) {
      val = "";
    }    
    row.set( i, val );
  }

  public void addField( String val ) {
    if ( val == null ) {
      val = "";
    }
    row.add( val );
  }
  
  public void setExtraFieldDesc( int i, SearchResultFieldDesc desc ) {
    this.extraFields.set( i, desc );
  }

  public void addExtraFieldDesc( SearchResultFieldDesc desc ) {
    this.extraFields.add( desc );
  }
  
  public SearchResultFieldDesc getExtraFieldDesc( int i ) {
    return this.extraFields.get( i );
  }
  
  public void setExtraFieldDescs( List<SearchResultFieldDesc> descs ) {
    this.extraFields.addAll( descs );
  }
  
  public ArrayList<SearchResultFieldDesc> getExtraFieldDescs( ) {
    return this.extraFields;
  }
  
  public ArrayList<String> getRow( ) {
    return this.row;
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
