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
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegex;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegexValue;


public class ListResourceRecordSetsResponseType extends Route53Message {


  @Nonnull
  private Boolean isTruncated;

  @Nonnull
  private String maxItems;

  @FieldRange(min = 1, max = 128)
  private String nextRecordIdentifier;

  @FieldRange(max = 1024)
  private String nextRecordName;

  @FieldRegex(FieldRegexValue.ENUM_RRTYPE)
  private String nextRecordType;

  @Nonnull
  private ResourceRecordSets resourceRecordSets;

  public Boolean getIsTruncated() {
    return isTruncated;
  }

  public void setIsTruncated(final Boolean isTruncated) {
    this.isTruncated = isTruncated;
  }

  public String getMaxItems() {
    return maxItems;
  }

  public void setMaxItems(final String maxItems) {
    this.maxItems = maxItems;
  }

  public String getNextRecordIdentifier() {
    return nextRecordIdentifier;
  }

  public void setNextRecordIdentifier(final String nextRecordIdentifier) {
    this.nextRecordIdentifier = nextRecordIdentifier;
  }

  public String getNextRecordName() {
    return nextRecordName;
  }

  public void setNextRecordName(final String nextRecordName) {
    this.nextRecordName = nextRecordName;
  }

  public String getNextRecordType() {
    return nextRecordType;
  }

  public void setNextRecordType(final String nextRecordType) {
    this.nextRecordType = nextRecordType;
  }

  public ResourceRecordSets getResourceRecordSets() {
    return resourceRecordSets;
  }

  public void setResourceRecordSets(final ResourceRecordSets resourceRecordSets) {
    this.resourceRecordSets = resourceRecordSets;
  }

}
