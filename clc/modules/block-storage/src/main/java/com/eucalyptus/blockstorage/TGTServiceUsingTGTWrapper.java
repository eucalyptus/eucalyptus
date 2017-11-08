/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.blockstorage;

import javax.annotation.Nonnull;

import com.eucalyptus.util.EucalyptusCloudException;

/**
 * Created by wesw on 6/18/14.
 */
public class TGTServiceUsingTGTWrapper implements TGTService {

  @Override
  public void start() {
    TGTWrapper.start();
  }

  @Override
  public void stop() {
    TGTWrapper.stop();
  }

  @Override
  public void precheckService(Long timeout) throws EucalyptusCloudException {
    TGTWrapper.precheckService(timeout);
  }

  @Override
  public void checkService(Long timeout) throws EucalyptusCloudException {
    TGTWrapper.checkService(timeout);
  }

  @Override
  public void createTarget(@Nonnull String volumeId, int tid, @Nonnull String name, @Nonnull Long timeout) throws EucalyptusCloudException {
    TGTWrapper.createTarget(volumeId, tid, name, timeout);
  }

  @Override
  public void deleteTarget(@Nonnull String volumeId, int tid, @Nonnull Long timeout, boolean force) throws EucalyptusCloudException {
    TGTWrapper.deleteTarget(volumeId, tid, timeout, force);
  }

  @Override
  public void createLun(@Nonnull String volumeId, int tid, int lun, @Nonnull String resourcePath, @Nonnull Long timeout)
      throws EucalyptusCloudException {
    TGTWrapper.createLun(volumeId, tid, lun, resourcePath, timeout);
  }

  @Override
  public void deleteLun(@Nonnull String volumeId, int tid, int lun, @Nonnull Long timeout) throws EucalyptusCloudException {
    TGTWrapper.deleteLun(volumeId, tid, lun, timeout);
  }

  @Override
  public void bindUser(@Nonnull String volumeId, @Nonnull String user, int tid, @Nonnull Long timeout) throws EucalyptusCloudException {
    TGTWrapper.bindUser(volumeId, user, tid, timeout);
  }

  @Override
  public void bindTarget(@Nonnull String volumeId, int tid, @Nonnull Long timeout) throws EucalyptusCloudException {
    TGTWrapper.bindTarget(volumeId, tid, timeout);
  }

  @Override
  public void unbindTarget(String volumeId, int tid, @Nonnull Long timeout) throws EucalyptusCloudException {
    TGTWrapper.unbindTarget(volumeId, tid, timeout);
  }

  @Override
  public boolean targetExists(@Nonnull String volumeId, int tid, String resource, @Nonnull Long timeout) throws EucalyptusCloudException {
    return TGTWrapper.targetExists(volumeId, tid, resource, timeout);
  }

  @Override
  public boolean targetConfigured(@Nonnull String volumeId, int tid, String resource, @Nonnull Long timeout, 
      String user, boolean checkInitiators) throws EucalyptusCloudException {
    return TGTWrapper.targetConfigured(volumeId, tid, resource, timeout, user, checkInitiators);
  }

  @Override
  public boolean targetHasLun(@Nonnull String volumeId, int tid, int lun, @Nonnull Long timeout) throws EucalyptusCloudException {
    return TGTWrapper.targetHasLun(volumeId, tid, lun, timeout);
  }

  @Override
  public void addUser(@Nonnull String username, @Nonnull String password, @Nonnull Long timeout) throws EucalyptusCloudException {
    TGTWrapper.addUser(username, password, timeout);
  }

  @Override
  public void deleteUser(@Nonnull String username, @Nonnull Long timeout) throws EucalyptusCloudException {
    TGTWrapper.deleteUser(username, timeout);
  }

  @Override
  public boolean userExists(@Nonnull String username, @Nonnull Long timeout) throws EucalyptusCloudException {
    return TGTWrapper.userExists(username, timeout);
  }
}
