/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.policy.condition;

import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import com.eucalyptus.auth.policy.PatternUtils;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 *
 */
@PolicyCondition( ArnLikeConditionOp.NAME )
public class ArnLikeConditionOp implements ArnConditionOp {
  static final String NAME = "ArnLike";

  static final Splitter arnSplitter = Splitter.on(':').limit(6);

  @Override
  public boolean check( @Nullable final String key, final String value ) {
    return key != null && value != null && matchesLike( value, key );
  }

  static boolean matchesLike( final String arnPattern, final String arn ) {
    boolean matches = true;

    final List<String> arnPatternParts = Lists.newArrayList( arnSplitter.split( arnPattern ) );
    final List<String> arnParts = Lists.newArrayList( arnSplitter.split( arn ) );

    if ( arnPatternParts.size( ) == 6 & arnParts.size( ) == 6  ) {
      for ( int i= 0; i<6; i++ ) {
        final String arnPatternPart = arnPatternParts.get( i ).toLowerCase( );
        final String arnPart = arnParts.get( i ).toLowerCase( );
        final String pattern = PatternUtils.toJavaPattern( arnPatternPart );
        if ( !Pattern.matches( pattern, arnPart ) ) {
          matches = false;
          break;
        }
      }
    } else {
      matches = false;
    }

    return matches;
  }
}
