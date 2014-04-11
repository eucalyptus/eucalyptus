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
package com.eucalyptus.compute.entities;

import java.util.List;
import java.util.concurrent.Callable;
import com.eucalyptus.address.Address;
import com.eucalyptus.blockstorage.Snapshot;
import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.AccountMetadata;
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.keys.SshKeyPair;
import com.eucalyptus.network.ExtantNetwork;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.network.PrivateNetworkIndex;
import com.eucalyptus.tags.Tag;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.vm.VmInstance;
import com.google.common.collect.ImmutableList;

/**
 * Update entities to clear possibly incorrect account names
 */
@Upgrades.PostUpgrade( value = Eucalyptus.class, since = Upgrades.Version.v4_0_0 )
public class AccountName400Upgrade extends AccountMetadata.AccountName400UpgradeSupport implements Callable<Boolean> {

  private static final List<Class<? extends AccountMetadata>> accountMetadataClasses =
      ImmutableList.<Class<? extends AccountMetadata>>builder()
          .add( Address.class )
          .add( ExtantNetwork.class )
          .add( ImageInfo.class )
          .add( NetworkGroup.class )
          .add( PrivateNetworkIndex.class )
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
