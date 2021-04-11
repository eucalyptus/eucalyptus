/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class CharacterSet extends EucalyptusData {

  private String characterSetDescription;

  private String characterSetName;

  public String getCharacterSetDescription() {
    return characterSetDescription;
  }

  public void setCharacterSetDescription(final String characterSetDescription) {
    this.characterSetDescription = characterSetDescription;
  }

  public String getCharacterSetName() {
    return characterSetName;
  }

  public void setCharacterSetName(final String characterSetName) {
    this.characterSetName = characterSetName;
  }

}
