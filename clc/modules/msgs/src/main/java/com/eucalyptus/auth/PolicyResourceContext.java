/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth;

import java.lang.reflect.Modifier;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 *
 */
public class PolicyResourceContext implements AutoCloseable {
  private static final Logger logger = Logger.getLogger( PolicyResourceContext.class );
  private static final List<PolicyResourceInterceptor> resourceInterceptors = Lists.newCopyOnWriteArrayList();

  private PolicyResourceContext( ) {
  }

  public static PolicyResourceContext of( final Object resource, final String action ) {
    notifyResourceInterceptors(
        resource instanceof PolicyResourceInfo ?
            (PolicyResourceInfo) resource :
            TypeMappers.transform( resource, PolicyResourceInfo.class ),
        action );
    return new PolicyResourceContext( );
  }

  public static PolicyResourceInfo resourceInfo( @Nullable final String accountNumber,
                                                 @Nonnull  final Object resourceObject ) {
    return resourceInfo( accountNumber, resourceObject, resourceObject.getClass( ) );
  }

  public static PolicyResourceInfo resourceInfo( @Nullable final String accountNumber,
                                                 @Nonnull  final Object resourceObject,
                                                 @Nonnull  final Class resourceClass  ) {
    return new PolicyResourceInfo( ) {
      @Nullable
      @Override
      public String getResourceAccountNumber( ) {
        return accountNumber;
      }

      @Nonnull
      @Override
      public Class getResourceClass( ) {
        return resourceClass;
      }

      @Nonnull
      @Override
      public Object getResourceObject( ) {
        return resourceObject;
      }
    };
  }

  @Override
  public void close( ) {
    notifyResourceInterceptors( null, null );
  }

  private static void notifyResourceInterceptors(
      final PolicyResourceInfo policyResourceInfo,
      final String action
  ) {
    for ( final PolicyResourceContext.PolicyResourceInterceptor interceptor : resourceInterceptors ) {
      interceptor.onResource( policyResourceInfo, action );
    }
  }

  public interface PolicyResourceInfo {
    @Nullable
    String getResourceAccountNumber( );

    @Nonnull
    Class getResourceClass( );

    @Nonnull
    Object getResourceObject( );
  }

  public interface PolicyResourceInterceptor {
    void onResource( @Nullable PolicyResourceInfo resource, @Nullable String action );
  }

  public static class AccountNumberPolicyResourceInterceptor implements PolicyResourceInterceptor {
    private static final ThreadLocal<String> accountNumberThreadLocal = new ThreadLocal<>( );

    @Override
    public void onResource( final PolicyResourceInfo resource, final String action ) {
      accountNumberThreadLocal.set( resource == null ? null : resource.getResourceAccountNumber( ) );
    }

    public static Optional<String> getCurrentResourceAccountNumber( ) {
      return Optional.fromNullable( accountNumberThreadLocal.get( ) );
    }
  }

  @SuppressWarnings( "UnusedDeclaration" )
	public static class PolicyResourceInterceptorDiscovery extends ServiceJarDiscovery {
    @SuppressWarnings( "unchecked" )
    @Override
    public boolean processClass( final Class candidate ) throws Exception {
      if ( PolicyResourceContext.PolicyResourceInterceptor.class.isAssignableFrom( candidate ) &&
          !Modifier.isAbstract( candidate.getModifiers() ) ) {
        logger.info( "Registered PolicyResourceInterceptor:    " + candidate );
        resourceInterceptors.add( (PolicyResourceContext.PolicyResourceInterceptor) Classes.newInstance( candidate ) );
        return true;
      }

      return false;
    }

    @Override
    public Double getPriority( ) {
      return 0.3d;
    }
  }
}
