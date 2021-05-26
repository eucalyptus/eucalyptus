/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist;

import com.eucalyptus.loadbalancingv2.service.persist.views.TagView;
import java.util.List;

public interface Taggable<T extends TagView> {

  T createTag(String key, String value);

  void updateTag(T tag, String value);

  List<T> getTags();

  void setTags(List<T> tags);
}
