/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.simplequeue.common.policy;

import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.ern.ServiceErnBuilder;
import net.sf.json.JSONException;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class SimpleQueueErnBuilder extends ServiceErnBuilder {

  public static final Pattern RESOURCE_PATTERN = Pattern.compile( "[A-Za-z0-9_-]+" );

  public SimpleQueueErnBuilder() {
    super( Collections.singleton( SimpleQueuePolicySpec.VENDOR_SIMPLEQUEUE ) );
  }

  @Override
  public Ern build( final String ern,
                    final String service,
                    final String region,
                    final String account,
                    final String resource ) throws JSONException {
    final Matcher matcher = RESOURCE_PATTERN.matcher( resource );
    if ( matcher.matches( ) ) {
      final String id = resource.toLowerCase().trim();
      return new SimpleQueueResourceName( region, account, id );
    }
    throw new JSONException( "'" + ern + "' is not a valid ARN" );
  }
}
