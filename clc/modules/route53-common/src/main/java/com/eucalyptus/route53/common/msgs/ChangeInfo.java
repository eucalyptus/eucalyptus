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


public class ChangeInfo extends EucalyptusData {

  @FieldRange(max = 256)
  private String comment;

  @Nonnull
  @FieldRange(max = 32)
  private String id;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_CHANGESTATUS)
  private String status;

  @Nonnull
  private java.util.Date submittedAt;

  public String getComment() {
    return comment;
  }

  public void setComment(final String comment) {
    this.comment = comment;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public java.util.Date getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(final java.util.Date submittedAt) {
    this.submittedAt = submittedAt;
  }

}
