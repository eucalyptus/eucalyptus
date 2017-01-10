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

import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import com.eucalyptus.auth.AuthException;

/**
 *
 */
public interface SystemAccountProvider {

  String getAlias( );

  default boolean isCreateAdminAccessKey( ) {
    return false;
  }

  default List<SystemAccountRole> getRoles( ) {
    return Collections.emptyList( );
  }

  static class Init {
    public static void initialize( final SystemAccountProvider provider ) throws AuthException {
      ServiceLoader.load( SystemAccountInitializer.class ).iterator( ).next( ).initialize( provider );
    }
  }

  interface SystemAccountInitializer {
    void initialize( SystemAccountProvider provider ) throws AuthException;
  }

  interface SystemAccountRole {
    String getName( );
    String getPath( );
    String getAssumeRolePolicy( );
    List<AttachedPolicy> getPolicies( );
  }

  interface AttachedPolicy {
    String getName( );
    String getPolicy( );
  }

  static final class BasicSystemAccountRole implements SystemAccountRole {
    private final String name;
    private final String path;
    private final String assumeRolePolicy;
    private final List<AttachedPolicy> policies;

    public BasicSystemAccountRole(
        final String name,
        final String path,
        final String assumeRolePolicy,
        final List<AttachedPolicy> policies
    ) {
      this.name = name;
      this.path = path;
      this.assumeRolePolicy = assumeRolePolicy;
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
      return assumeRolePolicy;
    }

    @Override
    public List<AttachedPolicy> getPolicies( ) {
      return policies;
    }
  }

  static final class BasicAttachedPolicy implements AttachedPolicy {
    private final String name;
    private final String policy;

    public BasicAttachedPolicy( final String name, final String policy ) {
      this.name = name;
      this.policy = policy;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getPolicy() {
      return policy;
    }
  }
}
