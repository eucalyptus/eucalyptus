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

package com.eucalyptus.blockstorage.ceph.entities;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.ceph.exceptions.EucalyptusCephException;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableIdentifier;
import com.eucalyptus.configurable.ConfigurableInit;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Transactions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

@Entity
@PersistenceContext(name = "eucalyptus_storage")
@Table(name = "ceph_rbd_info")
@ConfigurableClass(root = "storage", alias = "cephrbd", description = "Configuration for Ceph as an EBS backend", singleton = false, deferred = true)
public class CephRbdInfo extends AbstractPersistent {

  private static final long serialVersionUID = 1L;
  private static Logger LOG = Logger.getLogger(CephRbdInfo.class);

  public static final String POOL_IMAGE_DELIMITER = "/";
  public static final String IMAGE_SNAPSHOT_DELIMITER = "@";
  public static final String SNAPSHOT_ON_PREFIX = "sp-on-";
  public static final String SNAPSHOT_FOR_PREFIX = "sp-for-";
  public static final Splitter POOL_IMAGE_SPLITTER = Splitter.on(POOL_IMAGE_DELIMITER).trimResults().omitEmptyStrings();
  public static final Splitter IMAGE_SNAPSHOT_SPLITTER = Splitter.on(IMAGE_SNAPSHOT_DELIMITER).trimResults().omitEmptyStrings();

  private static final String DEFAULT_CEPH_USER = "eucalyptus";
  private static final String DEFAULT_CEPH_KEYRING_FILE = "/etc/ceph/ceph.client.eucalyptus.keyring";
  private static final String DEFAULT_CEPH_CONFIG_FILE = "/etc/ceph/ceph.conf";
  private static final String DEFAULT_POOL = "rbd";
  private static final String DELETED_IMAGE_COMMON_PREFIX = "del";

  @ConfigurableIdentifier
  @Column(name = "cluster_name", unique = true)
  private String clusterName;
  @ConfigurableField(description = "Ceph username employed by Eucalyptus operations. Default value is 'eucalyptus'", displayName = "Ceph Username",
      initial = "eucalyptus")
  @Column(name = "ceph_user")
  private String cephUser;
  @ConfigurableField(
      description = "Absolute path to Ceph keyring (ceph.client.eucalyptus.keyring) file. Default value is '/etc/ceph/ceph.client.eucalyptus.keyring'",
      displayName = "Ceph Keyring File", initial = "/etc/ceph/ceph.client.eucalyptus.keyring")
  @Column(name = "ceph_keyring_file")
  private String cephKeyringFile;
  @ConfigurableField(description = "Absolute path to Ceph configuration (ceph.conf) file. Default value is '/etc/ceph/ceph.conf'",
      displayName = "Ceph Configuration File", initial = "/etc/ceph/ceph.conf")
  @Column(name = "ceph_config_file")
  private String cephConfigFile;
  @ConfigurableField(
      description = "Ceph storage pool(s) made available to Eucalyptus for EBS volumes. Use a comma separated list for configuring multiple pools. "
          + "Default value is 'rbd'",
      displayName = "Ceph Volume Pools", initial = "rbd")
  @Column(name = "ceph_volume_pools")
  private String cephVolumePools;
  @ConfigurableField(
      description = "Ceph storage pool(s) made available to Eucalyptus for EBS snapshots. Use a comma separated list for configuring multiple pools. "
          + "Default value is 'rbd'",
      displayName = "Ceph Snapshot Pools", initial = "rbd")
  @Column(name = "ceph_snapshot_pools")
  private String cephSnapshotPools;
  @Column(name = "virsh_secret")
  private String virshSecret;
  @Column(name = "deleted_image_prefix")
  private String deletedImagePrefix;

