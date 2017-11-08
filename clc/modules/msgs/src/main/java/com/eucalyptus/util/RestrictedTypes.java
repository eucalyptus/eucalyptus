/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthContext;
import com.eucalyptus.auth.AuthEvaluationContext;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.AuthQuotaException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.PolicyEvaluationContext;
import com.eucalyptus.auth.PolicyEvaluationWriteContextKey;
import com.eucalyptus.auth.PolicyResourceContext;
import com.eucalyptus.auth.policy.annotation.PolicyAction;
import com.eucalyptus.auth.policy.annotation.PolicyResourceType;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.Principal.PrincipalType;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.TypedPrincipal;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.auth.type.LimitedType;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.system.Threads;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import com.eucalyptus.auth.AuthContextSupplier;
import static com.eucalyptus.auth.PolicyResourceContext.PolicyResourceInfo;
import static com.eucalyptus.util.Parameters.checkParam;
import static com.eucalyptus.auth.type.RestrictedType.AccountRestrictedType;
import static com.eucalyptus.auth.type.RestrictedType.PolicyRestrictedType;
import static com.eucalyptus.auth.type.RestrictedType.UserRestrictedType;
import static org.hamcrest.Matchers.notNullValue;

public class RestrictedTypes {
  static Logger LOG = Logger.getLogger( RestrictedTypes.class );

  private static final Interner<AllocationScope> allocationInterner = Interners.newWeakInterner( );

  private static final TypedKey<PrincipalType> principalTypeKey = TypedKey.create( "PrincipalType" );
  private static final TypedKey<String> principalNameKey = TypedKey.create( "PrincipalName" );

  public static final PolicyEvaluationWriteContextKey<PrincipalType> principalTypeContextKey =
      PolicyEvaluationWriteContextKey.create( principalTypeKey );
  public static final PolicyEvaluationWriteContextKey<String> principalNameContextKey =
      PolicyEvaluationWriteContextKey.create( principalNameKey );

  /**
   * Map request to policy language's action string.
   *
   * @param request The request message
   * @return The IAM ARN action string.
   */
  public static String requestToAction( BaseMessage request ) {
    if ( request != null ) {
      PolicyAction action = Ats.from( request ).get( PolicyAction.class );
      if ( action != null ) {
        return action.action( );
      }
    }
    return null;
  }

