/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.persist;

import com.eucalyptus.route53.common.msgs.ResourceRecord;
import com.eucalyptus.route53.common.msgs.ResourceRecords;
import com.eucalyptus.route53.service.persist.entities.ResourceRecordSet;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;

/**
 *
 */
public interface ResourceRecordSets {

  @TypeMapper
  enum ResourceRecordSetTransform implements Function<ResourceRecordSet, com.eucalyptus.route53.common.msgs.ResourceRecordSet> {
    INSTANCE;

    @Override
    public com.eucalyptus.route53.common.msgs.ResourceRecordSet apply(final ResourceRecordSet resourceRecordSet) {
      final com.eucalyptus.route53.common.msgs.ResourceRecordSet resultRrset = new com.eucalyptus.route53.common.msgs.ResourceRecordSet();
      resultRrset.setName(resourceRecordSet.getName());
      resultRrset.setType(resourceRecordSet.getType().name());
      resultRrset.setTTL(resourceRecordSet.getTtl() == null ? null : (long)resourceRecordSet.getTtl());

      final ResourceRecords resourceRecords = new ResourceRecords();
      for (final String value : resourceRecordSet.getValues()) {
        final ResourceRecord resourceRecord = new ResourceRecord();
        resourceRecord.setValue(value);
        resourceRecords.getMember().add(resourceRecord);
      }
      if (!resourceRecords.getMember().isEmpty()) {
        resultRrset.setResourceRecords(resourceRecords);
      }

      return resultRrset;
    }
  }
}
