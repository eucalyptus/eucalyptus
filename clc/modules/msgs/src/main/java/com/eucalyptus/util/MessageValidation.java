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
package com.eucalyptus.util;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.system.Ats;
import com.google.common.base.CaseFormat;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;

/**
 *
 */
public class MessageValidation {

  public static Map<String, String> validateRecursively(
      final Map<String, String> errorMap,
      final ValidationAssistant validationAssistant,
      final String prefix,
      final Object target
  ) {
    for ( final Field field : target.getClass( ).getDeclaredFields( ) ) {
      final Ats fieldAts = Ats.from( field );
      field.setAccessible( true );
      Object value;
      try {
        value = field.get( target );
      } catch ( IllegalAccessException e ) {
        value = null;
      }
      final String displayName = prefix + displayName( field );

      // validate null constraint
      if ( fieldAts.has( Nonnull.class ) && value == null ) {
        errorMap.put( displayName, displayName + " is required" );
      }

      // validate regex
      final Pattern regex = validationAssistant.regex( fieldAts );
      if ( regex != null && value != null &&
          !( value instanceof Iterable ) && !regex.matcher( String.valueOf( value ) ).matches( ) ) {
        errorMap.put( displayName, "'" + String.valueOf( value ) + "' for parameter " + displayName + " is invalid" );
      } else if ( regex != null && value instanceof Iterable ) {
        for ( final Tuple2<?, Integer> itemAndIndex : Stream.ofAll( (Iterable<?>) value ).zipWithIndex( ) ) {
          Object item = itemAndIndex._1;
          int index = itemAndIndex._2;
          if ( !regex.matcher( String.valueOf( item ) ).matches( ) ) {
            errorMap.put(
                displayName + "." + ( index + 1 ),
                "'" + String.valueOf( item ) + "' for parameter " + displayName + "." + ( index + 1 ) + " is invalid" );
          }
        }
      }

      // validate range
      final Pair<Long, Long> range = validationAssistant.range( fieldAts );
      if ( range != null && value instanceof Number ) {
        Long longValue = ( (Number) value ).longValue( );
        if ( longValue < range.getLeft( ) || longValue > range.getRight( ) ) {
          errorMap.put( displayName, String.valueOf( value ) + " for parameter " + displayName + " is invalid" );
        }
      }

      if ( range != null && value instanceof List ) {
        Long longValue = (long) ( (List) value ).size( );
        if ( longValue < range.getLeft( ) && range.getLeft( ) == 1 ) {
          errorMap.put( displayName + ".1", displayName + ".1 is required" );
        } else if ( longValue < range.getLeft( ) ) {
          errorMap.put( displayName, displayName + " length too short" );
        } else if ( longValue > range.getRight( ) ) {
          errorMap.put( displayName, displayName + " length too long" );
        }
      }

      // validate recursively
      if ( validationAssistant.validate( value ) ) {
        validateRecursively( errorMap, validationAssistant, displayName + ".", value );
      } else if ( value instanceof Iterable ) {
        for ( final Tuple2<?, Integer> itemAndIndex : Stream.ofAll( (Iterable<?>) value ).zipWithIndex( ) ) {
          Object item = itemAndIndex._1;
          int index = itemAndIndex._2;
          if ( validationAssistant.validate( item ) ) {
            validateRecursively( errorMap, validationAssistant, displayName + "." + ( index + 1 ) + ".", item );
          }
        }
      }
    }

    return errorMap;
  }

  public static String displayName( Field field ) {
    HttpParameterMapping httpParameterMapping = Ats.from( field ).get( HttpParameterMapping.class );
    return httpParameterMapping != null ?
        httpParameterMapping.parameter( )[ 0 ] :
        CaseFormat.LOWER_CAMEL.to( CaseFormat.UPPER_CAMEL, field.getName( ) );
  }

  public interface ValidatableMessage {
    Map<String, String> validate( );
  }

  public interface ValidationAssistant {

    boolean validate( Object object );

    Pair<Long, Long> range( Ats ats );

    Pattern regex( Ats ats );
  }
}
