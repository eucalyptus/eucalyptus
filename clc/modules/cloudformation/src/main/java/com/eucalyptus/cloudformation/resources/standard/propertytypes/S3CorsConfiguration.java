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
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.ArrayList;
import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class S3CorsConfiguration {

  @Required
  @Property
  private ArrayList<S3CorsConfigurationRule> corsRules = Lists.newArrayList( );

  public ArrayList<S3CorsConfigurationRule> getCorsRules( ) {
    return corsRules;
  }

  public void setCorsRules( ArrayList<S3CorsConfigurationRule> corsRules ) {
    this.corsRules = corsRules;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final S3CorsConfiguration that = (S3CorsConfiguration) o;
    return Objects.equals( getCorsRules( ), that.getCorsRules( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getCorsRules( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "corsRules", corsRules )
        .toString( );
  }
}
