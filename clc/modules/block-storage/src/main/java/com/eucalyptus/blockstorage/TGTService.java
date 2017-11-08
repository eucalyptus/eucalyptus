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
