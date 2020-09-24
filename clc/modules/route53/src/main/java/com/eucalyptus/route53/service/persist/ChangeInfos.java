/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.persist;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.route53.common.Route53Metadata.ChangeMetadata;
import com.eucalyptus.route53.service.persist.entities.ChangeInfo;
import com.eucalyptus.util.Intervals;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 */
@SuppressWarnings("Guava")
public interface ChangeInfos {

  long EXPIRY_AGE = Intervals.parse(
      System.getProperty("com.eucalyptus.route53.changeExpiry"),
      TimeUnit.SECONDS,
      TimeUnit.HOURS.toMillis(24));

  long PENDING_AGE = Intervals.parse(
      System.getProperty("com.eucalyptus.route53.syncDuration"),
      TimeUnit.SECONDS,
      TimeUnit.SECONDS.toMillis( 30 ));

  <T> T lookupByName(final OwnerFullName ownerFullName,
                     final String name,
                     final Predicate<? super ChangeInfo> filter,
                     final Function<? super ChangeInfo, T> transform) throws Route53MetadataException;

  <T> List<T> list(OwnerFullName ownerFullName,
                   Predicate<? super ChangeInfo> filter,
                   Function<? super ChangeInfo, T> transform) throws Route53MetadataException;

  <T> List<T> list( OwnerFullName ownerFullName,
                    Criterion criterion,
                    Map<String,String> aliases,
                    Predicate<? super ChangeInfo> filter,
                    Function<? super  ChangeInfo,T> transform ) throws Route53MetadataException;


  <T> List<T> listByExample(ChangeInfo example,
                            Predicate<? super ChangeInfo> filter,
                            Function<? super ChangeInfo, T> transform) throws Route53MetadataException;

  <T> T updateByExample(ChangeInfo example,
                        OwnerFullName ownerFullName,
                        String changeInfoId,
                        Function<? super ChangeInfo, T> updateTransform) throws Route53MetadataException;


  ChangeInfo save(ChangeInfo changeInfo) throws Route53MetadataException;

  boolean delete( final ChangeMetadata metadata ) throws Route53MetadataException;

  List<ChangeInfo> deleteByExample(ChangeInfo example) throws Route53MetadataException;

  AbstractPersistentSupport<ChangeMetadata, ChangeInfo, Route53MetadataException> withRetries();

  @TypeMapper
  enum ChangeInfoTransform implements Function<ChangeInfo, com.eucalyptus.route53.common.msgs.ChangeInfo> {
    INSTANCE;

    @Override
    public com.eucalyptus.route53.common.msgs.ChangeInfo apply(final ChangeInfo changeInfo) {
      final com.eucalyptus.route53.common.msgs.ChangeInfo resultChangeInfo = new com.eucalyptus.route53.common.msgs.ChangeInfo();
      resultChangeInfo.setId(changeInfo.getDisplayName());
      resultChangeInfo.setStatus(changeInfo.getState().name());
      resultChangeInfo.setComment(changeInfo.getComment());
      resultChangeInfo.setSubmittedAt(changeInfo.getCreationTimestamp());
      return resultChangeInfo;
    }
  }
}
