/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009 Ent. Services Development Corporation LP
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
/*
 *
 * Author: Neil Soman neil@eucalyptus.com
 */

package com.eucalyptus.blockstorage.san.common;

import java.util.ArrayList;
import java.util.List;

import com.eucalyptus.blockstorage.StorageResource;
import com.eucalyptus.blockstorage.exceptions.ConnectionInfoNotFoundException;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.msgs.ComponentProperty;

/**
 * SANProvider is the interface that all SAN plugins must extend in order for the Storage Controller to interact with the SAN device.
 * 
 * @author zhill
 *
 */
public interface SANProvider {

  /**
   * Initialize database entities that might be used by/required for configuring the storage provider
   * 
   * @throws ConnectionInfoNotFoundException
   * @throws EucalyptusCloudException
   */
  public void initialize() throws EucalyptusCloudException;

  /**
   * Configure the provider. This is typically called during initialization of the system but may be called repeatedly, so it should be idempotent.
   * 
   * @throws EucalyptusCloudException
   */
  public void configure() throws EucalyptusCloudException;

  /**
   * Check the connection from the SC to the SAN device itself. Ensure that the connection is open and valid.
   */
  public void checkConnection() throws EucalyptusCloudException;

  /**
   * Create a volume with the specified volumeId from the source snapshot (snapshotId), which may already exist on the device or not.
   * 
   * @param volumeId - The volume id that the created volume should have
   * @param snapshotId - The id of the source snapshot from which to create the volume
   * @param snapSize - The size of the snapshot in gigabytes
   * @param size - The size of the volume to be created (may be larger than the snapshot size)
   * @param snapshotIqn - The IQN of source snapshot from which to create the volume
   * @return - The string that is the iqn that a client can connect to
   * @throws EucalyptusCloudException
   */
  public String createVolume(String volumeId, String snapshotId, int snapSize, int size, String snapshotIqn) throws EucalyptusCloudException;

  /**
   * Clone existing parent volume identified by parentVolumeId into a new volume with name/id volumeId
   * 
   * @param volumeId
   * @param parentVolumeId
   * @param parentVolumeIqn
   * @return - The string that is the iqn/connection string that a client can connect to
   * @throws EucalyptusCloudException
   */
  public String cloneVolume(String volumeId, String parentVolumeId, String parentVolumeIqn) throws EucalyptusCloudException;

  /**
   * Change the size of a volume.
   *
   * @param volumeId The volume to change
   * @param size The new size for the volume in gigabytes (-1 to get current size only)
   * @return The current or updated size in gigabytes
   * @throws EucalyptusCloudException If the volume resize fails
   */
  public int resizeVolume(String volumeId, String volumeIqn, int size) throws EucalyptusCloudException;

  /**
   * Connect to the lun specified by the given iqn.
   * 
   * <p>
   * This method may invoke connect iSCSI script on SC in which case the call out to the script should be <strong>synchronized</strong>. This is to
   * ensure clean bring up and take down of iSCSI sessions (and avoid session rescans during the process).
   * </p>
   * 
   * <p>
   * See {@link #disconnectTarget(String, String, String)}
   * </p>
   * 
   * @param iqn
   * @param lun
   * @return
   * @throws EucalyptusCloudException
   */
  public StorageResource connectTarget(String iqn, String lun) throws EucalyptusCloudException;

  /**
   * Returns a string that contains a list of volume metadata concatanated together. The returned string has the format: <san host>,<volume
   * iqn>,<encrypted, using the NC public credential, volume's target password>
   * 
   * This should be all required information for the node controller to connect to volume resource on the SAN
   * 
   * @param volumeId
   * @return A string of the format <san host>,<volume iqn>,<volume target password encrypted with NC public key>
   */
  public String getVolumeConnectionString(String volumeId);

  /**
   * Creates a volume with the given name and size on the underlying storage device.
   * 
   * @param volumeName
   * @param size
   * @return The string target iqn of the created volume
   * @throws EucalyptusCloudException
   */
  public String createVolume(String volumeName, int size) throws EucalyptusCloudException;

  /**
   * Delete the specified volume on the underlying device
   * 
   * @param volumeName
   * @param volumeIqn
   * @return boolean result of the delete attempt
   */
  public boolean deleteVolume(String volumeName, String volumeIqn);

  /**
   * Creates a snapshot of the specified volume and gives it the specified snapshot Id
   * 
   * @param volumeId - source volumeId to use, the volumeId as assigned by the CLC
   * @param snapshotId - the CLC-designated name to give the snapshot.
   * @param snapshotPointId - if non-null, this specifies the snap point id to start the operation from rather than the source volume itself. This id
   *        is opaque.
   * @return A string iqn for the newly created snapshot where the string is of the form: <SAN iqn>,lunid example: iqn-xxxx,1
   * @throws EucalyptusCloudException
   */
  public String createSnapshot(String volumeId, String snapshotId, String snapshotPointId) throws EucalyptusCloudException;

  /**
   * Delete a snapshot
   * 
   * @param snapshotId
   * @param snapshotIqn
   * @param snapshotPointId
   * @return
   */
  public boolean deleteSnapshot(String snapshotId, String snapshotIqn, String snapshotPointId);

  /**
   * Delete the specified CHAP username from the device
   * 
   * @param userName
   * @throws EucalyptusCloudException
   */
  public void deleteUser(String userName) throws EucalyptusCloudException;

  /**
   * Add a new CHAP user to the device.
   * 
   * @param userName
   */
  public void addUser(String userName) throws EucalyptusCloudException;

