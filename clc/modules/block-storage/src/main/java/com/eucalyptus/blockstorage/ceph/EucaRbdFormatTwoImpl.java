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

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ceph.rbd.RbdException;
import com.ceph.rbd.RbdImage;
import com.ceph.rbd.jna.RbdSnapInfo;
import com.eucalyptus.blockstorage.ceph.entities.CephInfo;
import com.eucalyptus.blockstorage.ceph.exceptions.EucalyptusCephException;
import com.google.common.base.Function;

/**
 * Eucalyptus RBD adapter for managing Ceph RBD images of type Format 2
 */
public class EucaRbdFormatTwoImpl implements EucaRbd {

	private static final Logger LOG = Logger.getLogger(EucaRbdFormatTwoImpl.class);

	private CephInfo config;

	public EucaRbdFormatTwoImpl(CephInfo cephInfo) {
		this.config = cephInfo;
	}

	// Added this method so that a running operation using an older configuration does not get impacted.
	@Override
	public void setCephConfig(CephInfo cephInfo) {
		this.config = cephInfo;
	}

	@Override
	public void deleteImage(final String imageName) {
		findAndExecuteRbdOp(new Function<CephConnectionManager, String>() {

			@Override
			public String apply(@Nonnull CephConnectionManager arg0) {
				try {
					LOG.debug("Deleting image " + imageName + " from pool " + arg0.getPool());
					arg0.getRbd().remove(imageName);
					return null;
				} catch (RbdException e) {
					LOG.warn("Caught error while deleting image " + imageName + ": " + e.getMessage());
					throw new EucalyptusCephException("Failed to delete image " + imageName, e);
				}
			}
		}, imageName);
	}

	/**
	 * @return Returns Ceph representation of the image in the form: <b><code>pool/image</code></b>
	 */
	@Override
	public String createImage(final String imageName, final long imageSize) {
		return executeRbdOpInRandomPool(new Function<CephConnectionManager, String>() {

			@Override
			public String apply(@Nonnull CephConnectionManager arg0) {
				RbdImage image = null;
				try {
					LOG.debug("Creating image " + imageName + " of size " + imageSize + " bytes in pool " + arg0.getPool());
					// We only want layering and format 2
					int features = (1 << 0);
					arg0.getRbd().create(imageName, imageSize, features, 0);
					LOG.debug("Opening image " + imageName + " in read-only mode");
					image = arg0.getRbd().openReadOnly(imageName);
					return arg0.getPool() + CephInfo.POOL_IMAGE_DELIMITER + imageName;
				} catch (RbdException e) {
					LOG.warn("Caught error while creating image " + imageName + ": " + e.getMessage());
					throw new EucalyptusCephException("Failed to create image " + imageName, e);
				} finally {
					if (image != null) {
						try {
							arg0.getRbd().close(image);
						} catch (Exception e) {
							LOG.debug("Caught exception closing image " + imageName, e);
						}
					}
				}
			}
		}, imageName);
	}

	@Override
	public String getImagePool(final String imageName) {
		return findAndExecuteRbdOp(new Function<CephConnectionManager, String>() {

			@Override
			public String apply(@Nonnull CephConnectionManager arg0) {
				return arg0.getPool();
			}
		}, imageName);
	}

	/**
	 * @return Returns Ceph representation of the snapshot in the form: <b><code>pool/image@snapshot</code></b>
	 */
	@Override
	public String createSnapshot(final String parentName, final String snapName) {
		return findAndExecuteRbdOp(new Function<CephConnectionManager, String>() {

			@Override
			public String apply(@Nonnull CephConnectionManager arg0) {
				RbdImage image = null;
				try {
					LOG.debug("Opening image " + parentName);
					image = arg0.getRbd().open(parentName);
					LOG.debug("Creating snapshot " + snapName + " on parent " + parentName + " in pool " + arg0.getPool());
					image.snapCreate(snapName);
					for (RbdSnapInfo snap : image.snapList()) {
						if (snap.name.equals(snapName)) {
							return arg0.getPool() + CephInfo.POOL_IMAGE_DELIMITER + parentName + CephInfo.POOL_IMAGE_DELIMITER + snapName;
						}
					}
				} catch (Exception e) {
					LOG.warn("Caught error while creating snapshot " + snapName + " on parent " + parentName + ": " + e.getMessage());
					throw new EucalyptusCephException("Failed to create snapshot " + snapName + " on parent " + parentName, e);
				} finally {
					if (image != null) {
						try {
							arg0.getRbd().close(image);
						} catch (Exception e) {
							LOG.debug("Caught exception closing image " + parentName, e);
						}
					}
				}
				LOG.warn("Unable to find snapshot " + snapName + " on parent " + parentName);
				throw new EucalyptusCephException("Failed to find snapshot " + snapName + " on parent " + parentName);
			}
		}, parentName);
	}

