/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpNoContent;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.binding.HttpRequestMapping;
import com.eucalyptus.binding.HttpUriMapping;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegex;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegexValue;


@HttpRequestMapping(method = "GET", uri = "/2013-04-01/hostedzone/{Id}/rrset")
@HttpNoContent
public class ListResourceRecordSetsType extends Route53Message {

  @Nonnull
  @HttpUriMapping(uri = "Id")
  @FieldRange(max = 32)
  private String hostedZoneId;

  @HttpParameterMapping(parameter = "maxitems")
  private String maxItems;

  @HttpParameterMapping(parameter = "identifier")
  @FieldRange(min = 1, max = 128)
  private String startRecordIdentifier;

  @HttpParameterMapping(parameter = "name")
  @FieldRange(max = 1024)
  private String startRecordName;

  @HttpParameterMapping(parameter = "type")
  @FieldRegex(FieldRegexValue.ENUM_RRTYPE)
  private String startRecordType;

  public String getHostedZoneId() {
    return hostedZoneId;
  }

  public void setHostedZoneId(final String hostedZoneId) {
    this.hostedZoneId = hostedZoneId;
  }

  public String getMaxItems() {
    return maxItems;
  }

  public void setMaxItems(final String maxItems) {
    this.maxItems = maxItems;
  }

  public String getStartRecordIdentifier() {
    return startRecordIdentifier;
  }

  public void setStartRecordIdentifier(final String startRecordIdentifier) {
    this.startRecordIdentifier = startRecordIdentifier;
  }

  public String getStartRecordName() {
    return startRecordName;
  }

  public void setStartRecordName(final String startRecordName) {
    this.startRecordName = startRecordName;
  }

  public String getStartRecordType() {
    return startRecordType;
  }

  public void setStartRecordType(final String startRecordType) {
    this.startRecordType = startRecordType;
  }

}
