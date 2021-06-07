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
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegex;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegexValue;


@HttpRequestMapping(method = "GET", uri = "/2013-04-01/tags/{ResourceType}/{ResourceId}")
@HttpNoContent
public class ListTagsForResourceType extends Route53Message {

  @Nonnull
  @HttpUriMapping(uri = "ResourceId")
  @FieldRange(max = 64)
  private String resourceId;

  @Nonnull
  @HttpUriMapping(uri = "ResourceType")
  @FieldRegex(FieldRegexValue.ENUM_TAGRESOURCETYPE)
  private String resourceType;

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(final String resourceId) {
    this.resourceId = resourceId;
  }

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(final String resourceType) {
    this.resourceType = resourceType;
  }

}
