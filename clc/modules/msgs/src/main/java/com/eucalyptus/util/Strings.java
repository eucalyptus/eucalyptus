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
package com.eucalyptus.util;

import javax.annotation.Nullable;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * Utility functions for strings.
 */
public class Strings {

  /**
   * Null safe string conversion
   *
   * @param object The object to convert to a String
   * @return The object as a String or null if null                    
   */
  public static String toString( @Nullable final Object object ) {
    return toString( object, null );
  }

  /**
   * Null safe string conversion
   * 
   * @param object The object to convert to a String
   * @param defaultValue The default value to use
   * @return The object as a String or the default value if null                    
   */
  public static String toString( @Nullable final Object object,
                                 @Nullable final String defaultValue ) {
    return object == null ? defaultValue : object.toString();     
  }
  
  /**
   * Get a Function for trimming a String.
   *
   * <P>The returned function will pass through null values.</P>
   *
   * @return The trimming function
   * @see String#trim()
   */
  public static Function<String,String> trim() {
    return StringFunctions.TRIM;
  }

  /**
   * Get a Function for upper casing a String.
   *
   * <P>The returned function will pass through null values.</P>
   *
   * @return The upper casing function
   * @see String#toUpperCase()
   */
  public static Function<String,String> upper() {
    return StringFunctions.UPPER;
  }

  /**
   * Get a Function for lower casing a String.
   *
   * <P>The returned function will pass through null values.</P>
   *
   * @return The upper casing function
   * @see String#toLowerCase() ()
   */
  public static Function<String,String> lower() {
    return StringFunctions.LOWER;
  }

  /**
   * Get a Predicate for matching the start of a String.
   *
   * @param prefix The prefix to match
   * @return The predicate
   * @see String#startsWith(String)
   */
  public static Predicate<String> startsWith( final String prefix ) {
    return new Predicate<String>() {
      @Override
      public boolean apply( @Nullable final String text ) {
        return text != null && text.startsWith( prefix );
      }
    };
  }

  /**
   * Get a Predicate for matching the start of a String.
   *
   * @param text The text to perform a prefix match against
   * @return The predicate
   * @see String#startsWith(String)
   */
  public static Predicate<String> isPrefixOf( final String text ) {
    return text == null ?
        Predicates.<String>alwaysFalse() :
        new Predicate<String>() {
      @Override
      public boolean apply( @Nullable final String prefix ) {
        return prefix != null && text.startsWith( prefix );
      }
    };
  }

  /**
   * Convert an object to a string.
   *
   * <P>The returned function will pass through null values.</P>
   * 
   * @return The toString function
   * @see #toString(Object)
   */
  public static Function<Object,String> toStringFunction() {
    return StringerFunctions.TOSTRING;
  }
  
  private enum StringFunctions implements Function<String,String> {
    LOWER {
      @Override
      public String apply( final String text ) {
        return text == null ? null : text.toLowerCase();
      }
    },
    UPPER {
      @Override
      public String apply( final String text ) {
        return text == null ? null : text.toUpperCase();
      }
    },
    TRIM {
      @Override
      public String apply( final String text ) {
        return text == null ? null : text.toUpperCase();
      }
    }    
  }
  
  private enum StringerFunctions implements Function<Object,String> {
    TOSTRING {
      @Override
      public String apply( final Object object ) {
        return Strings.toString( object );
      }
    }
  }
}
