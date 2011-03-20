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
package com.eucalyptus.ws.client;

import java.net.InetSocketAddress;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.util.async.NioBootstrap;
import com.eucalyptus.ws.Client;
import com.eucalyptus.ws.handlers.NioResponseHandler;
import com.eucalyptus.ws.handlers.ResponseHandler;
import com.eucalyptus.ws.util.ChannelUtil;
import com.google.common.base.Supplier;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class NioClient implements Client {
  public interface ResponseHandlingPipeline extends ChannelPipelineFactory, Supplier<ResponseHandler> {};
  
  public static class DelegatingPipeline implements ResponseHandlingPipeline {
    private ChannelPipelineFactory pipelineFactory;
    private ResponseHandler        handler;
    DelegatingPipeline( ChannelPipelineFactory pipelineFactory, ResponseHandler handler ) {
      super( );
      this.pipelineFactory = pipelineFactory;
      this.handler = handler;
    }
    public ChannelPipeline getPipeline( ) throws Exception {
      ChannelPipeline pipeline = this.pipelineFactory.getPipeline( );
      pipeline.addLast( "response-handler", handler );
      return pipeline;
    }
    public BaseMessage getRequest( ) {
      return this.handler.getRequest( );
    }
    public BaseMessage getResponse( ) throws Exception {
      return this.handler.getResponse( );
    }
    public void waitForResponse( ) {
      this.handler.waitForResponse( );
    }
    @Override
    public ResponseHandler get( ) {
      return this.handler;
    }
    
  }
  
  private static Logger     LOG = Logger.getLogger( NioClient.class );
  
  private NioBootstrap      clientBootstrap;
  
  private String            hostname;
  private int               port;
  private String            servicePath;
  private InetSocketAddress remoteAddr;
  private ResponseHandler   responseHandler;
  private ChannelFuture     connectFuture;
  
  public NioClient( String hostname, int port, String servicePath, ChannelPipelineFactory clientPipeline ) {
    this.responseHandler = new NioResponseHandler( );
    this.clientBootstrap = ChannelUtil.getClientBootstrap( new DelegatingPipeline( clientPipeline, this.responseHandler ) );
    this.remoteAddr = new InetSocketAddress( hostname, port );
    this.hostname = hostname;
    this.port = port;
    this.servicePath = servicePath;
    this.connectFuture = this.clientBootstrap.connect( this.remoteAddr );
  }
  
  public void write( HttpRequest httpRequest ) throws Exception {
    this.connectFuture.addListener( ChannelUtil.WRITE( httpRequest ) );
  }
  
  @Override
  public BaseMessage send( final BaseMessage msg ) throws Exception {
    HttpRequest request = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, this.hostname, this.port, this.servicePath, msg );
    this.write( request );
    BaseMessage response = this.responseHandler.getResponse( );
    return response;
  }
  
  @Override
  public void dispatch( final BaseMessage msg ) throws Exception {
    HttpRequest request = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, this.hostname, this.port, this.servicePath, msg );
    this.write( request );
    BaseMessage response = this.responseHandler.getResponse( );
  }
  
  @Override
  public String getUri( ) {
    return "http://" + this.hostname + ":" + this.port + ( servicePath.startsWith( "/" )
      ? ""
      : "?" ) + servicePath;
  }
  
  public String getHostname( ) {
    return hostname;
  }
  
  public int getPort( ) {
    return port;
  }
  
  public String getServicePath( ) {
    return servicePath;
  }
  
}
