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

package com.eucalyptus.auth.ldap;

import java.util.Set;
import com.google.common.collect.Sets;

public class Selection {

  private String searchFilter;
  private Set<String> selected = Sets.newHashSet( );
  private Set<String> notSelected = Sets.newHashSet( );
  
  public Selection( ) {
  }
  
  public void setSearchFilter( String searchFilter ) {
    this.searchFilter = searchFilter;
  }
  public String getSearchFilter( ) {
    return searchFilter;
  }
  public void setSelected( Set<String> selected ) {
    this.selected = selected;
  }
  public Set<String> getSelected( ) {
    return selected;
  }
  public void setNotSelected( Set<String> notSelected ) {
    this.notSelected = notSelected;
  }
  public Set<String> getNotSelected( ) {
    return notSelected;
  }
  
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "filter='" ).append( this.searchFilter ).append( "';" );
    sb.append( "select='" ).append( this.selected ).append( "';" );
    sb.append( "not-select='" ).append( this.notSelected ).append( "'" );
    return sb.toString( );
  }
  
}
