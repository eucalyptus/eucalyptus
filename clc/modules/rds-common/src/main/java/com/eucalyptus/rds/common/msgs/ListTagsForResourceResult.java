/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ListTagsForResourceResult extends EucalyptusData {

  private TagList tagList;

  public TagList getTagList() {
    return tagList;
  }

  public void setTagList(final TagList tagList) {
    this.tagList = tagList;
  }

}
