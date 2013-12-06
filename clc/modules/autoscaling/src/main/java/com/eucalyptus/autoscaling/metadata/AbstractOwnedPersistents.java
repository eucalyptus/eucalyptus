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

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

import org.hibernate.criterion.Criterion;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.autoscaling.common.AutoScalingMetadata;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedType;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 */
public abstract class AbstractOwnedPersistents<AOP extends AbstractOwnedPersistent> {

  protected final String typeDescription;

  protected AbstractOwnedPersistents( final String typeDescription ) {
    this.typeDescription = typeDescription;
  }

  public <T> T lookupByExample( final AOP example,
                                @Nullable final OwnerFullName ownerFullName,
                                final String key,
                                final Predicate<? super AOP> filter,
                                final Function<? super AOP,T> transform ) throws AutoScalingMetadataException {
    try {
      return Transactions.one( example, filter, transform );
    } catch ( NoSuchElementException e ) {
      throw new AutoScalingMetadataNotFoundException( qualifyOwner("Unable to find "+typeDescription+" '"+key+"'", ownerFullName), e );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( qualifyOwner("Error finding "+typeDescription+" '"+key+"'", ownerFullName), e );
    }
  }

  public List<AOP> list( final OwnerFullName ownerFullName ) throws AutoScalingMetadataException {
    try {
      return Transactions.findAll( exampleWithOwner( ownerFullName ) );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( qualifyOwner( "Failed to find "+typeDescription+"s", ownerFullName ), e );
    }
  }

  public <T> List<T> list( final OwnerFullName ownerFullName,
                           final Predicate<? super AOP> filter,
                           final Function<? super AOP,T> transform ) throws AutoScalingMetadataException {
    try {
      return Transactions.filteredTransform( exampleWithOwner( ownerFullName ), filter, transform );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( qualifyOwner( "Failed to find "+typeDescription+"s", ownerFullName ), e );
    }
  }

  public <T> List<T> listByExample( final AOP example,
                                    final Predicate<? super AOP> filter,
                                    final Function<? super AOP,T> transform ) throws AutoScalingMetadataException {
    try {
      return Transactions.filteredTransform( example, filter, transform );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Failed to find "+typeDescription+"s by example: " + LogUtil.dumpObject(example), e );
    }
  }

  public <T> List<T> listByExample( final AOP example,
                                    final Predicate<? super AOP> filter,
                                    final Criterion criterion,
                                    final Map<String,String> aliases,
                                    final Function<? super AOP,T> transform ) throws AutoScalingMetadataException {
    try {
      return Transactions.filteredTransform( example, criterion, aliases, filter, transform );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Failed to find "+typeDescription+"s by example: " + LogUtil.dumpObject(example), e );
    }
  }

  public AOP updateByExample( final AOP example,
                              final OwnerFullName ownerFullName,
                              final String key,
                              final Callback<AOP> updateCallback ) throws AutoScalingMetadataException {
    try {
      return Transactions.one( example, updateCallback );
    } catch ( NoSuchElementException e ) {
      throw new AutoScalingMetadataNotFoundException( qualifyOwner( "Unable to find "+typeDescription+" '"+key+"'", ownerFullName ), e );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( qualifyOwner( "Error updating "+typeDescription+" '"+key+"'", ownerFullName ), e );
    }
  }

  public AOP save( final AOP metadata ) throws AutoScalingMetadataException {
    try {
      return Transactions.saveDirect( metadata );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error creating "+typeDescription+" '"+metadata.getDisplayName()+"'", e );
    }
  }

  public boolean delete( final AutoScalingMetadata metadata ) throws AutoScalingMetadataException {
    try {
      return Transactions.delete( exampleWithName( AccountFullName.getInstance( metadata.getOwner().getAccountNumber() ), metadata.getDisplayName() ) );
    } catch ( NoSuchElementException e ) {
      return false;
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error deleting "+typeDescription+" '"+describe( metadata )+"'", e );
    }
  }

  public List<AOP> deleteByExample( final AOP example ) throws AutoScalingMetadataException {
    try {
      return Transactions.each( example, new Callback<AOP>(){
        @Override
        public void fire( final AOP input ) {
          Entities.delete( input );
        }
      } );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error deleting "+typeDescription+"s by example: "+LogUtil.dumpObject( example ), e );
    }
  }

  public List<AOP> deleteByExample( final AOP example,
                                    final Criterion criterion,
                                    final Map<String,String> aliases ) throws AutoScalingMetadataException {
    try {
      return Transactions.each( example, criterion, aliases, new Callback<AOP>(){
        @Override
        public void fire( final AOP input ) {
          Entities.delete( input );
        }
      } );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error deleting "+typeDescription+"s by example: "+LogUtil.dumpObject( example ), e );
    }
  }


  //TODO:STEVE: Use transactions with retries where ever appropriate
  public <R> R transactionWithRetry( final Class<?> entityType,
                                     final WorkCallback<R> work ) throws AutoScalingMetadataException {
    final Function<Void,R> workFunction = new Function<Void,R>() {
      @Override
      public R apply( final Void nothing ) {
        try {
          return work.doWork();
        } catch ( AutoScalingMetadataException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    };

    try {
      return Entities.asTransaction( entityType, workFunction ).apply( null );
    } catch ( Exception e ) {
      final AutoScalingMetadataException cause = Exceptions.findCause( e, AutoScalingMetadataException.class );
      if ( cause != null ) {
        throw cause;
      }
      throw new AutoScalingMetadataException( "Transaction failed", e );
    }
  }

  public static interface WorkCallback<T> {
    public T doWork() throws AutoScalingMetadataException;
  }

  protected String describe( final RestrictedType metadata ) {
    return metadata.getDisplayName();  
  }

  protected abstract AOP exampleWithUuid( String uuid );

  protected abstract AOP exampleWithOwner( OwnerFullName ownerFullName );

  protected abstract AOP exampleWithName( OwnerFullName ownerFullName, String name );

  protected final String qualifyOwner( final String text, final OwnerFullName ownerFullName ) {
    return ownerFullName == null ? text : text + " for " + ownerFullName;
  }
}
