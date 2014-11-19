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
