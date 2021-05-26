/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Metadata;
import com.eucalyptus.loadbalancingv2.common.msgs.TagDescription;
import com.eucalyptus.loadbalancingv2.common.msgs.TagList;
import com.eucalyptus.loadbalancingv2.service.persist.entities.Tag;
import com.eucalyptus.loadbalancingv2.service.persist.views.TagView;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BinaryOperator;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;

public interface Tags {

  Comparator<TagDescription> COMPARATOR = Ordering.natural().onResultOf( TagDescription::getResourceArn )
      .compound( Ordering.natural().onResultOf( Tags::firstKey) )
      .compound( Ordering.natural().onResultOf( Tags::firstValue ) );

  <T> List<T> list(
      @Nullable OwnerFullName ownerFullName,
      Criterion criterion,
      Predicate<? super Tag<?>> filter,
      Function<? super Tag<?>, T> transform
  ) throws Loadbalancingv2MetadataException;

  boolean delete(Loadbalancingv2Metadata.TagMetadata metadata) throws Loadbalancingv2MetadataException;

  static Comparator<TagDescription> tagDescriptionComparator() {
    return COMPARATOR;
  }

  static List<TagDescription> merge(final List<TagDescription> tagDescriptions) {
    return Stream.ofAll(tagDescriptions)
        .sorted(COMPARATOR)
        .map(Collections::singletonList)
        .foldLeft(Lists.newArrayList(), TagDescriptionListReduce.INSTANCE);
  }

  static String firstKey(final TagDescription tagDescription) {
    return firstTagData(tagDescription, com.eucalyptus.loadbalancingv2.common.msgs.Tag::getKey);
  }

  static String firstValue(final TagDescription tagDescription) {
    return firstTagData(tagDescription, com.eucalyptus.loadbalancingv2.common.msgs.Tag::getValue);
  }

  static String firstTagData(
      final TagDescription tagDescription,
      final CompatFunction<com.eucalyptus.loadbalancingv2.common.msgs.Tag,String> tagData
  ) {
    String first = "";
    if (tagDescription != null) {
      final TagList tagList = tagDescription.getTags();
      if (tagList != null && !tagList.getMember().isEmpty()) {
        first = Option.of(tagList.getMember().get(0)).map(tagData).getOrElse(first);
      }
    }
    return first;
  }

  enum TagDescriptionListReduce implements BinaryOperator<List<TagDescription>> {
    INSTANCE;

    @Override public List<TagDescription> apply(
        final List<TagDescription> tagDescriptions1,
        final List<TagDescription> tagDescriptions2) {
      if (tagDescriptions1.isEmpty()) {
        tagDescriptions1.addAll(tagDescriptions2);
      } else {
        final TagDescription candidateTagDescription =
            tagDescriptions1.get(tagDescriptions1.size() - 1);
        for (final TagDescription tagDescription : tagDescriptions2) {
          if (Objects.toString(candidateTagDescription.getResourceArn(), "").equals(
              Objects.toString(tagDescription.getResourceArn(), ""))) {
            candidateTagDescription.getTags().getMember().addAll(
                tagDescription.getTags().getMember());
          } else {
            tagDescriptions1.add(tagDescription);
          }
        }
      }
      return tagDescriptions1;
    }
  }

  @TypeMapper
  enum TagViewToTagDescriptionTransform implements Function<TagView, TagDescription> {
    INSTANCE;

    @Nullable
    @Override
    public TagDescription apply(@Nullable final TagView view) {
      final TagDescription tagDescription = new TagDescription();
      tagDescription.setResourceArn(view.getResourceArn());
      final TagList tagList = new TagList();
      final com.eucalyptus.loadbalancingv2.common.msgs.Tag tag = new com.eucalyptus.loadbalancingv2.common.msgs.Tag();
      tag.setKey(view.getKey());
      tag.setValue(view.getValue());
      tagList.getMember().add(tag);
      tagDescription.setTags(tagList);
      return tagDescription;
    }
  }
}
