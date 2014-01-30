/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 *
 * Author: Neil Soman neil@eucalyptus.com
 */

package com.eucalyptus.blockstorage.san.common;

import java.util.ArrayList;

import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.msgs.ComponentProperty;

/**
 * SANProvider is the interface that all SAN plugins must extend in order for the Storage Controller to interact with the SAN device.
 * @author zhill
 *
 */
public interface SANProvider {

	/**
	 * Configure the provider. This is typically called during initialization of the system but may be called repeatedly, so it should be idempotent.
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
	 * @return - The string that is the iqn that a client can connect to
	 * @throws EucalyptusCloudException
	 */
	public String createVolume(String volumeId, String snapshotId, int snapSize, int size) throws EucalyptusCloudException;

	/**
	 * Clone existing parent volume identified by parentVolumeId into a new volume with name/id volumeId
	 * @param volumeId
	 * @param parentVolumeId
	 * @return - The string that is the iqn/connection string that a client can connect to
	 * @throws EucalyptusCloudException
	 */
	public String cloneVolume(String volumeId, String parentVolumeId) throws EucalyptusCloudException;

	/**
	 * Connect to the lun specified by the given iqn. 
	 * 
	 * <p>This method may invoke connect iSCSI script on SC in which case the call out to the script should be <strong>synchronized</strong>. 
	 * This is to ensure clean bring up and take down of iSCSI sessions (and avoid session rescans during the process).</p>
	 *  
	 * <p>See {@link #disconnectTarget(String, String)}</p>
	 * 
	 * @param iqn
	 * @return
	 * @throws EucalyptusCloudException
	 */
	public String connectTarget(String iqn) throws EucalyptusCloudException;

	/**
	 * Returns a string that contains a list of volume metadata concatanated together. The returned string has the format:
	 * <san host>,<volume iqn>,<encrypted, using the NC public credential, volume's target password>
	 * 
	 * This should be all required information for the node controller to connect to volume resource on the SAN
	 * 
	 * @param volumeId
	 * @return A string of the format <san host>,<volume iqn>,<volume target password encrypted with NC public key>
	 */
	public String getVolumeConnectionString(String volumeId);

	/**
	 * Creates a volume with the given name and size on the underlying storage device.
	 * @param volumeName
	 * @param size
	 * @return The string target iqn of the created volume
	 * @throws EucalyptusCloudException
	 */
	public String createVolume(String volumeName, int size) throws EucalyptusCloudException;

	/**
	 * Delete the specified volume on the underlying device
	 * @param volumeName
	 * @return boolean result of the delete attempt
	 */
	public boolean deleteVolume(String volumeName);

	/**
	 * Creates a snapshot of the specified volume and gives it the specified snapshot Id
	 * @param volumeId - source volumeId to use, the volumeId as assigned by the CLC
	 * @param snapshotId - the CLC-designated name to give the snapshot.
	 * @param snapshotPointId - if non-null, this specifies the snap point id to start the operation from rather than the source volume itself. This id is opaque.
	 * @return A string iqn for the newly created snapshot where the string is of the form: <SAN iqn>,lunid example: iqn-xxxx,1
	 * @throws EucalyptusCloudException
	 */
	public String createSnapshot(String volumeId, String snapshotId, String snapshotPointId) throws EucalyptusCloudException;

	/**
	 * Delete a snapshot -- This is never used in SANManager.
	 * @param volumeId
	 * @param snapshotId
	 * @param locallyCreated
	 * @return
	 */
	public boolean deleteSnapshot(String volumeId, String snapshotId, boolean locallyCreated);

	/**
	 * Delete the specified CHAP username from the device
	 * @param userName
	 * @throws EucalyptusCloudException
	 */
	public void deleteUser(String userName) throws EucalyptusCloudException;

	/**
	 * Add a new CHAP user to the device.
	 * @param userName
	 */
	public void addUser(String userName) throws EucalyptusCloudException;

	/**
	 * Disconnects the SC from the specified snapshot. The iqn is that of the SC itself.
	 * 
	 * <p>This method may invoke disconnect iSCSI script on SC in which case the call out to the script should be <strong>synchronized</strong>. 
	 * This is to ensure clean bring up and take down of iSCSI sessions (and avoid session rescans during the process).</p>
	 *  
	 * <p>See {@link #connectTarget(String)}</p>
	 * 
	 * @param snapshotId
	 * @param iqn
	 * @throws EucalyptusCloudException
	 */
	public void disconnectTarget(String snapshotId, String iqn) throws EucalyptusCloudException;

	public void checkPreconditions() throws EucalyptusCloudException;

	/**
	 * Adds the initiator rule and sets up access so that the listed nodeIqns can attach the specified volume. Returns the LUN of the volume.
	 * Effectively sets up the target side so that nodes can attach to the iscsi lun
	 * @param volumeId
	 * @param nodeIqn
	 * @return Integer id of the lun exported
	 * @throws EucalyptusCloudException
	 */
	public Integer addInitiatorRule(String volumeId, String nodeIqn) throws EucalyptusCloudException;

	/**
	 * Removes the node permission for the volume for the specified iqn. After this operation a node should not be able to connect to the volume
	 * @param volumeId
	 * @param nodeIqn
	 * @throws EucalyptusCloudException
	 */
	public void removeInitiatorRule(String volumeId, String nodeIqn) throws EucalyptusCloudException;

	/**
	 * Removes permission for the volume for all hosts. After this operation no node should be able to connect to the volume
	 * @param volumeId
	 * @param nodeIqn
	 * @throws EucalyptusCloudException
	 */
	public void removeAllInitiatorRules(String volumeId) throws EucalyptusCloudException;

	
	public void getStorageProps(ArrayList<ComponentProperty> componentProperties);

	public void setStorageProps(ArrayList<ComponentProperty> storageProps);

	public void stop() throws EucalyptusCloudException;
	
	/**
	 * Returns the required authentication type to access resources on the underlying device. This is tyepically a mode as specified in SANProperties.IscsiAuthType
	 * @return
	 */
	public String getAuthType();
	
	/**
	 * Not used by NetApp or Equallogic
	 * @return
	 */
	public String getOptionalChapUser();
	
	public void checkVolume(String volumeId) throws EucalyptusCloudException;

	/**
	 * Creates a volume for holding a snapshot that does not exist on the SAN. Snapshot is probably downloaded (from ObjectStorage) and written to the newly created
	 * volume there by making it available to the SAN
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
	 * @return True if the volume exists on the SAN and false if it does not
	 * @throws EucalyptusCloudException
	 */
	public boolean snapshotExists(String snapshotId) throws EucalyptusCloudException;
	
	/**
	 * Creates a snapshot point only. This does not do a transfer or ensure that the snapshot
	 * is fully independent from the source volume. This method is intended to be very fast
	 * and can be called synchronously from the CLC request.
	 * @param snapshotId
	 * @throws EucalyptusCloudException
	 */
	public String createSnapshotPoint(String parentVolumeId, String volumeId) throws EucalyptusCloudException;
	
	/**
	 * Delete the created snapshot point, not the entire snapshot lun
	 * @param snapshotId
	 * @throws EucalyptusCloudException
	 */
	public void deleteSnapshotPoint(String snapshotPointId) throws EucalyptusCloudException;

	public boolean checkSANCredentialsExist();

	/**
	 * Checks for the snapshot on the SAN backend and returns true or false accordingly
	 * 
	 * @param snapshotId
	 * @return True if the volume exists on the SAN and false if it does not
	 * @throws EucalyptusCloudException
	 */
	public boolean volumeExists(String volumeId) throws EucalyptusCloudException;
		
}

