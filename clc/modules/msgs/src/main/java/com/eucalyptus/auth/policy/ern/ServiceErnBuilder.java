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

import java.util.Set;
import com.google.common.collect.ImmutableSet;
import net.sf.json.JSONException;

/**
 *
 */
public abstract class ServiceErnBuilder {

  private final Set<String> services;

  protected ServiceErnBuilder( final Iterable<String> services ) {
    this.services = ImmutableSet.copyOf( services );
  }

  public final boolean supports( final String service ) {
    return services.contains( service );
  }

  public abstract Ern build( String ern,
                             String service,
                             String region,
                             String account,
                             String resource ) throws JSONException;
}
