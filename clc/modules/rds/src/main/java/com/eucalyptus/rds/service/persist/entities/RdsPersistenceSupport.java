/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.entities;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.rds.service.persist.RdsMetadataException;
import com.eucalyptus.rds.service.persist.RdsMetadataNotFoundException;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.CompatPredicate;
import com.eucalyptus.util.Exceptions;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import org.hibernate.criterion.Criterion;

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

  public <T> List<T> list(
      final OwnerFullName ownerFullName,
      final Predicate<? super AP> filter,
      final Function<? super AP, T> transform
  ) throws RdsMetadataException {
    return super.list(ownerFullName, gpredicate(filter), gfunction(transform));
  }

  public <T> List<T> list(
      final OwnerFullName ownerFullName,
      final Criterion criterion,
      final Map<String, String> aliases,
      final Predicate<? super AP> filter,
      final Function<? super AP, T> transform
  ) throws RdsMetadataException {
    return super.list(ownerFullName, criterion, aliases, gpredicate(filter), gfunction(transform));
  }

  public <T> List<T> listByExample(
      final AP example,
      final Predicate<? super AP> filter,
      final Function<? super AP, T> transform
  ) throws RdsMetadataException {
    return super.listByExample(example, gpredicate(filter), gfunction(transform));
  }

  public <T> T lookupByName(
      final OwnerFullName ownerFullName,
      final String name,
      final Predicate<? super AP> filter,
      final Function<? super AP, T> transform
  ) throws RdsMetadataException {
    return super.lookupByName(ownerFullName, name, gpredicate(filter), gfunction(transform));
  }

  public <T> T lookupByExample(
      final AP example,
      final OwnerFullName ownerFullName,
      final String name,
      final Predicate<? super AP> filter,
      final Function<? super AP, T> transform
  ) throws RdsMetadataException {
    return super.lookupByExample(example, ownerFullName, name, gpredicate(filter), gfunction(transform));
  }

  public <T> T updateByExample(
      final AP example,
      final OwnerFullName ownerFullName,
      final String desc,
      final Function<? super AP, T> updateTransform
  ) throws RdsMetadataException {
    return super.updateByExample(example, ownerFullName, desc, gfunction(updateTransform));
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

  private static <F, T> com.google.common.base.Function<F, T> gfunction(final Function<F, T> function) {
    return function == null ? null : CompatFunction.of(function);
  }

  private static <T> com.google.common.base.Predicate<T> gpredicate(final Predicate<T> predicate) {
    return predicate == null ? null : CompatPredicate.of(predicate);
  }
}
