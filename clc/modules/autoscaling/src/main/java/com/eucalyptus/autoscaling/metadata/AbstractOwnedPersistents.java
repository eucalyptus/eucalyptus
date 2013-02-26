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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.autoscaling.instances.AutoScalingInstance;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.OwnerFullName;
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

  public AOP lookupByExample( final AOP example,
                              @Nullable final OwnerFullName ownerFullName,
                              final String key ) throws AutoScalingMetadataException {
    try {
      return Transactions.find( example );
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

  public List<AOP> list( final OwnerFullName ownerFullName,
                         final Predicate<? super AOP> filter ) throws AutoScalingMetadataException {
    try {
      return Transactions.filter( exampleWithOwner( ownerFullName ), filter );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( qualifyOwner( "Failed to find "+typeDescription+"s", ownerFullName ), e );
    }
  }

  public List<AOP> listByExample( final AOP example,
                                  final Predicate<? super AOP> filter ) throws AutoScalingMetadataException {
    try {
      return Transactions.filter( example, filter );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Failed to find "+typeDescription+"s by example: " + LogUtil.dumpObject(example), e );
    }
  }

  public List<AOP> listByExample( final AOP example,
                                  final Predicate<? super AOP> filter,
                                  final Criterion criterion,
                                  final Map<String,String> aliases ) throws AutoScalingMetadataException {
    try {
      return Transactions.filter( example, filter, criterion, aliases );
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

  public boolean delete( final AOP metadata ) throws AutoScalingMetadataException {
    try {
      return Transactions.delete( exampleWithUuid( metadata.getNaturalId( ) ) );
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

  public static Function<AutoScalingInstance,Date> createdDate() {
    return AbstractOwnedPersistentDateProperties.CREATED;  
  }
  
  protected String describe( final AOP metadata ) {
    return metadata.getDisplayName();  
  }      
  
  protected abstract AOP exampleWithUuid( String uuid );

  protected abstract AOP exampleWithOwner( OwnerFullName ownerFullName );

  protected abstract AOP exampleWithName( OwnerFullName ownerFullName, String name );

  protected final String qualifyOwner( final String text, final OwnerFullName ownerFullName ) {
    return ownerFullName == null ? text : text + " for " + ownerFullName;
  }

  private enum AbstractOwnedPersistentDateProperties implements Function<AutoScalingInstance,Date> {
    CREATED {
      @Override
      public Date apply( final AutoScalingInstance autoScalingInstance ) {
        return autoScalingInstance.getCreationTimestamp();
      }
    }
  }

}
