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
package com.eucalyptus.portal.provider;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.portal.common.provider.TagProvider;
import com.google.common.collect.Maps;

/**
 *
 */
public class TagProviders {

  private static final Map<String,TagProvider> providers = Maps.newConcurrentMap( );

  static boolean register( final TagProvider provider ) {
    return providers.putIfAbsent( provider.getVendor( ), provider ) == null;
  }

  public static Set<String> getTagKeys( final User user ) {
    return providers.values( ).stream( )
        .flatMap( provider -> provider.getTagKeys( user ).stream( ) )
        .collect( Collectors.toSet( ) );
  }
}
