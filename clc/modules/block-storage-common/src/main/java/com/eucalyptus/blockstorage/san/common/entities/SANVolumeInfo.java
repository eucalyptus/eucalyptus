/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009 Ent. Services Development Corporation LP
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
/*
 * Author: Neil Soman neil@eucalyptus.com
 */

package com.eucalyptus.blockstorage.san.common.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.AbstractPersistent;

@Entity
@PersistenceContext(name = "eucalyptus_storage")
@Table(name = "san_volume_info")
public class SANVolumeInfo extends AbstractPersistent {
  @Column(name = "volumeid")
  protected String volumeId;
  @Column(name = "scname")
  private String scName;
  @Column(name = "iqn")
  private String iqn;
  @Column(name = "storeuser")
  private String storeUser;
  @Column(name = "encryptedpassword")
  @Type(type="text")
  private String encryptedPassword;
  @Column(name = "size")
  protected Integer size;
  @Column(name = "status")
  private String status;
  @Column(name = "snapshot_of")
  private String snapshotOf;
  @Column(name = "san_volume_Id")
  private String sanVolumeId;

  public SANVolumeInfo() {
    this.scName = StorageProperties.NAME;
  }

  public SANVolumeInfo(String volumeId) {
    this();
    this.volumeId = volumeId;
  }

  public SANVolumeInfo(String volumeId, String iqn, int size) {
    this();
    this.volumeId = volumeId;
    this.iqn = iqn;
    this.size = size;
  }

  public String getIqn() {
    return iqn;
  }

  public void setIqn(String iqn) {
    this.iqn = iqn;
  }

  public String getStoreUser() {
    return storeUser;
  }

  public void setStoreUser(String storeUser) {
    this.storeUser = storeUser;
  }

  public String getEncryptedPassword() {
    return encryptedPassword;
  }

  public void setEncryptedPassword(String encryptedPassword) {
    this.encryptedPassword = encryptedPassword;
  }

  public String getVolumeId() {
    return volumeId;
  }

  public void setVolumeId(String volumeId) {
    this.volumeId = volumeId;
  }

  public String getScName() {
    return scName;
  }

  public void setScName(String scName) {
    this.scName = scName;
  }

  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  public SANVolumeInfo withSize(Integer size) {
    this.size = size;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getSnapshotOf() {
    return snapshotOf;
  }

  public void setSnapshotOf(String snapshotOf) {
    this.snapshotOf = snapshotOf;
  }

  public SANVolumeInfo withSnapshotOf(String snapshotOf) {
    this.snapshotOf = snapshotOf;
    return this;
  }

  public String getSanVolumeId() {
    return sanVolumeId;
  }

  public void setSanVolumeId(String sanVolumeId) {
    this.sanVolumeId = sanVolumeId;
  }

  public SANVolumeInfo withSanVolumeId(String sanVolumeId) {
    this.sanVolumeId = sanVolumeId;
    return this;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((volumeId == null) ? 0 : volumeId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SANVolumeInfo other = (SANVolumeInfo) obj;
    if (volumeId == null) {
      if (other.volumeId != null)
        return false;
    } else if (!volumeId.equals(other.volumeId))
      return false;
    return true;
  }

}
