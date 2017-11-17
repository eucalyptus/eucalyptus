/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
