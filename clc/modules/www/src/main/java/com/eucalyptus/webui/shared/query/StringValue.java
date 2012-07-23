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

package com.eucalyptus.webui.shared.query;

public class StringValue implements QueryValue {
  
  private String value;
  
  public StringValue( String v ) {
    this.value = v;
  }
  
  @Override
  public String getValue( ) {
    return this.value;
  }
  
  @Override
  public String toString( ) {
    return this.value;
  }
  
  @Override
  public boolean equals( Object other ) {
    if ( other == null ) {
      return false;
    }
    if ( !(other instanceof StringValue) ) {
      return false;
    }
    StringValue otherValue = ( StringValue )other;
    if ( this.value == null && otherValue.getValue( ) == null ) {
      return true;
    }
    if ( this.value != null && this.value.equals( otherValue.getValue( ) ) ) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode( ) {
    return this.value.hashCode( );
  }
}
