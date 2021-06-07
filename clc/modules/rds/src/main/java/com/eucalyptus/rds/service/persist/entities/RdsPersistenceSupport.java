/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.entities;

import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.rds.service.persist.RdsMetadataException;
import com.eucalyptus.rds.service.persist.RdsMetadataNotFoundException;
import com.eucalyptus.util.Exceptions;

/**
 *
 */
public abstract class RdsPersistenceSupport<RT extends RestrictedType, AP extends AbstractPersistent & RestrictedType>
    extends AbstractPersistentSupport<RT, AP, RdsMetadataException> {

  protected RdsPersistenceSupport( final String typeDescription ) {
    super( typeDescription );
  }

  @Override
  public AbstractPersistentSupport<RT, AP, RdsMetadataException> withRetries( ) {
    return super.withRetries( 50 );
  }

  @Override
  protected RdsMetadataException notFoundException( final String message, final Throwable cause ) {
    final RdsMetadataNotFoundException existingException =
        Exceptions.findCause( cause, RdsMetadataNotFoundException.class );
    if ( existingException != null ) {
      return existingException;
    } else {
      return new RdsMetadataNotFoundException( message, cause );
    }
  }

  @Override
  protected RdsMetadataException metadataException( final String message, final Throwable cause ) {
    final RdsMetadataException existingException =
        Exceptions.findCause( cause, RdsMetadataException.class );
    if ( existingException != null ) {
      return existingException;
    } else {
      return new RdsMetadataException( message, cause );
    }
  }
}
