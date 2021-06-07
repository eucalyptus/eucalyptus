/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ValidDBInstanceModificationsMessage extends EucalyptusData {

  private ValidStorageOptionsList storage;

  private AvailableProcessorFeatureList validProcessorFeatures;

  public ValidStorageOptionsList getStorage() {
    return storage;
  }

  public void setStorage(final ValidStorageOptionsList storage) {
    this.storage = storage;
  }

  public AvailableProcessorFeatureList getValidProcessorFeatures() {
    return validProcessorFeatures;
  }

  public void setValidProcessorFeatures(final AvailableProcessorFeatureList validProcessorFeatures) {
    this.validProcessorFeatures = validProcessorFeatures;
  }

}
