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
package com.eucalyptus.auth.util;

import java.io.IOException;
import com.eucalyptus.auth.DatabaseAuthBootstrapper;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/**
 * System role provider that loads policies from classpath resources.
 *
 * E.g. ProviderName ->
 *        provider-name-assume-role-policy.json
 *        provider-name-policy.json
 */
public abstract class ClassPathSystemRoleProvider implements DatabaseAuthBootstrapper.SystemRoleProvider {

  @Override
  public String getAssumeRolePolicy( ) {
    return loadPolicy( getResourceName( "AssumeRolePolicy" ) );
  }

  @Override
  public String getPolicy( ) {
    return loadPolicy( getResourceName( "Policy" ) );
  }

  private String getResourceName( String type ) {
    return CaseFormat.UPPER_CAMEL.to( CaseFormat.LOWER_HYPHEN, getName( ) + type ) + ".json";
  }

  private String loadPolicy( final String resourceName ) {
    try {
      return Resources.toString( Resources.getResource( getClass( ), resourceName ), Charsets.UTF_8 );
    } catch ( final IOException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }
}
