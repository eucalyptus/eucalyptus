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
package com.eucalyptus.blockstorage;

import javax.annotation.Nonnull;

import com.eucalyptus.util.EucalyptusCloudException;

/**
 * Created by wesw on 6/18/14.
 */
public interface TGTService {

  public void start();

  public void stop();

  public void precheckService(Long timeout) throws EucalyptusCloudException;

  public void checkService(Long timeout) throws EucalyptusCloudException;

  public void createTarget(@Nonnull String volumeId, int tid, @Nonnull String name, @Nonnull Long timeout) throws EucalyptusCloudException;

  public void deleteTarget(@Nonnull String volumeId, int tid, @Nonnull Long timeout, boolean force) throws EucalyptusCloudException;

  public void createLun(@Nonnull String volumeId, int tid, int lun, @Nonnull String resourcePath, @Nonnull Long timeout)
      throws EucalyptusCloudException;

  public void deleteLun(@Nonnull String volumeId, int tid, int lun, @Nonnull Long timeout) throws EucalyptusCloudException;

  public void bindUser(@Nonnull String volumeId, @Nonnull String user, int tid, @Nonnull Long timeout) throws EucalyptusCloudException;

  public void bindTarget(@Nonnull String volumeId, int tid, @Nonnull Long timeout) throws EucalyptusCloudException;

  public void unbindTarget(String volumeId, int tid, @Nonnull Long timeout) throws EucalyptusCloudException;

  public boolean targetExists(@Nonnull String volumeId, int tid, String resource, @Nonnull Long timeout) throws EucalyptusCloudException;

  public boolean targetConfigured(@Nonnull String volumeId, int tid, String resource, @Nonnull Long timeout, 
      String user, boolean checkInitiators) throws EucalyptusCloudException;

  public boolean targetHasLun(@Nonnull String volumeId, int tid, int lun, @Nonnull Long timeout) throws EucalyptusCloudException;

  public void addUser(@Nonnull String username, @Nonnull String password, @Nonnull Long timeout) throws EucalyptusCloudException;

  public void deleteUser(@Nonnull String username, @Nonnull Long timeout) throws EucalyptusCloudException;

  public boolean userExists(@Nonnull String username, @Nonnull Long timeout) throws EucalyptusCloudException;

}
