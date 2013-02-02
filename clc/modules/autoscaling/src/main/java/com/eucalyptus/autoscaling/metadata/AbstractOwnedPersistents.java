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
import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;
import com.eucalyptus.autoscaling.common.AutoScalingResourceName;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Predicate;

/**
 *
 */
public abstract class AbstractOwnedPersistents<AOP extends AbstractOwnedPersistent & AutoScalingMetadataWithResourceName> {

  private final String typeDescription;
  private final AutoScalingResourceName.Type type;
  @Nullable
  private final AutoScalingResourceName.Type scopeType;

  protected AbstractOwnedPersistents( final AutoScalingResourceName.Type type ) {
    this( type, null );
  }

  protected AbstractOwnedPersistents( final AutoScalingResourceName.Type type,
                                      @Nullable final AutoScalingResourceName.Type scopeType ) {
    this.type = type;
    this.scopeType = scopeType;
    this.typeDescription = type.describe();
  }

  public final AOP lookup( final OwnerFullName ownerFullName,
                           final String nameOrArn ) throws AutoScalingMetadataException {
    return lookup( ownerFullName, null, nameOrArn );
  }


  public final AOP lookup( final OwnerFullName ownerFullName,
                           @Nullable final String scopeNameOrArn,
                           final String nameOrArn ) throws AutoScalingMetadataException {
    if ( AutoScalingResourceName.isResourceName().apply( nameOrArn ) ) {
      return lookupByUuid( AutoScalingResourceName.parse( nameOrArn, type ).getUuid() );
    } else {
      final String scopeName = getNameFromScopeNameOrArn( scopeNameOrArn );
      return lookupByName( ownerFullName, scopeName, nameOrArn );
    }
  }

  public List<AOP> list( final OwnerFullName ownerFullName ) throws AutoScalingMetadataException {
    try {
      return Transactions.findAll( exampleWithOwner( ownerFullName ) );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Failed to find "+typeDescription+"s for " + ownerFullName, e );
    }
  }

  public List<AOP> list( final OwnerFullName ownerFullName,
                         final Predicate<? super AOP> filter ) throws AutoScalingMetadataException {
    try {
      return Transactions.filter( exampleWithOwner( ownerFullName ), filter );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Failed to find "+typeDescription+"s for " + ownerFullName, e );
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
  
    try {
      return Transactions.one( example, updateCallback );
    } catch ( NoSuchElementException e ) {
      throw new AutoScalingMetadataNotFoundException( "Unable to find "+typeDescription+" '"+nameOrArn+"' for " + ownerFullName, e );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error updating "+typeDescription+" '"+nameOrArn+"' for " + ownerFullName, e );
    }
  }

  public boolean delete( final AOP metadata ) throws AutoScalingMetadataException {
    try {
      return Transactions.delete( exampleWithUuid( metadata.getNaturalId() ) );
    } catch ( NoSuchElementException e ) {
      return false;
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error deleting "+typeDescription+" '"+metadata.getArn()+"'", e );
    }
  }

  public AOP save( final AOP metadata ) throws AutoScalingMetadataException {
    try {
      return Transactions.saveDirect( metadata );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error creating "+typeDescription+" '"+metadata.getDisplayName()+"'", e );
    }
  }

  protected abstract AOP exampleWithOwner( OwnerFullName ownerFullName );

  protected abstract AOP exampleWithName( OwnerFullName ownerFullName, String name );

  protected AOP exampleWithName( OwnerFullName ownerFullName, String scope, String name ) {
    return exampleWithName( ownerFullName, name );
  }

  protected abstract AOP exampleWithUuid( String uuid );

  protected AOP lookupByName( final OwnerFullName ownerFullName, 
                              final String scope, 
                              final String name ) throws AutoScalingMetadataException {
    return lookupByExample( exampleWithName( ownerFullName, scope, name ), ownerFullName, name );
  }

  protected AOP lookupByUuid( final String uuid ) throws AutoScalingMetadataException {
    return lookupByExample( exampleWithUuid( uuid ), null, uuid );
  }

  private AOP lookupByExample( final AOP example,
                               @Nullable final OwnerFullName ownerFullName,
                               final String key ) throws AutoScalingMetadataException {
    try {
      return Transactions.find( example );
    } catch ( NoSuchElementException e ) {
      throw new AutoScalingMetadataNotFoundException( qualifyOwner("Unable to find "+typeDescription+" '"+key+"'", ownerFullName), e );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException(  qualifyOwner("Error finding "+typeDescription+" '"+key+"'", ownerFullName), e );
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

  private String qualifyOwner( final String text, final OwnerFullName ownerFullName ) {
    return ownerFullName == null ? text : text + " for " + ownerFullName;
  }
}
