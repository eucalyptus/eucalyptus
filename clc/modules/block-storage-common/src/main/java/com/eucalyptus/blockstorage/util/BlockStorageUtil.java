/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.blockstorage.util;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;


import javax.crypto.Cipher;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.BaseRole;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.util.EucalyptusCloudException;

public class BlockStorageUtil {
  private static Logger LOG = Logger.getLogger(BlockStorageUtil.class);

  /**
   * Returns the corresponding partition for the requested componentId class running on the local host
   * 
   * @param compClass
   * @return
   */
  public static <C extends ComponentId> Partition getPartitionForLocalService(Class<C> compClass) throws EucalyptusCloudException {
    try {
      return Partitions.lookup(Components.lookup(compClass).getLocalServiceConfiguration());
    } catch (Exception e) {
      LOG.error("Error finding partition for local component: " + compClass.getCanonicalName());
      throw new EucalyptusCloudException("Failed lookup", e);
    }
  }

  public static String encryptNodeTargetPassword(String password, Partition partition) throws EucalyptusCloudException {
    try {
      if (partition == null) {
        throw new EucalyptusCloudException("Invalid partition specified. Got null");
      } else {
        PublicKey ncPublicKey = partition.getNodeCertificate().getPublicKey();
        Cipher cipher = Ciphers.RSA_PKCS1.get();
        cipher.init(Cipher.ENCRYPT_MODE, ncPublicKey, Crypto.getSecureRandomSupplier().get());
        return new String(Base64.encode(cipher.doFinal(password.getBytes())));
      }
    } catch (Exception e) {
      LOG.error("Unable to encrypt storage target password: " + e.getMessage(), e);
      throw new EucalyptusCloudException("Unable to encrypt storage target password: " + e.getMessage(), e);
    }
  }

  public static String encryptSCTargetPassword(String password) throws EucalyptusCloudException {
    PublicKey scPublicKey = SystemCredentials.lookup(Storage.class).getKeyPair().getPublic();
    Cipher cipher;
    try {
      cipher = Ciphers.RSA_PKCS1.get();
      cipher.init(Cipher.ENCRYPT_MODE, scPublicKey, Crypto.getSecureRandomSupplier().get());
      return new String(Base64.encode(cipher.doFinal(password.getBytes())));
    } catch (Exception e) {
      LOG.error("Unable to encrypted storage target password");
      throw new EucalyptusCloudException(e.getMessage(), e);
    }
  }

  public static String decryptSCTargetPassword(String encryptedPassword) throws EucalyptusCloudException {
    PrivateKey scPrivateKey = SystemCredentials.lookup(Storage.class).getPrivateKey();
    try {
      Cipher cipher = Ciphers.RSA_PKCS1.get();
      cipher.init(Cipher.DECRYPT_MODE, scPrivateKey, Crypto.getSecureRandomSupplier().get());
      return new String(cipher.doFinal(Base64.decode(encryptedPassword)));
    } catch (Exception ex) {
      LOG.error(ex);
      throw new EucalyptusCloudException("Unable to decrypt storage target password", ex);
    }
  }

  // Encrypt data using the node public key
  public static String encryptForNode(String data, Partition partition) throws EucalyptusCloudException {
    try {
      if (partition == null) {
        throw new EucalyptusCloudException("Invalid partition specified. Got null");
      } else {
        PublicKey ncPublicKey = partition.getNodeCertificate().getPublicKey();
        Cipher cipher = Ciphers.RSA_PKCS1.get();
        cipher.init(Cipher.ENCRYPT_MODE, ncPublicKey, Crypto.getSecureRandomSupplier().get());
        return new String(Base64.encode(cipher.doFinal(data.getBytes())));
      }
    } catch (Exception e) {
      LOG.error("Unable to encrypt data: " + e.getMessage(), e);
      throw new EucalyptusCloudException("Unable to encrypt data: " + e.getMessage(), e);
    }
  }

  // Decrypt data using the node private key. Primarly for VMwareBroker
  public static String decryptForNode(String data, Partition partition) throws EucalyptusCloudException {
    try {
      if (partition == null) {
        throw new EucalyptusCloudException("Invalid partition specified. Got null");
      } else {
        PrivateKey ncPrivateKey = partition.getNodePrivateKey();
        Cipher cipher = Ciphers.RSA_PKCS1.get();
        cipher.init(Cipher.DECRYPT_MODE, ncPrivateKey, Crypto.getSecureRandomSupplier().get());
        return new String(cipher.doFinal(Base64.decode(data)));
      }
    } catch (Exception e) {
      LOG.error("Unable to dencrypt data with node private key: " + e.getMessage(), e);
      throw new EucalyptusCloudException("Unable to encrypt data with node private key: " + e.getMessage(), e);
    }
  }

