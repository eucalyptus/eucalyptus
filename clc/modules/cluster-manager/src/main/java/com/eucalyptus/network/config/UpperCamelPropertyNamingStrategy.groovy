/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.network.config

import com.google.common.base.CaseFormat
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.codehaus.jackson.map.MapperConfig
import org.codehaus.jackson.map.PropertyNamingStrategy
import org.codehaus.jackson.map.introspect.AnnotatedField
import org.codehaus.jackson.map.introspect.AnnotatedMethod

/**
 *
 */
@CompileStatic
@PackageScope
class UpperCamelPropertyNamingStrategy extends PropertyNamingStrategy {
  @Override
  String nameForField( final MapperConfig<?> mapperConfig,
                       final AnnotatedField field,
                       final String defaultName ) {
    upperCamel( defaultName )
  }

  @Override
  String nameForGetterMethod( final MapperConfig<?> mapperConfig,
                              final AnnotatedMethod method,
                              final String defaultName ) {
    upperCamel( defaultName )
  }

  @Override
  String nameForSetterMethod( final MapperConfig<?> mapperConfig,
                              final AnnotatedMethod method,
                              final String defaultName ) {
    upperCamel( defaultName )
  }

  private String upperCamel( final String name ) {
    CaseFormat.LOWER_CAMEL.to( CaseFormat.UPPER_CAMEL, name )
  }
}
