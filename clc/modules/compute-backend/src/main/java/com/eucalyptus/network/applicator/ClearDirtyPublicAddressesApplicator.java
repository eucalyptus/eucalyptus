/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.network.applicator;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import com.eucalyptus.cluster.common.broadcast.BNIInstance;
import com.eucalyptus.cluster.common.broadcast.BNINetworkInterface;
import com.eucalyptus.cluster.common.broadcast.BNetworkInfo;
import com.eucalyptus.network.NetworkMode;
import com.eucalyptus.network.PublicAddresses;

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

    final BNetworkInfo networkInfo = context.getNetworkInfo( );
    if ( networkInfo.version( ).isDefined( ) && networkInfo.appliedVersion( ).isDefined( ) &&
        networkInfo.version( ).get( ).equals( networkInfo.appliedVersion( ).get( ) ) ) {
      final Set<String> broadcastPublicIps = networkInfo.instances( )
          .flatMap( BNIInstance::networkInterfaces )
          .flatMap( BNINetworkInterface::publicIp )
          .collect( Collectors.toSet( ) );

      PublicAddresses.dirtySnapshot( ).stream( )
          .filter( publicIp -> !broadcastPublicIps.contains( publicIp ) )
          .forEach( PublicAddresses::clearDirty );
    }

    chain.applyNext( context );
  }
}
