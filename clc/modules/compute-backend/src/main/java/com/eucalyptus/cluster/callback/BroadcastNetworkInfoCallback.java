/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.cluster.callback;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cluster.common.msgs.BroadcastNetworkInfoResponseType;
import com.eucalyptus.cluster.common.msgs.BroadcastNetworkInfoType;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.BroadcastCallback;

/**
 *
 */
public class BroadcastNetworkInfoCallback extends BroadcastCallback<BroadcastNetworkInfoType, BroadcastNetworkInfoResponseType> {

  private static Logger logger = Logger.getLogger( BroadcastNetworkInfoCallback.class );

  private final String networkInfo;
  private final String version;
  private final String appliedVersion;

  public BroadcastNetworkInfoCallback(
      final String encodedNetworkInfo,
      final String version,
      final String appliedVersion
  ) {
    this.networkInfo = encodedNetworkInfo;
    this.version = version;
    this.appliedVersion = appliedVersion;

    this.setRequest( new BroadcastNetworkInfoType( version, appliedVersion, encodedNetworkInfo ) );
  }

  @Override
  public void fire( BroadcastNetworkInfoResponseType response ) {
  }

  @Override
  public void initialize( BroadcastNetworkInfoType request ) {
  }

  @Override
  public void fireException( Throwable e ) {
    if ( Bootstrap.isShuttingDown( ) ) {
      logger.warn( "Error in network information broadcast: " + e.getMessage( ) );
    } else {
      logger.error( "Error in network information broadcast: " + e.getMessage( ), e );
    }
  }

  @Override
  public BroadcastCallback<BroadcastNetworkInfoType, BroadcastNetworkInfoResponseType> newInstance( ) {
    return new BroadcastNetworkInfoCallback( this.networkInfo, this.version, this.appliedVersion );
  }

  @Override
  public String toString( ) {
    return "BroadcastNetworkInfoCallback " + this.version + "/" + this.appliedVersion;
  }
}
