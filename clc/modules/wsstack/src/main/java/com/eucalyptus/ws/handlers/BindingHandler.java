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
package com.eucalyptus.ws.handlers;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.auth.User;
import com.eucalyptus.ws.MappingHttpMessage;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.WebServicesException;
import com.eucalyptus.ws.binding.Binding;
import com.eucalyptus.ws.binding.BindingManager;

import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

@ChannelPipelineCoverage( "all" )
public class BindingHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger( BindingHandler.class );

  private Binding binding;
 
  public BindingHandler( ) {
    super( );
  }

  public BindingHandler( final Binding binding ) {
    this.binding = binding;
  }

  @Override
  public void incomingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      MappingHttpMessage httpMessage = ( MappingHttpMessage ) event.getMessage( );
      //:: TODO: need an index of message types based on name space :://
      Class msgType = Class.forName( "edu.ucsb.eucalyptus.msgs." + httpMessage.getOmMessage( ).getLocalName( ) + "Type" );
      EucalyptusMessage msg = null;
      OMElement elem = httpMessage.getOmMessage( );
      OMNamespace omNs = elem.getNamespace( );
      String namespace = omNs.getNamespaceURI( );
      try {
        this.binding = BindingManager.getBinding( BindingManager.sanitizeNamespace( namespace ) );
      } catch ( Exception e1 ) {
        if( this.binding == null ) {
          throw new WebServicesException(e1);
        }
      }
      try {
        if(httpMessage instanceof MappingHttpRequest ) {
          msg = ( EucalyptusMessage ) this.binding.fromOM( httpMessage.getOmMessage( ), msgType );
        } else {
          msg = ( EucalyptusMessage ) this.binding.fromOM( httpMessage.getOmMessage( ) );          
        }
      } catch ( Exception e1 ) {
        LOG.fatal( "FAILED TO PARSE:\n" + httpMessage.getMessageString( ) );
        throw new WebServicesException(e1);
      }
      httpMessage.setMessage( msg );
    }
  }

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      MappingHttpMessage httpRequest = ( MappingHttpMessage ) event.getMessage( );
      if( httpRequest.getMessage( ) instanceof EucalyptusErrorMessageType ) {
        return;
      }
      Class targetClass = httpRequest.getMessage( ).getClass( );
      while ( !targetClass.getSimpleName( ).endsWith( "Type" ) )
        targetClass = targetClass.getSuperclass( );
      Class responseClass = Class.forName( targetClass.getName( ) );
      ctx.setAttachment( responseClass );
      OMElement omElem = this.binding.toOM( httpRequest.getMessage( ) );
      httpRequest.setOmMessage( omElem );
    }
  }

}
