/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.cassandra.common.util;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.List;
import com.google.common.collect.Lists;

/**
 * Utility class for working with CQL
 */
public class CqlUtil {

  private static final char SINGLE_QUOTE = '\'';
  private static final char DOUBLE_QUOTE = '"';
  private static final char DASH_COMMENT = '-';
  private static final char SLASH_COMMENT = '/';
  private static final char STAR_COMMENT = '*';
  private static final char EOL = '\n';
  private static final char EOS = ';';

  /**
   * Split a cql script into statements removing comments and whitespace.
   */
  public static List<String> splitCql( final String cql ) throws ParseException {
    final List<String> statements = Lists.newArrayList( );
    final LineNumberReader reader = new LineNumberReader( new StringReader( cql ) );

    // cql allows /**/ multiline comments without nesting and // -- single line comments
    // string quoting can be ' or " and quotes are escaped with a repeated quote char
    int character;
    int charCount = -1;
    try {
      final StringBuilder statement = new StringBuilder( 512 );
      boolean inquote = false;
      boolean lastinquote = false;
      char quotechar = SINGLE_QUOTE;
      boolean incomment = false;
      int commenttype = DASH_COMMENT;
      int last = 0;
      while ( ( character = reader.read( ) ) > 0 ) {
        charCount++;
        if ( inquote && last == quotechar ) { // check for end of quote or escaped quote
          if ( character == quotechar && lastinquote ) {
            last = character;
            lastinquote = false;
            continue;
          } else if ( lastinquote ) {
            inquote = false;
          }
        }
        if ( inquote ) {
          lastinquote = true;
          statement.append( (char)character );
        } else {
          lastinquote = false;
          if ( incomment ) { // check for comment end
            switch ( character ) {
              case EOL:
                if ( commenttype == SLASH_COMMENT || commenttype == DASH_COMMENT ) {
                  last = character;
                  incomment = false;
                  continue;
                }
                break;
              case SLASH_COMMENT:
                if ( commenttype == STAR_COMMENT && last == STAR_COMMENT ) {
                  last = character;
                  incomment = false;
                  continue;
                }
                break;
            }
          } else { // handle statement text, detect start of comment or quote
            char commentIf = 0;
            switch ( character ) {
              case SINGLE_QUOTE:
              case DOUBLE_QUOTE:
                inquote = true;
                quotechar = (char)character;
                break;
              case DASH_COMMENT:
                commentIf = DASH_COMMENT;
                break;
              case SLASH_COMMENT:
                commentIf = SLASH_COMMENT;
                break;
              case STAR_COMMENT:
                commentIf = SLASH_COMMENT;
                break;
              case EOS:
                statements.add( statement.toString( ).trim( ) );
                statement.setLength( 0 );
                last = character;
                continue;
            }
            if ( commentIf != 0 && commentIf == last ) {
              incomment = true;
              commenttype = character;
              last = character;
              statement.setLength( statement.length( ) - 1 );
              continue;
            }
            statement.append( (char)character );
          }
        }

        last = character;
      }

      // check exit state
      if ( inquote ) {
        throw new ParseException(
            "Unterminated quoted string " + quotechar + ", line " + reader.getLineNumber(), charCount );
      }
      if ( incomment && commenttype == STAR_COMMENT ) {
        throw new ParseException( "Unterminated comment, line " + reader.getLineNumber(), charCount );
      }
      if ( !statement.toString( ).trim( ).isEmpty( ) ) {
        throw new ParseException(
            "Unterminated statement: " + statement.toString( ).trim( ) + ", line " + reader.getLineNumber(), charCount );
      }
    } catch ( IOException e ) {
      throw (ParseException) new ParseException(
          "Error reading data, line " + reader.getLineNumber(), charCount ).initCause( e );
    }
    return statements;
  }
}
