/*************************************************************************
 * Copyright 2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.blockstorage;

import java.util.ArrayList;
import java.util.List;

import com.eucalyptus.blockstorage.LogicalStorageManager;
import com.eucalyptus.blockstorage.StorageManagers.StorageManagerProperty;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.msgs.ComponentProperty;

/**
 * A storage manager for mock-testing the BlockStorageController interface.
 * Does no-ops for all operations, so they return with success
 */
@StorageManagerProperty("no-op")
public class ProbabalisticFailureStorageManager implements LogicalStorageManager{

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
	public void cleanSnapshot(String snapshotId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public StorageResource createSnapshot(String volumeId, String snapshotId,
			String snapshotPointId, Boolean shouldTransferSnapshots)
			throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> prepareForTransfer(String snapshotId)
			throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createVolume(String volumeId, int size)
			throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int createVolume(String volumeId, String snapshotId, int size)
			throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void cloneVolume(String volumeId, String parentVolumeId)
			throws EucalyptusCloudException {
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
	public void deleteSnapshot(String snapshotId)
			throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getVolumeConnectionString(String volumeId)
			throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void loadSnapshots(List<String> snapshotSet,
			List<String> snapshotFileNames) throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getSnapshotSize(String snapshotId)
			throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void finishVolume(String snapshotId) throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public StorageResource prepareSnapshot(String snapshotId, int sizeExpected,
			long actualSizeInMB) throws EucalyptusCloudException {
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
	public String getVolumePath(String volumeId)
			throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void importVolume(String volumeId, String volumePath, int size)
			throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getSnapshotPath(String snapshotId)
			throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void importSnapshot(String snapshotId, String snapPath,
			String volumeId, int size) throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String exportVolume(String volumeId, String nodeIqn)
			throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unexportVolume(String volumeId, String nodeIqn)
			throws EucalyptusCloudException, UnsupportedOperationException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unexportVolumeFromAll(String volumeId)
			throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String createSnapshotPoint(String parentVolumeId, String volumeId)
			throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteSnapshotPoint(String parentVolumeId, String volumeId,
			String snapshotPointId) throws EucalyptusCloudException {
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
	public boolean getFromBackend(String snapshotId, int size)
			throws EucalyptusCloudException {
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

}
