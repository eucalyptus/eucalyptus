/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class CreateOptionGroupResult extends EucalyptusData {

  private OptionGroup optionGroup;

  public OptionGroup getOptionGroup() {
    return optionGroup;
  }

  public void setOptionGroup(final OptionGroup optionGroup) {
    this.optionGroup = optionGroup;
  }

}
