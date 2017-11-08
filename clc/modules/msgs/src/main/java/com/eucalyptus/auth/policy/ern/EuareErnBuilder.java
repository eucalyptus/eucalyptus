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
package com.eucalyptus.auth.policy.ern;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import net.sf.json.JSONException;

/**
 *
 */
public class EuareErnBuilder extends ServiceErnBuilder {

  public static final Pattern ARN_PATTERN =
      Pattern.compile( "arn:aws:iam::([0-9]{12}|eucalyptus):(?:(user|group|role|instance-profile|oidc-provider|server-certificate|policy)((?:/[^/\\s]+)+)|\\*)" );

  public static final int ARN_PATTERNGROUP_IAM_NAMESPACE = 1;
  public static final int ARN_PATTERNGROUP_IAM_TYPE = 2;
  public static final int ARN_PATTERNGROUP_IAM_ID = 3;

  public EuareErnBuilder( ) {
    super( Collections.singleton( "iam" ) );
  }

  @Override
  public Ern build( final String ern,
                    final String service,
                    final String region,
                    final String account,
                    final String resource ) throws JSONException {
    final Matcher matcher = ARN_PATTERN.matcher( ern );
    if ( matcher.matches( ) ) {
      final Optional<String> pathName = Optional.fromNullable( matcher.group( ARN_PATTERNGROUP_IAM_ID ) );
      final String path;
      final String name;
      int lastSlash = pathName.isPresent() ? pathName.get().lastIndexOf( '/' ) : 0;
      if ( lastSlash == 0 ) {
        path = "/";
        name = pathName.isPresent() ? pathName.get().substring( 1 ) : "*";
      } else {
        path = pathName.get().substring( 0, lastSlash );
        name = pathName.get().substring( lastSlash + 1 );
      }
      final String accountId = matcher.group( ARN_PATTERNGROUP_IAM_NAMESPACE );
      final String type = Objects.firstNonNull( matcher.group( ARN_PATTERNGROUP_IAM_TYPE ), "*" );
      return new EuareResourceName( accountId, type, path, name);
    }
    throw new JSONException( "'" + ern + "' is not a valid ARN" );
  }
}
