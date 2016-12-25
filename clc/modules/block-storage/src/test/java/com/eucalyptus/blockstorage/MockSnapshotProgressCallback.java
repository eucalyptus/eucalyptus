package com.eucalyptus.blockstorage;

public class MockSnapshotProgressCallback extends SnapshotProgressCallback {

  public MockSnapshotProgressCallback() {}

  @Override
  public void setUploadSize(long uploadSize) {}

  @Override
  public void updateUploadProgress(final long bytesTransferred) {}

  @Override
  public void updateBackendProgress(final int percentComplete) {}
}
