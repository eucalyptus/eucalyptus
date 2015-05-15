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
package com.eucalyptus.auth.util;

import java.io.IOException;
import java.util.List;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/**
 *
 */
public class ClassPathSystemAccountProvider implements SystemAccountProvider {

  private final String alias;
  private final boolean createAdminAccessKey;
  private final List<SystemAccountRole> roles;

  public ClassPathSystemAccountProvider(
      final String alias,
      final boolean createAdminAccessKey,
      final List<SystemAccountRole> roles ) {
    this.alias = alias;
    this.createAdminAccessKey = createAdminAccessKey;
    this.roles = roles;

    for ( final SystemAccountRole role : roles ) {
      if ( role instanceof ClassPathSystemAccountRole ) {
        ((ClassPathSystemAccountRole)role).setResourceClass( getClass( ) );
      }
    }
  }

  @Override
  public String getAlias( ) {
    return alias;
  }

  @Override
  public boolean isCreateAdminAccessKey( ) {
    return createAdminAccessKey;
  }

  @Override
  public List<SystemAccountRole> getRoles( ) {
    return roles;
  }

  protected static AttachedPolicy newAttachedPolicy( final String name ) {
    return new ClassPathAttachedPolicy( name );
  }

  protected static SystemAccountRole newSystemAccountRole( final String name,
                                                           final String path,
                                                           final List<AttachedPolicy> policies ) {
    return new ClassPathSystemAccountRole( name, path, policies );
  }

  private static String getResourceName( String name, String type ) {
    return CaseFormat.UPPER_CAMEL.to( CaseFormat.LOWER_HYPHEN, name + type ) + ".json";
  }

  private static String loadResource( final Class<?> resourceClass, final String resourceName ) {
    try {
      return Resources.toString(
          Resources.getResource( resourceClass, resourceName ),
          Charsets.UTF_8 );
    } catch ( final IOException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  private static final class ClassPathAttachedPolicy implements AttachedPolicy {
    private Class<?> resourceClass;
    private final String name;

    public ClassPathAttachedPolicy( final String name  ) {
      this.name = name;
    }

    @Override
    public String getName( ) {
      return name;
    }

    @Override
    public String getPolicy( ) {
      return loadResource( resourceClass, getResourceName( getName(), "Policy" ) );
    }

    void setResourceClass( final Class<?> resourceClass ) {
      this.resourceClass = resourceClass;
    }
  }

  private static final class ClassPathSystemAccountRole implements SystemAccountRole {
    private Class<?> resourceClass;
    private final String name;
    private final String path;
    private final List<AttachedPolicy> policies;

    public ClassPathSystemAccountRole(
        final String name,
        final String path,
        final List<AttachedPolicy> policies
    ) {
      this.name = name;
      this.path = path;
      this.policies = policies;
    }

    @Override
    public String getName( ) {
      return name;
    }

    @Override
    public String getPath( ) {
      return path;
    }

    @Override
    public String getAssumeRolePolicy( ) {
      return loadResource( resourceClass, getResourceName( getName( ), "AssumeRolePolicy" ) );
    }

    @Override
    public List<AttachedPolicy> getPolicies() {
      return policies;
    }

    void setResourceClass( final Class<?> resourceClass ) {
      this.resourceClass = resourceClass;
      for ( final AttachedPolicy policy : policies ) {
        if ( policy instanceof ClassPathAttachedPolicy ) {
          ((ClassPathAttachedPolicy)policy).setResourceClass( resourceClass );
        }
      }
    }
  }

}
