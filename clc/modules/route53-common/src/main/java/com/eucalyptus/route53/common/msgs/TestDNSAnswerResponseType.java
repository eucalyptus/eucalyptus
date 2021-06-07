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


public class TestDNSAnswerResponseType extends Route53Message {


  @Nonnull
  @FieldRange(max = 255)
  private String nameserver;

  @Nonnull
  private String protocol;

  @Nonnull
  private RecordData recordData;

  @Nonnull
  @FieldRange(max = 1024)
  private String recordName;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_RRTYPE)
  private String recordType;

  @Nonnull
  private String responseCode;

  public String getNameserver() {
    return nameserver;
  }

  public void setNameserver(final String nameserver) {
    this.nameserver = nameserver;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(final String protocol) {
    this.protocol = protocol;
  }

  public RecordData getRecordData() {
    return recordData;
  }

  public void setRecordData(final RecordData recordData) {
    this.recordData = recordData;
  }

  public String getRecordName() {
    return recordName;
  }

  public void setRecordName(final String recordName) {
    this.recordName = recordName;
  }

  public String getRecordType() {
    return recordType;
  }

  public void setRecordType(final String recordType) {
    this.recordType = recordType;
  }

  public String getResponseCode() {
    return responseCode;
  }

  public void setResponseCode(final String responseCode) {
    this.responseCode = responseCode;
  }

}
