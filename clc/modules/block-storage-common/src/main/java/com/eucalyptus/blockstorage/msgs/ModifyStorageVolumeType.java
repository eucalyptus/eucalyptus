/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.blockstorage.msgs;

public class ModifyStorageVolumeType extends StorageRequestType {

  private String volumeId;
  private Integer size;

  public ModifyStorageVolumeType() {
  }

  public ModifyStorageVolumeType(String volumeId, Integer size) {
    this.volumeId = volumeId;
    this.size = size;
  }

  public String getVolumeId() {
    return volumeId;
  }

  public void setVolumeId(String volumeId) {
    this.volumeId = volumeId;
  }

  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }
}
