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
import javax.annotation.Nullable;
import com.eucalyptus.autoscaling.common.AutoScalingMetadatas;
import com.eucalyptus.autoscaling.common.AutoScalingResourceName;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;

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

  public final AOP lookup( final OwnerFullName ownerFullName,
                           final String nameOrArn ) throws AutoScalingMetadataException {
    return lookup( ownerFullName, null, nameOrArn );
  }

  public final AOP lookup( final OwnerFullName ownerFullName,
                           @Nullable final String scopeNameOrArn,
                           final String nameOrArn ) throws AutoScalingMetadataException {
    if ( AutoScalingResourceName.isResourceName().apply( nameOrArn ) ) {
      return validateOwner( ownerFullName, lookupByUuid( AutoScalingResourceName.parse( nameOrArn, type ).getUuid() ), nameOrArn );
    } else {
      final String scopeName = getNameFromScopeNameOrArn( scopeNameOrArn );
      return lookupByName( ownerFullName, scopeName, nameOrArn );
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
    return updateByExample( example, ownerFullName, nameOrArn, validateOwner( ownerFullName, updateCallback ) );
  }
  
  protected AOP exampleWithName( OwnerFullName ownerFullName, String scope, String name ) {
    return exampleWithName( ownerFullName, name );
  }

  protected AOP lookupByUuid( final String uuid ) throws AutoScalingMetadataException {
    return lookupByExample( exampleWithUuid( uuid ), null, uuid );
  }

  protected AOP lookupByName( final OwnerFullName ownerFullName,
                              final String scope,
                              final String name ) throws AutoScalingMetadataException {
    return lookupByExample( exampleWithName( ownerFullName, scope, name ), ownerFullName, name );
  }

  @Override
  protected final String describe( final AOP metadata ) {
    return metadata.getArn();
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

  private AOP validateOwner( final OwnerFullName ownerFullName,
                             final AOP aop,
                             final String key ) throws AutoScalingMetadataNotFoundException {
    if ( !AutoScalingMetadatas.filterByOwner( ownerFullName ).apply( aop ) ) {
      throw new AutoScalingMetadataNotFoundException( qualifyOwner("Unable to find "+typeDescription+" '"+key+"'", ownerFullName) );
    }
    return aop;
  }

  private Callback<AOP> validateOwner( final OwnerFullName ownerFullName,
                                       final Callback<AOP> nested ) {
    return new Callback<AOP>(){
      @Override
      public void fire( final AOP aop ) {
        if ( AutoScalingMetadatas.filterByOwner( ownerFullName ).apply( aop ) ) {
          nested.fire( aop );
        }
      }
    };
  }
}
