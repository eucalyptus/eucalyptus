/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
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
