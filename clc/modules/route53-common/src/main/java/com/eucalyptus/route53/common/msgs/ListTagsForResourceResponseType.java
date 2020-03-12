/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;


public class ListTagsForResourceResponseType extends Route53Message {


  @Nonnull
  private ResourceTagSet resourceTagSet;

  public ResourceTagSet getResourceTagSet() {
    return resourceTagSet;
  }

  public void setResourceTagSet(final ResourceTagSet resourceTagSet) {
    this.resourceTagSet = resourceTagSet;
  }

}
