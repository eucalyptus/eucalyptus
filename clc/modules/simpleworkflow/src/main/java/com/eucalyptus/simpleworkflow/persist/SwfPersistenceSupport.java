/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