  /**
   * Annotation for use on a Class implementing Function<String,T extends RestrictedType>,
   * that is, one which converts a string reference into a type reference for the object T
   * referenced by {@code identifier}.
   * <p>
   * {@code public abstract T apply( String identifier );}
   * </p>
   *
   * <p>
   * The method should:
   * <ul>
   *   <li>return T the object referenced by the given {@code identifier}</li>
   *   <li>throw PersistenceException if an error occurred in the underlying retrieval mechanism</li>
   *   <li>throw NoSuchElementException if the requested {@code identifier} does not exist and the
   *       user is authorized.</li>
   * </ul>
   * </p>
   */
  @Target( { ElementType.TYPE } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface Resolver {
    Class<?> value( );
  }

  private static final Map<Class, Function<?, ?>> resourceResolvers = Maps.newHashMap();

  @SuppressWarnings( "unchecked" )
  public static <T extends RestrictedType> Function<String, T> resolver( Class<T> type ) {
    return ( Function<String, T> ) checkMapByType( type, resourceResolvers );
  }

  /**
   * Implementations <strong>measure</strong> the quantity of {@code T}, the <i>resource type</i>,
   * currently ascribed to a user, via {@link com.eucalyptus.auth.principal.OwnerFullName}. In other words, types annotated with
   * this encapsulate a service and resource-specific method for computing the current
   * {@code quantity} of {@code resource type} ascribed to {@code ownerFullName}.
   */
  @Target( { ElementType.TYPE } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface UsageMetricFunction {
    Class<?> value( );
  }

  private static final Map<Class, Function<?, ?>> usageMetricFunctions = Maps.newHashMap( );

  @SuppressWarnings( "unchecked" )
  public static Function<OwnerFullName, Long> usageMetricFunction( Class type ) {
    return ( Function<OwnerFullName, Long> ) checkMapByType( type, usageMetricFunctions );
  }

  @Target( { ElementType.TYPE } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface QuantityMetricFunction {
    Class<?> value( );
  }

  private static final Map<Class, Function<?, ?>> quantityMetricFunctions = Maps.newHashMap( );

  @SuppressWarnings( "unchecked" )
  public static Function<OwnerFullName, Long> quantityMetricFunction( Class type ) {
    return ( Function<OwnerFullName, Long> ) checkMapByType( type, quantityMetricFunctions );
  }

  private static Function<?, ?> checkMapByType( Class type, Map<Class, Function<?, ?>> map ) {
    for ( Class subType : Classes.ancestors( type ) ) {
      if ( map.containsKey( subType ) ) {
        return map.get( subType );
      }
    }
    throw new NoSuchElementException( "Failed to lookup function (@" + Threads.currentStackFrame( 1 ).getMethodName( ) + ") for type: " + type );
  }

  /**
   * Allocate {@code quantity} unitless resources, correctly rolling their allocation back in the
   * case of partial failures.
   */
  private static <T> List<T> runAllocator( int quantity, Supplier<T> allocator, Predicate<T> rollback ) {
    List<T> res = Lists.newArrayList( );
    try {
      for ( int i = 0; i < quantity; i++ ) {
        T rsc = allocator.get( );
        if ( rsc == null ) {
          throw new NoSuchElementException( "Attempt to allocate " + quantity + " resources failed." );
        }
        res.add( rsc );
      }
    } catch ( Exception ex ) {
      for ( T rsc : res ) {
        try {
          rollback.apply( rsc );
        } catch ( Exception ex1 ) {
          LOG.trace( ex1, ex1 );
        }
      }
      if ( ex.getCause( ) != null ) {
        throw Exceptions.toUndeclared( ex.getCause( ) );
      } else {
        throw Exceptions.toUndeclared( ex );
      }
    }
    return res;
  }

  /**
   * Special case of allocating a single countable resource.
   *
   * {@inheritDoc RestrictedTypes#allocateCountableResources(Integer, Supplier)}
   *
   * @see RestrictedTypes#allocateUnitlessResources(Integer, Supplier)
   */
  @SuppressWarnings( { "cast", "unchecked" } )
  public static <T extends LimitedType> T allocateUnitlessResource( Supplier<T> allocator ) throws AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    return allocateUnitlessResources( 1, allocator ).get( 0 );
  }

  /**
   * Allocation of a dimension-less type; i.e. only the quantity matters and the characteristics of
   * the allocated resource cannot be parameterized in any way.
   *
   * @param <T> type to be allocated
   * @param quantity quantity to be allocated
   * @param allocator Supplier which performs allocation of a single unit.
   * @return List<T> of size {@code quantity} of new allocations of {@code <T>}
   */
  @SuppressWarnings( { "cast", "unchecked" } )
  public static <T extends LimitedType> List<T> allocateUnitlessResources( Integer quantity, Supplier<T> allocator ) throws AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    return allocateUnitlessResources( findResourceClass( allocator ), quantity, allocator );
  }

  /**
   * Allocation of a dimension-less type; i.e. only the quantity matters and the characteristics of
   * the allocated resource cannot be parameterized in any way.
   *
   * @param <T> type to be allocated
   * @param rscType class for type to be allocated
   * @param quantity quantity to be allocated
   * @param allocator Supplier which performs allocation of a single unit.
   * @return List<T> of size {@code quantity} of new allocations of {@code <T>}
   */
  @SuppressWarnings( { "cast", "unchecked" } )
  public static <T extends LimitedType> List<T> allocateUnitlessResources(
          final Class<?> rscType,
          final Integer quantity,
          final Supplier<T> allocator
  ) throws AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
      return doAllocate( false, rscType, quantity, null, new Supplier<List<T>>() {
          @Override
          public List<T> get( ) {
              return runAllocator( quantity, allocator, ( Predicate ) Predicates.alwaysTrue( ) );
          }
      } );
  }

