/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpRequestMapping;
import com.eucalyptus.binding.HttpUriMapping;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;


@HttpRequestMapping(method = "POST", uri = "/2013-04-01/trafficpolicy/{Id}")
public class CreateTrafficPolicyVersionType extends Route53Message {

  @FieldRange(max = 1024)
  private String comment;

  @Nonnull
  @FieldRange(max = 102400)
  private String document;

  @Nonnull
  @HttpUriMapping(uri = "Id")
  @FieldRange(min = 1, max = 36)
  private String id;

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

}
