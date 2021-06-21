/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegex;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import javax.annotation.Nonnull;


public class Filter extends EucalyptusData {

  @Nonnull
  @FieldRegex(FieldRegexValue.STRING_128)
  private String name;

  @Nonnull
  private FilterValueList values;

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public FilterValueList getValues() {
    return values;
  }

  public void setValues(final FilterValueList values) {
    this.values = values;
  }

}
