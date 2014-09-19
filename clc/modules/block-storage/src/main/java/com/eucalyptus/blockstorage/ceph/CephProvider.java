/*******************************************************************************
 *Copyright (c) 2014  Eucalyptus Systems, Inc.
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

package com.eucalyptus.blockstorage.ceph;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ceph.rbd.Rbd;
import com.eucalyptus.blockstorage.StorageManagers.StorageManagerProperty;
import com.eucalyptus.blockstorage.StorageResource;
import com.eucalyptus.blockstorage.ceph.entities.CephInfo;
import com.eucalyptus.blockstorage.ceph.exceptions.EucalyptusCephException;
import com.eucalyptus.blockstorage.san.common.SANManager;
import com.eucalyptus.blockstorage.san.common.SANProvider;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.msgs.ComponentProperty;

/**
 * CephProvider implements the Eucalyptus Storage Controller plug-in for interacting with a Ceph cluster
 * 
 */
@StorageManagerProperty(value = "ceph-rbd", manager = SANManager.class)
public class CephProvider implements SANProvider {

	private static final Logger LOG = Logger.getLogger(CephProvider.class);

	private EucaRbd rbdService;
	private String configFile;
	private String user;
	private String volumePools;
	private String snapshotPools;

	@Override
	public void initialize() {
		// Create and persist ceph info entity if its not already there
		LOG.info("Initializing CephInfo entity");
		CephInfo.getStorageInfo();
	}

	@Override
	public void configure() throws EucalyptusCloudException {
		CephInfo cephInfo = CephInfo.getStorageInfo();
		initializeRbdProvider(cephInfo);
	}

	private void initializeRbdProvider(CephInfo info) {
		LOG.info("Initializing Ceph RBD service provider");

		configFile = info.getCephConfigFile();
		user = info.getCephUser();
		volumePools = info.getCephVolumePools();
		snapshotPools = info.getCephSnapshotPools();

		if (rbdService == null) {
			rbdService = new EucaRbdFormatTwoImpl(info);
		} else {
			// Changing the configuration in the existing reference rather than instantiating a new object as that might end up interrupting an already existing
			// operation
			rbdService.setCephConfig(info);
		}
	}

	@Override
	public void checkConnection() throws EucalyptusCloudException {
		CephInfo localInfo = CephInfo.getStorageInfo();
		if (localInfo != null && !isSame(localInfo)) {
			LOG.info("Detected a change in Ceph configuration");
			initializeRbdProvider(localInfo);
		} else {
			// Nothing to do here
		}

		// TODO Some way to check connectivity to ceph cluster
	}

	private boolean isSame(CephInfo info) {
		return StringUtils.equals(configFile, info.getCephConfigFile()) && StringUtils.equals(user, info.getCephUser())
				&& StringUtils.equals(volumePools, info.getCephVolumePools()) && StringUtils.equals(snapshotPools, info.getCephSnapshotPools());
	}

	@Override
	public String createVolume(String volumeId, String snapshotId, int snapSize, int size) throws EucalyptusCloudException {
		String iqn = null;
		if (size > snapSize) {
			iqn = rbdService.cloneAndResizeImage(snapshotId, null, volumeId, Long.valueOf(size * StorageProperties.GB));
		} else {
			iqn = rbdService.cloneAndResizeImage(snapshotId, null, volumeId, null);
		}
		return iqn;
	}

	@Override
	public String cloneVolume(String volumeId, String parentVolumeId) throws EucalyptusCloudException {
		String iqn = rbdService.cloneAndResizeImage(parentVolumeId, null, volumeId, null);
		return iqn;
	}

	@Override
	public StorageResource connectTarget(String iqn) throws EucalyptusCloudException {
		// iqn here is in the format pool/image or pool/image,pool/image
		// SANManager changes the ID, so dont bother setting the volume ID (first parameter) here
		if (iqn.contains(",")) {
			String[] parts = iqn.split(",");
			return new CephImageResource(parts[0], parts[0]);
		} else {
			return new CephImageResource(iqn, iqn);
		}
	}

	@Override
	public String getVolumeConnectionString(String volumeId) {
		return CephInfo.getStorageInfo().getVirshSecret() + ","; // <virsh secret uuid>,<empty path>
	}

	@Override
	public String createVolume(String volumeName, int size) throws EucalyptusCloudException {
		long sizeInBytes = size * StorageProperties.GB; // need to go from gb to bytes
		String iqn = rbdService.createImage(volumeName, sizeInBytes);
		return iqn;
	}

