/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.network.applicator;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.eucalyptus.cluster.NIInstance;
import com.eucalyptus.cluster.NINetworkInterface;
import com.eucalyptus.cluster.NetworkInfo;
import com.eucalyptus.network.NetworkMode;
import com.eucalyptus.network.PublicAddresses;
import com.eucalyptus.util.FUtils;

/**
 *
 */
public class ClearDirtyPublicAddressesApplicator extends ModeSpecificApplicator {

  public ClearDirtyPublicAddressesApplicator( ) {
    super( EnumSet.of( NetworkMode.VPCMIDO ) );
  }

  @Override
  protected void modeApply(
      final NetworkMode mode,
      final ApplicatorContext context,
      final ApplicatorChain chain
  ) throws ApplicatorException {

    final NetworkInfo networkInfo = context.getNetworkInfo( );
    if ( networkInfo.getVersion( ) != null && networkInfo.getVersion().equals( networkInfo.getAppliedVersion( ) ) ) {
      final Set<String> broadcastPublicIps = networkInfo.getInstances( ).stream( )
          .flatMap( FUtils.chain( NIInstance::getNetworkInterfaces, Collection::stream ) )
          .map( NINetworkInterface::getPublicIp )
          .filter( Objects::nonNull )
          .collect( Collectors.toSet( ) );

      PublicAddresses.dirtySnapshot( ).stream( )
          .filter( publicIp -> !broadcastPublicIps.contains( publicIp ) )
          .forEach( PublicAddresses::clearDirty );
    }

    chain.applyNext( context );
  }
}
