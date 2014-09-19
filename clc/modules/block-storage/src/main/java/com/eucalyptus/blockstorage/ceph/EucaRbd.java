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

import com.eucalyptus.blockstorage.ceph.entities.CephInfo;

public interface EucaRbd {

	/**
	 * Use this to change the ceph configuration after the class is instantiated
	 * 
	 * @param cephInfo
	 */
	public void setCephConfig(CephInfo cephInfo);

	/**
	 * Create a new RBD image
	 * 
	 * @param imageName Name of the image to be created
	 * @param imageSize Size of the image in bytes
	 * @return Returns a representation of the newly created image
	 */
	public String createImage(String imageName, long imageSize);

	/**
	 * Delete RBD image
	 * 
	 * @param imageName Name of the image to be deleted
	 */
	public void deleteImage(String imageName);

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
	 * @return Returns a representation of the newly created snapshot
	 */
	public String createSnapshot(String parentName, String snapName);

	/**
	 * Delete the RBD snapshot
	 * 
	 * @param parentName Name of the parent image
	 * @param snapName Name of the snapshot
	 */
	public void deleteSnapshot(String parentName, String snapName);

	/**
	 * Clone an image from the parent using the snapshot on the parent. If no snapshot is passed, a new snapshot is created on the parent and used for cloning.
	 * Resize the cloned image if size is passed in
	 * 
	 * @param parentName Name of the parent image
	 * @param snapName Name of the snapshot on parent image to be used for cloning
	 * @param cloneName Name of the image to be cloned
	 * @param size Size of the cloned image if it needs to resized
	 * @return Returns a representation of the cloned image
	 */
	public String cloneAndResizeImage(String parentName, String snapName, String cloneName, Long size);
}
