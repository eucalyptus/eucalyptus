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
public interface BlockStorageUtilSvc {

  public <C extends ComponentId> Partition getPartitionForLocalService(Class<C> compClass) throws EucalyptusCloudException;

  public String encryptNodeTargetPassword(String password, Partition partition) throws EucalyptusCloudException;

  public String encryptSCTargetPassword(String password) throws EucalyptusCloudException;

  public String decryptSCTargetPassword(String encryptedPassword) throws EucalyptusCloudException;

  public String encryptForNode(String data, Partition partition) throws EucalyptusCloudException;

  public String decryptForNode(String data, Partition partition) throws EucalyptusCloudException;

  public String encryptForCloud(String data) throws EucalyptusCloudException;

  public String decryptWithCloud(String data) throws EucalyptusCloudException;

  public Criterion getFailedCriterion();

  public Criterion getExpiredCriterion(Integer deletedResourceExpiration);
  
  public List<SnapshotInfo> getSnapshotChain(List<SnapshotInfo> snapshotList, String lastSnapshotId);
  
}
