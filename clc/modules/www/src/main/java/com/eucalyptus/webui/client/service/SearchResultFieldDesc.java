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
import java.util.List;

public class SearchResultFieldDesc implements Serializable {

  private static final long serialVersionUID = 1L;
  
  public static enum TableDisplay {
    MANDATORY,
    OPTIONAL,
    NONE,
  }
  
  public static enum Type {
    TEXT,       // single line text string
    ARTICLE,    // multi-line text
    HIDDEN,     // password like text
    REVEALING,  // text revealing itself when mouseover (for security related stuff, like secret key)
    BOOLEAN,    // boolean
    DATE,       // date in long
    ENUM,       // enum value
    KEYVAL,     // dynamic key value (like single line text but can be removed)
    NEWKEYVAL,  // empty key value (for adding new)
    LINK,       // URL link
    ACTION      // custom action, usually causing a popup
  }
  
  private String name;                // ID of the field, also used as the key of a KEYVAL
  private String title;               // title for display
  private Boolean sortable;           // if sortable in table display
  private String width;               // width of column for table display
  private TableDisplay tableDisplay;  // table display type
  private Type type;                  // value type
  private Boolean editable;           // if this field is editable
  private Boolean hidden;             // if this field should be hidden in properties panel
  private List<String> enumValues;    // the list of enum values for an ENUM

  public SearchResultFieldDesc( ) {
  }
  
  public SearchResultFieldDesc( String title, Boolean sortable, String width ) {
    this.name = title;
    this.title = title;
    this.sortable = sortable;
    this.width = width;
    this.tableDisplay = TableDisplay.MANDATORY;
    this.type = Type.TEXT;
    this.setEditable( true );
    this.setHidden( false );
  }

  public SearchResultFieldDesc( String title, Boolean sortable, String width, TableDisplay tableDisplay, Type type, Boolean editable, Boolean hidden ) {
    this.name = title;
    this.title = title;
    this.sortable = sortable;
    this.width = width;
    this.tableDisplay = tableDisplay;
    this.type = type;
    this.editable = editable;
    this.hidden = hidden;
  }
  
  public SearchResultFieldDesc( String name, String title, Boolean sortable, String width, TableDisplay tableDisplay, Type type, Boolean editable, Boolean hidden ) {
    this.name = name;
    this.title = title;
    this.sortable = sortable;
    this.width = width;
    this.tableDisplay = tableDisplay;
    this.type = type;
    this.editable = editable;
    this.hidden = hidden;
  }
  
  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "(" );
    sb.append( "name=" ).append( name ).append( "," );
    sb.append( "title=" ).append( title ).append( "," );
    sb.append( "sortable=" ).append( sortable ).append( "," );
    sb.append( "width=" ).append( width ).append( "," );
    sb.append( "tableDisplay=" ).append( tableDisplay ).append( "," );
    sb.append( "type=" ).append( type ).append( "," );
    sb.append( "editable=" ).append( editable ).append( "," );
    sb.append( "hidden=" ).append( hidden );
    sb.append( ")" );
    return sb.toString( );
  }
  
  public void setTitle( String title ) {
    this.title = title;
  }
  public String getTitle( ) {
    return title;
  }
  public void setSortable( Boolean sortable ) {
    this.sortable = sortable;
  }
  public Boolean getSortable( ) {
    return sortable;
  }
  public void setWidth( String width ) {
    this.width = width;
  }
  public String getWidth( ) {
    return width;
  }

  public void setTableDisplay( TableDisplay tableDisplay ) {
    this.tableDisplay = tableDisplay;
  }

  public TableDisplay getTableDisplay( ) {
    return tableDisplay;
  }

  public void setType( Type type ) {
    this.type = type;
  }

  public Type getType( ) {
    return type;
  }

  public void setEditable( Boolean editable ) {
    this.editable = editable;
  }

  public Boolean getEditable( ) {
    return editable;
  }

  public void setHidden( Boolean hidden ) {
    this.hidden = hidden;
  }

  public Boolean getHidden( ) {
    return hidden;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getName( ) {
    return name;
  }

  public void setEnumValues( List<String> enumValues ) {
    this.enumValues = enumValues;
  }

  public List<String> getEnumValues( ) {
    return enumValues;
  }
  
}
