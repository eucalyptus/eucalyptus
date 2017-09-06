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

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;

import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.upgrade.Upgrades.Version;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Predicate;

@PersistenceContext(name = "eucalyptus_storage")
@Table(name = "ISCSIVolumeInfo")
@Entity
public class ISCSIVolumeInfo extends LVMVolumeInfo {
  @Column(name = "storename")
  private String storeName;
  @Column(name = "tid")
  private Integer tid;
  @Column(name = "lun")
  private Integer lun;
  @Column(name = "storeuser")
  private String storeUser;

  @Column(name = "encryptedpassword")
  @Type(type="text")
  private String encryptedPassword;

  public String toString() {
    return storeName + "," + tid + "," + lun + "," + storeUser + "," + super.toString();
  }

  public ISCSIVolumeInfo() {}

  public ISCSIVolumeInfo(String volumeId) {
    this.volumeId = volumeId;
  }

  public String getStoreName() {
    return storeName;
  }

  public void setStoreName(String storeName) {
    this.storeName = storeName;
  }

  public Integer getTid() {
    return tid == null ? -1 : tid;
  }

  public void setTid(Integer tid) {
    this.tid = tid;
  }

  public Integer getLun() {
    return lun;
  }

  public void setLun(Integer lun) {
    this.lun = lun;
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

  /**
   * This upgrade is to push the snapshot size from this entity to the SnapshoInfo entity because Snapshot info cannot have a dependency on the
   * backend volume entities.
   *
   */
  @EntityUpgrade(entities = {ISCSIVolumeInfo.class}, since = Version.v3_4_0, value = Storage.class)
  public enum ISCSIVolumeInfoSnapshotSizeUpgrade3_4 implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger(ISCSIVolumeInfo.ISCSIVolumeInfoSnapshotSizeUpgrade3_4.class);

    @Override
    public boolean apply(Class arg0) {
      EntityTransaction db = Entities.get(ISCSIVolumeInfo.class);
      try {
        ISCSIVolumeInfo example = new ISCSIVolumeInfo();
        example.setScName(null);
        List<ISCSIVolumeInfo> entities = Entities.query(example);
        for (ISCSIVolumeInfo entry : entities) {
          if (entry.getVolumeId().startsWith("snap-")) {
            EntityTransaction snapDb = Entities.get(SnapshotInfo.class);
            try {
              SnapshotInfo exampleSnap = new SnapshotInfo(entry.getVolumeId());
              exampleSnap.setScName(null); // all clusters.
              List<SnapshotInfo> snaps = Entities.query(exampleSnap);
              for (SnapshotInfo snap : snaps) {
                if (snap.getSizeGb() == null) {
                  snap.setSizeGb(entry.getSize());
                  LOG.debug("Upgrading: " + entry.getVolumeId() + " putting size from back-end to SnapshotInfo. Setting size to " + snap.getSizeGb());
                } else {
                  // Size already set, do nothing
                }
              }
              snapDb.commit();
            } finally {
              snapDb.rollback();
              snapDb = null;
            }
          } else {
            // Skip because not a snapshot record
            LOG.debug("Skipping snapshot upgrade of " + entry.getVolumeId() + " because not a snapshot");
          }

        }
        db.commit();
        return true;
      } catch (Exception ex) {
        db.rollback();
        throw Exceptions.toUndeclared(ex);
      }
    }
  }

}