	@Override
	public boolean deleteVolume(String volumeName) {
		try {
			rbdService.deleteImage(volumeName);
		} catch (EucalyptusCephException e) {
			return false;
		}
		return true;
	}

	@Override
	public String createSnapshot(String volumeId, String snapshotId, String snapshotPointId) throws EucalyptusCloudException {
		String iqn = rbdService.cloneAndResizeImage(volumeId, snapshotPointId, snapshotId, null);
		return iqn;
	}

	@Override
	public boolean deleteSnapshot(String volumeId, String snapshotId, boolean locallyCreated) {
		try {
			rbdService.deleteImage(snapshotId);
		} catch (EucalyptusCephException e) {
			return false;
		}
		return true;
	}

	@Override
	public void deleteUser(String userName) throws EucalyptusCloudException {

	}

	@Override
	public void addUser(String userName) throws EucalyptusCloudException {

	}

	@Override
	public void disconnectTarget(String snapshotId, String iqn) throws EucalyptusCloudException {

	}

	@Override
	public void checkPreconditions() throws EucalyptusCloudException {
		// If librbd is not installed, things don't get this far. The classloader tries to load Rbd JNA bindings which statically invoke librbd and things go
		// spiralling downward from there
		try {
			int[] version = Rbd.getVersion();
			if (version != null && version.length == 3) {
				LOG.info("librbd version: " + new StringBuffer().append(version[0]).append('.').append(version[1]).append('.').append(version[2]).toString());
			} else {
				throw new EucalyptusCloudException("Invalid librbd version info");
			}
		} catch (Exception e) {
			LOG.warn("librbd version not found, librbd may not be installed!");
			throw new EucalyptusCloudException("librbd version not found, librbd may not be installed!", e);
		}
	}

	// TODO post-nc-changes replace this with the commented method below after changing the method signature in SANProvider interface
	@Override
	public Integer addInitiatorRule(String volumeId, String nodeIqn) throws EucalyptusCloudException {
		return -1;
	}

	// @Override
	// public String addInitiatorRule(String volumeId, String nodeIqn) throws EucalyptusCloudException {
	// try {
	// String pool = rbdService.getImagePool(volumeId);
	// return pool + '/' + volumeId;
	// } catch (Exception e) {
	// throw new EucalyptusCloudException("Unable to export " + volumeId, e);
	// }
	// }

	@Override
	public void removeInitiatorRule(String volumeId, String nodeIqn) throws EucalyptusCloudException {

	}

	@Override
	public void removeAllInitiatorRules(String volumeId) throws EucalyptusCloudException {

	}

	@Override
	public void getStorageProps(ArrayList<ComponentProperty> componentProperties) {
		// TODO
	}

	@Override
	public void setStorageProps(ArrayList<ComponentProperty> storageProps) {
		// TODO
	}

	@Override
	public void stop() throws EucalyptusCloudException {

	}

	@Override
	public String getAuthType() {
		return null;
	}

	@Override
	public String getOptionalChapUser() {
		return null;
	}

	@Override
	public String createSnapshotHolder(String snapshotId, long snapSizeInMB) throws EucalyptusCloudException {
		long sizeInBytes = snapSizeInMB * StorageProperties.MB; // need to go from mb to bytes
		String iqn = rbdService.createImage(snapshotId, sizeInBytes);
		return iqn;
	}

	@Override
	public boolean snapshotExists(String snapshotId) throws EucalyptusCloudException {
		return volumeExists(snapshotId);
	}

	@Override
	public String createSnapshotPoint(String parentVolumeId, String volumeId) throws EucalyptusCloudException {
		String snapshotPoint = "sp-" + volumeId;
		rbdService.createSnapshot(parentVolumeId, snapshotPoint);
		return snapshotPoint;
	}

	@Override
	public void deleteSnapshotPoint(String parentVolumeId, String snapshotPointId) throws EucalyptusCloudException {
		rbdService.deleteSnapshot(parentVolumeId, snapshotPointId);
	}

	@Override
	public boolean checkSANCredentialsExist() {
		return true;
	}

	@Override
	public boolean volumeExists(String volumeId) throws EucalyptusCloudException {
		try {
			if (null != rbdService.getImagePool(volumeId)) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			LOG.debug("Caught error in check for " + volumeId, e);
			return false;
		}
	}

	// TODO post-nc-changes uncomment these methods after making changes in SAN Provider
	// @Override
	// public String getProtocol() {
	// return "rbd";
	// }
	//
	// @Override
	// public String getProviderName() {
	// return "ceph";
	// }
}
