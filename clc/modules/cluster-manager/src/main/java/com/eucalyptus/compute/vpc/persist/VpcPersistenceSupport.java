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
