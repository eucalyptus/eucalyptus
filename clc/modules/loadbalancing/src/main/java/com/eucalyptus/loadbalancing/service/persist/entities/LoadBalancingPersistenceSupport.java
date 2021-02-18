/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist.entities;

import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.loadbalancing.service.persist.LoadBalancingMetadataException;
import com.eucalyptus.loadbalancing.service.persist.LoadBalancingMetadataNotFoundException;
import com.eucalyptus.util.Exceptions;

public abstract class LoadBalancingPersistenceSupport<RT extends RestrictedType, AP extends AbstractPersistent & RestrictedType>
    extends AbstractPersistentSupport<RT, AP, LoadBalancingMetadataException> {

  protected LoadBalancingPersistenceSupport(final String typeDescription) {
    super(typeDescription);
  }

  @Override
  protected LoadBalancingMetadataException notFoundException(final String message,
      final Throwable cause) {
    final LoadBalancingMetadataNotFoundException existingException =
        Exceptions.findCause(cause, LoadBalancingMetadataNotFoundException.class);
    if (existingException != null) {
      return existingException;
    } else {
      return new LoadBalancingMetadataNotFoundException(message, cause);
    }
  }

  @Override
  protected LoadBalancingMetadataException metadataException(final String message,
      final Throwable cause) {
    final LoadBalancingMetadataException existingException =
        Exceptions.findCause(cause, LoadBalancingMetadataException.class);
    if (existingException != null) {
      return existingException;
    } else {
      return new LoadBalancingMetadataException(message, cause);
    }
  }
}
