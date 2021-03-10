/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist.entities;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.loadbalancingv2.service.persist.Loadbalancingv2MetadataException;
import com.eucalyptus.loadbalancingv2.service.persist.Loadbalancingv2MetadataNotFoundException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.base.Predicate;


public abstract class Loadbalancingv2PersistenceSupport<RT extends RestrictedType, AP extends AbstractPersistent & RestrictedType>
    extends AbstractPersistentSupport<RT, AP, Loadbalancingv2MetadataException> {

  protected Loadbalancingv2PersistenceSupport(final String typeDescription) {
    super(typeDescription);
  }

  public <T> T updateByExample(
      final AP example,
      final OwnerFullName ownerFullName,
      final String key,
      final Predicate<? super AP> filter,
      final Function<? super AP, T> updateTransform
  ) throws Loadbalancingv2MetadataException {
    return updateByExample(example, ownerFullName, key, entity -> {
      if (!filter.apply(entity)) {
        throw Exceptions.toUndeclared(notFoundException(
            qualifyOwner("Filter denied " + typeDescription + " '" + key + "'", ownerFullName),
            null));
      }
      return updateTransform.apply(entity);
    });
  }

  @Override
  protected Loadbalancingv2MetadataException notFoundException(final String message,
      final Throwable cause) {
    final Loadbalancingv2MetadataNotFoundException existingException =
        Exceptions.findCause(cause, Loadbalancingv2MetadataNotFoundException.class);
    if (existingException != null) {
      return existingException;
    } else {
      return new Loadbalancingv2MetadataNotFoundException(message, cause);
    }
  }

  @Override
  protected Loadbalancingv2MetadataException metadataException(final String message,
      final Throwable cause) {
    final Loadbalancingv2MetadataException existingException =
        Exceptions.findCause(cause, Loadbalancingv2MetadataException.class);
    if (existingException != null) {
      return existingException;
    } else {
      return new Loadbalancingv2MetadataException(message, cause);
    }
  }
}
