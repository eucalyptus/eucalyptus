/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRange;


public class ListImportsType extends CloudFormationMessage {

  @Nonnull
  private String exportName;

  @FieldRange(min = 1, max = 1024)
  private String nextToken;

  public String getExportName() {
    return exportName;
  }

  public void setExportName(final String exportName) {
    this.exportName = exportName;
  }

  public String getNextToken() {
    return nextToken;
  }

  public void setNextToken(final String nextToken) {
    this.nextToken = nextToken;
  }

}
