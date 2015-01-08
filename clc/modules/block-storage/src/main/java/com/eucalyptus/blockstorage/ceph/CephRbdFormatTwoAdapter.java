/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.blockstorage.ceph;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;

import com.ceph.rbd.RbdException;
import com.ceph.rbd.RbdImage;
import com.ceph.rbd.jna.RbdSnapInfo;
import com.eucalyptus.blockstorage.ceph.entities.CephRbdInfo;
import com.eucalyptus.blockstorage.ceph.exceptions.CannotDeleteCephImageException;
import com.eucalyptus.blockstorage.ceph.exceptions.CephImageNotFoundException;
import com.eucalyptus.blockstorage.ceph.exceptions.EucalyptusCephException;
import com.google.common.base.Function;

/**
 * Eucalyptus RBD adapter for managing Ceph RBD images of type Format 2
 */
public class CephRbdFormatTwoAdapter implements CephRbdAdapter {

	private static final Logger LOG = Logger.getLogger(CephRbdFormatTwoAdapter.class);

	private CephRbdInfo config;

	public CephRbdFormatTwoAdapter(CephRbdInfo cephInfo) {
		this.config = cephInfo;
	}

	// Added this method so that a running operation using an older configuration does not get impacted.
	@Override
	public void setCephConfig(CephRbdInfo cephInfo) {
		this.config = cephInfo;
	}

	/**
	 * @return Returns Ceph representation of the image in the form: <b><code>pool/image</code></b>
	 */
	@Override
	public String createImage(final String imageName, final long imageSize) {
		return executeRbdOpInRandomPool(new Function<CephRbdConnectionManager, String>() {

			@Override
			public String apply(@Nonnull CephRbdConnectionManager arg0) {
				RbdImage image = null;
				try {
					LOG.debug("Creating image=" + imageName + ", pool=" + arg0.getPool() + ", size=" + imageSize);
					// We only want layering and format 2
					int features = (1 << 0);
					arg0.getRbd().create(imageName, imageSize, features, 0);
					LOG.trace("Opening image=" + imageName + ", pool=" + arg0.getPool() + ", mode=read-only");
					image = arg0.getRbd().openReadOnly(imageName);
					return arg0.getPool() + CephRbdInfo.POOL_IMAGE_DELIMITER + imageName;
				} catch (RbdException e) {
					LOG.warn("Caught error while creating image " + imageName + ": " + e.getMessage());
					throw new EucalyptusCephException("Failed to create image " + imageName, e);
				} finally {
					if (image != null) {
						try {
							LOG.trace("Closing image=" + imageName + ", pool=" + arg0.getPool());
							arg0.getRbd().close(image);
						} catch (Exception e) {
							LOG.debug("Caught exception closing image " + imageName + ", pool=" + arg0.getPool(), e);
						}
					}
				}
			}
		}, imageName);
	}

	@Override
	public void deleteImage(final String imageName, final String poolName) {
		executeRbdOpInPool(new Function<CephRbdConnectionManager, String>() {

			@Override
			public String apply(CephRbdConnectionManager arg0) {
				RbdImage image = null;
				try {
					LOG.trace("Opening image=" + imageName + ", pool=" + arg0.getPool() + ", mode=read-write");
					image = arg0.getRbd().open(imageName);

					boolean canBeDeleted = true;
					List<String> snapChildren = null;

					LOG.trace("Listing snapshots of image=" + imageName + ", pool=" + arg0.getPool());
					List<RbdSnapInfo> snapList = image.snapList();

					if (snapList != null && !snapList.isEmpty()) {
						for (RbdSnapInfo snap : snapList) {
							LOG.trace("Listing clones of snapshot=" + snap.name + ", image=" + imageName + ", pool=" + arg0.getPool());
							if ((snapChildren = image.listChildren(snap.name)) != null && !snapChildren.isEmpty()) {
								LOG.trace("Found clones of snapshot=" + snap.name + ": " + snapChildren);
								canBeDeleted = false;
								break;
							} else {
								LOG.trace("No clones found for snapshot=" + snap.name);
								if (image.snapIsProtected(snap.name)) {
									LOG.trace("Unprotecting snapshot=" + snap.name + ", image=" + imageName + ", pool=" + arg0.getPool());
									image.snapUnprotect(snap.name);
								}
								LOG.debug("Removing snapshot=" + snap.name + ", image=" + imageName + ", pool=" + arg0.getPool());
								image.snapRemove(snap.name);
								continue;
							}
						}
					} else {
						// Nothing to do here
					}

					if (canBeDeleted) {
						arg0.getRbd().close(image);
						image = null;
						LOG.debug("Deleting image=" + imageName + ", pool=" + arg0.getPool());
						arg0.getRbd().remove(imageName);
					} else {
						LOG.debug("Cannot delete image " + imageName + " in pool " + poolName
								+ " as it may be associated with other images by a parent-child relationship");
						throw new CannotDeleteCephImageException("Cannot delete image " + imageName + " in pool " + poolName
								+ " as it may be associated with other images by a parent-child relationship");
					}

					return null;
				} catch (RbdException e) {
					LOG.warn("Caught error while checking and or deleting image " + imageName + " in pool " + poolName + ": " + e.getMessage());
					throw new EucalyptusCephException("Caught error while checking and or deleting image " + imageName + " in pool " + poolName, e);
				} finally {
					if (image != null) {
						try {
							LOG.trace("Closing image=" + imageName + ", pool=" + arg0.getPool());
							arg0.getRbd().close(image);
						} catch (Exception e) {
							LOG.debug("Caught exception closing image " + imageName, e);
						}
					}
				}
			}
		}, poolName);
	}

