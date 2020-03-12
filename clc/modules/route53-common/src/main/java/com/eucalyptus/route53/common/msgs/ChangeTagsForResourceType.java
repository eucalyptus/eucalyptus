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


@HttpRequestMapping(method = "POST", uri = "/2013-04-01/tags/{ResourceType}/{ResourceId}")
public class ChangeTagsForResourceType extends Route53Message {

  @FieldRange(min = 1, max = 10)
  private TagList addTags;

  @FieldRange(min = 1, max = 10)
  private TagKeyList removeTagKeys;

  @Nonnull
  @HttpUriMapping(uri = "ResourceId")
  @FieldRange(max = 64)
  private String resourceId;

  @Nonnull
  @HttpUriMapping(uri = "ResourceType")
  @FieldRegex(FieldRegexValue.ENUM_TAGRESOURCETYPE)
  private String resourceType;

  public TagList getAddTags() {
    return addTags;
  }

  public void setAddTags(final TagList addTags) {
    this.addTags = addTags;
  }

  public TagKeyList getRemoveTagKeys() {
    return removeTagKeys;
  }

  public void setRemoveTagKeys(final TagKeyList removeTagKeys) {
    this.removeTagKeys = removeTagKeys;
  }

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
