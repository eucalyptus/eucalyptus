/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.client;

import java.security.GeneralSecurityException;

import org.apache.log4j.Logger;
import org.mule.DefaultMuleMessage;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;

import com.eucalyptus.ws.client.pipeline.ClusterClientPipeline;
import com.eucalyptus.ws.client.pipeline.InternalClientPipeline;
import com.eucalyptus.ws.client.pipeline.LogClientPipeline;
import com.eucalyptus.ws.handlers.NioResponseHandler;
import com.eucalyptus.ws.handlers.wssecurity.InternalWsSecHandler;

import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

public class NioMessageDispatcher extends AbstractMessageDispatcher {
  private static Logger LOG = Logger.getLogger( NioMessageDispatcher.class );
  private Client client;

  public NioMessageDispatcher( OutboundEndpoint outboundEndpoint ) {
    super( outboundEndpoint );
    this.doActivate( outboundEndpoint );
  }
  
  public void doActivate( OutboundEndpoint outboundEndpoint ) {
    if( this.client != null ) {
      this.client.cleanup();
      this.client = null;
    }
    String host = outboundEndpoint.getEndpointURI( ).getHost( );
    int port = outboundEndpoint.getEndpointURI( ).getPort( );
    String servicePath = outboundEndpoint.getEndpointURI( ).getPath( );
    try {
      this.client = new NioClient( host, port, servicePath, new InternalClientPipeline( new NioResponseHandler( ) ) );
    } catch ( GeneralSecurityException e ) {
      LOG.error( e );
    }    
  }
  
  @Override
  protected void doDispatch( final MuleEvent muleEvent ) throws Exception {
    this.client.dispatch( (EucalyptusMessage) muleEvent.getMessage( ).getPayload( ) );
  }

  @Override
  protected MuleMessage doSend( final MuleEvent muleEvent ) throws Exception {
    MuleMessage muleMsg = muleEvent.getMessage( );
    EucalyptusMessage request = ( EucalyptusMessage ) muleMsg.getPayload( );
    EucalyptusMessage response = client.send( request );
    return new DefaultMuleMessage( response );
  }

  @Override
  protected void doDispose() {}

  @Override
  protected void doConnect() throws Exception {}

  @Override
  protected void doDisconnect() throws Exception {}
}
