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

package com.eucalyptus.blockstorage.util;

import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.system.BaseDirectory;
import com.google.common.collect.ImmutableSet;

import edu.ucsb.eucalyptus.util.StreamConsumer;

public class StorageProperties {
  private static Logger LOG = Logger.getLogger(StorageProperties.class);

  /*
   * Threshold after which the SC will cleanup the failed volume record by deleting it. This value ensures that the SC can send the volume state to
   * the CLC possibly multiple times so that a single missed message will not cause the CLC to hit its much longer timeout for volume state (2 hrs).
   */
  public static final long FAILED_STATE_CLEANUP_THRESHOLD_MS = 10 * 60 * 1000l; // 10 minutes
  public static final String storageRootDirectory = BaseDirectory.VAR.getChildPath("volumes");
  public static final long GB = 1024 * 1024 * 1024;
  public static final long MB = 1024 * 1024;
  public static final long KB = 1024;
  public static final int MAX_TOTAL_VOLUME_SIZE = 500;
  public static final int MAX_VOLUME_SIZE = 100;
  public static final String zeroFillVolumesTxt = "false";
  public static final boolean zeroFillVolumes = Boolean.valueOf( zeroFillVolumesTxt );
  public static final long timeoutInMillis = 10000;

  public static boolean enableSnapshots = false;
  public static boolean enableStorage = false;
  public static boolean shouldEnforceUsageLimits = true;
  public static final String DEFAULT_STORE_PREFIX = "iqn.2009-06.com.eucalyptus.";
  public static String STORE_PREFIX = DEFAULT_STORE_PREFIX;
  public static String WALRUS_URL = "http://localhost:8773/services/objectstorage";
  public static String NAME = "unregistered";
  public static String STORAGE_HOST = "127.0.0.1";
  public static final String ISCSI_INITIATOR_NAME_CONF = "/etc/iscsi/initiatorname.iscsi";
  public static String SC_INITIATOR_IQN = null;
  public static final String EUCA_ROOT_WRAPPER = BaseDirectory.LIBEXEC.toString() + "/euca_rootwrap";
  public static final String blockSize = "1M";
  public static String DAS_DEVICE = "/dev/blockdev";
  public static final String TOKEN_PREFIX = "sc://"; // Used to indicate a token should be resolved to an SC

  public static final String SNAPSHOT_BUCKET_PREFIX = "snapshots-";
  public static final String EBS_ROLE_NAME = "EBSUpload";
  public static final String S3_BUCKET_ACCESS_POLICY_NAME = "S3EBSBucketAccess";
  public static final String S3_OBJECT_ACCESS_POLICY_NAME = "S3EBSObjectAccess";

