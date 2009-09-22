/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.handlers;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.eucalyptus.util.EucalyptusClusterException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.MappingHttpMessage;
import com.eucalyptus.ws.MappingHttpResponse;

import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

@ChannelPipelineCoverage( "one" )
public class NioResponseHandler extends SimpleChannelHandler {
  private static Logger                 LOG           = Logger.getLogger( NioResponseHandler.class );
  
  private Throwable exception = null;
  private EucalyptusMessage response = null;
  protected BlockingQueue<Object> responseQueue = new LinkedBlockingQueue<Object>( );
  protected BlockingQueue<EucalyptusMessage> requestQueue = new LinkedBlockingQueue<EucalyptusMessage>( );

  public boolean hasException( ) {
    return this.responseQueue.isEmpty( ) && this.responseQueue.peek( ) instanceof Throwable;
  }
  
  public boolean hasResponse( ) {
    return this.responseQueue.isEmpty( ) && this.responseQueue.peek( ) instanceof EucalyptusMessage;
  }
  
  public final boolean isReady( ) {
    return !this.getRequestQueue( ).isEmpty( );
  }
  
  public EucalyptusMessage getResponse( ) throws Exception {
    Object response = null;
    LOG.debug( "-> Waiting for response to event of type: " + this.getClass().getCanonicalName( ) );
    do {
      try {
        response = this.responseQueue.poll( 1, TimeUnit.SECONDS );
      } catch ( final InterruptedException e ) {}
    } while ( ( response == null ) );
    if ( response != null ) {
      if ( response instanceof EucalyptusMessage ) {
        this.response = ( EucalyptusMessage ) response;
        return this.response;
      } else if ( response instanceof MappingHttpResponse ) {
        MappingHttpResponse httpResponse = (MappingHttpResponse) response;
        if( httpResponse.getMessage( ) != null ) {
          this.response = (EucalyptusMessage) httpResponse.getMessage( );
          return this.response;
        } else {
          this.exception = new EucalyptusClusterException( httpResponse.getMessageString( ) );
        }
      } else if ( response instanceof Throwable ) {
        this.exception = (Throwable) response;
        throw new EucalyptusClusterException( "Exception in NIO request.", (Throwable) response );
      }
    }
    throw new EucalyptusClusterException( "Failed to retrieve result of asynchronous operation." );
  }
  
  @Override
  public void exceptionCaught( final ChannelHandlerContext ctx, final ExceptionEvent e ) {
    LOG.debug( e.getCause( ), e.getCause( ) );
    this.responseQueue.offer( e.getCause( ) );
    e.getChannel( ).close( );
  }
  
  @Override
  public void messageReceived( final ChannelHandlerContext ctx, final MessageEvent e ) throws Exception {
    final MappingHttpMessage httpResponse = ( MappingHttpMessage ) e.getMessage( );
    final EucalyptusMessage reply = ( EucalyptusMessage ) httpResponse.getMessage( );
    this.responseQueue.offer( reply );
    e.getChannel( ).close( );
  }

  public BlockingQueue<Object> getResponseQueue( ) {
    return this.responseQueue;
  }

  public BlockingQueue<EucalyptusMessage> getRequestQueue( ) {
    return this.requestQueue;
  }

  public Throwable getException( ) {
    return this.exception;
  }

  @Override
  public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    super.handleUpstream( ctx, e );
  }

}
