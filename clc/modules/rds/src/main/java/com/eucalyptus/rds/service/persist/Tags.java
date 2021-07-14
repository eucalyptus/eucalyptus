/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.rds.common.RdsMetadata;
import com.eucalyptus.rds.service.persist.entities.Tag;
import com.eucalyptus.rds.service.persist.views.TagView;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.TypeMapper;
import com.google.common.collect.Ordering;
import io.vavr.collection.Stream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;

public interface Tags {

  Comparator<com.eucalyptus.rds.common.msgs.Tag> COMPARATOR =
      Ordering.natural().onResultOf(com.eucalyptus.rds.common.msgs.Tag::getKey)
          .compound(Ordering.natural().onResultOf(com.eucalyptus.rds.common.msgs.Tag::getValue));

  <T> List<T> list(
      @Nullable OwnerFullName ownerFullName,
      Criterion criterion,
      Map<String,String> aliases,
      Predicate<? super Tag<?>> filter,
      Function<? super Tag<?>, T> transform
  ) throws RdsMetadataException;

  boolean delete(RdsMetadata.TagMetadata metadata) throws RdsMetadataException;

  static Comparator<com.eucalyptus.rds.common.msgs.Tag> tagComparator() {
    return COMPARATOR;
  }

  static List<com.eucalyptus.rds.common.msgs.Tag> sort(final List<com.eucalyptus.rds.common.msgs.Tag> tags) {
    return Stream.ofAll(tags)
        .sorted(COMPARATOR)
        .toJavaList();
  }

  @TypeMapper
  enum TagViewToTagTransform implements CompatFunction<TagView, com.eucalyptus.rds.common.msgs.Tag> {
    INSTANCE;

    @Nullable
    @Override
    public com.eucalyptus.rds.common.msgs.Tag apply(@Nullable final TagView view) {
      if (view == null) return null;
      final com.eucalyptus.rds.common.msgs.Tag tag = new com.eucalyptus.rds.common.msgs.Tag();
      tag.setKey(view.getKey());
      tag.setValue(view.getValue());
      return tag;
    }
  }
}
