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
package com.eucalyptus.compute.entities;

import java.util.List;
import java.util.concurrent.Callable;
import com.eucalyptus.compute.common.internal.address.AllocatedAddressEntity;
import com.eucalyptus.compute.common.internal.blockstorage.Snapshot;
import com.eucalyptus.compute.common.internal.blockstorage.Volume;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.AccountMetadata;
import com.eucalyptus.compute.common.internal.images.ImageInfo;
import com.eucalyptus.compute.common.internal.keys.SshKeyPair;
import com.eucalyptus.compute.common.internal.network.NetworkGroup;
import com.eucalyptus.compute.common.internal.tags.Tag;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.google.common.collect.ImmutableList;

/**
 * Update entities to clear possibly incorrect account names
 */
@Upgrades.PostUpgrade( value = Eucalyptus.class, since = Upgrades.Version.v4_0_0 )
public class AccountName400Upgrade extends AccountMetadata.AccountName400UpgradeSupport implements Callable<Boolean> {

  private static final List<Class<? extends AccountMetadata>> accountMetadataClasses =
      ImmutableList.<Class<? extends AccountMetadata>>builder()
          .add( AllocatedAddressEntity.class )
          .add( ImageInfo.class )
          .add( NetworkGroup.class )
          .add( Snapshot.class )
          .add( SshKeyPair.class )
          .add( Tag.class )
          .add( VmInstance.class )
          .add( Volume.class )
          .build();

  @Override
  protected List<Class<? extends AccountMetadata>> getAccountMetadataClasses() {
    return accountMetadataClasses;
  }
}
