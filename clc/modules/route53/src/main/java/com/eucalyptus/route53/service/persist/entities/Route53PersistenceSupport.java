/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.persist.entities;

import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.route53.service.persist.Route53MetadataException;
import com.eucalyptus.route53.service.persist.Route53MetadataNotFoundException;
import com.eucalyptus.util.Exceptions;

/**
 *
 */
public abstract class Route53PersistenceSupport<RT extends RestrictedType, AP extends AbstractPersistent & RestrictedType>
    extends AbstractPersistentSupport<RT, AP, Route53MetadataException> {

  protected Route53PersistenceSupport(final String typeDescription) {
    super(typeDescription);
  }

  @Override
  public AbstractPersistentSupport<RT, AP, Route53MetadataException> withRetries() {
    return super.withRetries(50);
  }

  @Override
  protected Route53MetadataException notFoundException(final String message, final Throwable cause) {
    final Route53MetadataNotFoundException existingException =
        Exceptions.findCause(cause, Route53MetadataNotFoundException.class);
    if (existingException != null) {
      return existingException;
    } else {
      return new Route53MetadataNotFoundException(message, cause);
    }
  }

  @Override
  protected Route53MetadataException metadataException(final String message, final Throwable cause) {
    final Route53MetadataException existingException =
        Exceptions.findCause(cause, Route53MetadataException.class);
    if (existingException != null) {
      return existingException;
    } else {
      return new Route53MetadataException(message, cause);
    }
  }
}

