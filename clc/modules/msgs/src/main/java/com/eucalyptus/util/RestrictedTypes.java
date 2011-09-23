/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicyAction;
import com.eucalyptus.auth.policy.PolicyResourceType;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.PolicyVendor;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.system.Threads;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class RestrictedTypes {
  static Logger LOG = Logger.getLogger( RestrictedTypes.class );
  
  /**
   * Class implementing {@link Function<String,T extends RestrictedResource>}, that is, one which
   * converts a string reference into a type reference for the object {@code T} referenced by
   * {@code identifier}.
   * 
   * {@code public abstract T apply( String identifier );}
   * 
   * @param identifier
   * @return T the object referenced by the given {@code identifier}
   * @throws PersistenceException if an error occurred in the underlying retrieval mechanism
   * @throws NoSuchElementException if the requested {@code identifier} does not exist and the user
   *           is authorized.
   */
  @Target( { ElementType.TYPE } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface Resolver {
    Class<?> value( );
  }
  
  private static final Map<Class, Function<?, ?>> resourceResolvers = Maps.newHashMap( );
  
  public static final <T extends RestrictedType<T>> Function<String, T> resolver( Class<?> type ) {
    return ( Function<String, T> ) checkMapByType( type, resourceResolvers );
  }
  
  /**
   * Implementations <strong>measure</strong> the quantity of {@code T}, the <i>resource type</i>,
   * currently ascribed to a user, via {@link OwnerFullName}. In other words, types annotated with
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
  
  @SuppressWarnings( { "cast", "unchecked" } )
  public static <T extends RestrictedType> T doPrivileged( String identifier, Class<T> type ) throws AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    return ( T ) doPrivileged( identifier, ( Function<String, T> ) checkMapByType( type, resourceResolvers ) );
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
  public static <T extends RestrictedType> T doPrivileged( String identifier, Function<String, T> resolverFunction ) throws AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    assertThat( "Resolver function must be not null: " + identifier, resolverFunction, notNullValue( ) );
    Context ctx = Contexts.lookup( );
    if ( ctx.hasAdministrativePrivileges( ) ) {
      return resolverFunction.apply( identifier );
    } else {
      Class<? extends BaseMessage> msgType = ctx.getRequest( ).getClass( );
      LOG.debug( "Attempting to lookup " + identifier + " using lookup: " + resolverFunction.getClass( ) + " typed as "
                 + Classes.genericsToClasses( resolverFunction ) );
      List<Class<?>> lookupTypes = Classes.genericsToClasses( resolverFunction );
      if ( lookupTypes.isEmpty( ) ) {
        throw new IllegalArgumentException( "Failed to find required generic type for lookup " + resolverFunction.getClass( )
                                            + " so the policy type for looking up " + identifier + " cannot be determined." );
      } else {
        Class<?> rscType;
        try {
          rscType = Iterables.find( lookupTypes, new Predicate<Class<?>>( ) {
            
            @Override
            public boolean apply( Class<?> arg0 ) {
              return RestrictedType.class.isAssignableFrom( arg0 );
            }
          } );
        } catch ( NoSuchElementException ex1 ) {
          LOG.error( ex1, ex1 );
          throw ex1;
        }
        Ats ats = Ats.inClassHierarchy( rscType );
        Ats msgAts = Ats.inClassHierarchy( msgType );
        if ( !ats.has( PolicyVendor.class ) && !msgAts.has( PolicyVendor.class ) ) {
          throw new IllegalArgumentException( "Failed to determine policy for looking up identifier " + identifier
                                              + ": required @PolicyVendor missing in resource type hierarchy " + rscType.getCanonicalName( )
                                              + " and request type hierarchy " + msgType.getCanonicalName( ) );
        } else if ( !ats.has( PolicyResourceType.class ) && !msgAts.has( PolicyResourceType.class ) ) {
          throw new IllegalArgumentException( "Failed to determine policy for looking up identifier " + identifier
                                              + ": required @PolicyResourceType missing in resource type hierarchy " + rscType.getCanonicalName( )
                                              + " and request type hierarchy " + msgType.getCanonicalName( ) );
        } else {
          PolicyVendor vendor = ats.get( PolicyVendor.class );
          PolicyResourceType type = ats.get( PolicyResourceType.class );
          String action = PolicySpec.requestToAction( ctx.getRequest( ) );
          if ( action == null ) {
            action = vendor.value( ) + ":" + ctx.getRequest( ).getClass( ).getSimpleName( ).replaceAll( "(ResponseType|Type)$", "" ).toLowerCase( );
          }
          
          User requestUser = ctx.getUser( );
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
          
          Account owningAccount = Accounts.lookupUserById( requestedObject.getOwner( ).getUniqueId( ) ).getAccount( );
          if ( !Permissions.isAuthorized( vendor.value( ), type.value( ), identifier, owningAccount, action, requestUser ) ) {
            throw new AuthException( "Not authorized to use " + type.value( ) + " identified by " + identifier + " as the user " + requestUser.getName( ) );
          }
          return requestedObject;
        }
      }
    }
  }
  
  public static <T extends RestrictedType> Predicate<T> filterPrivileged( ) {
    return new Predicate<T>( ) {
      
      @Override
      public boolean apply( T arg0 ) {
        Context ctx = Contexts.lookup( );
        if ( ctx.hasAdministrativePrivileges( ) ) {
          return true;
        } else {
          Class<? extends BaseMessage> msgType = ctx.getRequest( ).getClass( );
          Class<?> rscType = arg0.getClass( );
          Ats ats = Ats.inClassHierarchy( rscType );
          Ats msgAts = Ats.inClassHierarchy( msgType );
          if ( !ats.has( PolicyVendor.class ) && !msgAts.has( PolicyVendor.class ) ) {
            throw new IllegalArgumentException( "Failed to determine policy for looking up type instance " + arg0.toString( )
                                                + ": required @PolicyVendor missing in resource type hierarchy " + rscType.getCanonicalName( )
                                                + " and request type hierarchy " + msgType.getCanonicalName( ) );
          } else if ( !ats.has( PolicyResourceType.class ) && !msgAts.has( PolicyResourceType.class ) ) {
            throw new IllegalArgumentException( "Failed to determine policy for looking up type instance " + arg0.toString( )
                                                + ": required @PolicyResourceType missing in resource type hierarchy " + rscType.getCanonicalName( )
                                                + " and request type hierarchy " + msgType.getCanonicalName( ) );
          } else {
            PolicyVendor vendor = ats.get( PolicyVendor.class );
            PolicyResourceType type = ats.get( PolicyResourceType.class );
            String action = PolicySpec.requestToAction( ctx.getRequest( ) );
            if ( action == null ) {
              action = vendor.value( ) + ":" + ctx.getRequest( ).getClass( ).getSimpleName( ).replaceAll( "(ResponseType|Type)$", "" ).toLowerCase( );
            }
            User requestUser = ctx.getUser( );
            try {
              Account owningAccount = Accounts.lookupAccountByName( arg0.getOwner( ).getAccountName( ) );
              return Permissions.isAuthorized( vendor.value( ), type.value( ), arg0.getDisplayName( ), owningAccount, action, requestUser );
            } catch ( AuthException ex ) {
              return false;
            }
          }
        }
      }
      
    };
  }
  
  public static class ResourceMetricFunctionDiscovery extends ServiceJarDiscovery {
    
    public ResourceMetricFunctionDiscovery( ) {
      super( );
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    public boolean processClass( Class candidate ) throws Exception {
      if ( Ats.from( candidate ).has( UsageMetricFunction.class ) && Function.class.isAssignableFrom( candidate ) ) {
        UsageMetricFunction measures = Ats.from( candidate ).get( UsageMetricFunction.class );
        Class measuredType = measures.value( );
        RestrictedTypes.usageMetricFunctions.put( measuredType, ( Function<OwnerFullName, Long> ) Classes.newInstance( candidate ) );
        return true;
      } else if ( Ats.from( candidate ).has( QuantityMetricFunction.class ) && Function.class.isAssignableFrom( candidate ) ) {
        QuantityMetricFunction measures = Ats.from( candidate ).get( QuantityMetricFunction.class );
        Class measuredType = measures.value( );
        RestrictedTypes.quantityMetricFunctions.put( measuredType, ( Function<OwnerFullName, Long> ) Classes.newInstance( candidate ) );
        return true;
      } else if ( Ats.from( candidate ).has( Resolver.class ) && Function.class.isAssignableFrom( candidate ) ) {
        Resolver resolver = Ats.from( candidate ).get( Resolver.class );
        Class resolverFunction = resolver.value( );
        RestrictedTypes.resourceResolvers.put( resolverFunction, ( Function<String, RestrictedType<?>> ) Classes.newInstance( candidate ) );
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
  
  public static <T> Function<String, T> listAll( Class<T> type ) {
    //TODO:GRZE:WTF FINISH THIS SHIT.
    return null;
  }
}
