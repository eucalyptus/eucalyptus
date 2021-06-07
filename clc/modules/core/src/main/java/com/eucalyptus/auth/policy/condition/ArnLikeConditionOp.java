/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth.policy.condition;

import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import com.eucalyptus.auth.policy.PolicyUtils;
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
        final String pattern = PolicyUtils.toJavaPattern( arnPatternPart );
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
