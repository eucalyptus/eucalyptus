/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Parameter extends EucalyptusData {

  private String parameterKey;

  private String parameterValue;

  private String resolvedValue;

  private Boolean usePreviousValue;

  public Parameter() {
  }

  public Parameter(final String parameterKey, final String parameterValue) {
    this.parameterKey = parameterKey;
    this.parameterValue = parameterValue;
  }

  public String getParameterKey() {
    return parameterKey;
  }

  public void setParameterKey(final String parameterKey) {
    this.parameterKey = parameterKey;
  }

  public String getParameterValue() {
    return parameterValue;
  }

  public void setParameterValue(final String parameterValue) {
    this.parameterValue = parameterValue;
  }

  public String getResolvedValue() {
    return resolvedValue;
  }

  public void setResolvedValue(final String resolvedValue) {
    this.resolvedValue = resolvedValue;
  }

  public Boolean getUsePreviousValue() {
    return usePreviousValue;
  }

  public void setUsePreviousValue(final Boolean usePreviousValue) {
    this.usePreviousValue = usePreviousValue;
  }

}
