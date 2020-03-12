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


public class ChangeBatch extends EucalyptusData {

  @Nonnull
  @FieldRange(min = 1)
  private Changes changes;

  @FieldRange(max = 256)
  private String comment;

  public Changes getChanges() {
    return changes;
  }

  public void setChanges(final Changes changes) {
    this.changes = changes;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(final String comment) {
    this.comment = comment;
  }

}