  public CephRbdInfo() {
    this.clusterName = StorageProperties.NAME;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getCephUser() {
    return cephUser;
  }

  public void setCephUser(String cephUser) {
    this.cephUser = cephUser;
  }

  public String getCephKeyringFile() {
    return cephKeyringFile;
  }

  public void setCephKeyringFile(String cephKeyringFile) {
    this.cephKeyringFile = cephKeyringFile;
  }

  public String getCephConfigFile() {
    return cephConfigFile;
  }

  public void setCephConfigFile(String cephConfigFile) {
    this.cephConfigFile = cephConfigFile;
  }

  public String getCephVolumePools() {
    return cephVolumePools;
  }

  public void setCephVolumePools(String cephVolumePools) {
    this.cephVolumePools = cephVolumePools;
  }

  public String getCephSnapshotPools() {
    return cephSnapshotPools;
  }

  public void setCephSnapshotPools(String cephSnapshotPools) {
    this.cephSnapshotPools = cephSnapshotPools;
  }

  public String getVirshSecret() {
    return virshSecret;
  }

  public void setVirshSecret(String virshSecret) {
    this.virshSecret = virshSecret;
  }

  public String getDeletedImagePrefix() {
    return deletedImagePrefix;
  }

  public void setDeletedImagePrefix(String deletedImagePrefix) {
    this.deletedImagePrefix = deletedImagePrefix;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((clusterName == null) ? 0 : clusterName.hashCode());
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
    CephRbdInfo other = (CephRbdInfo) obj;
    if (clusterName == null) {
      if (other.clusterName != null)
        return false;
    } else if (!clusterName.equals(other.clusterName))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "[cephuser=" + cephUser + ", cephkeyringfile=" + cephKeyringFile + ", cephconfigfile=" + cephConfigFile + ", cephvolumepools="
        + cephVolumePools + ", cephsnapshotPools=" + cephSnapshotPools + "]";
  }

  @PrePersist
  public void checkPrePersist() {
    if (Strings.isNullOrEmpty(virshSecret)) {
      virshSecret = UUID.randomUUID().toString();
    }
    if (Strings.isNullOrEmpty(deletedImagePrefix)) {
      deletedImagePrefix = DELETED_IMAGE_COMMON_PREFIX + Crypto.generateAlphanumericId(8) + '-';
    }
  }

  public boolean isSame(CephRbdInfo other) {
    if (other == null)
      return false;
    if (this == other)
      return true;
    if (clusterName == null) {
      if (other.clusterName != null)
        return false;
    } else if (!clusterName.equals(other.clusterName))
      return false;
    if (cephUser == null) {
      if (other.cephUser != null)
        return false;
    } else if (!cephUser.equals(other.cephUser))
      return false;
    if (cephKeyringFile == null) {
      if (other.cephKeyringFile != null)
        return false;
    } else if (!cephKeyringFile.equals(other.cephKeyringFile))
      return false;
    if (cephConfigFile == null) {
      if (other.cephConfigFile != null)
        return false;
    } else if (!cephConfigFile.equals(other.cephConfigFile))
      return false;
    if (cephVolumePools == null) {
      if (other.cephVolumePools != null)
        return false;
    } else if (!cephVolumePools.equals(other.cephVolumePools))
      return false;
    if (cephSnapshotPools == null) {
      if (other.getCephSnapshotPools() != null)
        return false;
    } else if (!cephSnapshotPools.equals(other.cephSnapshotPools))
      return false;
    return true;
  }


  @ConfigurableInit
  public CephRbdInfo init( ) {
    setCephUser(DEFAULT_CEPH_USER);
    setCephKeyringFile(DEFAULT_CEPH_KEYRING_FILE);
    setCephConfigFile(DEFAULT_CEPH_CONFIG_FILE);
    setCephVolumePools(DEFAULT_POOL);
    setCephSnapshotPools(DEFAULT_POOL);
    return this;
  }

  private static CephRbdInfo generateDefault() {
    return new CephRbdInfo( ).init( );
  }

  public static CephRbdInfo getStorageInfo() {
    CephRbdInfo info = null;

    try {
      info = Transactions.find(new CephRbdInfo());
    } catch (Exception e) {
      LOG.warn("Ceph-RBD information for " + StorageProperties.NAME + " not found. Loading defaults.");
      try {
        info = Transactions.saveDirect(generateDefault());
      } catch (Exception e1) {
        try {
          info = Transactions.find(new CephRbdInfo());
        } catch (Exception e2) {
          LOG.warn("Failed to persist and retrieve CephInfo entity");
        }
      }
    }

    if (info == null) {
      info = generateDefault();
    }

    return info;
  }

  public String[] getAllVolumePools() {
    String[] allPools = cephVolumePools.split(",");
    if (allPools != null && allPools.length > 0) {
      return allPools;
    } else {
      LOG.warn(
          "No ceph pools defined, retry after defining at least one pool using euca-modify-property -p <cluster>.storage.cephvolumepools=<pool-name>");
      throw new EucalyptusCephException(
          "No ceph pools defined, retry after defining at least one pool using euca-modify-property -p <cluster>.storage.cephvolumepools=<pool-name>");
    }
  }

  public String[] getAllSnapshotPools() {
    String[] allPools = cephSnapshotPools.split(",");
    if (allPools != null && allPools.length > 0) {
      return allPools;
    } else {
      LOG.warn(
          "No ceph pools defined, retry after defining at least one pool using euca-modify-property -p <cluster>.storage.cephsnapshotpools=<pool-name>");
      throw new EucalyptusCephException(
          "No ceph pools defined, retry after defining at least one pool using euca-modify-property -p <cluster>.storage.cephsnapshotpools=<pool-name>");
    }
  }
}
