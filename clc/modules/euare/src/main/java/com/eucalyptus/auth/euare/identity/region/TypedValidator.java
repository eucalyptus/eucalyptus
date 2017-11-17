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
import io.vavr.Tuple2;
import io.vavr.collection.Stream;

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
    for ( Tuple2<?, Integer> itemAndIndex : Stream.ofAll( MoreObjects.firstNonNull( namedProperty.get( ), Collections.emptyList( ) ) ).zipWithIndex( ) ) {
      Object target = itemAndIndex._1;
      final Integer index = itemAndIndex._2;
      try {
        getErrors( ).pushNestedPath( field + "[" + String.valueOf( index ) + "]" );
        ValidationUtils.invokeValidator( validator, target, getErrors( ) );
      } finally {
        getErrors( ).popNestedPath( );
      }

    }

  }

}
