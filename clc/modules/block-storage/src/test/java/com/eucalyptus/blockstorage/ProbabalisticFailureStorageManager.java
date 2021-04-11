/*************************************************************************
 * Copyright 2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.blockstorage;

import java.util.ArrayList;
import java.util.List;

import com.eucalyptus.blockstorage.StorageManagers.StorageManagerProperty;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;

import edu.ucsb.eucalyptus.msgs.ComponentProperty;

/**
 * A storage manager for mock-testing the BlockStorageController interface. Does no-ops for all operations, so they return with success
 */
@StorageManagerProperty("no-op")
public class ProbabalisticFailureStorageManager implements LogicalStorageManager {

  @Override
  public void initialize() throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public void configure() throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public void checkPreconditions() throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public void reload() {
    // TODO Auto-generated method stub

  }

  @Override
  public void startupChecks() throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public void cleanVolume(String volumeId) {
    // TODO Auto-generated method stub

  }

  @Override
  public void cleanSnapshot(String snapshotId, String snapshotPointId) {
    // TODO Auto-generated method stub

  }

  @Override
  public void createSnapshot(String volumeId, String snapshotId, String snapshotPointId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub
  }

  @Override
  public List<String> prepareForTransfer(String snapshotId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void createVolume(String volumeId, int size) throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public int createVolume(String volumeId, String snapshotId, int size) throws EucalyptusCloudException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int resizeVolume(String volumeId, int size) throws EucalyptusCloudException {
    return -1;
  }

  @Override
  public void cloneVolume(String volumeId, String parentVolumeId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public void addSnapshot(String snapshotId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public void deleteVolume(String volumeId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public void deleteSnapshot(String snapshotId, String snapshotPointId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public String getVolumeConnectionString(String volumeId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void loadSnapshots(List<String> snapshotSet, List<String> snapshotFileNames) throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public int getSnapshotSize(String snapshotId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void finishVolume(String snapshotId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public StorageResourceWithCallback prepSnapshotForDownload(String snapshotId, int sizeExpected, long actualSizeInMB)
      throws EucalyptusCloudException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ArrayList<ComponentProperty> getStorageProps() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setStorageProps(ArrayList<ComponentProperty> storageParams) {
    // TODO Auto-generated method stub

  }

  @Override
  public String getStorageRootDirectory() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getVolumePath(String volumeId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void importVolume(String volumeId, String volumePath, int size) throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public String getSnapshotPath(String snapshotId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void importSnapshot(String snapshotId, String snapPath, String volumeId, int size) throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public String exportVolume(String volumeId, String nodeIqn) throws EucalyptusCloudException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void unexportVolume(String volumeId, String nodeIqn) throws EucalyptusCloudException, UnsupportedOperationException {
    // TODO Auto-generated method stub

  }

  @Override
  public void unexportVolumeFromAll(String volumeId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public String createSnapshotPoint(String parentVolumeId, String volumeId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void deleteSnapshotPoint(String parentVolumeId, String volumeId, String snapshotPointId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public void checkReady() throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public void stop() throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public void enable() throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public void disable() throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean getFromBackend(String snapshotId, int size) throws EucalyptusCloudException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void checkVolume(String volumeId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public List<CheckerTask> getCheckers() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean supportsIncrementalSnapshots() throws EucalyptusCloudException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public StorageResourceWithCallback prepIncrementalSnapshotForUpload(String volumeId, String snapshotId, String snapPointId, String prevSnapshotId,
      String prevSnapPointId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public StorageResource prepSnapshotForUpload(String volumeId, String snapshotId, String snapPointId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public StorageResourceWithCallback prepSnapshotBaseForRestore(String snapshotId, int size, String snapshotPointId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void restoreSnapshotDelta(String currentSnapId, String prevSnapId, String baseId, StorageResource sr) throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public <F, T> T executeCallback(Function<F, T> callback, F input) throws EucalyptusCloudException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void completeSnapshotRestorationFromDeltas(String snapshotId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }
}
