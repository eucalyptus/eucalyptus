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

public class S3CorsConfigurationRule {

  @Property
  private ArrayList<String> allowedHeaders = Lists.newArrayList( );

  @Required
  @Property
  private ArrayList<String> allowedMethods = Lists.newArrayList( );

  @Required
  @Property
  private ArrayList<String> allowedOrigins = Lists.newArrayList( );

  @Property
  private ArrayList<String> exposedHeaders = Lists.newArrayList( );

  @Property
  private String id;

  @Property
  private Integer maxAge;

  public ArrayList<String> getAllowedHeaders( ) {
    return allowedHeaders;
  }

  public void setAllowedHeaders( ArrayList<String> allowedHeaders ) {
    this.allowedHeaders = allowedHeaders;
  }

  public ArrayList<String> getAllowedMethods( ) {
    return allowedMethods;
  }

  public void setAllowedMethods( ArrayList<String> allowedMethods ) {
    this.allowedMethods = allowedMethods;
  }

  public ArrayList<String> getAllowedOrigins( ) {
    return allowedOrigins;
  }

  public void setAllowedOrigins( ArrayList<String> allowedOrigins ) {
    this.allowedOrigins = allowedOrigins;
  }

  public ArrayList<String> getExposedHeaders( ) {
    return exposedHeaders;
  }

  public void setExposedHeaders( ArrayList<String> exposedHeaders ) {
    this.exposedHeaders = exposedHeaders;
  }

  public String getId( ) {
    return id;
  }

  public void setId( String id ) {
    this.id = id;
  }

  public Integer getMaxAge( ) {
    return maxAge;
  }

  public void setMaxAge( Integer maxAge ) {
    this.maxAge = maxAge;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final S3CorsConfigurationRule that = (S3CorsConfigurationRule) o;
    return Objects.equals( getAllowedHeaders( ), that.getAllowedHeaders( ) ) &&
        Objects.equals( getAllowedMethods( ), that.getAllowedMethods( ) ) &&
        Objects.equals( getAllowedOrigins( ), that.getAllowedOrigins( ) ) &&
        Objects.equals( getExposedHeaders( ), that.getExposedHeaders( ) ) &&
        Objects.equals( getId( ), that.getId( ) ) &&
        Objects.equals( getMaxAge( ), that.getMaxAge( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getAllowedHeaders( ), getAllowedMethods( ), getAllowedOrigins( ), getExposedHeaders( ), getId( ), getMaxAge( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "allowedHeaders", allowedHeaders )
        .add( "allowedMethods", allowedMethods )
        .add( "allowedOrigins", allowedOrigins )
        .add( "exposedHeaders", exposedHeaders )
        .add( "id", id )
        .add( "maxAge", maxAge )
        .toString( );
  }
}
