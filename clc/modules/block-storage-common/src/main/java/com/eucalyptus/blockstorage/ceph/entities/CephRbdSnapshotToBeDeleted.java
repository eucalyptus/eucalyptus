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
@Table(name = "ceph_rbd_snapshot_to_be_deleted")
public class CephRbdSnapshotToBeDeleted extends AbstractPersistent {

  private static final long serialVersionUID = 1L;

  @Column(name = "availability_zone")
  private String az;

  @Column(name = "pool")
  private String pool;

  @Column(name = "image")
  private String image;

  @Column(name = "snapshot")
  private String snapshot;

  public CephRbdSnapshotToBeDeleted() {
    this.az = StorageProperties.NAME;
  }

  public CephRbdSnapshotToBeDeleted(String pool, String image, String snapshot) {
    this();
    this.pool = pool;
    this.image = image;
    this.snapshot = snapshot;
  }

  public String getAz() {
    return az;
  }

  public void setAz(String az) {
    this.az = az;
  }

  public String getPool() {
    return pool;
  }

  public void setPool(String pool) {
    this.pool = pool;
  }

  public CephRbdSnapshotToBeDeleted withPool(String pool) {
    this.pool = pool;
    return this;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public CephRbdSnapshotToBeDeleted withImage(String image) {
    this.image = image;
    return this;
  }

  public String getSnapshot() {
    return snapshot;
  }

  public void setSnapshot(String snapshot) {
    this.snapshot = snapshot;
  }

  public CephRbdSnapshotToBeDeleted withSnapshot(String snapshot) {
    this.snapshot = snapshot;
    return this;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((az == null) ? 0 : az.hashCode());
    result = prime * result + ((image == null) ? 0 : image.hashCode());
    result = prime * result + ((pool == null) ? 0 : pool.hashCode());
    result = prime * result + ((snapshot == null) ? 0 : snapshot.hashCode());
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
    CephRbdSnapshotToBeDeleted other = (CephRbdSnapshotToBeDeleted) obj;
    if (az == null) {
      if (other.az != null)
        return false;
    } else if (!az.equals(other.az))
      return false;
    if (image == null) {
      if (other.image != null)
        return false;
    } else if (!image.equals(other.image))
      return false;
    if (pool == null) {
      if (other.pool != null)
        return false;
    } else if (!pool.equals(other.pool))
      return false;
    if (snapshot == null) {
      if (other.snapshot != null)
        return false;
    } else if (!snapshot.equals(other.snapshot))
      return false;
    return true;
  }
}
