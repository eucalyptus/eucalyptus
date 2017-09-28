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
package com.eucalyptus.auth.euare.identity.region;

import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.List;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import com.google.common.base.CaseFormat;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import javaslang.Tuple2;
import javaslang.collection.Stream;

/**
 *
 */
abstract class TypedValidator<T> implements Validator {

  public static String toFieldName( String methodName ) {
    return CaseFormat.UPPER_CAMEL.to( CaseFormat.LOWER_CAMEL, methodName.substring( 3 ) );
  }

  public static String toPropertyName( String fieldName ) {
    return CaseFormat.LOWER_CAMEL.to( CaseFormat.UPPER_CAMEL, fieldName );
  }

  @Override
  public boolean supports( final Class<?> aClass ) {
    return aClass.equals( getTargetClass( ) );
  }

  @SuppressWarnings( "unchecked" )
  @Override
  public void validate( final Object o, final Errors errors ) {
    validate( (T) o );
  }

  public abstract Errors getErrors( );

  public Class<?> getTargetClass( ) {
    return (Class<?>) ( (ParameterizedType) getClass( ).getGenericSuperclass( ) ).getActualTypeArguments( )[ 0 ];
  }

  public void validate( T target ) {
  }

  public String pathTranslate( String path, String name ) {
    String pathPrefix = Strings.isNullOrEmpty( path ) ? "" : path + (path.endsWith( "." ) ? "" : ".");
    String fullPath = name!=null ? pathPrefix + name : path;
    return Stream.of( fullPath.split( "\\." ) ).map( TypedValidator::toPropertyName ).mkString( "." );
  }

  public String pathTranslate( String path ) {
    return pathTranslate( path, null );
  }

  public void require( NamedProperty<?> namedProperty ) {
    String fieldName = toFieldName( namedProperty.getMethod( ) );
    ValidationUtils.rejectIfEmptyOrWhitespace( getErrors( ), fieldName, "property.required", new Object[]{ pathTranslate( getErrors( ).getNestedPath( ), fieldName ) }, "Missing required property \"{0}\"" );
  }

  public void validate( NamedProperty<?> namedProperty, Validator validator ) {
    validate( namedProperty.get( ), toFieldName( namedProperty.getMethod( ) ), validator );
  }

  public void validate( Object target, String path, Validator validator ) {
    try {
      getErrors( ).pushNestedPath( path );
      ValidationUtils.invokeValidator( validator, target, getErrors( ) );
    } finally {
      getErrors( ).popNestedPath( );
    }

  }

  public void validateAll( NamedProperty<List<?>> namedProperty, Validator validator ) {
    final String field = toFieldName( namedProperty.getMethod( ) );
    for ( Tuple2<?, Long> itemAndIndex : Stream.ofAll( MoreObjects.firstNonNull( namedProperty.get( ), Collections.emptyList( ) ) ).zipWithIndex( ) ) {
      Object target = itemAndIndex._1;
      final Long index = itemAndIndex._2;
      try {
        getErrors( ).pushNestedPath( field + "[" + String.valueOf( index ) + "]" );
        ValidationUtils.invokeValidator( validator, target, getErrors( ) );
      } finally {
        getErrors( ).popNestedPath( );
      }

    }

  }

}
