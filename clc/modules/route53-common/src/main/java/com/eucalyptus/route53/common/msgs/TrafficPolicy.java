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
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class TrafficPolicy extends EucalyptusData {

  @FieldRange(max = 1024)
  private String comment;

  @Nonnull
  @FieldRange(max = 102400)
  private String document;

  @Nonnull
  @FieldRange(min = 1, max = 36)
  private String id;

  @Nonnull
  @FieldRange(max = 512)
  private String name;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_RRTYPE)
  private String type;

  @Nonnull
  @FieldRange(min = 1, max = 1000)
  private Integer version;

  public String getComment() {
    return comment;
  }

  public void setComment(final String comment) {
    this.comment = comment;
  }

  public String getDocument() {
    return document;
  }

  public void setDocument(final String document) {
    this.document = document;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(final Integer version) {
    this.version = version;
  }

}
