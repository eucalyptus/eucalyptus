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
package com.eucalyptus.ws.client;

import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mule.api.component.JavaComponent;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.ConnectException;

import com.eucalyptus.ws.server.InternalQueryPipeline;
import com.eucalyptus.ws.server.InternalSoapPipeline;
import com.eucalyptus.ws.util.PipelineRegistry;

public class NioMessageReceiver extends AbstractMessageReceiver {

 private static Logger LOG = Logger.getLogger( NioMessageReceiver.class );

 @SuppressWarnings( "unchecked" )
 public NioMessageReceiver( Connector connector, Service service, InboundEndpoint endpoint ) throws CreateException {
   super( connector, service, endpoint );
   Class serviceClass = ( ( JavaComponent ) this.getService().getComponent() ).getObjectType();
 }

 public void doConnect() throws ConnectException {
   PipelineRegistry.getInstance( ).register( new InternalSoapPipeline( this, this.getService( ).getName( ), this.getEndpointURI( ).getPath( ) ) );
   PipelineRegistry.getInstance( ).register( new InternalQueryPipeline( this, this.getService( ).getName( ), this.getEndpointURI( ).getPath( ) ) );
 }

 public void doStart() {
 }

 public void doStop() {}

 public void doDispose() {}

 public void doDisconnect() throws ConnectException {}

}

