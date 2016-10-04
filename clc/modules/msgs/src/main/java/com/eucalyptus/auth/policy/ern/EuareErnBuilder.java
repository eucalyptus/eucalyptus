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
      Pattern.compile( "arn:aws:iam::([0-9]{12}|eucalyptus):(?:(user|group|role|instance-profile|oidc-provider|server-certificate)((?:/[^/\\s]+)+)|\\*)" );

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
