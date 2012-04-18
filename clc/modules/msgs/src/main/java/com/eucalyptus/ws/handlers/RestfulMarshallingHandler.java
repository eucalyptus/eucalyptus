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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.handlers;

import java.io.ByteArrayOutputStream;
import java.util.MissingFormatArgumentException;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelLocal;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.protocol.RequiredQueryParams;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;

@ChannelPipelineCoverage( "one" )
public abstract class RestfulMarshallingHandler extends MessageStackHandler {
  private static Logger        LOG                     = Logger.getLogger( RestfulMarshallingHandler.class );
  private String               namespace;
  private final String         namespacePattern;
  private String               defaultBindingNamespace = BindingManager.defaultBindingNamespace();
  private Binding              defaultBinding          = BindingManager.getDefaultBinding( );
  private Binding              binding;
  
  public RestfulMarshallingHandler( String namespacePattern ) {
    this.namespacePattern = namespacePattern;
    try {
      this.setNamespace( String.format( namespacePattern ) );
    } catch ( MissingFormatArgumentException ex ) {}
  }
  
  public RestfulMarshallingHandler( String namespacePattern, String defaultVersion ) {
    this( namespacePattern );
    this.defaultBindingNamespace = String.format( namespacePattern, defaultVersion );
    this.defaultBinding = BindingManager.getBinding( BindingManager.sanitizeNamespace( this.defaultBindingNamespace ) );
  }
  
  @Override
  public void incomingMessage( MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
      String bindingVersion = httpRequest.getParameters( ).remove( RequiredQueryParams.Version.toString( ) );
      if ( bindingVersion.matches( "\\d\\d\\d\\d-\\d\\d-\\d\\d" ) ) {
        this.setNamespaceVersion( bindingVersion );
      } else {
        this.setNamespace( BindingManager.defaultBindingName() );
      }
      try {
        BaseMessage msg = ( BaseMessage ) this.bind( httpRequest );
        httpRequest.setMessage( msg );
      } catch ( Exception e ) {
        if ( !( e instanceof BindingException ) ) {
          e = new BindingException( e );
        }
        throw e;
      }
    }
  }
  
  protected void setNamespace( String namespace ) {
    this.namespace = namespace;
    this.binding = BindingManager.getBinding( BindingManager.sanitizeNamespace( this.namespace ) );
  }
  
  private void setNamespaceVersion( String bindingVersion ) {
    String newNs = null;
    try {
      newNs = String.format( this.namespacePattern );
    } catch ( MissingFormatArgumentException e ) {
      newNs = String.format( this.namespacePattern, bindingVersion );
    }
    this.setNamespace( newNs );
  }
  
  public abstract Object bind( MappingHttpRequest httpRequest ) throws Exception;
  
  @Override
  public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpResponse ) {
      MappingHttpResponse httpResponse = ( MappingHttpResponse ) event.getMessage( );
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream( );
      HoldMe.canHas.lock( );
      try {
        if ( httpResponse.getMessage( ) == null ) {
/** TODO:GRZE: doing nothing here may be needed for streaming? double check... **/
//          String response = Binding.createRestFault( this.requestType.get( ctx.getChannel( ) ), "Recieved an response from the service which has no content.", "" );
//          byteOut.write( response.getBytes( ) );
//          httpResponse.setStatus( HttpResponseStatus.INTERNAL_SERVER_ERROR );
        } else if ( httpResponse.getMessage( ) instanceof EucalyptusErrorMessageType ) {
          EucalyptusErrorMessageType errMsg = ( EucalyptusErrorMessageType ) httpResponse.getMessage( );
          byteOut.write( Binding.createRestFault( errMsg.getSource( ), errMsg.getMessage( ), errMsg.getCorrelationId( ) ).getBytes( ) );
          httpResponse.setStatus( HttpResponseStatus.BAD_REQUEST );
        } else if ( httpResponse.getMessage( ) instanceof ExceptionResponseType ) {//handle error case specially
          ExceptionResponseType msg = ( ExceptionResponseType ) httpResponse.getMessage( );
          if( msg.getException( ) != null ) {
            Logs.extreme( ).debug( msg, msg.getException( ) );
          }
          String response = Binding.createRestFault( msg.getRequestType( ), msg.getMessage( ), msg.getError( ) );
          byteOut.write( response.getBytes( ) );
          httpResponse.setStatus( msg.getHttpStatus( ) );
        } else {//actually try to bind response
          try {//use request binding
            this.binding.toOM( httpResponse.getMessage( ) ).serialize( byteOut );
          } catch ( BindingException ex ) {
            try {//use default binding with request namespace
              BindingManager.getDefaultBinding( ).toOM( httpResponse.getMessage( ), this.namespace ).serialize( byteOut );
            } catch ( BindingException ex1 ) {//use default binding
              BindingManager.getDefaultBinding( ).toOM( httpResponse.getMessage( ) ).serialize( byteOut );
            }
          } catch ( Exception e ) {
            LOG.debug( e );
            Logs.exhaust( ).error( e, e );
            throw e;
          }
        }
        byte[] req = byteOut.toByteArray( );
        ChannelBuffer buffer = ChannelBuffers.copiedBuffer( req );
        httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes( ) ) );
        httpResponse.addHeader( HttpHeaders.Names.CONTENT_TYPE, "application/xml; charset=UTF-8" );
        httpResponse.setContent( buffer );
      } finally {
        HoldMe.canHas.unlock( );
      }
    }
  }
  
  /**
   * @return the namespace
   */
  public String getNamespace( ) {
    return this.namespace;
  }
  
  /**
   * @return the binding
   */
  public Binding getBinding( ) {
    return this.binding;
  }

  public Binding getDefaultBinding( ) {
    return this.defaultBinding;
  }
  
}
