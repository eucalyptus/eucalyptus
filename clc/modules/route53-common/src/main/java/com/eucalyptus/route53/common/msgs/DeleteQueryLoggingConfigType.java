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
import com.eucalyptus.binding.HttpRequestMapping;
import com.eucalyptus.binding.HttpUriMapping;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;


@HttpRequestMapping(method = "DELETE", uri = "/2013-04-01/queryloggingconfig/{Id}")
@HttpNoContent
public class DeleteQueryLoggingConfigType extends Route53Message {

  @Nonnull
  @HttpUriMapping(uri = "Id")
  @FieldRange(min = 1, max = 36)
  private String id;

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

}
