/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.compute.common.internal.tags.Filters;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import javaslang.collection.Stream;

public class Filter extends EucalyptusData {

  private String name;
  @HttpParameterMapping( parameter = "Value" )
  private ArrayList<String> valueSet = new ArrayList<String>( );

  /**
   * Create a filter with any wildcards in the value escaped.
   */
  public static Filter filter( String name, String value ) {
    Filter filter = new Filter( );
    filter.name = name;
    filter.valueSet.add( Filters.escape( value ) );
    return filter;
  }

  /**
   * Create a filter with any wildcards in the values escaped.
   */
  public static Filter filter( String name, Iterable<String> values ) {
    Filter filter = new Filter( );
    filter.setName( name );
    filter.getValueSet( ).addAll( Stream.ofAll( values ).map( Filters.escape( ) ).toJavaList( ) );
    return filter;
  }

  public String getName( ) {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public ArrayList<String> getValueSet( ) {
    return valueSet;
  }

  public void setValueSet( ArrayList<String> valueSet ) {
    this.valueSet = valueSet;
  }
}
