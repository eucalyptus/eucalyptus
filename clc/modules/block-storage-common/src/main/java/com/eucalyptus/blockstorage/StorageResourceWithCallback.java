package com.eucalyptus.blockstorage;

import com.google.common.base.Function;

public class StorageResourceWithCallback {

  private StorageResource sr;

  private Function<StorageResource, String> callback;

  public StorageResourceWithCallback(StorageResource sr, Function<StorageResource, String> callback) {
    this.sr = sr;
    this.callback = callback;
  }

  public StorageResource getSr() {
    return sr;
  }

  public void setSr(StorageResource sr) {
    this.sr = sr;
  }

  public Function<StorageResource, String> getCallback() {
    return callback;
  }

  public void setCallback(Function<StorageResource, String> callback) {
    this.callback = callback;
  }

}
