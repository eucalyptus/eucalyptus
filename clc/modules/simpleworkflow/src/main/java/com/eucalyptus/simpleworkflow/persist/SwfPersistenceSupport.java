/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.simpleworkflow.persist;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.simpleworkflow.SwfMetadataException;
import com.eucalyptus.simpleworkflow.SwfMetadataNotFoundException;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.util.Exceptions;

/**
 *
 */
public abstract class SwfPersistenceSupport<RT extends RestrictedType, AP extends AbstractPersistent & RestrictedType>
    extends AbstractPersistentSupport<RT, AP, SwfMetadataException> {

  protected SwfPersistenceSupport( final String typeDescription ) {
    super( typeDescription );
  }

  @Override
  public AbstractPersistentSupport<RT, AP, SwfMetadataException> withRetries( ) {
    return super.withRetries( 50 );
  }

  @Override
  protected SwfMetadataException notFoundException( final String message, final Throwable cause ) {
    final SwfMetadataNotFoundException existingException =
      Exceptions.findCause( cause, SwfMetadataNotFoundException.class );
    if ( existingException != null ) {
      return existingException;
    } else {
      return new SwfMetadataNotFoundException( message, cause );
    }
  }

  @Override
  protected SwfMetadataException metadataException( final String message, final Throwable cause ) {
    final SwfMetadataException existingException =
      Exceptions.findCause( cause, SwfMetadataException.class );
    if ( existingException != null ) {
      return existingException;
    } else {
      return new SwfMetadataException( message, cause );
    }
  }
}
