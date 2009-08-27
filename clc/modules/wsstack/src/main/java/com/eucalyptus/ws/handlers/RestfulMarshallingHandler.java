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
package com.eucalyptus.ws.handlers;

import java.io.ByteArrayOutputStream;

import org.apache.axiom.om.OMElement;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import com.eucalyptus.ws.BindingException;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.MappingHttpResponse;
import com.eucalyptus.ws.binding.Binding;
import com.eucalyptus.ws.binding.BindingManager;
import com.eucalyptus.ws.server.EucalyptusQueryPipeline.RequiredQueryParams;

import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;

@ChannelPipelineCoverage("one")
public abstract class RestfulMarshallingHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger( RestfulMarshallingHandler.class );
  protected String namespace;

  @Override
  public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
      String bindingVersion = httpRequest.getParameters( ).remove( RequiredQueryParams.Version.toString( ) );
      if( bindingVersion.matches( "\\d\\d\\d\\d-\\d\\d-\\d\\d" ) ) {
        this.namespace = "http://ec2.amazonaws.com/doc/" + bindingVersion + "/";        
      } else {
        this.namespace = "http://msgs.eucalyptus.ucsb.edu";
      }
      // TODO: get real user data here too
      httpRequest.setMessage( this.bind( "admin", true, httpRequest ) );
    }
  }

  public abstract Object bind( String user, boolean admin, MappingHttpRequest httpRequest ) throws Exception;

  @Override
  public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpResponse ) {
      MappingHttpResponse httpResponse = ( MappingHttpResponse ) event.getMessage( );
      Binding binding = BindingManager.getBinding( BindingManager.sanitizeNamespace( this.namespace ) );
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      if( httpResponse.getMessage( ) instanceof EucalyptusErrorMessageType ) {
        EucalyptusErrorMessageType errMsg = (EucalyptusErrorMessageType) httpResponse.getMessage( );
        Binding.createFault( errMsg.getSource( ), errMsg.getMessage( ), errMsg.getStatusMessage( ) ).serialize( byteOut );
      } else {
        OMElement omMsg = binding.toOM( httpResponse.getMessage( ), this.namespace );
        omMsg.serialize( byteOut );        
      }
      byte[] req = byteOut.toByteArray();
      ChannelBuffer buffer = ChannelBuffers.copiedBuffer( req );
      httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes() ) );
      httpResponse.addHeader( HttpHeaders.Names.CONTENT_TYPE, "application/xml; charset=UTF-8" );
      httpResponse.setContent( buffer );
    }
  }

}
