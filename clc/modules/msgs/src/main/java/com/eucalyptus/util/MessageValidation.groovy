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
package com.eucalyptus.util

import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.system.Ats
import com.google.common.base.CaseFormat
import groovy.transform.CompileStatic

import javax.annotation.Nonnull
import java.lang.reflect.Field
import java.util.regex.Pattern

/**
 *
 */
@CompileStatic
class MessageValidation {

  static interface ValidationAssistant {
    boolean validate( Object object )
    Pair<Long,Long> range( Ats ats )
    Pattern regex( Ats ats )
  }

  static Map<String,String> validateRecursively(
      final Map<String,String> errorMap,
      final ValidationAssistant validationAssistant,
      final String prefix,
      final Object target
  ) {
    target.class.declaredFields.each { Field field ->
      final Ats fieldAts = Ats.from( field )
      field.setAccessible( true )
      final Object value = field.get( target );
      final String displayName = prefix + displayName( field )

      // validate null constraint
      if ( fieldAts.has( Nonnull.class ) && value == null ) {
        errorMap.put( displayName, displayName + " is required" )
      }

      // validate regex
      final Pattern regex = validationAssistant.regex( fieldAts )
      if ( regex && value != null && !(value instanceof Iterable) && !regex.matcher( String.valueOf( value ) ).matches( ) ) {
        errorMap.put( displayName, "'" + String.valueOf(value) + "' for parameter " + displayName + " is invalid" )
      } else if ( regex && value instanceof Iterable ) {
        value.eachWithIndex { Object item, int index  ->
          if ( !regex.matcher( String.valueOf( item ) ).matches( ) ) {
            errorMap.put( displayName + "." + (index + 1), "'" + String.valueOf(item) + "' for parameter " + displayName + "." + (index + 1) + " is invalid" )
          }
        }
      }

      // validate range
      final Pair<Long,Long> range = validationAssistant.range( fieldAts )
      if ( range != null && value instanceof Number ) {
        Long longValue = ((Number) value).longValue()
        if ( longValue < range.getLeft() || longValue > range.getRight() ) {
          errorMap.put( displayName, String.valueOf(value) + " for parameter " + displayName + " is invalid" )
        }
      }
      if ( range != null && value instanceof List ) {
        Long longValue = (long) ((List)value).size()
        if ( longValue < range.getLeft() && range.getLeft() == 1 ) {
          errorMap.put( displayName + ".1", displayName + ".1 is required" )
        } else if ( longValue < range.getLeft() ) {
          errorMap.put( displayName, displayName + " length too short" )
        } else if ( longValue > range.getRight() ) {
          errorMap.put( displayName, displayName + " length too long" )
        }
      }

      // validate recursively
      if ( validationAssistant.validate( value ) ) {
        validateRecursively( errorMap, validationAssistant, displayName + ".", value )
      } else if ( value instanceof Iterable ) {
        value.eachWithIndex { Object item, int index ->
          if ( validationAssistant.validate( item )) {
            validateRecursively( errorMap, validationAssistant, displayName + "." + (index + 1) + ".", item )
          }
        }
      }
    }
    errorMap
  }

  public static String displayName( Field field ) {
    HttpParameterMapping httpParameterMapping = Ats.from( field ).get( HttpParameterMapping.class );
    return httpParameterMapping != null ?
        httpParameterMapping.parameter()[0] :
        CaseFormat.LOWER_CAMEL.to( CaseFormat.UPPER_CAMEL, field.getName() );
  }
}
