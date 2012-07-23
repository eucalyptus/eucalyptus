/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.storage;

import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.msgs.ComponentProperty;


import java.util.ArrayList;
import java.util.List;

public interface LogicalStorageManager {
	public void initialize() throws EucalyptusCloudException;

	public void configure() throws EucalyptusCloudException;

	public void checkPreconditions() throws EucalyptusCloudException;

	public void reload();

	public void startupChecks();

	public void cleanVolume(String volumeId);

	public void cleanSnapshot(String snapshotId);

	public List<String> createSnapshot(String volumeId, String snapshotId, Boolean shouldTransferSnapshots) throws EucalyptusCloudException;

	public List<String> prepareForTransfer(String snapshotId) throws EucalyptusCloudException;

	public void createVolume(String volumeId, int size) throws EucalyptusCloudException;

	public int createVolume(String volumeId, String snapshotId, int size) throws EucalyptusCloudException;

	public void cloneVolume(String volumeId, String parentVolumeId) throws EucalyptusCloudException;

	public void addSnapshot(String snapshotId) throws EucalyptusCloudException;

	public void deleteVolume(String volumeId) throws EucalyptusCloudException;

	public void deleteSnapshot(String snapshotId) throws EucalyptusCloudException;

	public String getVolumeProperty(String volumeId) throws EucalyptusCloudException;

	public void loadSnapshots(List<String> snapshotSet, List<String> snapshotFileNames) throws EucalyptusCloudException;

	public int getSnapshotSize(String snapshotId) throws EucalyptusCloudException;

	public void finishVolume(String snapshotId) throws EucalyptusCloudException;

	public String prepareSnapshot(String snapshotId, int sizeExpected) throws EucalyptusCloudException;

	public ArrayList<ComponentProperty> getStorageProps();

	public void setStorageProps(ArrayList<ComponentProperty> storageParams);

	public String getStorageRootDirectory();
	
	public String getVolumePath(String volumeId) throws EucalyptusCloudException;

	public void importVolume(String volumeId, String volumePath, int size) throws EucalyptusCloudException;

	public String getSnapshotPath(String snapshotId) throws EucalyptusCloudException;

	public void importSnapshot(String snapshotId, String snapPath, String volumeId, int size) throws EucalyptusCloudException;

	public String attachVolume(String volumeId, List<String> nodeIqns) throws EucalyptusCloudException;

	public void detachVolume(String volumeId, String nodeIqn) throws EucalyptusCloudException;

	public void checkReady() throws EucalyptusCloudException;

	public void stop() throws EucalyptusCloudException;

	public void enable() throws EucalyptusCloudException;

	public void disable() throws EucalyptusCloudException;

	public boolean getFromBackend(String snapshotId) throws EucalyptusCloudException;
}
