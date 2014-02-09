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
package com.eucalyptus.autoscaling.metadata;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AutoScalingMetadataWithResourceName;

import java.util.NoSuchElementException;

import javax.annotation.Nullable;

import com.eucalyptus.autoscaling.common.AutoScalingMetadata;
import com.eucalyptus.autoscaling.common.AutoScalingResourceName;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedType;
import com.google.common.base.Function;
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

  public AOP update( final OwnerFullName ownerFullName,
                     @Nullable final String scopeNameOrArn,
                     final String nameOrArn,
                     final Callback<AOP> updateCallback ) throws AutoScalingMetadataException {
    final AOP example;
    if ( AutoScalingResourceName.isResourceName().apply( nameOrArn ) ) {
      example = exampleWithUuid( AutoScalingResourceName.parse( nameOrArn, type ).getUuid() );
    } else {
      final String scopeName = getNameFromScopeNameOrArn( scopeNameOrArn );
      example = exampleWithName( ownerFullName, scopeName, nameOrArn );
    }
    return updateByExample( example, ownerFullName, nameOrArn, updateCallback );
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
    if ( metadata instanceof AutoScalingMetadataWithResourceName ) {
      try {
        return Transactions.delete( exampleWithUuid( AutoScalingResourceName.parse( ((AutoScalingMetadataWithResourceName)metadata).getArn(), type ).getUuid() ) );
      } catch ( NoSuchElementException e ) {
        return false;
      } catch ( Exception e ) {
        throw new AutoScalingMetadataException( "Error deleting "+typeDescription+" '"+describe( metadata )+"'", e );
      }
    } else {
      return super.delete( metadata );
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