	@Override
	public void renameImage(final String imageName, final String newImageName) {
		findAndExecuteRbdOp(new Function<CephRbdConnectionManager, String>() {

			@Override
			public String apply(@Nonnull CephRbdConnectionManager arg0) {
				try {
					LOG.debug("Renaming image=" + imageName + ", pool=" + arg0.getPool() + " to image=" + newImageName);
					arg0.getRbd().rename(imageName, newImageName);
					return null;
				} catch (RbdException e) {
					LOG.warn("Caught error while renaming image " + imageName + ": " + e.getMessage());
					throw new EucalyptusCephException("Failed to delete image " + imageName, e);
				}
			}
		}, imageName);
	}

	@Override
	public String getImagePool(final String imageName) {
		return findAndExecuteRbdOp(new Function<CephRbdConnectionManager, String>() {

			@Override
			public String apply(@Nonnull CephRbdConnectionManager arg0) {
				return arg0.getPool();
			}
		}, imageName);
	}

	/**
	 * @return Returns Ceph representation of the snapshot in the form: <b><code>pool/image@snapshot</code></b>
	 */
	@Override
	public String createSnapshot(final String parentName, final String snapName) {
		return findAndExecuteRbdOp(new Function<CephRbdConnectionManager, String>() {

			@Override
			public String apply(@Nonnull CephRbdConnectionManager arg0) {
				RbdImage image = null;
				try {
					LOG.trace("Opening image=" + parentName + ", pool=" + arg0.getPool() + ", mode=read-write");
					image = arg0.getRbd().open(parentName);
					LOG.debug("Creating snapshot=" + snapName + ", image=" + parentName + ", pool=" + arg0.getPool());
					image.snapCreate(snapName);
					LOG.debug("Protecting snapshot=" + snapName + ", image=" + parentName + ", pool=" + arg0.getPool());
					image.snapProtect(snapName);

					return arg0.getPool() + CephRbdInfo.POOL_IMAGE_DELIMITER + parentName + CephRbdInfo.IMAGE_SNAPSHOT_DELIMITER + snapName;
				} catch (Exception e) {
					LOG.warn("Caught error while creating snapshot " + snapName + " on parent " + parentName + ": " + e.getMessage());
					throw new EucalyptusCephException("Failed to create snapshot " + snapName + " on parent " + parentName, e);
				} finally {
					if (image != null) {
						try {
							LOG.trace("Closing image=" + parentName + ", pool=" + arg0.getPool());
							arg0.getRbd().close(image);
						} catch (Exception e) {
							LOG.debug("Caught exception closing image " + parentName, e);
						}
					}
				}
			}
		}, parentName);
	}

