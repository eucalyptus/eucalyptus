/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2017 Ent. Services Development Corporation LP
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

package com.eucalyptus.blockstorage.ceph;

import java.util.List;

import com.eucalyptus.blockstorage.ceph.entities.CephRbdInfo;
import com.google.common.collect.SetMultimap;

public interface CephRbdAdapter {

  /**
   * Use this to change the ceph configuration after the class is instantiated
   * 
   * @param cephInfo
   */
  public void setCephConfig(CephRbdInfo cephInfo);

  /**
   * Create a new RBD image
   * 
   * @param imageName Name of the image to be created
   * @param imageSize Size of the image in bytes
   * @return Returns a representation of the newly created image
   */
  public String createImage(String imageName, long imageSize);

  /**
   * Change the size of an existing RBD image
   *
   * @param imageName Name of the image to modify
   * @param poolName Name of the pool to which the image belongs
   * @param imageSize Desired size of the image in bytes
   */
  public void resizeImage(String imageName, String poolName, long imageSize);

  /**
   * Delete RBD image
   * 
   * @param imageName Name of the image to be deleted
   * @param poolName Name of the pool to which the image belongs
   * @param
   */
  public void deleteImage(String imageName, String poolName);

  /**
   * List images in a pool and try deleting an image if it starts with the prefix or is present in the toBeDeleted set. If the deletion fails and the
   * image does not start with the prefix, rename the image by appending the prefix to the original name. It might seem like this method is doing too
   * many things but its intentional to limit the number of rbd connections
   * 
   * @param poolName Name of the pool
   * @param imagePrefix Prefix of images that are marked for deletion
   * @param toBeDeleted Set of images that have never been cleaned up
   * @return Returns a list of RBD snapshot names that were deleted, so their RBD snapshots (not clones) can be removed
   * from the snapshot-to-be-deleted table. Necessary because the image might be gone by the time cleanUpSnapshots 
   * runs, and thus it would be unable to delete these snapshots.
   */
  public List<String> cleanUpImages(String poolName, String imagePrefix, List<String> toBeDeleted);

  /**
   * Try deleting RBD snapshots and return the ones that cannot deleted since they are busy (parent-child relationship with other images)
   * 
   * @param poolName Name of the pool
   * @param imagePrefix Prefix of images that are marked for deletion
   * @param toBeDeleted Mapping of image and RBD snapshots to be deleted
   * @return Returns a mapping of RBD image and RBD snapshots that cannot be deleted due to their inheritance
   */
  public SetMultimap<String, String> cleanUpSnapshots(String poolName, String imagePrefix, SetMultimap<String, String> toBeDeleted);

  /**
   * Rename RBD image
   * 
   * @param imageName Image to be renamed
   * @param newImageName New name of the image
   * @param poolName Name of the pool to which the image belongs
   */
  public void renameImage(String imageName, String newImageName, String poolName);

  /**
   * Check if the image exists in any of the configured pools and return the pool name
   * 
   * @param imageName Name of the image to be checked on
   * @return Returns true if the image exists and false otherwise
   */
  public String getImagePool(String imageName);

  /**
   * Create an RBD snapshot
   * 
   * @param parentName Name of the parent image
   * @param snapName Name of the snapshot
   * @param parentPoolName Name of the pool to which the parent image belongs
   * @return Returns a representation of the newly created snapshot
   */
  public String createSnapshot(String parentName, String snapName, String parentPoolName);

  /**
   * Delete the RBD snapshot
   * 
   * @param parentName Name of the parent image
   * @param snapName Name of the snapshot
   * @param parentPoolName Name of the pool to which the parent image belongs
   */
  public void deleteSnapshot(String parentName, String snapName, String parentPoolName);

  /**
   * Clone an image from the parent using the snapshot on the parent. If no snapshot is passed, a new snapshot is created on the parent and used for
   * cloning. Resize the cloned image if size is passed in
   * 
   * @param parentName Name of the parent image
   * @param snapName Name of the snapshot on parent image to be used for cloning
   * @param cloneName Name of the image to be cloned
   * @param size Size of the cloned image if it needs to resized
   * @param parentPoolName Name of the pool to which the parent image belongs
   * @return Returns a representation of the cloned image
   */
  public String cloneAndResizeImage(String parentName, String snapName, String cloneName, Long size, String parentPoolName);

  /**
   * List RBD images in pool
   * 
   * @param poolName Name of the pool
   * @return
   */
  public List<String> listPool(String poolName);

  /**
   * Delete all existing RBD snapshots on the image and create a new RBD snapshot
   * 
   * @param imageName Name of the image
   * @param poolName Name of the pool to which the image belongs
   * @param snapName Name of the new snapshot to be created
   * @return Returns a representation of the newly created snapshot
   */
  public String deleteAllSnapshots(String imageName, String poolName, String snapName);
}
