/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth.util;

import java.io.IOException;
import java.util.List;
import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
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
      throw Throwables.propagate( e );
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