	@Override
	public void deleteSnapshot(final String parentName, final String snapName) {
		findAndExecuteRbdOp(new Function<CephRbdConnectionManager, String>() {

			@Override
			public String apply(@Nonnull CephRbdConnectionManager arg0) {
				RbdImage image = null;
				try {
					LOG.trace("Opening image=" + parentName + ", pool=" + arg0.getPool() + ", mode=read-write");
					image = arg0.getRbd().open(parentName);
					if (image.snapIsProtected(snapName)) {
						LOG.debug("Unprotecting snapshot=" + snapName + ", image=" + parentName + ", pool=" + arg0.getPool());
						image.snapUnprotect(snapName);
					}
					LOG.debug("Removing snapshot=" + snapName + ", image=" + parentName + ", pool=" + arg0.getPool());
					image.snapRemove(snapName);
					return null;
				} catch (Exception e) {
					LOG.warn("Caught error while deleting snapshot " + snapName + " on parent " + parentName + ": " + e.getMessage());
					throw new EucalyptusCephException("Failed to delete snapshot " + snapName + " on parent " + parentName, e);
				} finally {
					if (image != null) {
						try {
							LOG.trace("Closing image=" + parentName + ", pool=" + arg0.getPool());
							arg0.getRbd().close(image);
						} catch (Exception e) {
							LOG.debug("Caught exception closing image " + parentName, e);
						}
					}
				}
			}
		}, parentName);
	}

	/**
	 * <p> In its current state, following is the order of steps. Please document if any of the steps change </p>
	 * 
	 * <ol><li>Protect snapshot if its not protected</li> <li>Clone parent using snapshot</li> <li>Resize clone if necessary</li> <li>If cloned image is an EBS
	 * snapshot, create snapshot on cloned image and protect it</li> </ol>
	 * 
	 * @return Returns Ceph representation of the image in the form: <b><code>pool/image</code></b>
	 */
	@Override
	public String cloneAndResizeImage(final String parentName, final String snapName, final String cloneName, final Long size) {
		return findAndExecuteRbdOp(new Function<CephRbdConnectionManager, String>() {

			@Override
			public String apply(final CephRbdConnectionManager parentConn) {
				RbdImage source = null;

				try {
					LOG.trace("Opening image=" + parentName + ", pool=" + parentConn.getPool() + ", mode=read-write");
					source = parentConn.getRbd().open(parentName);

					try { // try-catch block for protecting snapshot
						if (source.snapIsProtected(snapName)) {
							LOG.debug("Snapshot=" + snapName + ", image=" + parentName + ", pool=" + parentConn.getPool() + " is protected");
						} else {
							LOG.debug("Protecting snapshot=" + snapName + ", image=" + parentName + ", pool=" + parentConn.getPool());
							source.snapProtect(snapName);
						}
					} catch (Exception e) {
						throw new EucalyptusCephException("Failed to protect snapshot " + snapName + " on parent " + parentName);
					}

					return executeRbdOpInRandomPool(new Function<CephRbdConnectionManager, String>() {

						@Override
						public String apply(CephRbdConnectionManager cloneConn) {
							RbdImage clone = null;

							try {
								// We only want layering and format 2
								int features = (1 << 0);

								LOG.debug("Cloning snapshot=" + snapName + ", image=" + parentName + ", pool=" + parentConn.getPool() + " to image="
										+ cloneName + ", pool=" + cloneConn.getPool());
								parentConn.getRbd().clone(parentName, snapName, cloneConn.getIoContext(), cloneName, features, 0);

								LOG.trace("Opening image=" + cloneName + ", pool=" + cloneConn.getPool() + ", mode=read-write");
								clone = cloneConn.getRbd().open(cloneName);

								if (size != null) {
									LOG.debug("Resizing image=" + cloneName + ", pool=" + cloneConn.getPool() + " to " + size + " bytes");
									clone.resize(size.longValue());
								} else {
									// nothing to do here
								}

								// If its an EBS snapshot, create a snapshot point and protect it
								if (cloneName.contains("snap-")) {
									String spOnSnapshot = CephRbdInfo.SNAPSHOT_ON_PREFIX + cloneName;
									LOG.debug("Creating snapshot=" + spOnSnapshot + ", image=" + cloneName + ", pool=" + cloneConn.getPool());
									clone.snapCreate(spOnSnapshot);
									LOG.debug("Protecting snapshot=" + spOnSnapshot + ", image=" + cloneName + ", pool=" + cloneConn.getPool());
									clone.snapProtect(spOnSnapshot);
								}

								return cloneConn.getPool() + CephRbdInfo.POOL_IMAGE_DELIMITER + cloneName;
							} catch (RbdException e) {
								LOG.warn("Caught error while cloning/resizing image " + cloneName + " from source image " + parentName + ": " + e.getMessage());
								throw new EucalyptusCephException("Failed to clone/resize image " + cloneName + " from source image " + parentName, e);
							} finally {
								if (clone != null) {
									try {
										LOG.trace("Closing image=" + cloneName + ", pool=" + cloneConn.getPool());
										cloneConn.getRbd().close(clone);
									} catch (Exception e) {
										LOG.debug("Caught exception closing image " + cloneName, e);
									}
								}
							}
						}
					}, cloneName);
				} catch (EucalyptusCephException e) {
					throw e;
				} catch (Exception e) {
					LOG.warn("Caught error while cloning/resizing image " + cloneName + " from source image " + parentName + ": " + e.getMessage());
					throw new EucalyptusCephException("Failed to clone/resize image " + cloneName + " from source image " + parentName, e);
				} finally {
					if (source != null) {
						try {
							LOG.trace("Opening image=" + parentName + ", pool=" + parentConn.getPool());
							parentConn.getRbd().close(source);
						} catch (Exception e) {
							LOG.debug("Caught exception closing image " + parentName, e);
						}
					}
				}
			}
		}, parentName);
	}