  public static final String DEFAULT_ASSUME_ROLE_POLICY =
      "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":[\"s3.amazonaws.com\"]},\"Action\":[\"sts:AssumeRole\"]}]}";

  public static final String S3_SNAPSHOT_BUCKET_ACCESS_POLICY =
      "{\"Statement\":[" + "{" + "\"Effect\":\"Allow\"," + "\"Action\": [\"s3:*\"]," + "\"Resource\": \"arn:aws:s3:::*\"" + "}" + "]}";

  public static final String S3_SNAPSHOT_OBJECT_ACCESS_POLICY =
      "{\"Statement\":[" + "{" + "\"Effect\":\"Allow\"," + "\"Action\": [\"s3:*\"]," + "\"Resource\": \"arn:aws:s3:::*/*\"" + "}" + "]}";

  public static final ImmutableSet<String> DELTA_GENERATION_STATE_EXCLUSION = ImmutableSet.of(StorageProperties.Status.failed.toString(),
      StorageProperties.Status.deleting.toString(), StorageProperties.Status.deletedfromebs.toString(), StorageProperties.Status.deleted.toString());

  public static final ImmutableSet<String> DELTA_RESTORATION_STATE_EXCLUSION = ImmutableSet.of(StorageProperties.Status.failed.toString(),
      StorageProperties.Status.deleted.toString());

  public static final Criterion SNAPSHOT_DELTA_GENERATION_CRITERION =
      Restrictions.not(Restrictions.in("status", DELTA_GENERATION_STATE_EXCLUSION));
  
  public static final Criterion SNAPSHOT_DELTA_RESTORATION_CRITERION =
      Restrictions.not(Restrictions.in("status", DELTA_RESTORATION_STATE_EXCLUSION));


  public static String formatVolumeAttachmentTokenForTransfer(String token, String volumeId) {
    return TOKEN_PREFIX + volumeId + "," + token;
  }

  private static String getSCIqn() {
    try {
      Runtime rt = Runtime.getRuntime();
      Process proc = rt.exec(new String[] {StorageProperties.EUCA_ROOT_WRAPPER, "cat", ISCSI_INITIATOR_NAME_CONF});
      StreamConsumer error = new StreamConsumer(proc.getErrorStream());
      ConfigParser output = new ConfigParser(proc.getInputStream());
      error.start();
      output.start();
      output.join();
      error.join();
      if (output.getValues() != null && output.getValues().containsKey("InitiatorName")) {
        return output.getValues().get("InitiatorName");
      }
    } catch (Exception t) {
      LOG.error("Failed to get local SC's initiator iqn from " + ISCSI_INITIATOR_NAME_CONF, t);
    }
    return null;
  }

  public static String getStorageIqn() {
    if (SC_INITIATOR_IQN == null) {
      SC_INITIATOR_IQN = getSCIqn();
    }
    return SC_INITIATOR_IQN;
  }

  public static void updateName() {
    try {
      StorageProperties.NAME = Components.lookup(Storage.class).getLocalServiceConfiguration().getPartition();
    } catch (NoSuchElementException ex) {
      LOG.error(ex, ex);
      LOG.error("Failed to configure Storage Controller NAME.");
      throw ex;
    }
  }

  public static void updateStorageHost() {
    try {
      STORAGE_HOST = Components.lookup(Storage.class).getLocalServiceConfiguration().getHostName();
    } catch (NoSuchElementException ex) {
      LOG.error(ex, ex);
      LOG.error("Failed to configure Storage Controller HOST (given the name " + StorageProperties.NAME + ".");
    }
  }

  public static void updateStorageHost(String hostName) {
    STORAGE_HOST = hostName;
  }

  public static void updateWalrusUrl() {
    try {
      ServiceConfiguration walrusConfig = Topology.lookup(ObjectStorage.class);
      WALRUS_URL = ServiceUris.remote(walrusConfig).toASCIIString();
      StorageProperties.enableSnapshots = true;
      LOG.debug("Setting WALRUS_URL to: " + WALRUS_URL);
    } catch (Exception e) {
      LOG.warn("Could not obtain walrus information. Snapshot functionality may be unavailable. Have you registered ObjectStorage?");
      StorageProperties.enableSnapshots = false;
    }
  }

  //TODO: Split this enum which is overloaded, used by snapshots and volumes, and
  // use AWS's states for compatibility where possible. see:
  // http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/ec2/model/SnapshotState.html
  // http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/ec2/model/VolumeState.html
  // Snapshots: 
  //   "pending"   == AWS "Pending" \_ Are these two any different in Euca?
  //   "creating"  == AWS "Pending" /
  //   "available" == AWS "Completed"
  //   "failed"    == AWS "Error"
  //   "delet*" are Euca-specific
  // Volumes:
  //   "creating"  == AWS "Creating"
  //   "available" == AWS "Available"
  //   "failed"    == AWS "Error"
  //   "deleting"  == AWS "Deleting"
  //   "deleted"   == AWS "Deleted"
  //   "pending" and "deletedfromebs" not used for volumes

  public enum Status {
    creating, available, pending, failed, deleting, deleted, deletedfromebs
  }
}
