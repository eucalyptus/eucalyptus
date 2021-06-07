/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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

package com.eucalyptus.blockstorage.entities;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceContext;

import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.AbstractPersistent;

@PersistenceContext(name = "eucalyptus_storage")
@MappedSuperclass
public class LVMVolumeInfo extends AbstractPersistent {
  @Column(name = "volume_name")
  protected String volumeId;
  @Column(name = "sc_name")
  private String scName;
  @Column(name = "lodev_name")
  private String loDevName;
  @Column(name = "lofile_name")
  private String loFileName;
  @Column(name = "pv_name")
  private String pvName;
  @Column(name = "vg_name")
  private String vgName;
  @Column(name = "lv_name")
  private String lvName;
  @Column(name = "size")
  protected Integer size;
  @Column(name = "status")
  private String status;
  @Column(name = "snapshot_of")
  private String snapshotOf;
  @Column(name = "cleanup")
  private Boolean cleanup;

  public static final String LVM_ROOT_DIRECTORY = "/dev";
  public static final String PATH_SEPARATOR = "/";

  /**
   * Returns a string representation of the path to this resource. Null if not available.
   * 
   * @return
   */
  public String getAbsoluteLVPath() {
    if (this.getVgName() != null && this.getLvName() != null)
      return LVM_ROOT_DIRECTORY + PATH_SEPARATOR + this.getVgName() + PATH_SEPARATOR + this.getLvName();
    return null;
  }

  public String toString() {
    return volumeId + "," + scName + "," + loDevName + "," + loFileName + "," + pvName + "," + vgName + "," + lvName + "," + size + "," + status
        + "," + snapshotOf + "," + cleanup;
  }

  public LVMVolumeInfo() {
    super();
    this.scName = StorageProperties.NAME;
  }

  public LVMVolumeInfo(String volumeId) {
    this();
    this.volumeId = volumeId;
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

  public String getLoDevName() {
    return loDevName;
  }

  public void setLoDevName(String loDevName) {
    this.loDevName = loDevName;
  }

  public String getLoFileName() {
    return loFileName;
  }

  public void setLoFileName(String loFileName) {
    this.loFileName = loFileName;
  }

  public String getPvName() {
    return pvName;
  }

  public void setPvName(String pvName) {
    this.pvName = pvName;
  }

  public String getVgName() {
    return vgName;
  }

  public void setVgName(String vgName) {
    this.vgName = vgName;
  }

  public String getLvName() {
    return lvName;
  }

  public void setLvName(String lvName) {
    this.lvName = lvName;
  }

  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
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

  public Boolean getCleanup() {
    return cleanup == null ? false : cleanup;
  }

  public void setCleanup(Boolean cleanup) {
    this.cleanup = cleanup;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((scName == null) ? 0 : scName.hashCode());
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
    LVMVolumeInfo other = (LVMVolumeInfo) obj;
    if (scName == null) {
      if (other.scName != null)
        return false;
    } else if (!scName.equals(other.scName))
      return false;
    if (volumeId == null) {
      if (other.volumeId != null)
        return false;
    } else if (!volumeId.equals(other.volumeId))
      return false;
    return true;
  }

}
