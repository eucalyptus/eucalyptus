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
package com.eucalyptus.autoscaling.common.internal.metadata;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AutoScalingMetadataWithResourceName;

import javax.annotation.Nullable;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.autoscaling.common.AutoScalingMetadata;
import com.eucalyptus.autoscaling.common.AutoScalingResourceName;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.util.Callback;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.type.RestrictedType;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 *
 */
public abstract class AbstractOwnedPersistentsWithResourceNameSupport<AOP extends AbstractOwnedPersistent & AutoScalingMetadataWithResourceName> extends AbstractOwnedPersistents<AOP> {

  private final AutoScalingResourceName.Type type;
  @Nullable
  private final AutoScalingResourceName.Type scopeType;

  protected AbstractOwnedPersistentsWithResourceNameSupport( final AutoScalingResourceName.Type type ) {
    this( type, null );
  }

  protected AbstractOwnedPersistentsWithResourceNameSupport( final AutoScalingResourceName.Type type,
                                                             @Nullable final AutoScalingResourceName.Type scopeType ) {
    super( type.describe() );
    this.type = type;
    this.scopeType = scopeType;
  }

  public final <T> T lookup( final OwnerFullName ownerFullName,
                             final String nameOrArn,
                             final Function<? super AOP,T> transform ) throws AutoScalingMetadataException {
    return lookup( ownerFullName, null, nameOrArn, transform );
  }

  public final <T> T lookup( final OwnerFullName ownerFullName,
                             @Nullable final String scopeNameOrArn,
                             final String nameOrArn,
                             final Function<? super AOP,T> transform ) throws AutoScalingMetadataException {
    if ( AutoScalingResourceName.isResourceName().apply( nameOrArn ) ) {
      return lookupByUuid(
          AutoScalingResourceName.parse( nameOrArn, type ).getUuid(),
          Predicates.alwaysTrue( ),
          transform );
    } else {
      final String scopeName = getNameFromScopeNameOrArn( scopeNameOrArn );
      return lookupByName( ownerFullName, scopeName, nameOrArn, transform );
    }
  }

  public AOP update( final OwnerFullName ownerFullName,
                     final String nameOrArn,
                     final Callback<AOP> updateCallback ) throws AutoScalingMetadataException {
     return update( ownerFullName, null, nameOrArn, updateCallback );
  }

  public AOP updateWithRetries( final OwnerFullName ownerFullName,
                                final String nameOrArn,
                                final Callback<AOP> updateCallback ) throws AutoScalingMetadataException {
    return updateWithRetries( ownerFullName, null, nameOrArn, updateCallback );
  }

  public AOP update( final OwnerFullName ownerFullName,
                     @Nullable final String scopeNameOrArn,
                     final String nameOrArn,
                     final Callback<AOP> updateCallback ) throws AutoScalingMetadataException {
    return update( ownerFullName, scopeNameOrArn, nameOrArn, updateCallback, Functions.<AbstractPersistentSupport<AutoScalingMetadata, AOP, AutoScalingMetadataException>>identity( ) );
  }

  public AOP updateWithRetries( final OwnerFullName ownerFullName,
                                @Nullable final String scopeNameOrArn,
                                final String nameOrArn,
                                final Callback<AOP> updateCallback ) throws AutoScalingMetadataException {
    return update( ownerFullName, scopeNameOrArn, nameOrArn, updateCallback, retries( ) );
  }

  private AOP update( final OwnerFullName ownerFullName,
                      @Nullable final String scopeNameOrArn,
                      final String nameOrArn,
                      final Callback<AOP> updateCallback,
                      final Function<AbstractPersistentSupport<AutoScalingMetadata, AOP, AutoScalingMetadataException>,AbstractPersistentSupport<AutoScalingMetadata, AOP, AutoScalingMetadataException>> persistenceTransformer ) throws AutoScalingMetadataException {
    final AOP example;
    if ( AutoScalingResourceName.isResourceName().apply( nameOrArn ) ) {
      example = exampleWithUuid( AutoScalingResourceName.parse( nameOrArn, type ).getUuid() );
    } else {
      final String scopeName = getNameFromScopeNameOrArn( scopeNameOrArn );
      example = exampleWithName( ownerFullName, scopeName, nameOrArn );
    }
    //noinspection ConstantConditions
    return persistenceTransformer.apply( this ).updateByExample( example, ownerFullName, nameOrArn, updateCallback );
  }
  
  protected AOP exampleWithName( OwnerFullName ownerFullName, String scope, String name ) {
    return exampleWithName( ownerFullName, name );
  }

  protected <T> T lookupByUuid( final String uuid,
                                final Predicate<? super AOP> filter,
                                final Function<? super AOP,T> transform ) throws AutoScalingMetadataException {
    return lookupByExample( exampleWithUuid( uuid ), null, uuid, filter, transform );
  }

  protected <T> T lookupByName( final OwnerFullName ownerFullName,
                                final String scope,
                                final String name,
                                final Function<? super AOP,T> transform ) throws AutoScalingMetadataException {
    return lookupByExample( exampleWithName( ownerFullName, scope, name ), ownerFullName, name, Predicates.alwaysTrue(), transform );
  }

  @Override
  public boolean delete( final AutoScalingMetadata metadata ) throws AutoScalingMetadataException {
    try {
      return !withRetries( ).deleteByExample(
          metadata instanceof AutoScalingMetadataWithResourceName ?
              exampleWithUuid( AutoScalingResourceName.parse( ((AutoScalingMetadataWithResourceName)metadata).getArn(), type ).getUuid() ) :
              exampleWithName( AccountFullName.getInstance( metadata.getOwner().getAccountNumber() ), metadata.getDisplayName( ) )
      ).isEmpty( );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error deleting "+typeDescription+" '"+describe( metadata )+"'", e );
    }
  }

  @Override
  protected final String describe( final RestrictedType metadata ) {
    if ( metadata instanceof AutoScalingMetadataWithResourceName ) {
      return ((AutoScalingMetadataWithResourceName)metadata).getArn();
    } else {
      return super.describe( metadata );
    }
  }

  protected abstract AOP exampleWithUuid( String uuid );

  private Function<AbstractPersistentSupport<AutoScalingMetadata, AOP, AutoScalingMetadataException>,AbstractPersistentSupport<AutoScalingMetadata, AOP, AutoScalingMetadataException>> retries( ) {
    return new Function<AbstractPersistentSupport<AutoScalingMetadata, AOP, AutoScalingMetadataException>,AbstractPersistentSupport<AutoScalingMetadata, AOP, AutoScalingMetadataException>>(){
      @Override
      public AbstractPersistentSupport<AutoScalingMetadata, AOP, AutoScalingMetadataException> apply(
          final AbstractPersistentSupport<AutoScalingMetadata, AOP, AutoScalingMetadataException> persistence
      ) {
        return persistence.withRetries( );
      }
    };
  }

  private String getNameFromScopeNameOrArn( final String scopeNameOrArn ) {
    final String scopeName;
    if ( scopeNameOrArn != null &&
        scopeType != null &&
        AutoScalingResourceName.isResourceName().apply( scopeNameOrArn ) ) {
      scopeName = AutoScalingResourceName.parse( scopeNameOrArn, type ).getName( scopeType );
    } else {
      scopeName = scopeNameOrArn;
    }
    return scopeName;
  }
}
