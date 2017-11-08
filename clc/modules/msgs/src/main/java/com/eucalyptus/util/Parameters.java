/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

/**
 *
 */
public class Parameters {

  public static <T> T checkParam( final T value,
                                  final Matcher<? super T> matcher ) {
    return checkParam( "", value, matcher );
  }

  public static <T> T checkParam( final String reason,
                                  final T value,
                                  final Matcher<? super T> matcher ) {
    if ( !matcher.matches( value ) ) {
      final Description description = new StringDescription();
      description.appendText( reason )
          .appendText( "\nExpected: " )
          .appendDescriptionOf( matcher )
          .appendText( "\n     but: " );
      matcher.describeMismatch( value, description );

      throw new IllegalArgumentException( description.toString() );
    }

    return value;
  }

  public static <T> T checkParamNotNull( final String reason,
                                         final T value ) {
    return checkParam( reason, value, notNullValue( ) );
  }

  public static String checkParamNotNullOrEmpty( final String reason,
                                                 final String value ) {
    return checkParam( reason, value, not( isEmptyOrNullString( ) ) );
  }
}