  // Encrypt data using the cloud public key
  public static String encryptForCloud(String data) throws EucalyptusCloudException {
    try {
      PublicKey clcPublicKey = SystemCredentials.lookup(Eucalyptus.class).getCertificate().getPublicKey();
      Cipher cipher = Ciphers.RSA_PKCS1.get();
      cipher.init(Cipher.ENCRYPT_MODE, clcPublicKey, Crypto.getSecureRandomSupplier().get());
      return new String(Base64.encode(cipher.doFinal(data.getBytes())));
    } catch (Exception e) {
      LOG.error("Unable to encrypt data: " + e.getMessage(), e);
      throw new EucalyptusCloudException("Unable to encrypt data: " + e.getMessage(), e);
    }
  }

  // Decrypt data encrypted with the Cloud public key
  public static String decryptWithCloud(String data) throws EucalyptusCloudException {
    PrivateKey clcPrivateKey = SystemCredentials.lookup(Eucalyptus.class).getPrivateKey();
    try {
      Cipher cipher = Ciphers.RSA_PKCS1.get();
      cipher.init(Cipher.DECRYPT_MODE, clcPrivateKey, Crypto.getSecureRandomSupplier().get());
      return new String(cipher.doFinal(Base64.decode(data)));
    } catch (Exception ex) {
      LOG.error(ex);
      throw new EucalyptusCloudException("Unable to decrypt data with cloud private key", ex);
    }
  }

  /**
   *
   */
  public static BaseRole getBlockStorageRole() throws EucalyptusCloudException {
    try {
      String accountNumber = Accounts.lookupAccountIdentifiersByAlias( AccountIdentifiers.BLOCKSTORAGE_SYSTEM_ACCOUNT ).getAccountNumber( );
      return Accounts.lookupRoleByName( accountNumber, StorageProperties.EBS_ROLE_NAME );
    } catch (Exception e) {
        LOG.warn("Could not find " + StorageProperties.EBS_ROLE_NAME + " role for " + AccountIdentifiers.BLOCKSTORAGE_SYSTEM_ACCOUNT
            + " account and failed to assign the role to the account", e);
        throw new EucalyptusCloudException("Could not find " + StorageProperties.EBS_ROLE_NAME + " role for " + AccountIdentifiers.BLOCKSTORAGE_SYSTEM_ACCOUNT
            + " account and failed to assign the role to the account");
    }
  }

  public static final Criterion getFailedCriterion() {
    return Restrictions.and(Restrictions.like("status", StorageProperties.Status.failed.toString()), Restrictions.isNull("deletionTime"));
  }

  public static final Criterion getExpiredCriterion(Integer deletedResourceExpiration) {
    return Restrictions.lt("deletionTime",
        new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(deletedResourceExpiration, TimeUnit.MINUTES)));
  }

  /**
   * From a given list of snapshots and a given snapshot ID, return a new list of 
   * snapshots containing the given snapshot and each parent (previous) 
   * snapshot.
   *  
   * @param  snapshotList   the list of snapshots to search for members of the chain.
   *                         The list must be in descending date order, so a 
   *                         parent snapshot must never appear earlier in the 
   *                         list than any of its children.
   * @param  lastSnapshotId the snapshot that ends the chain we will construct
   * @return null, or a new list of existing SnapshotInfo objects. The last
   *          element will be the SnapshotInfo object for the lastSnapshotId.
   *          Each subsequent parent (previous) snapshot found in the 
   *          snapshotList will be inserted at the beginning of the list. 
   *          List processing ends when either the parent of the current 
   *          snapshot does not exist in the given snapshotList, or the parent 
   *          snapshot ID stored in the current snapshot is null.
   *          The returned linked list of snapshots can be applied in order
   *          to create a volume from the given snapshot ID, if the first 
   *          element of the returned list is a full snapshot. Note the 
   *          returned list does not guarantee that the first element is a 
   *          full snapshot.
   *          If the new list is empty, 'null' will be returned.
   */
  public static final List<SnapshotInfo> getSnapshotChain(List<SnapshotInfo> snapshotList, String lastSnapshotId) {
    if (snapshotList == null || lastSnapshotId == null) {
      return null;
    }
    LinkedList<SnapshotInfo> snapshotChain = new LinkedList<SnapshotInfo>();
    String currentSnapshotId = lastSnapshotId;
    for (SnapshotInfo currentSnapshot : snapshotList) {
      if (currentSnapshot != null) {
        if (currentSnapshot.getSnapshotId() != null &&
            currentSnapshot.getSnapshotId().equals(currentSnapshotId)) {
          snapshotChain.add(0, currentSnapshot);
          currentSnapshotId = currentSnapshot.getPreviousSnapshotId();
          if (currentSnapshotId == null) {
            break;
          }
        }
      }
    }
    if (snapshotChain.isEmpty()) {
      return null;
    } else {
      return snapshotChain;
    }
  }  // end getSnapshotChain

}
