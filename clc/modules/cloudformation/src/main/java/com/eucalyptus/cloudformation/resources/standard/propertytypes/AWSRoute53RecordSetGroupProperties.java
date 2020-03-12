/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.ArrayList;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 *
 */
public class AWSRoute53RecordSetGroupProperties implements ResourceProperties {

  @Property
  private String comment;

  @Property
  private String hostedZoneId;

  @Property
  private String hostedZoneName;

  @Property
  private ArrayList<Route53RecordSet> recordSets = Lists.newArrayList();

  public String getComment() {
    return comment;
  }

  public void setComment(final String comment) {
    this.comment = comment;
  }

  public String getHostedZoneId() {
    return hostedZoneId;
  }

  public void setHostedZoneId(final String hostedZoneId) {
    this.hostedZoneId = hostedZoneId;
  }

  public String getHostedZoneName() {
    return hostedZoneName;
  }

  public void setHostedZoneName(final String hostedZoneName) {
    this.hostedZoneName = hostedZoneName;
  }

  public ArrayList<Route53RecordSet> getRecordSets() {
    return recordSets;
  }

  public void setRecordSets(final ArrayList<Route53RecordSet> recordSets) {
    this.recordSets = recordSets;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("comment", comment)
        .add("hostedZoneId", hostedZoneId)
        .add("hostedZoneName", hostedZoneName)
        .add("recordSets", recordSets)
        .toString();
  }
}
