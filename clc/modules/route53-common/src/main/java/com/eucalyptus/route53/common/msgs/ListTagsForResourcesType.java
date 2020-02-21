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
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegex;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegexValue;


@HttpRequestMapping(method = "POST", uri = "/2013-04-01/tags/{ResourceType}")
public class ListTagsForResourcesType extends Route53Message {

  @Nonnull
  @FieldRange(min = 1, max = 10)
  private TagResourceIdList resourceIds;

  @Nonnull
  @HttpUriMapping(uri = "ResourceType")
  @FieldRegex(FieldRegexValue.ENUM_TAGRESOURCETYPE)
  private String resourceType;

  public TagResourceIdList getResourceIds() {
    return resourceIds;
  }

  public void setResourceIds(final TagResourceIdList resourceIds) {
    this.resourceIds = resourceIds;
  }

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(final String resourceType) {
    this.resourceType = resourceType;
  }

}
