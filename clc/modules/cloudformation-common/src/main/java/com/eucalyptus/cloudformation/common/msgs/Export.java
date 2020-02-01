/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Export extends EucalyptusData {

  private String exportingStackId;

  private String name;

  private String value;

  public String getExportingStackId() {
    return exportingStackId;
  }

  public void setExportingStackId(final String exportingStackId) {
    this.exportingStackId = exportingStackId;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(final String value) {
    this.value = value;
  }

}
