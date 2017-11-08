/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

package com.eucalyptus.blockstorage.ceph.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.AbstractPersistent;

@Entity
@PersistenceContext(name = "eucalyptus_storage")
@Table(name = "ceph_rbd_image_to_be_deleted")
public class CephRbdImageToBeDeleted extends AbstractPersistent {

  private static final long serialVersionUID = 1L;

  @Column(name = "cluster_name")
  private String clusterName;

  @Column(name = "image_name")
  private String imageName;

  @Column(name = "pool_name")
  private String poolName;

  public CephRbdImageToBeDeleted() {
    this.clusterName = StorageProperties.NAME;
  }

  public CephRbdImageToBeDeleted(String imageName, String poolName) {
    this();
    this.imageName = imageName;
    this.poolName = poolName;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getImageName() {
    return imageName;
  }

  public void setImageName(String imageName) {
    this.imageName = imageName;
  }

  public CephRbdImageToBeDeleted withImageName(String imageName) {
    this.imageName = imageName;
    return this;
  }

  public String getPoolName() {
    return poolName;
  }

  public void setPoolName(String poolName) {
    this.poolName = poolName;
  }

  public CephRbdImageToBeDeleted withPoolName(String poolName) {
    this.poolName = poolName;
    return this;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((clusterName == null) ? 0 : clusterName.hashCode());
    result = prime * result + ((imageName == null) ? 0 : imageName.hashCode());
    result = prime * result + ((poolName == null) ? 0 : poolName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    CephRbdImageToBeDeleted other = (CephRbdImageToBeDeleted) obj;
    if (clusterName == null) {
      if (other.clusterName != null)
        return false;
    } else if (!clusterName.equals(other.clusterName))
      return false;
    if (imageName == null) {
      if (other.imageName != null)
        return false;
    } else if (!imageName.equals(other.imageName))
      return false;
    if (poolName == null) {
      if (other.poolName != null)
        return false;
    } else if (!poolName.equals(other.poolName))
      return false;
    return true;
  }
}