	@Override
	public List<String> listPool(final String poolName) {
		return executeRbdOpInPool(new Function<CephRbdConnectionManager, List<String>>() {

			@Override
			public List<String> apply(CephRbdConnectionManager arg0) {
				try {
					LOG.trace("Listing images in pool=" + poolName);
					return Arrays.asList(arg0.getRbd().list());
				} catch (RbdException e) {
					LOG.warn("Caught error while listing images in pool " + poolName + ": " + e.getMessage());
					throw new EucalyptusCephException("Failed to list images in pool " + poolName, e);
				}
			}
		}, poolName);
	}

	private <T> T executeRbdOpInPool(Function<CephRbdConnectionManager, T> function, String poolName) {
		T output = null;
		CephRbdConnectionManager conn = null;

		try {
			conn = CephRbdConnectionManager.getConnection(config, poolName);

			output = function.apply(conn);
		} catch (EucalyptusCephException e) {
			throw e;
		} catch (Exception e) {
			throw new EucalyptusCephException("Caught error during ceph operation" + e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		return output;
	}

	private <T> T executeRbdOpInRandomPool(Function<CephRbdConnectionManager, T> function, String imageName) {
		T output = null;
		CephRbdConnectionManager conn = null;

		try {
			if (imageName.contains("vol-")) {
				conn = CephRbdConnectionManager.getRandomVolumePoolConnection(config);
			} else {
				conn = CephRbdConnectionManager.getRandomSnapshotPoolConnection(config);
			}

			output = function.apply(conn);
		} catch (EucalyptusCephException e) {
			throw e;
		} catch (Exception e) {
			throw new EucalyptusCephException("Caught error during ceph operation" + e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		return output;
	}

	private <T> T findAndExecuteRbdOp(Function<CephRbdConnectionManager, T> function, String imageName) {
		T output = null;
		CephRbdConnectionManager conn = null;

		try {
			String[] allPools = null;
			if (imageName.contains("vol-")) {
				allPools = CephRbdConnectionManager.getAllVolumePools(config);
			} else {
				allPools = CephRbdConnectionManager.getAllSnapshotPools(config);
			}

			for (int i = 0; i < allPools.length; i++) {
				conn = CephRbdConnectionManager.getConnection(config, allPools[i]);
				try {
					LOG.debug("Searching for image=" + imageName + ", pool=" + allPools[i]);
					RbdImage image = conn.getRbd().openReadOnly(imageName);
					conn.getRbd().close(image);
					break;
				} catch (Exception e) {
					LOG.trace("Image=" + imageName + " not found in pool=" + allPools[i] + ". Reason: " + e.getMessage());
					conn.disconnect();
					conn = null;
				}
			}

			if (conn == null) {
				throw new CephImageNotFoundException("Unable to find image " + imageName + " in configured pools: " + Arrays.toString(allPools));
			}
			output = function.apply(conn);
		} catch (EucalyptusCephException e) {
			throw e;
		} catch (Exception e) {
			throw new EucalyptusCephException("Caught error during ceph operation" + e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		return output;
	}
}
