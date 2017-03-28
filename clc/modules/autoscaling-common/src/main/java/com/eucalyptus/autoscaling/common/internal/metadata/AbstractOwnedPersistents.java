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
package com.eucalyptus.autoscaling.common.internal.metadata;

import com.eucalyptus.autoscaling.common.AutoScalingMetadata;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.type.RestrictedType;
import com.google.common.base.Function;

/**
 *
 */
public abstract class AbstractOwnedPersistents<AOP extends AbstractOwnedPersistent & RestrictedType> extends AbstractPersistentSupport<AutoScalingMetadata, AOP, AutoScalingMetadataException> {

  protected AbstractOwnedPersistents( final String typeDescription ) {
    super( typeDescription );
  }

  public static interface WorkCallback<T> {
    public T doWork() throws AutoScalingMetadataException;
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
      throw metadataException( "Transaction failed", e );
    }
  }

  protected AutoScalingMetadataException notFoundException( String message, Throwable cause ) {
    final AutoScalingMetadataNotFoundException existingException =
        Exceptions.findCause( cause, AutoScalingMetadataNotFoundException.class );
    if ( existingException != null ) {
      return existingException;
    } else {
      return new AutoScalingMetadataNotFoundException( message, cause );
    }
  }

  protected AutoScalingMetadataException metadataException( String message, Throwable cause ) {
    final AutoScalingMetadataException existingException =
        Exceptions.findCause( cause, AutoScalingMetadataException.class );
    if ( existingException != null ) {
      return existingException;
    } else {
      return new AutoScalingMetadataException( message, cause );
    }
  }
}
