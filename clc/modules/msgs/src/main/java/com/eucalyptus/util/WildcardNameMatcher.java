/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
