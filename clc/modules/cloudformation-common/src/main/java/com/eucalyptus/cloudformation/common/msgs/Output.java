/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRange;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Output extends EucalyptusData {

  @FieldRange(min = 1, max = 1024)
  private String description;

  private String exportName;

  private String outputKey;

  private String outputValue;

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getExportName() {
    return exportName;
  }

  public void setExportName(final String exportName) {
    this.exportName = exportName;
  }

  public String getOutputKey() {
    return outputKey;
  }

  public void setOutputKey(final String outputKey) {
    this.outputKey = outputKey;
  }

  public String getOutputValue() {
    return outputValue;
  }

  public void setOutputValue(final String outputValue) {
    this.outputValue = outputValue;
  }

}
