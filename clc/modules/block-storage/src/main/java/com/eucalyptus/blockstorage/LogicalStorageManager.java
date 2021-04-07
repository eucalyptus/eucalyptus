/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.blockstorage;

import java.util.ArrayList;
import java.util.List;

import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;

import edu.ucsb.eucalyptus.msgs.ComponentProperty;

/**
 * This is the interface that ALL backend block storage managers must implement. To make a manager configurable by the admin/user the class should be
 * annotated with:
 * 
 * @StorageManagerProperty(name) annotation where 'name' is the string value that admins will use to enable that manager for a specific partition.
 *
 */
public interface LogicalStorageManager {
  public void initialize() throws EucalyptusCloudException;

  public void configure() throws EucalyptusCloudException;

  public void checkPreconditions() throws EucalyptusCloudException;

  public void reload();

  public void startupChecks() throws EucalyptusCloudException;

  public void cleanVolume(String volumeId);

  public void cleanSnapshot(String snapshotId, String snapshotPointId);

  /**
   * If snapshotPointId == null, then create full snapshot, if != null then use snapPointId as starting point and complete snapshot creation
   * 
   * TODO use another method to check for snapshot completion
   * 
   * @param volumeId
   * @param snapshotId
   * @param snapshotPointId - The opaque id used to identify a snap point on the given backend. This is backend-specific and not universal.
   * @throws EucalyptusCloudException
   */
  public void createSnapshot(String volumeId, String snapshotId, String snapshotPointId) throws EucalyptusCloudException;

  public List<String> prepareForTransfer(String snapshotId) throws EucalyptusCloudException;

  public void createVolume(String volumeId, int size) throws EucalyptusCloudException;

  public int createVolume(String volumeId, String snapshotId, int size) throws EucalyptusCloudException;

  public int resizeVolume(String volumeId, int size) throws EucalyptusCloudException;

  public void cloneVolume(String volumeId, String parentVolumeId) throws EucalyptusCloudException;

  public void addSnapshot(String snapshotId) throws EucalyptusCloudException;

  public void deleteVolume(String volumeId) throws EucalyptusCloudException;

  public void deleteSnapshot(String snapshotId, String snapshotPointId) throws EucalyptusCloudException;

  /**
   * Gets the connection string for the volume. This is no longer used. Connection string should be returned on the attach call since it may be
   * specific for a given node iqn.
   * 
   * @param volumeId
   * @return
   * @throws EucalyptusCloudException
   */
  public String getVolumeConnectionString(String volumeId) throws EucalyptusCloudException;

  public void loadSnapshots(List<String> snapshotSet, List<String> snapshotFileNames) throws EucalyptusCloudException;

  public int getSnapshotSize(String snapshotId) throws EucalyptusCloudException;

  public void finishVolume(String snapshotId) throws EucalyptusCloudException;

  public ArrayList<ComponentProperty> getStorageProps();

  public void setStorageProps(ArrayList<ComponentProperty> storageParams);

  public String getStorageRootDirectory();

  public String getVolumePath(String volumeId) throws EucalyptusCloudException;

  public void importVolume(String volumeId, String volumePath, int size) throws EucalyptusCloudException;

  public String getSnapshotPath(String snapshotId) throws EucalyptusCloudException;

  public void importSnapshot(String snapshotId, String snapPath, String volumeId, int size) throws EucalyptusCloudException;

  /**
   * Authorize the specified iqn to access the volume. A client must be able to connect using the returned string from this method. The string should
   * be opaque to all but the NC This MUST be idempotent and synchronous. Upon return a client should be able to view/connect to the volume.
   * 
   * @param volumeId
   * @param nodeIqn
   * @return
   * @throws EucalyptusCloudException
   */
  public String exportVolume(String volumeId, String nodeIqn) throws EucalyptusCloudException;

  /**
   * Remove authorization/export status for the specified iqn to the specified volume. This MUST be idempotent and synchronous. Upon return the
   * process should be done such that if the client rescans or refreshes the view the volume will no longer be visible to it.
   * 
   * @param volumeId
   * @param nodeIqn
   * @throws EucalyptusCloudException
   */
  public void unexportVolume(String volumeId, String nodeIqn) throws EucalyptusCloudException, UnsupportedOperationException;

  /**
   * Same as unexportVolume but should remove authorization for all clients. This should be used to enforce a final state on the volume. This MUST be
   * idempotent and should be synchronous
   * 
   * @param volumeId
   * @throws EucalyptusCloudException
   */
  public void unexportVolumeFromAll(String volumeId) throws EucalyptusCloudException;

  /* Added to allow synchronous snapshot setting */
  /**
   * Should return either the id of the snapshot point created, or null if the point was not created.
   * 
   * @param parentVolumeId
   * @param volumeId
   * @return
   * @throws EucalyptusCloudException
   */
  public String createSnapshotPoint(String parentVolumeId, String volumeId) throws EucalyptusCloudException;

  /**
   * Deletes the specified snapshot point
   * 
   * @param parentVolumeId
   * @param volumeId
   * @throws EucalyptusCloudException
   */
  public void deleteSnapshotPoint(String parentVolumeId, String volumeId, String snapshotPointId) throws EucalyptusCloudException;

  public void checkReady() throws EucalyptusCloudException;

  public void stop() throws EucalyptusCloudException;

  public void enable() throws EucalyptusCloudException;

  public void disable() throws EucalyptusCloudException;

  public boolean getFromBackend(String snapshotId, int size) throws EucalyptusCloudException;

  public void checkVolume(String volumeId) throws EucalyptusCloudException;

  public List<CheckerTask> getCheckers();

  /**
   * 
   * @return
   * @throws EucalyptusCloudException
   */
  public boolean supportsIncrementalSnapshots() throws EucalyptusCloudException;

  public StorageResourceWithCallback prepIncrementalSnapshotForUpload(String volumeId, String snapshotId, String snapPointId, String prevSnapshotId,
      String prevSnapPointId) throws EucalyptusCloudException;

  public StorageResource prepSnapshotForUpload(String volumeId, String snapshotId, String snapPointId) throws EucalyptusCloudException;

  public StorageResourceWithCallback prepSnapshotForDownload(String snapshotId, int sizeExpected, long actualSizeInMB)
      throws EucalyptusCloudException;

  public StorageResourceWithCallback prepSnapshotBaseForRestore(String snapshotId, int size, String snapshotPointId) throws EucalyptusCloudException;

  public void restoreSnapshotDelta(String currentSnapId, String prevSnapId, String baseId, StorageResource sr) throws EucalyptusCloudException;

  public void completeSnapshotRestorationFromDeltas(String snapshotId) throws EucalyptusCloudException;

  public <F, T> T executeCallback(Function<F, T> callback, F input) throws EucalyptusCloudException;

}