  /**
   * Disconnects the SC from the specified snapshot. The iqn is that of the SC itself.
   * 
   * <p>
   * This method may invoke disconnect iSCSI script on SC in which case the call out to the script should be <strong>synchronized</strong>. This is to
   * ensure clean bring up and take down of iSCSI sessions (and avoid session rescans during the process).
   * </p>
   * 
   * <p>
   * See {@link #connectTarget(String, String)}
   * </p>
   * 
   * @param snapshotId
   * @param iqn
   * @param lun
   * @throws EucalyptusCloudException
   */
  public void disconnectTarget(String snapshotId, String iqn, String lun) throws EucalyptusCloudException;

  public void checkPreconditions() throws EucalyptusCloudException;

  /**
   * Adds the initiator rule and sets up access so that the listed nodeIqns can attach the specified volume. Returns the LUN of the volume.
   * Effectively sets up the target side so that nodes can attach to the iscsi lun
   * 
   * @param volumeId
   * @param nodeIqn
   * @param volumeIqn
   * @return String id of the lun exported
   * @throws EucalyptusCloudException
   */
  public String exportResource(String volumeId, String nodeIqn, String volumeIqn) throws EucalyptusCloudException;

  /**
   * Removes the node permission for the volume for the specified iqn. After this operation a node should not be able to connect to the volume
   * 
   * @param volumeId
   * @param nodeIqn
   * @throws EucalyptusCloudException
   */
  public void unexportResource(String volumeId, String nodeIqn) throws EucalyptusCloudException;

  /**
   * Removes permission for the volume for all hosts. After this operation no node should be able to connect to the volume
   * 
   * @param volumeId
   * @param nodeIqn
   * @throws EucalyptusCloudException
   */
  public void unexportResourceFromAll(String volumeId) throws EucalyptusCloudException;

  public void getStorageProps(ArrayList<ComponentProperty> componentProperties);

  public void setStorageProps(ArrayList<ComponentProperty> storageProps);

  public void stop() throws EucalyptusCloudException;

  /**
   * Returns the required authentication type to access resources on the underlying device. This is tyepically a mode as specified in
   * SANProperties.IscsiAuthType
   * 
   * @return
   */
  public String getAuthType();

  /**
   * Not used by NetApp or Equallogic
   * 
   * @return
   */
  public String getOptionalChapUser();

  /**
   * Creates a volume for holding a snapshot that does not exist on the SAN. Snapshot is probably downloaded (from ObjectStorage) and written to the
   * newly created volume there by making it available to the SAN
   * 
   * @param snapshotId
   * @param snapSizeInMB
   * @return A string iqn for the newly created volume for holding the snapshot
   * @throws EucalyptusCloudException
   */
  public String createSnapshotHolder(String snapshotId, long snapSizeInMB) throws EucalyptusCloudException;

  /**
   * Checks for the snapshot on the SAN backend and returns true or false accordingly
   * 
   * @param snapshotId
   * @param snapshotIqn
   * @return True if the volume exists on the SAN and false if it does not
   * @throws EucalyptusCloudException
   */
  public boolean snapshotExists(String snapshotId, String snapshotIqn) throws EucalyptusCloudException;

  /**
   * Creates a snapshot point only. This does not do a transfer or ensure that the snapshot is fully independent from the source volume. This method
   * is intended to be very fast and can be called synchronously from the CLC request.
   * 
   * @param parentVolumeIqn
   * @param snapshotId
   * 
   * @throws EucalyptusCloudException
   */
  public String createSnapshotPoint(String parentVolumeId, String volumeId, String parentVolumeIqn) throws EucalyptusCloudException;

  /**
   * Delete the created snapshot point, not the entire snapshot lun
   * 
   * @param parentVolumeId
   * @param snapshotPointId
   * @param parentVolumeIqn
   * @throws EucalyptusCloudException
   */
  public void deleteSnapshotPoint(String parentVolumeId, String snapshotPointId, String parentVolumeIqn) throws EucalyptusCloudException;

  public void checkConnectionInfo() throws EucalyptusCloudException;

  /**
   * Checks for the snapshot on the SAN backend and returns true or false accordingly
   * 
   * @param volumeId
   * @param volumeIqn
   * 
   * @return True if the volume exists on the SAN and false if it does not
   * @throws EucalyptusCloudException
   */
  public boolean volumeExists(String volumeId, String volumeIqn) throws EucalyptusCloudException;

  /**
   * Returns the protocol to be used for data transfers with the storage backend
   * 
   * @return string representing protocol such as iscsi or rbd
   */
  public String getProtocol();

  /**
   * 
   * @return string representing block storage provider name
   */
  public String getProviderName();

  /**
   * Check if the snapshot is in progress and wait for it to complete before returning back to caller
   * 
   * @param snapshotId
   * @param snapshotIqn
   * @throws EucalyptusCloudException
   */
  public void waitAndComplete(String snapshotId, String snapshotIqn) throws EucalyptusCloudException;

  /**
   * 
   * @return A list of tasks that the provider needs to run periodically
   */
  public List<CheckerTask> getCheckers();

  /**
   * 
   * @return
   * @throws EucalyptusCloudException
   */
  public boolean supportsIncrementalSnapshots() throws EucalyptusCloudException;

  public StorageResource generateSnapshotDelta(String volumeId, String snapshotId, String snapPointId, String prevSnapshotId, String prevSnapPointId)
      throws EucalyptusCloudException;

  public void cleanupSnapshotDelta(String snapshotId, StorageResource sr) throws EucalyptusCloudException;

  public String completeSnapshotBaseRestoration(String snapshotId, String snapshotPointId, String snapshotIqn) throws EucalyptusCloudException;

  public void restoreSnapshotDelta(String baseIqn, StorageResource sr) throws EucalyptusCloudException;

  public void completeSnapshotDeltaRestoration(String snapshotId, String snapshotIqn) throws EucalyptusCloudException;
}
