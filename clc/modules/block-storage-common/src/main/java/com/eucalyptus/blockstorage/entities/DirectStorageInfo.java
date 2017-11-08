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
import javax.persistence.PersistenceContext;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableIdentifier;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.upgrade.Upgrades.Version;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Predicate;

@Entity
@PersistenceContext(name = "eucalyptus_storage")
@Table(name = "direct_storage_info")
@ConfigurableClass(root = "storage", alias = "direct", description = "Basic storage controller configuration.", singleton = false, deferred = true)
public class DirectStorageInfo extends AbstractPersistent {
  private static Logger LOG = Logger.getLogger(DirectStorageInfo.class);

  @ConfigurableIdentifier
  @Column(name = "storage_name", unique = true)
  private String name;
  @ConfigurableField(description = "Storage volumes directory.", displayName = "Volumes path")
  @Column(name = "system_storage_volumes_dir")
  private String volumesDir;
  @ConfigurableField(description = "Should volumes be zero filled.", displayName = "Zero-fill volumes", type = ConfigurableFieldType.BOOLEAN)
  @Column(name = "zero_fill_volumes")
  private Boolean zeroFillVolumes;
  @ConfigurableField(description = "Timeout value in milli seconds for storage operations", displayName = "Timeout in milli seconds")
  @Column(name = "timeout_in_millis")
  private Long timeoutInMillis;

  public DirectStorageInfo() {
    this.name = StorageProperties.NAME;
  }

  public DirectStorageInfo(final String name) {
    this.name = name;
  }

  public DirectStorageInfo(final String name, final String storageInterface, final String volumesDir, final Boolean zeroFillVolumes,
      final Long timeoutInMillis) {
    this.name = name;
    this.volumesDir = volumesDir;
    this.zeroFillVolumes = zeroFillVolumes;
    this.timeoutInMillis = timeoutInMillis;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVolumesDir() {
    return volumesDir;
  }

  public void setVolumesDir(String volumesDir) {
    this.volumesDir = volumesDir;
  }

  public Boolean getZeroFillVolumes() {
    return zeroFillVolumes;
  }

  public void setZeroFillVolumes(Boolean zeroFillVolumes) {
    this.zeroFillVolumes = zeroFillVolumes;
  }

  public Long getTimeoutInMillis() {
    return timeoutInMillis;
  }

  public void setTimeoutInMillis(Long timeoutInMillis) {
    this.timeoutInMillis = timeoutInMillis;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    DirectStorageInfo other = (DirectStorageInfo) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return this.name;
  }

  @PreUpdate
  public void preUpdateChecks() {
    // EUCA-3597 Introduced a new column for timeout. Ensure that its populated in the DB the first time
    if (null == this.getTimeoutInMillis()) {
      this.setTimeoutInMillis(StorageProperties.timeoutInMillis);
    }
  }

  public static DirectStorageInfo getStorageInfo() {
    DirectStorageInfo conf = null;

    try {
      conf = Transactions.find(new DirectStorageInfo());
    } catch (Exception e) {
      LOG.warn("Direct storage information for " + StorageProperties.NAME + " not found. Loading defaults.");
      try {
        conf =
            Transactions.saveDirect(new DirectStorageInfo(StorageProperties.NAME, StorageProperties.iface, StorageProperties.storageRootDirectory,
                StorageProperties.zeroFillVolumes, StorageProperties.timeoutInMillis));
      } catch (Exception e1) {
        try {
          conf = Transactions.find(new DirectStorageInfo());
        } catch (Exception e2) {
          LOG.warn("Failed to persist and retrieve DirectStorageInfo entity");
        }
      }
    }

    if (conf == null) {
      conf =
          new DirectStorageInfo(StorageProperties.NAME, StorageProperties.iface, StorageProperties.storageRootDirectory,
              StorageProperties.zeroFillVolumes, StorageProperties.timeoutInMillis);
    }

    return conf;
  }

  @EntityUpgrade(entities = {DirectStorageInfo.class}, since = Version.v3_2_0, value = Storage.class)
  public enum DirectStorageInfoUpgrade implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger(DirectStorageInfo.DirectStorageInfoUpgrade.class);

    @Override
    public boolean apply(Class arg0) {
      try (TransactionResource tr = Entities.transactionFor(DirectStorageInfo.class)) {
        List<DirectStorageInfo> entities = Entities.query(new DirectStorageInfo());
        for (DirectStorageInfo entry : entities) {
          LOG.debug("Upgrading: " + entry);
          entry.setTimeoutInMillis(StorageProperties.timeoutInMillis);
        }
        tr.commit();
        return true;
      } catch (Exception ex) {
        throw Exceptions.toUndeclared(ex);
      }
    }
  }
}
