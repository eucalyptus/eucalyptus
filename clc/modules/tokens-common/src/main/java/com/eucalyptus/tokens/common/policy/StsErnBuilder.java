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
package com.eucalyptus.tokens.common.policy;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.ern.ServiceErnBuilder;
import net.sf.json.JSONException;

/**
 *
 */
public class StsErnBuilder extends ServiceErnBuilder {

  private static final Pattern RESOURCE_PATTERN = Pattern.compile( "(assumed-role|federated-user)/(\\S+)" );

  private static final int ARN_PATTERNGROUP_STS_TYPE = 1;
  private static final int ARN_PATTERNGROUP_STS_NAME = 2;

  public StsErnBuilder( ) {
    super( Collections.singleton( "sts" ) );
  }

  @Override
  public Ern build( final String ern,
                    final String service,
                    final String region,
                    final String account,
                    final String resource ) throws JSONException {
    final Matcher matcher = RESOURCE_PATTERN.matcher( resource );
    if ( matcher.matches( ) ) {
      final String type = matcher.group( ARN_PATTERNGROUP_STS_TYPE );
      final String name = matcher.group( ARN_PATTERNGROUP_STS_NAME );
      return new StsResourceName( region, account, type, name );
    }
    throw new JSONException( "'" + ern + "' is not a valid ARN" );
  }
}