  /**
   * Allocation of a dimension-less type; i.e. only the quantity matters and the characteristics of
   * the allocated resource cannot be parameterized in any way.
   *
   * @param <T> type to be allocated
   * @param rscType class for type to be allocated
   * @param min minimum quantity to be allocated
   * @param max maximum quantity to be allocated
   * @param allocator Supplier which performs allocation of a single unit.
   * @param example Example resource
   * @return List<T> of size {@code quantity} of new allocations of {@code <T>}
   */
  @SuppressWarnings( { "cast", "unchecked" } )
  public static <T extends LimitedType> List<T> allocateUnitlessResources(
      final Class<?> rscType,
      final int min,
      final int max,
      final BatchAllocator<T> allocator,
      final RestrictedType example
  ) throws AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    return doAllocate( false, rscType, max, example, new Supplier<List<T>>( ){
      @Override
      public List<T> get() {
        return allocator.allocate( min, max );
      }
    });
  }

  /**
   * Reallocation of a dimension-less type; i.e. only the quantity matters and the characteristics of
   * the allocated resource cannot be parameterized in any way.
   *
   * Assumes permission check is elsewhere, only handles quotas
   *
   * @param <T> type to be allocated
   * @param rscType class for type to be allocated
   * @param allocator Supplier which performs allocation of a single unit.
   * @return List<T> of size {@code quantity} of new allocations of {@code <T>}
   */
  @SuppressWarnings( { "cast", "unchecked" } )
  public static <T extends LimitedType> List<T> reallocateUnitlessResource(
      final Class<?> rscType,
      final BatchAllocator<T> allocator
  ) throws AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    return doAllocate( true, rscType, 1, null, new Supplier<List<T>>( ){
      @Override
      public List<T> get() {
        return allocator.allocate( 1, 1 );
      }
    });
  }

  private static <A> A doAllocate( final boolean skipAuth, final Class<?> rscType, final Integer quantity, final Object example, final Supplier<A> allocator ) throws AuthException, IllegalContextAccessException {
    String identifier = "";
    Context ctx = Contexts.lookup( );
    if ( !ctx.hasAdministrativePrivileges( ) ) {
      Ats ats = findPolicyAnnotations( rscType );
      PolicyVendor vendor = ats.get( PolicyVendor.class );
      PolicyResourceType type = ats.get( PolicyResourceType.class );
      String action = getIamActionByMessageType();
      String qualifiedAction = PolicySpec.qualifiedName( vendor.value( ), action );
      AuthContextSupplier userContext = ctx.getAuthContext( );
      try ( final PolicyResourceContext context = example == null ?
          PolicyResourceContext.of( ctx.getAccountNumber( ), rscType, qualifiedAction ) :
          PolicyResourceContext.of( example, qualifiedAction )
      ) {
        if ( !skipAuth && !Permissions.isAuthorized( vendor.value( ), type.value( ), identifier, null, action, userContext )){
          throw new AuthException( "Not authorized to create: " + type.value( ) + " by user: " + ctx.getUserFullName( ) );
        }
        final Lock lock = allocationInterner.intern( new AllocationScope( vendor.value( ), type.value( ), userContext.get( ).getAccountNumber( ) ) ).lock( );
        lock.lock( );
        try {
          if ( !Permissions.canAllocate( vendor.value( ), type.value( ), identifier, action, userContext, (long) quantity ) ) {
            throw new AuthQuotaException( type.value( ), "Quota exceeded while trying to create: " + type.value( ) + " by user: " + ctx.getUserFullName( ) );
          }
          return allocator.get( );
        } finally {
          lock.unlock( );
        }
      }
    } else {
      return allocator.get( );
    }
  }
  /**
   * Allocate a resource and subsequently verify naming restrictions.
   *
   * @see RestrictedTypes#allocateUnitlessResources(Integer, Supplier)
   */
  @SuppressWarnings( "ConstantConditions" )
  public static <T extends RestrictedType> List<T> allocateNamedUnitlessResources( Integer quantity, Supplier<T> allocator, Predicate<T> rollback ) throws AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    Context ctx = Contexts.lookup( );
    Class<?> rscType = findResourceClass( allocator );
    if ( ctx.hasAdministrativePrivileges( ) ) {
      return runAllocator( quantity, allocator, rollback ); // may throw RuntimeException
    } else {
      Ats ats = findPolicyAnnotations( rscType );
      PolicyVendor vendor = ats.get( PolicyVendor.class );
      PolicyResourceType type = ats.get( PolicyResourceType.class );
      String action = getIamActionByMessageType();
      AuthContextSupplier userContext = ctx.getAuthContext( );
      List<T> res = Lists.newArrayList( );
      for ( int i = 0; i < quantity; i++ ) {
        T rsc = null;
        try {
          rsc = allocator.get( );
        } catch ( RuntimeException ex1 ) {
          if ( rsc != null ) {
            rollback.apply( rsc );
          }
          throw ex1;
        }
        if ( rsc == null ) {
          throw new NoSuchElementException( "Attempt to allocate " + quantity + " " + type + " failed." );
        }
        try ( final PolicyResourceContext context = PolicyResourceContext.of( ctx.getAccountNumber( ), rscType, PolicySpec.qualifiedName( vendor.value( ), action ) )  ) {
          String identifier = rsc.getDisplayName( );
          if ( !Permissions.isAuthorized( vendor.value( ), type.value( ), identifier, null, action, userContext ) ) {
            throw new AuthException( "Not authorized to create: " + type.value() + " by user: " + ctx.getUserFullName( ) );
          } else if ( !Permissions.canAllocate( vendor.value( ), type.value( ), identifier, action, userContext, ( long ) quantity ) ) {
            throw new AuthQuotaException( type.value( ), "Quota exceeded while trying to create: " + type.value() + " by user: " + ctx.getUserFullName( ) );
          }
        } catch ( AuthException ex ) {
          if ( rsc != null ) {
            rollback.apply( rsc );
          }
          throw ex;
        }
        res.add( rsc );
      }
      return res;
    }
  }

  /**
   * Allocation of a type which requires dimensional parameters (e.g., size of a volume) where
   * {@code amount} indicates the desired value for the measured dimensional parameter.
   *
   * @param <T> type to be allocated
   * @param amount amount to be allocated
   * @param allocator Supplier which performs allocation of a single unit.
   * @return List<T> of size {@code quantity} of new allocations of {@code <T>}
   */
  public static <T extends LimitedType> T allocateMeasurableResource(
      final Long amount,
      final Function<Long, T> allocator
  ) throws AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    return allocateMeasurableResource( amount, allocator, null );
  }

  /**
   * Allocation of a type which requires dimensional parameters (e.g., size of a volume) where
   * {@code amount} indicates the desired value for the measured dimensional parameter.
   *
   * @param <T> type to be allocated
   * @param amount amount to be allocated
   * @param allocator Supplier which performs allocation of a single unit.
   * @param example Example resource
   * @return List<T> of size {@code quantity} of new allocations of {@code <T>}
   */
  public static <T extends LimitedType> T allocateMeasurableResource(
      final Long amount,
      final Function<Long, T> allocator,
      final T example
  ) throws AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    Context ctx = Contexts.lookup( );
    if ( !ctx.hasAdministrativePrivileges( ) ) {
      String action = getIamActionByMessageType( ctx.getRequest( ) );
      return allocateMeasurableResource( ctx.getAuthContext( ), ctx.getUserFullName( ), action, amount, allocator, example );
    } else {
      return allocator.apply( amount );
    }
  }

  /**
   * Allocation of a type which requires dimensional parameters (e.g., size of a volume) where
   * {@code amount} indicates the desired value for the measured dimensional parameter.
   *
   * @param <T> type to be allocated
   * @param userContext The authorization context to use,
   * @param userDescription Description of the principal related to the allocation
   * @param action The unqualified API action related to the allocation
   * @param amount amount to be allocated
   * @param allocator Supplier which performs allocation of a single unit.
   * @param example Example resource
   * @return List<T> of size {@code quantity} of new allocations of {@code <T>}
   */
  public static <T extends LimitedType> T allocateMeasurableResource(
      final AuthContextSupplier userContext,
      final UserFullName userDescription,
      final String action,
      final Long amount,
      final Function<Long, T> allocator,
      final T example
  ) throws AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    String identifier = "";
    final AuthContext authContext = userContext.get( );
    if ( !authContext.isSystemAdmin( ) ) {
      final Class<?> rscType = findResourceClass( allocator );
      Ats ats = findPolicyAnnotations( rscType );
      PolicyVendor vendor = ats.get( PolicyVendor.class );
      PolicyResourceType type = ats.get( PolicyResourceType.class );
      final String qualifiedAction = PolicySpec.qualifiedName( vendor.value( ), action );
      try ( final PolicyResourceContext context = example == null ?
          PolicyResourceContext.of( authContext.getAccountNumber( ), rscType, qualifiedAction ) :
          PolicyResourceContext.of( example, qualifiedAction )
      ) {
        if ( RestrictedType.class.isAssignableFrom( rscType ) && !Permissions.isAuthorized( vendor.value( ), type.value( ), identifier, null, action, userContext ) ) {
          throw new AuthException( "Not authorized to create: " + type.value( ) + " by user: " + userDescription );
        } else if ( !Permissions.canAllocate( vendor.value( ), type.value( ), identifier, action, userContext, amount ) ) {
          throw new AuthQuotaException( type.value( ), "Quota exceeded while trying to create: " + type.value( ) + " by user: " + userDescription );
        }
      }
    }
    return allocator.apply( amount );
  }

  @SuppressWarnings( { "cast", "unchecked" } )
  public static <T extends RestrictedType> T doPrivileged( String identifier, Class<T> type ) throws AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    return doPrivileged( identifier, ( Function<String, T> ) checkMapByType( type, resourceResolvers ) );
  }

  @SuppressWarnings( "rawtypes" )
  public static <T extends RestrictedType> T doPrivileged( String identifier, Function<String, T> resolverFunction ) throws AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    return doPrivileged( identifier, resolverFunction, false );
  }

  /**
   * Check access permission without regard for resource ownership.
   *
   * This check should only be used for resources that are public or that have
   * an additional permission check applied (for example EC2 images can be
   * shared between accounts)
   *
   * @see #doPrivileged(String, Class)
   */
  @SuppressWarnings( "rawtypes" )
  public static <T extends RestrictedType> T doPrivilegedWithoutOwner( String identifier, Function<? super String, T> resolverFunction ) throws AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    return doPrivileged( identifier, resolverFunction, true );
  }

  /**
   * Uses the provided {@code lookupFunction} to resolve the {@code identifier} to the underlying
   * object {@code T} with privileges determined by the current messaging context.
   *
   * @param <T> type of object which needs looking up
   * @param identifier identifier of the desired object
   * @param resolverFunction class which resolves string identifiers to the underlying object
   * @return the object corresponding with the given {@code identifier}
   * @throws AuthException if the user is not authorized
   * @throws PersistenceException if an error occurred in the underlying retrieval mechanism
   * @throws NoSuchElementException if the requested {@code identifier} does not exist and the user
   *           is authorized.
   * @throws IllegalContextAccessException if the current request context cannot be determined.
   */
  @SuppressWarnings( "rawtypes" )
  private static <T extends RestrictedType> T doPrivileged( final String identifier,
                                                            final Function<? super String, T> resolverFunction,
                                                            final boolean ignoreOwningAccount ) throws AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    checkParam( "Resolver function must be not null: " + identifier, resolverFunction, notNullValue() );
    Context ctx = Contexts.lookup( );
    if ( ctx.hasAdministrativePrivileges( ) ) {
      return resolverFunction.apply( identifier );
    } else {
      Class<? extends BaseMessage> msgType = ctx.getRequest( ).getClass( );
      LOG.debug( "Attempting to lookup " + identifier + " using lookup: " + resolverFunction.getClass( ) + " typed as "
                 + Classes.genericsToClasses( resolverFunction ) );
      Class<?> rscType = findResourceClass( resolverFunction );
      Ats ats = findPolicyAnnotations( rscType );
      PolicyVendor vendor = ats.get( PolicyVendor.class );
      PolicyResourceType type = ats.get( PolicyResourceType.class );
      String action = getIamActionByMessageType( );
      String actionVendor = findPolicyVendor( msgType );
      AuthContextSupplier authContextSupplier = ctx.getAuthContext( );
      UserPrincipal requestUser = ctx.getUser( );
      Map<String,String> evaluatedKeys = ctx.evaluateKeys( );
      T requestedObject;
      try {
        requestedObject = resolverFunction.apply( identifier );
        if ( requestedObject == null ) {
          throw new NoSuchElementException( "Failed to lookup requested " + rscType.getCanonicalName( ) + " with id " + identifier + " using "
                                            + resolverFunction.getClass( ) );
        }
      } catch ( NoSuchElementException ex ) {
        throw ex;
      } catch ( PersistenceException ex ) {
        Logs.extreme( ).error( ex, ex );
        LOG.error( ex );
        throw ex;
      } catch ( Exception ex ) {
        Logs.extreme( ).error( ex, ex );
        LOG.error( ex );
        throw new PersistenceException( "Error occurred while attempting to lookup " + identifier + " using lookup: " + resolverFunction.getClass( )
                                        + " typed as "
                                        + rscType, ex );
      }

      final PolicyEvaluationContext policyEvaluationContext = PolicyEvaluationContext.get( );
      final Set<TypedPrincipal> principals;
      if ( policyEvaluationContext.hasAttribute( principalTypeKey ) &&
          policyEvaluationContext.hasAttribute( principalNameKey ) ) {
        final PrincipalType principalType = policyEvaluationContext.getAttribute( principalTypeKey );
        final String principalName = policyEvaluationContext.getAttribute( principalNameKey );
        principals = ImmutableSet.of( TypedPrincipal.of( principalType, principalName ) );
      } else {
        principals = Principals.typedSet( requestUser );
      }

      final AccountFullName objectOwnerAccount =
          AccountFullName.getInstance( requestedObject.getOwner( ).getAccountNumber( ) );
      AccountFullName owningAccount = null;
      if ( !ignoreOwningAccount ) {
        owningAccount = Principals.nobodyFullName( ).getAccountNumber( ).equals( requestedObject.getOwner( ).getAccountNumber( ) )
          ? null
          : objectOwnerAccount;
      }

      final String qualifiedAction = PolicySpec.qualifiedName( actionVendor, action );
      //noinspection unused
      try ( final PolicyResourceContext policyResourceContext = PolicyResourceContext.of( requestedObject, qualifiedAction ) ) {
        if ( !Permissions.isAuthorized( principals, findPolicy( requestedObject, actionVendor, action ), objectOwnerAccount,
                                        PolicySpec.qualifiedName( vendor.value( ), type.value( ) ), identifier, owningAccount,
                                        qualifiedAction, requestUser, authContextSupplier.get( ).getPolicies( ), evaluatedKeys ) ) {
          throw new AuthException( "Not authorized to use " + type.value( ) + " identified by " + identifier + " as the user "
                                   + UserFullName.getInstance( requestUser ) );
        }
      }
      return requestedObject;
    }
  }

  public static <T extends RestrictedType> CompatPredicate<T> filterPrivileged( ) {
    return filterPrivileged( false, ContextSupplier.INSTANCE );
  }

  /**
   * Check access permission without regard for resource ownership.
   *
   * This check should only be used for resources that are public or that have
   * an additional permission check applied (for example EC2 images can be
   * shared between accounts)
   *
   * @see #filterPrivileged
   */
  public static <T extends RestrictedType> CompatPredicate<T> filterPrivilegedWithoutOwner( ) {
    return filterPrivileged( true, ContextSupplier.INSTANCE );
  }

  public static <T extends RestrictedType> CompatFunction<T, String> toDisplayName( ) {
    return new CompatFunction<T, String>( ) {
      @Override
      public String apply( T arg0 ) {
        return arg0 == null ? null : arg0.getDisplayName( );
      }
    };
  }

  public static <T extends RestrictedType> CompatPredicate<T> filterById( final Collection<String> requestedIdentifiers ) {
    return filterByProperty( requestedIdentifiers, toDisplayName() );
  }

  public static <T extends RestrictedType> CompatPredicate<T> filterByProperty( final String requestedValue,
                                                                          final Function<? super T,String> extractor ) {
    return filterByProperty( CollectionUtils.<String>listUnit().apply( requestedValue ), extractor );
  }

  public static <T extends RestrictedType> CompatPredicate<T> filterByProperty( final Collection<String> values,
                                                                          final Function<? super T,String> extractor ) {
    return new CompatPredicate<T>( ) {
      final ImmutableList<String> requestedValues = values == null ? null : ImmutableList.copyOf( values );

      @Override
      public boolean apply( T input ) {
        return requestedValues == null || requestedValues.isEmpty( ) || requestedValues.contains( extractor.apply( input ) );
      }
    };
  }

  public static <T extends RestrictedType> CompatPredicate<T> filterByOwningAccount( final Collection<String> identifiers ) {
    return new CompatPredicate<T>( ) {
      final ImmutableList<String> requestedIdentifiers = identifiers == null ? null : ImmutableList.copyOf( identifiers );

      @Override
      public boolean apply( T input ) {
        return requestedIdentifiers == null || requestedIdentifiers.isEmpty( ) || requestedIdentifiers.contains( input.getOwner( ).getAccountNumber( ) );
      }
    };
  }

  public static <T extends RestrictedType> FilterBuilder<T> filteringFor( final Class<T> metadataClass ) {
    return new FilterBuilder<T>(metadataClass  );
  }

  /*
   * Please, ignoreOwningAccount here is necessary. Consult me first before making any changes.
   *  -- Ye Wen (wenye@eucalyptus.com)
   */
  private static <T extends RestrictedType> CompatPredicate<T> filterPrivileged( final boolean ignoreOwningAccount, final Function<? super Class<?>, AuthEvaluationContext> contextFunction ) {
    return new CompatPredicate<T>( ) {

      @SuppressWarnings( { "ConstantConditions", "unused" } )
      @Override
      public boolean apply( T arg0 ) {
        Context ctx = Contexts.lookup( );
        if ( !ctx.hasAdministrativePrivileges( ) ) {
          try {
            String owningAccountNumber = null;
            if ( !ignoreOwningAccount ) {
              owningAccountNumber = Principals.nobodyFullName( ).getAccountNumber( ).equals( arg0.getOwner( ).getAccountNumber( ) )
                ? null
                : arg0.getOwner( ).getAccountNumber( );
            }
            final AuthEvaluationContext evaluationContext = contextFunction.apply( arg0.getClass( ) );
            try  ( final PolicyResourceContext policyResourceContext =
                       PolicyResourceContext.of( arg0, evaluationContext.getAction( ) ) ) {
              return Permissions.isAuthorized( evaluationContext, owningAccountNumber, arg0.getDisplayName() );
            }
          } catch ( Exception ex ) {
            return false;
          }
        }
        return true;
      }

    };
  }

  private enum ContextSupplier implements Function<Class<?>, AuthEvaluationContext> {
    INSTANCE;

    @Override
    public AuthEvaluationContext apply( final Class<?> rscType ) {
      try {
        final Context ctx = Contexts.lookup();
        final Ats ats = findPolicyAnnotations( rscType );
        final PolicyVendor vendor = ats.get( PolicyVendor.class );
        final PolicyResourceType type = ats.get( PolicyResourceType.class );
        final String action = getIamActionByMessageType( );
        return ctx.getAuthContext( ).get( ).evaluationContext( vendor.value( ), type.value( ), action );
      } catch ( AuthException e ) {
        throw Exceptions.toUndeclared( e );
      }
    }
  }

  /**
   * Filter by account and possibly user.
   *
   * <p>If the given owner is null the returned predicate will always match.</p>
   */
  @Nonnull
  public static Predicate<AccountRestrictedType> filterByOwner( @Nullable final OwnerFullName owner ) {
    return owner == null ?
        Predicates.<AccountRestrictedType>alwaysTrue() :
        Predicates.<AccountRestrictedType>and(
            filterByAccount( owner.getAccountNumber() ),
            typeSafeFilterByUser( owner.getUserId() )
            );
  }

  @Nonnull
  public static Predicate<AccountRestrictedType> filterByAccount( @Nonnull final String accountNumber ) {
    return new Predicate<AccountRestrictedType>() {
      @Override
      public boolean apply( @Nullable final AccountRestrictedType restricted ) {
        return restricted == null || accountNumber.equals( restricted.getOwnerAccountNumber() );
      }
    };
  }

  @Nonnull
  public static Predicate<UserRestrictedType> filterByUser( @Nonnull final String userId ) {
    return new Predicate<UserRestrictedType>() {
      @Override
      public boolean apply( @Nullable final UserRestrictedType restricted ) {
        return restricted == null || userId.equals( restricted.getOwnerUserId() );
      }
    };
  }

  @Nonnull
  private static Predicate<AccountRestrictedType> typeSafeFilterByUser( @Nullable final String userId ) {
    final Predicate<UserRestrictedType> userFilter = userId == null ?
        Predicates.<UserRestrictedType>alwaysTrue() :
        filterByUser( userId );
    return new Predicate<AccountRestrictedType>() {
      @Override
      public boolean apply( @Nullable final AccountRestrictedType restricted ) {
        return !(restricted instanceof UserRestrictedType) ||
            userFilter.apply( (UserRestrictedType) restricted );
      }
    };
  }

  @TypeMapper
  public enum RestrictedTypeToPolicyResourceInfo implements Function<RestrictedType,PolicyResourceInfo> {
    INSTANCE;

    @Nullable
    @Override
    public PolicyResourceInfo apply( final RestrictedType restrictedType ) {
      final String accountNumber;
      if ( restrictedType instanceof UserRestrictedType ) {
        accountNumber = ( (UserRestrictedType) restrictedType ).getOwnerAccountNumber( );
      } else if ( restrictedType instanceof AccountRestrictedType ) {
        accountNumber = ( (AccountRestrictedType) restrictedType ).getOwnerAccountNumber( );
      } else {
        accountNumber = restrictedType.getOwner( ).getAccountNumber( );
      }
      return PolicyResourceContext.resourceInfo( accountNumber, restrictedType );
    }
  }

  public static class FilterBuilder<T extends RestrictedType> {
    private final Class<T> metadataClass;
    private final List<Predicate<? super T>> predicates = Lists.newArrayList();

    private FilterBuilder( final Class<T> metadataClass ) {
      this.metadataClass = metadataClass;
    }

    public FilterBuilder<T> byId( final Collection<String> requestedIdentifiers ) {
      predicates.add( filterById( requestedIdentifiers ) );
      return this;
    }

    public <T extends RestrictedType> Predicate<T> filterByProperty( final Collection<String> requestedValues,
                                                                     final Function<? super T,String> extractor ) {
      return new Predicate<T>( ) {
        @Override
        public boolean apply( T input ) {
          return requestedValues == null || requestedValues.isEmpty() || requestedValues.contains( extractor.apply(input) );
        }
      };
    }

    public FilterBuilder<T> byProperty(final Collection<String> requestedValues, final Function<? super T, String> extractor) {
      predicates.add(filterByProperty(requestedValues, extractor));
      return this;

    }
    public FilterBuilder<T> byPrivileges() {
      predicates.add( RestrictedTypes.filterPrivileged( false, Functions.constant( ContextSupplier.INSTANCE.apply( metadataClass ) ) ) );
      return this;
    }

    public FilterBuilder<T> byPrivilegesWithoutOwner() {
      predicates.add( RestrictedTypes.filterPrivileged( true, Functions.constant( ContextSupplier.INSTANCE.apply( metadataClass ) ) ) );
      return this;
    }

    public FilterBuilder<T> byOwningAccount( final Collection<String> requestedIdentifiers ) {
      predicates.add( filterByOwningAccount( requestedIdentifiers ) );
      return this;
    }

    public FilterBuilder<T> byPredicate( final Predicate<? super T> predicate ) {
      predicates.add( predicate );
      return this;
    }

    public Predicate<? super T> buildPredicate() {
      return Predicates.and( predicates );
    }
  }

  public interface BatchAllocator<T> {
    List<T> allocate( int min, int max );
  }

  public static class ResourceMetricFunctionDiscovery extends ServiceJarDiscovery {

    public ResourceMetricFunctionDiscovery( ) {
      super( );
    }

    @SuppressWarnings( { "synthetic-access", "unchecked" } )
    @Override
    public boolean processClass( Class candidate ) throws Exception {
      if ( Ats.from( candidate ).has( UsageMetricFunction.class ) && Function.class.isAssignableFrom( candidate ) ) {
        UsageMetricFunction measures = Ats.from( candidate ).get( UsageMetricFunction.class );
        Class<?> measuredType = measures.value( );
        LOG.info( "Registered @UsageMetricFunction:    " + measuredType.getSimpleName( ) + " => " + candidate );
        RestrictedTypes.usageMetricFunctions.put( measuredType, ( Function<OwnerFullName, Long> ) Classes.newInstance( candidate ) );
        return true;
      } else if ( Ats.from( candidate ).has( QuantityMetricFunction.class ) && Function.class.isAssignableFrom( candidate ) ) {
        QuantityMetricFunction measures = Ats.from( candidate ).get( QuantityMetricFunction.class );
        Class<?> measuredType = measures.value( );
        LOG.info( "Registered @QuantityMetricFunction: " + measuredType.getSimpleName( ) + " => " + candidate );
        RestrictedTypes.quantityMetricFunctions.put( measuredType, ( Function<OwnerFullName, Long> ) Classes.newInstance( candidate ) );
        return true;
      } else if ( Ats.from( candidate ).has( Resolver.class ) && Function.class.isAssignableFrom( candidate ) ) {
        Resolver resolver = Ats.from( candidate ).get( Resolver.class );
        Class<?> resolverFunctionType = resolver.value( );
        LOG.info( "Registered @Resolver:               " + resolverFunctionType.getSimpleName( ) + " => " + candidate );
        RestrictedTypes.resourceResolvers.put( resolverFunctionType, ( Function<String, RestrictedType> ) Classes.newInstance( candidate ) );
        return true;
      } else {
        return false;
      }
    }

    @Override
    public Double getPriority( ) {
      return 0.3d;
    }

  }

  public static String getIamActionByMessageType( ) {
    return getIamActionByMessageType( Contexts.lookup( ).getRequest( ) );
  }

  public static String getIamActionByMessageType( final BaseMessage request ) {
    String action = requestToAction( request );
    if ( action == null ) {
      if ( request != null ) {
        return request.getClass( ).getSimpleName( ).replaceAll( "(ResponseType|Type)$", "" ).toLowerCase( );
      } else {
        return null;
      }
    } else {
      return action;
    }
  }

  private static Class<?> findResourceClass( Object allocator ) throws IllegalArgumentException, NoSuchElementException {
    List<Class> lookupTypes = Classes.genericsToClasses( allocator );
    if ( lookupTypes.isEmpty( ) ) {
      throw new IllegalArgumentException( "Failed to find required generic type for lookup " + allocator.getClass( )
                                          + " so the policy type for looking up " + allocator + " cannot be determined." );
    }
    Class<?> rscType;
    try {
      rscType = Iterables.find( lookupTypes, new Predicate<Class>( ) {

        @Override
        public boolean apply( Class arg0 ) {
          return LimitedType.class.isAssignableFrom( arg0 );
        }
      } );
    } catch ( NoSuchElementException ex1 ) {
      LOG.error( ex1, ex1 );
      throw ex1;
    }
    return rscType;
  }

  private static Ats findPolicyAnnotations( Class<?> rscType ) throws IllegalArgumentException {
    Ats ats = Ats.inClassHierarchy( rscType );
    if ( !ats.has( PolicyVendor.class ) ) {
      throw new IllegalArgumentException( "Failed to determine policy for allocating type instance " + rscType.getCanonicalName( )
                                          + ": required @PolicyVendor missing in resource type hierarchy" );
    } else if ( !ats.has( PolicyResourceType.class ) ) {
      throw new IllegalArgumentException( "Failed to determine policy for looking up type instance " + rscType.getCanonicalName( )
                                          + ": required @PolicyResourceType missing in resource type hierarchy" );
    }
    return ats;
  }

  public static String findPolicyVendor( Class<? extends BaseMessage > msgType ) throws IllegalArgumentException {
    final Ats ats = Ats.inClassHierarchy( msgType );

    if ( ats.has( PolicyVendor.class ) ) {
      return ats.get( PolicyVendor.class ).value();
    }

    if ( ats.has( PolicyAction.class ) ) {
      return ats.get( PolicyAction.class ).vendor();
    }

    if ( ats.has( ComponentMessage.class ) ) {
      final Class<? extends ComponentId> componentIdClass =
          ats.get( ComponentMessage.class ).value();
      final Ats componentAts = Ats.inClassHierarchy( componentIdClass );
      if ( componentAts.has( PolicyVendor.class ) ) {
        return componentAts.get( PolicyVendor.class ).value();
      }
    }

    throw new IllegalArgumentException( "Failed to determine policy"
        + ": require @PolicyVendor, @PolicyAction or @ComponentMessage in request type hierarchy "
        + msgType.getCanonicalName( ) );
  }

  private static PolicyVersion findPolicy( final RestrictedType object,
                                    final String vendor,
                                    final String action ) throws AuthException {
    PolicyVersion policy = null;
    if ( object instanceof PolicyRestrictedType ) {
      final Ats ats = Ats.inClassHierarchy( object.getClass() );
      final PolicyResourceType policyResourceType = ats.get( PolicyResourceType.class );
      if ( policyResourceType == null || Lists.newArrayList( policyResourceType.resourcePolicyActions() ).contains( PolicySpec.qualifiedName( vendor, action ).toLowerCase() ) ) {
        try {
          policy = ((PolicyRestrictedType) object).getPolicy();
          if ( policy == null ) {
            throw new AuthException( "Policy not found for resource" );
          }
        } catch ( Exception e ) {
          throw new AuthException( "Error finding policy", e );
        }
      }
    }
    return policy;
  }

  private static final class AllocationScope {
    private final String resourceVendor;
    private final String resourceType;
    private final String accountNumber;
    private final Lock lock = new ReentrantLock( );

    private AllocationScope( final String resourceVendor,
                             final String resourceType,
                             final String accountNumber ) {
      Parameters.checkParam( "resourceVendor", resourceVendor, notNullValue( ) );
      Parameters.checkParam( "resourceType", resourceType, notNullValue( ) );
      Parameters.checkParam( "accountNumber", accountNumber, notNullValue( ) );
      this.resourceVendor = resourceVendor;
      this.resourceType = resourceType;
      this.accountNumber = accountNumber;
    }

    public Lock lock( ) {
      return lock;
    }

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass() != o.getClass() ) return false;

      final AllocationScope that = (AllocationScope) o;

      if ( !accountNumber.equals( that.accountNumber ) ) return false;
      if ( !resourceType.equals( that.resourceType ) ) return false;
      if ( !resourceVendor.equals( that.resourceVendor ) ) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = resourceVendor.hashCode();
      result = 31 * result + resourceType.hashCode();
      result = 31 * result + accountNumber.hashCode();
      return result;
    }

    public String getResourceVendor( ) {
      return resourceVendor;
    }

    public String getResourceType( ) {
      return resourceType;
    }

    public String getAccountNumber( ) {
      return accountNumber;
    }
  }
}
