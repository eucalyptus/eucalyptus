/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegex;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class PropertyDifference extends EucalyptusData {

  @Nonnull
  private String actualValue;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_DIFFERENCETYPE)
  private String differenceType;

  @Nonnull
  private String expectedValue;

  @Nonnull
  private String propertyPath;

  public String getActualValue() {
    return actualValue;
  }

  public void setActualValue(final String actualValue) {
    this.actualValue = actualValue;
  }

  public String getDifferenceType() {
    return differenceType;
  }

  public void setDifferenceType(final String differenceType) {
    this.differenceType = differenceType;
  }

  public String getExpectedValue() {
    return expectedValue;
  }

  public void setExpectedValue(final String expectedValue) {
    this.expectedValue = expectedValue;
  }

  public String getPropertyPath() {
    return propertyPath;
  }

  public void setPropertyPath(final String propertyPath) {
    this.propertyPath = propertyPath;
  }

}
