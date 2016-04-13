/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cluster.callback

import com.eucalyptus.util.async.BroadcastCallback
import edu.ucsb.eucalyptus.msgs.BroadcastNetworkInfoResponseType
import edu.ucsb.eucalyptus.msgs.BroadcastNetworkInfoType
import org.apache.log4j.Logger

/**
 *
 */
class BroadcastNetworkInfoCallback extends BroadcastCallback<BroadcastNetworkInfoType, BroadcastNetworkInfoResponseType> {

  private static Logger logger = Logger.getLogger( BroadcastNetworkInfoCallback.class );

  private final String networkInfo
  private final String version
  private final String appliedVersion

  public BroadcastNetworkInfoCallback(
      final String encodedNetworkInfo,
      final String version,
      final String appliedVersion
  ) {
    this.networkInfo = encodedNetworkInfo
    this.version = version
    this.appliedVersion = appliedVersion

    this.setRequest( new BroadcastNetworkInfoType(
        networkInfo: encodedNetworkInfo,
        version: version,
        appliedVersion: appliedVersion
    ) )
  }

  @Override
  public void fire( BroadcastNetworkInfoResponseType response ) {
  }

  @Override
  public void initialize( BroadcastNetworkInfoType request ) {
  }

  @Override
  public void fireException( Throwable e ) {
    logger.error( "Error in network information broadcast", e )
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
