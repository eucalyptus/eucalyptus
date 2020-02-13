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
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Tag extends EucalyptusData {

  @Nonnull
  @FieldRange(min = 1, max = 128)
  private String key;

  @Nonnull
  @FieldRange(min = 1, max = 256)
  private String value;

  public String getKey() {
    return key;
  }

  public void setKey(final String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(final String value) {
    this.value = value;
  }

}
