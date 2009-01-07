/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.transport.client;

import edu.ucsb.eucalyptus.transport.Axis2MessageDispatcher;
import edu.ucsb.eucalyptus.transport.config.Axis2OutProperties;
import org.apache.log4j.Logger;
import org.mule.api.*;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Axis2MessageDispatcherFactory implements MessageDispatcherFactory {

  private static Logger LOG = Logger.getLogger( Axis2MessageDispatcherFactory.class );

  private static Map<String, ClientPool> endpointMap = new ConcurrentHashMap<String, ClientPool>();

  public MessageDispatcher create( OutboundEndpoint endpoint ) throws MuleException
  {
    try
    {
      Axis2MessageDispatcher dispatcher = new Axis2MessageDispatcher( endpoint );
      this.activate( endpoint, dispatcher );
      return dispatcher;
    }
    catch ( Exception e )
    {
      throw new DefaultMuleException( e );
    }
  }

  public void activate( OutboundEndpoint endpoint, MessageDispatcher dispatcher ) throws MuleException
  {
    Axis2MessageDispatcher msgDispatcher = ( Axis2MessageDispatcher ) dispatcher;
    Axis2OutProperties properties = msgDispatcher.getProperties();
    String address = endpoint.getEndpointURI().getAddress();
    if ( !endpointMap.containsKey( address ) )
      endpointMap.put( address, new ClientPool( new ClientFactory( address, properties ), properties ) );
    ClientPool clientPool = endpointMap.get( address );
    try
    {
      BasicClient client = clientPool.lease();
      msgDispatcher.setClient( client );
    }
    catch ( Exception e )
    {
      LOG.fatal( e, e );
    }
  }

  public boolean validate( OutboundEndpoint endpoint, MessageDispatcher dispatcher )
  {
    Axis2MessageDispatcher msgDispatcher = ( Axis2MessageDispatcher ) dispatcher;
    BasicClient client = msgDispatcher.getClient();
    return client != null;
  }

  public void passivate( OutboundEndpoint endpoint, MessageDispatcher dispatcher )
  {
    Axis2MessageDispatcher msgDispatcher = ( Axis2MessageDispatcher ) dispatcher;
    BasicClient client = msgDispatcher.getClient();
    if ( client != null )
      try
      {
        endpointMap.get( endpoint.getEndpointURI().getAddress() ).release( client );
      }
      catch ( Exception e ) {}
  }

  public void destroy( OutboundEndpoint endpoint, MessageDispatcher dispatcher ) {}

  public boolean isCreateDispatcherPerRequest()
  {
    return true;
  }

}
