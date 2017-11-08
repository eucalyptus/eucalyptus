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
