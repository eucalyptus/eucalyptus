/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

/**
 *
 */
public class WildcardNameMatcher {

  private static final String PATTERN_DEFAULT = "(?!x)x"; // x but not x, will not match anything
  private static final Pattern PATTERN_DEFAULT_COMPILED = Pattern.compile( PATTERN_DEFAULT );

  private AtomicReference<Pair<String,Pattern>> patternPairRef =
      new AtomicReference<>( Pair.pair( "", PATTERN_DEFAULT_COMPILED ) );

  public boolean matches( final String patternList, final CharSequence value ) {
    final Pattern pattern = getPattern( patternList );
    return pattern.matcher( value ).matches( );
  }

  private Pattern getPattern( final String patternList ) {
    final Pair<String,Pattern> patternPair = patternPairRef.get( );
    if ( patternPair == null || !patternPair.getLeft( ).equals( patternList ) ) {
      final Pattern pattern = buildPattern( patternList );
      patternPairRef.compareAndSet( patternPair, Pair.pair( patternList, pattern ) );
      return pattern;
    } else {
      return patternPair.getRight( );
    }
  }

  private Pattern buildPattern( final String patternList ) {
    if ( !patternList.trim( ).isEmpty( ) ) {
      final Splitter splitter =
          Splitter.on( CharMatcher.WHITESPACE.or( CharMatcher.anyOf( ",;|" ) ) ).trimResults( ).omitEmptyStrings( );
      final StringBuilder builder = new StringBuilder( );
      builder.append( "(" );
      for ( final String nameWithWildcards : splitter.split( patternList ) ) {
        builder.append( toPattern( nameWithWildcards ) );
        builder.append( '|' );
      }
      builder.append( PATTERN_DEFAULT );
      builder.append( ")" );
      return Pattern.compile( builder.toString( ) );

    }
    return PATTERN_DEFAULT_COMPILED;
  }

  private static String toPattern( final String nameWithWildcards ) {
    if ( !nameWithWildcards.matches( "[\\w*.-]+" ) ) {
      return PATTERN_DEFAULT;
    }
    return nameWithWildcards.replace( ".", "\\." ).replace( "*", ".*" );
  }

}
