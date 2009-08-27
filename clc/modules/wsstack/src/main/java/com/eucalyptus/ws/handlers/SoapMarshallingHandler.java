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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.eucalyptus.ws.MappingHttpMessage;
import com.eucalyptus.ws.MappingHttpRequest;

@ChannelPipelineCoverage( "all" )
public class SoapMarshallingHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger( SoapMarshallingHandler.class );

  @Override
  public void incomingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      MappingHttpMessage httpMessage = ( MappingHttpMessage ) event.getMessage( );
      String content = httpMessage.getContent( ).toString( "UTF-8" );
      httpMessage.setMessageString( content );
      ByteArrayInputStream byteIn = new ByteArrayInputStream( content.getBytes( ) );
      XMLStreamReader xmlStreamReader = XMLInputFactory.newInstance( ).createXMLStreamReader( byteIn );
      StAXSOAPModelBuilder soapBuilder = new StAXSOAPModelBuilder( xmlStreamReader, SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI );
      SOAPEnvelope env = ( SOAPEnvelope ) soapBuilder.getDocumentElement( );
      httpMessage.setSoapEnvelope( env );
    }
  }

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      MappingHttpMessage httpMessage = ( MappingHttpMessage ) event.getMessage( );
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream( );
      httpMessage.getSoapEnvelope( ).serialize( byteOut );
      ChannelBuffer buffer = ChannelBuffers.wrappedBuffer( byteOut.toByteArray( ) );
      httpMessage.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes( ) ) );
      httpMessage.addHeader( HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=UTF-8" );
      httpMessage.setContent( buffer );
    }
  }

}
