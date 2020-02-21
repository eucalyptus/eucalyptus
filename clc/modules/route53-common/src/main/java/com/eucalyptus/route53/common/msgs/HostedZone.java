/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class HostedZone extends EucalyptusData {

  @Nonnull
  @FieldRange(min = 1, max = 128)
  private String callerReference;

  private HostedZoneConfig config;

  @Nonnull
  @FieldRange(max = 32)
  private String id;

  private LinkedService linkedService;

  @Nonnull
  @FieldRange(max = 1024)
  private String name;

  private Long resourceRecordSetCount;

  public String getCallerReference() {
    return callerReference;
  }

  public void setCallerReference(final String callerReference) {
    this.callerReference = callerReference;
  }

  public HostedZoneConfig getConfig() {
    return config;
  }

  public void setConfig(final HostedZoneConfig config) {
    this.config = config;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public LinkedService getLinkedService() {
    return linkedService;
  }

  public void setLinkedService(final LinkedService linkedService) {
    this.linkedService = linkedService;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public Long getResourceRecordSetCount() {
    return resourceRecordSetCount;
  }

  public void setResourceRecordSetCount(final Long resourceRecordSetCount) {
    this.resourceRecordSetCount = resourceRecordSetCount;
  }

}
