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


@HttpRequestMapping(method = "POST", uri = "/2013-04-01/trafficpolicy/{Id}/{Version}")
public class UpdateTrafficPolicyCommentType extends Route53Message {

  @Nonnull
  @FieldRange(max = 1024)
  private String comment;

  @Nonnull
  @HttpUriMapping(uri = "Id")
  @FieldRange(min = 1, max = 36)
  private String id;

  @Nonnull
  @HttpUriMapping(uri = "Version")
  @FieldRange(min = 1, max = 1000)
  private Integer version;

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

  public Integer getVersion() {
    return version;
  }

  public void setVersion(final Integer version) {
    this.version = version;
  }

}
