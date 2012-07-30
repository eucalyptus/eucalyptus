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
