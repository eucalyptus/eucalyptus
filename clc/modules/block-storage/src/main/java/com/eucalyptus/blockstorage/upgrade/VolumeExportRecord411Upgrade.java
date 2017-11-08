/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

package com.eucalyptus.blockstorage.upgrade;

import static com.eucalyptus.upgrade.Upgrades.Version.v4_1_1;

import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.config.StorageControllerConfiguration;
import com.eucalyptus.blockstorage.entities.VolumeExportRecord;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

/**
 * Upgrade code for modifying elements of VolumeExportRecord rows from 4.0.2 format to 4.1.0 format. This upgrade logic should technically reside in
 * block-storage-common package along with VolumeExportRecord.java but a new class has been created here since the code refers to
 * StorageControllerConfiguration which is accessible only from the block-storage package
 * 
 * @author Swathi Gangisetty
 */
public class VolumeExportRecord411Upgrade {

  @EntityUpgrade(entities = {VolumeExportRecord.class}, since = v4_1_1, value = Storage.class)
  public static enum UpdateConnectionStringPrefix implements Predicate<Class> {
    INSTANCE;
    private static final Logger LOG = Logger.getLogger(UpdateConnectionStringPrefix.class);

    @Override
    public boolean apply(@Nullable Class arg0) {
      LOG.info("Entity upgrade for VolumeExportRecord entities - modifying connection_string field");

      final Map<String, String> clusterEbsBackendMap = Maps.newHashMap();

      // populate partition name and ebs backend map, use StorageControllerConfiguration entity
      try {
        LOG.info("Gathering EBS backends for all Storage Controllers in the cloud");
        Transactions.each(new StorageControllerConfiguration(), new Callback<StorageControllerConfiguration>() {

          @Override
          public void fire(StorageControllerConfiguration input) {
            clusterEbsBackendMap.put(input.getPartition(), input.getBlockStorageManager());
          }
        });
      } catch (Exception e) {
        LOG.warn("Failed to lookup Storage Controller Configuration. Insufficient information for updating VolumeExportRecord", e);
        Exceptions.toUndeclared("Failed to lookup Storage Controller Configuration. Insufficient information for updating VolumeExportRecord", e);
      }

      // tag the protocol and provider name as prefix to connection string if its not already there
      try {
        LOG.info("Iterating through all VolumeExportRecords in the cloud");
        Transactions.each(new VolumeExportRecord(), new Callback<VolumeExportRecord>() {

          @Override
          public void fire(VolumeExportRecord input) {

            try {
              if (input.getIsActive() != null && input.getIsActive()) { // check if export record is active
                String ebsBackend = null;
                String connectionString = input.getConnectionString();
                LOG.debug("VolumeExportRecord connection_string under scrutiny: " + connectionString);

                // Look for ebs backend in the map using partition name of the export record as key
                if ((ebsBackend = clusterEbsBackendMap.get(input.getVolume().getScName())) != null) {
                  String prefix = new String();
                  switch (ebsBackend) {

                    case "das":
                    case "overlay":
                      if (connectionString.startsWith("iscsi,tgt,")) {
                        LOG.debug("Connection string already has the correct prefix. Skipping connection string update");
                        return;
                      } else {
                        prefix = "iscsi,tgt,";
                        break;
                      }
                    case "equallogic":
                      if (connectionString.startsWith("iscsi,equallogic,")) {
                        LOG.debug("Connection string already has the correct prefix. Skipping connection string update");
                        return;
                      } else {
                        prefix = "iscsi,equallogic,";
                        break;
                      }
                    case "emc-vnx":
                      if (connectionString.startsWith("iscsi,emc-vnx,")) {
                        LOG.debug("Connection string already has the correct prefix. Skipping connection string update");
                        return;
                      } else {
                        prefix = "iscsi,emc-vnx,";
                        break;
                      }
                    case "emc-vnx-flare31":
                      if (connectionString.startsWith("iscsi,emc-vnx-flare31,")) {
                        LOG.debug("Connection string already has the correct prefix. Skipping connection string update");
                        return;
                      } else {
                        prefix = "iscsi,emc-vnx-flare31,";
                        break;
                      }
                    case "netapp":
                      if (connectionString.startsWith("iscsi,netapp,")) {
                        LOG.debug("Connection string already has the correct prefix. Skipping connection string update");
                        return;
                      } else {
                        prefix = "iscsi,netapp,";
                        break;
                      }
                    case "(netapp-nextgen)":
                      if (connectionString.startsWith("iscsi,netapp-nextgen,")) {
                        LOG.debug("Connection string already has the correct prefix. Skipping connection string update");
                        return;
                      } else {
                        prefix = "iscsi,netapp-nextgen,";
                        break;
                      }
                    case "ceph-rbd":
                      if (connectionString.startsWith("rbd,ceph,")) {
                        LOG.debug("Connection string already has the correct prefix. Skipping connection string update");
                        return;
                      } else {
                        prefix = "rbd,ceph,";
                        break;
                      }
                    default:
                      LOG.warn("Unknown block storage manager: " + ebsBackend + ". Skipping connection string update");
                      return;
                  }

                  if (StringUtils.isNotBlank(prefix)) {
                    String newConnectionString = prefix + connectionString;
                    LOG.debug("Modifying VolumeExportRecord connection_string to: " + newConnectionString);
                    input.setConnectionString(newConnectionString);
                    return;
                  } else {
                    LOG.warn("Unable to construct a valid prefix. Skipping connection string update");
                    return;
                  }

                } else {
                  LOG.warn("No EBS backend found in cloud for SC " + input.getVolume().getScName() + ". Skipping connection string update");
                  return;
                }
              } else {
                LOG.debug("Export record is invalidated. Skipping prefix check of connection string");
                return;
              }
            } catch (Exception e) {
              LOG.warn("Failed to process VolumeExportRecord", e);
              return;
            }
          }
        });
      } catch (Exception e) {
        LOG.warn("Failed to perform entity upgrade for VolumeExportRecord entities", e);
        Exceptions.toUndeclared("Failed to perform entity upgrade for VolumeExportRecord entities", e);
      }

      return true;
    }
  }
}
