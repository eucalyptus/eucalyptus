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
package com.eucalyptus.cluster.callback

import com.eucalyptus.crypto.util.B64
import com.eucalyptus.util.async.BroadcastCallback
import com.google.common.base.Charsets
import edu.ucsb.eucalyptus.msgs.BroadcastNetworkInfoResponseType
import edu.ucsb.eucalyptus.msgs.BroadcastNetworkInfoType
import org.apache.log4j.Logger

/**
 *
 */
class BroadcastNetworkInfoCallback extends BroadcastCallback<BroadcastNetworkInfoType, BroadcastNetworkInfoResponseType> {

  private static Logger logger = Logger.getLogger( BroadcastNetworkInfoCallback.class );

  final String networkInfo

  public BroadcastNetworkInfoCallback( final String networkInfo ) {
    this.networkInfo = networkInfo
    this.setRequest( new BroadcastNetworkInfoType(
        networkInfo: new String( B64.standard.enc( networkInfo.getBytes( Charsets.UTF_8 ) ), Charsets.UTF_8 )
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
    return new BroadcastNetworkInfoCallback( this.networkInfo );
  }

  @Override
  public String toString( ) {
    return "BroadcastNetworkInfoCallback " + this.networkInfo;
  }
}
