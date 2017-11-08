/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.blockstorage.util;

import org.hibernate.criterion.Criterion;

import com.eucalyptus.auth.principal.BaseRole;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.Partition;
import com.eucalyptus.util.EucalyptusCloudException;

import java.util.List;

/**
 * Created by wesw on 6/18/14.
 */
public class BlockStorageUtilSvcImpl implements BlockStorageUtilSvc {

  @Override
  public <C extends ComponentId> Partition getPartitionForLocalService(Class<C> compClass) throws EucalyptusCloudException {
    return BlockStorageUtil.getPartitionForLocalService(compClass);
  }

  @Override
  public String encryptNodeTargetPassword(String password, Partition partition) throws EucalyptusCloudException {
    return BlockStorageUtil.encryptNodeTargetPassword(password, partition);
  }

  @Override
  public String encryptSCTargetPassword(String password) throws EucalyptusCloudException {
    return BlockStorageUtil.encryptSCTargetPassword(password);
  }

  @Override
  public String decryptSCTargetPassword(String encryptedPassword) throws EucalyptusCloudException {
    return BlockStorageUtil.decryptSCTargetPassword(encryptedPassword);
  }

  @Override
  public String encryptForNode(String data, Partition partition) throws EucalyptusCloudException {
    return BlockStorageUtil.encryptForNode(data, partition);
  }

  @Override
  public String decryptForNode(String data, Partition partition) throws EucalyptusCloudException {
    return BlockStorageUtil.decryptForNode(data, partition);
  }

  @Override
  public String encryptForCloud(String data) throws EucalyptusCloudException {
    return BlockStorageUtil.encryptForCloud(data);
  }

  @Override
  public String decryptWithCloud(String data) throws EucalyptusCloudException {
    return BlockStorageUtil.decryptWithCloud(data);
  }

  @Override
  public Criterion getFailedCriterion() {
    return BlockStorageUtil.getFailedCriterion();
  }

  @Override
  public Criterion getExpiredCriterion(Integer deletedResourceExpiration) {
    return BlockStorageUtil.getExpiredCriterion(deletedResourceExpiration);
  }

  @Override
  public List<SnapshotInfo> getSnapshotChain(List<SnapshotInfo> snapshotList, String lastSnapshotId) {
    return BlockStorageUtil.getSnapshotChain(snapshotList, lastSnapshotId);
  }

}
