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
package com.eucalyptus.compute.vpc.persist;

import com.eucalyptus.compute.common.internal.vpc.VpcMetadataException;
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataNotFoundException;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.util.Exceptions;

/**
 *
 */
public abstract class VpcPersistenceSupport<RT extends RestrictedType, AP extends AbstractPersistent & RestrictedType> extends AbstractPersistentSupport<RT, AP, VpcMetadataException> {

  protected VpcPersistenceSupport( final String typeDescription ) {
    super( typeDescription );
  }

  @Override
  protected VpcMetadataException notFoundException( final String message, final Throwable cause ) {
    final VpcMetadataNotFoundException existingException =
        Exceptions.findCause( cause, VpcMetadataNotFoundException.class );
    if ( existingException != null ) {
      return existingException;
    } else {
      return new VpcMetadataNotFoundException( message, cause );
    }
  }

  @Override
  protected VpcMetadataException metadataException( final String message, final Throwable cause ) {
    final VpcMetadataException existingException =
        Exceptions.findCause( cause, VpcMetadataException.class );
    if ( existingException != null ) {
      return existingException;
    } else {
      return new VpcMetadataException( message, cause );
    }
  }

}
