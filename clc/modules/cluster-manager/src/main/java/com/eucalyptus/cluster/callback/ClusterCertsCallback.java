/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.cluster.callback;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.util.InvalidCredentialsException;
import com.eucalyptus.util.async.FailedRequestException;
import edu.ucsb.eucalyptus.msgs.GetKeysResponseType;
import edu.ucsb.eucalyptus.msgs.GetKeysType;

public class ClusterCertsCallback extends ClusterLogMessageCallback<GetKeysType, GetKeysResponseType> {
  private static Logger LOG = Logger.getLogger( ClusterCertsCallback.class );

  public ClusterCertsCallback( ) {
    super( ( GetKeysType ) new GetKeysType( "self" ).regarding( ) );
  }
  
  /**
   * TODO: DOCUMENT
   * @param msg
   */
  @Override
  public void fire( GetKeysResponseType msg ) {
    if( !this.getSubject( ).checkCerts( msg.getCerts( ) ) ) {
      throw new InvalidCredentialsException( "Cluster credentials are invalid: " + this.getSubject( ).getName( ) );
//    } else if( !Bootstrap.isFinished( ) ) {
//      throw new InvalidCredentialsException( "Bootstrap hasn't finished yet -- stalling." );
    }
  }

  /**
   * @see com.eucalyptus.cluster.callback.StateUpdateMessageCallback#fireException(com.eucalyptus.util.async.FailedRequestException)
   * @param t
   */
  @Override
  public void fireException( FailedRequestException t ) {
    LOG.debug( "Request to " + this.getSubject( ).getName( ) + " failed: " + t.getMessage( ) );
  }

}