	@Override
	public void deleteSnapshot(final String parentName, final String snapName) {
		findAndExecuteRbdOp(new Function<CephConnectionManager, String>() {

			@Override
			public String apply(@Nonnull CephConnectionManager arg0) {
				RbdImage image = null;
				try {
					image = arg0.getRbd().open(parentName);
					if (image.snapIsProtected(snapName)) {
						LOG.debug("Unprotecting snapshot " + snapName + " on parent " + parentName + " in pool " + arg0.getPool());
						image.snapUnprotect(snapName);
					}
					LOG.debug("Removing snapshot " + snapName + " on parent " + parentName);
					image.snapRemove(snapName);
					return null;
				} catch (Exception e) {
					LOG.warn("Caught error while deleting snapshot " + snapName + " on parent " + parentName + ": " + e.getMessage());
					throw new EucalyptusCephException("Failed to delete snapshot " + snapName + " on parent " + parentName, e);
				} finally {
					if (image != null) {
						try {
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
	 * <ol> <li>Create a snpahsot on the parent if one was not provided</li> <li>Protect the snapshot</li> <li>Clone the parent using the snapshot</li>
	 * <li>Flatten the clone (copies the blocks over, time taking operation</li> <li>Resize the clone if necessary</li> <li>Unprotect the snapshot</li>
	 * <li>Delete the snapshot</li> </ol>
	 * 
	 * @return Returns Ceph representation of the image in the form: <b><code>pool/image</code></b>
	 */
	@Override
	public String cloneAndResizeImage(final String parentName, final String snapName, final String cloneName, final Long size) {
		return findAndExecuteRbdOp(new Function<CephConnectionManager, String>() {

			@Override
			public String apply(final CephConnectionManager parentConn) {
				RbdImage source = null;
				final String snapshot;

				try {
					source = parentConn.getRbd().open(parentName);

					if (StringUtils.isBlank(snapName)) { // If a valid snapshot is not provided, create one
						snapshot = "sp-" + cloneName;
						LOG.debug("Creating snapshot " + snapshot + " on parent " + parentName + " in pool " + parentConn.getPool());
						source.snapCreate(snapshot);
					} else {
						snapshot = snapName;
					}

					try { // try-catch block for protecting and unprotecting snapshot
						LOG.debug("Protecting snapshot " + snapshot);
						source.snapProtect(snapshot);

						if (source.snapIsProtected(snapshot)) {

							return executeRbdOpInRandomPool(new Function<CephConnectionManager, String>() {

								@Override
								public String apply(CephConnectionManager cloneConn) {
									RbdImage clone = null;

									try {
										// We only want layering and format 2
										int features = (1 << 0);

										LOG.debug("Cloning snapshot " + snapshot + " to " + cloneName + " in pool " + cloneConn.getPool());
										parentConn.getRbd().clone(parentName, snapshot, cloneConn.getIoContext(), cloneName, features, 0);

										LOG.debug("Flattening cloned image " + cloneName);
										clone = cloneConn.getRbd().open(cloneName);
										clone.flatten();

										if (size != null) {
											LOG.debug("Resizing image " + cloneName + " to " + size + " bytes");
											clone.resize(size.longValue());
										} else {
											// nothing to do here
										}

										return cloneConn.getPool() + CephInfo.POOL_IMAGE_DELIMITER + cloneName;
									} catch (RbdException e) {
										LOG.warn("Caught error while cloning/resizing image " + cloneName + " from source image " + parentName + ": "
												+ e.getMessage());
										throw new EucalyptusCephException("Failed to clone/resize image " + cloneName + " from source image " + parentName, e);
									} finally {
										if (clone != null) {
											try {
												cloneConn.getRbd().close(clone);
											} catch (Exception e) {
												LOG.debug("Caught exception closing image " + cloneName, e);
											}
										}
									}
								}
							}, cloneName);
						} else {
							throw new EucalyptusCephException("Failed to protect snapshot before creating a clone");
						}
					} finally {
						try {
							LOG.debug("Unprotecting snapshot " + snapshot + " on parent " + parentName);
							source.snapUnprotect(snapshot);
						} catch (Exception e) {
							LOG.debug("Caught exception unprotecting snapshot " + snapshot, e);
						}

						try {
							LOG.debug("Removing snapshot " + snapshot + " on parent " + parentName);
							source.snapRemove(snapshot);
						} catch (Exception e) {
							LOG.debug("Caught exception removing snapshot " + snapshot, e);
						}
					}
				} catch (EucalyptusCephException e) {
					throw e;
				} catch (Exception e) {
					LOG.warn("Caught error while cloning/resizing image " + cloneName + " from source image " + parentName + ": " + e.getMessage());
					throw new EucalyptusCephException("Failed to clone/resize image " + cloneName + " from source image " + parentName, e);
				} finally {
					if (source != null) {
						try {
							parentConn.getRbd().close(source);
						} catch (Exception e) {
							LOG.debug("Caught exception closing image " + parentName, e);
						}
					}
				}
			}
		}, parentName);
	}

	private <T> T executeRbdOpInRandomPool(Function<CephConnectionManager, T> function, String imageName) {
		T output = null;
		CephConnectionManager conn = null;

		try {
			if (imageName.contains("vol-")) {
				conn = CephConnectionManager.getRandomVolumePoolConnection(config);
			} else {
				conn = CephConnectionManager.getRandomSnapshotPoolConnection(config);
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

	private <T> T findAndExecuteRbdOp(Function<CephConnectionManager, T> function, String imageName) {
		T output = null;
		CephConnectionManager conn = null;

		try {
			String[] allPools = null;
			if (imageName.contains("vol-")) {
				allPools = CephConnectionManager.getAllVolumePools(config);
			} else {
				allPools = CephConnectionManager.getAllSnapshotPools(config);
			}

			for (int i = 0; i < allPools.length; i++) {
				conn = CephConnectionManager.getConnection(config, allPools[i]);
				try {
					LOG.debug("Searching for image " + imageName + " in pool " + allPools[i]);
					RbdImage image = conn.getRbd().openReadOnly(imageName);
					conn.getRbd().close(image);
					break;
				} catch (Exception e) {
					LOG.trace("Image " + imageName + " not found in pool " + allPools[i] + ". Reason: " + e.getMessage());
					conn.disconnect();
					conn = null;
				}
			}

			if (conn == null) {
				throw new EucalyptusCephException("Unable to find image " + imageName + " in configured pools: " + allPools);
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
