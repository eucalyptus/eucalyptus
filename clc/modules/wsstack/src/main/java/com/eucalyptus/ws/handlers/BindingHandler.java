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
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
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
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.WebServicesException;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

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
      BaseMessage msg = null;
      OMElement elem = httpMessage.getOmMessage( );
      OMNamespace omNs = elem.getNamespace( );
      String namespace = omNs.getNamespaceURI( );
      Class msgType = null;
      try {
        this.binding = BindingManager.getBinding( BindingManager.sanitizeNamespace( namespace ) );
        msgType = this.binding.getElementClass( httpMessage.getOmMessage( ).getLocalName( ) );
      } catch ( Exception e1 ) {
        if( this.binding == null ) {
          throw new WebServicesException(e1);
        } else {
          throw new WebServicesException( "Failed to find binding for namespace: " + namespace + " due to: " + e1.getMessage( ), e1 );
        }
      }
      try {
        if(httpMessage instanceof MappingHttpRequest ) {
          if( msgType != null ) {
            msg = ( BaseMessage ) this.binding.fromOM( httpMessage.getOmMessage( ), msgType );
          } else {
            msg = ( BaseMessage ) this.binding.fromOM( httpMessage.getOmMessage( ) );
          }
        } else {
          msg = ( BaseMessage ) this.binding.fromOM( httpMessage.getOmMessage( ) );          
        }
      } catch ( Exception e1 ) {
        LOG.fatal( "FAILED TO PARSE:\n" + httpMessage.getMessageString( ) );
        throw new WebServicesException(e1);
      }
      msg.setCorrelationId( httpMessage.getCorrelationId( ) );
      httpMessage.setMessage( msg );
    }
  }

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      MappingHttpMessage httpRequest = ( MappingHttpMessage ) event.getMessage( );
      if( httpRequest.getMessage( ) instanceof EucalyptusErrorMessageType || httpRequest.getMessage( ) == null ) {
        return;
      }
       Class targetClass = httpRequest.getMessage( ).getClass( );
      while ( !targetClass.getSimpleName( ).endsWith( "Type" ) )
        targetClass = targetClass.getSuperclass( );
      Class responseClass = ClassLoader.getSystemClassLoader().loadClass( targetClass.getName( ) );
      ctx.setAttachment( responseClass );
      OMElement omElem = this.binding.toOM( httpRequest.getMessage( ) );
      httpRequest.setOmMessage( omElem );
    }
  }

}
