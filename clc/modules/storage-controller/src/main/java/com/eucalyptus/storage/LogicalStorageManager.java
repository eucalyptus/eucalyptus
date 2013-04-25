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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
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

	public void startupChecks() throws EucalyptusCloudException;

	public void cleanVolume(String volumeId);

	public void cleanSnapshot(String snapshotId);

	/**
	 * If snapshotPointId == null, then create full snapshot, if != null then use snapPointId as starting point and complete snapshot creation
	 * @param volumeId
	 * @param snapshotId
	 * @param snapshotPointId - The opaque id used to identify a snap point on the given backend. This is backend-specific and not universal.
	 * @param shouldTransferSnapshots
	 * @return
	 * @throws EucalyptusCloudException
	 */
	public List<String> createSnapshot(String volumeId, String snapshotId, String snapshotPointId, Boolean shouldTransferSnapshots) throws EucalyptusCloudException;

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

	public String prepareSnapshot(String snapshotId, int sizeExpected, long actualSizeInMB) throws EucalyptusCloudException;

	public ArrayList<ComponentProperty> getStorageProps();

	public void setStorageProps(ArrayList<ComponentProperty> storageParams);

	public String getStorageRootDirectory();

	public String getVolumePath(String volumeId) throws EucalyptusCloudException;

	public void importVolume(String volumeId, String volumePath, int size) throws EucalyptusCloudException;

	public String getSnapshotPath(String snapshotId) throws EucalyptusCloudException;

	public void importSnapshot(String snapshotId, String snapPath, String volumeId, int size) throws EucalyptusCloudException;

	public String attachVolume(String volumeId, List<String> nodeIqns) throws EucalyptusCloudException;

	public void detachVolume(String volumeId, String nodeIqn) throws EucalyptusCloudException;

	/* Added to allow synchronous snapshot setting */
	/**
	 * Should return either the id of the snapshot point created, or null if the point was not created.
	 * @param parentVolumeId
	 * @param volumeId
	 * @return
	 * @throws EucalyptusCloudException
	 */
	public String createSnapshotPoint(String parentVolumeId, String volumeId) throws EucalyptusCloudException;
	
	/**
	 * Deletes the specified snapshot point
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
}
