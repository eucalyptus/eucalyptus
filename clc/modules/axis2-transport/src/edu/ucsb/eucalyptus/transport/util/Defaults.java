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

package edu.ucsb.eucalyptus.transport.util;

import edu.ucsb.eucalyptus.transport.Axis2Connector;
import edu.ucsb.eucalyptus.transport.Axis2MessageDispatcher;
import edu.ucsb.eucalyptus.transport.client.Axis2MessageDispatcherFactory;
import edu.ucsb.eucalyptus.transport.client.ClientFactory;
import edu.ucsb.eucalyptus.transport.client.ClientPool;
import edu.ucsb.eucalyptus.transport.config.Axis2OutProperties;
import edu.ucsb.eucalyptus.transport.config.Key;
import edu.ucsb.eucalyptus.transport.config.Mep;
import edu.ucsb.eucalyptus.util.BaseDirectory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.log4j.Logger;
import org.mule.RegistryContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.endpoint.DefaultOutboundEndpoint;
import org.mule.endpoint.MuleEndpointURI;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class Defaults {
  private static Logger LOG = Logger.getLogger( Defaults.class );

  public static String WS_SECURITY_MODULE = "rampart";
  public static String WS_ADDRESSING_MODULE = "addressing";

  public static MultiThreadedHttpConnectionManager getDefaultHttpManager( )
  {
    MultiThreadedHttpConnectionManager httpConnMgr = new MultiThreadedHttpConnectionManager();
    HttpConnectionManagerParams params = httpConnMgr.getParams();
    params.setDefaultMaxConnectionsPerHost( 16 );
    params.setMaxTotalConnections( 16 );
    params.setTcpNoDelay( true );
    params.setConnectionTimeout( 120*1000 );
//    params.setReceiveBufferSize( 8388608 );
//    params.setSendBufferSize( 8388608 );
    params.setStaleCheckingEnabled( true );
    params.setSoTimeout( 120*1000 );
//    params.setLinger( -1 );
    return httpConnMgr;
  }

  @SuppressWarnings( "unchecked" )
  public static ClientPool getOneoffClientPool( String address ) throws AxisFault
  {
    Map props = Key.getDefaultProperties();
    props.put(Key.CONF.getKey(), BaseDirectory.CONF.toString() + File.separator + "axis2.xml");
    props.put( Key.MEP.getKey(), Mep.OUT_IN.name() );
    props.put(Key.WSSEC_POLICY.getKey(), BaseDirectory.CONF.toString() + File.separator + "policy.xml");
    props.put( Key.WSSEC_FLOW.getKey(), Mep.OUT_ONLY.name() );
    ConfigurationContext ctx = ConfigurationContextFactory.createConfigurationContextFromFileSystem( BaseDirectory.VAR.toString(), Key.CONF.getString( props ) );
    Axis2OutProperties properties = new Axis2OutProperties( props, ctx );
    return new ClientPool( new ClientFactory( address, properties ), properties );
  }

  @SuppressWarnings( "unchecked" )
  public static OutboundEndpoint getDefaultOutboundEndpoint( String uri, String namespace, int timeout, int minIdle, int maxIdle )
  {
    try
    {
      Axis2Connector connector = ( Axis2Connector ) RegistryContext.getRegistry().lookupConnector( "axis2" );
      URI uriObj = new URI(uri);
      MuleEndpointURI endpointUri = new MuleEndpointURI( uri, null, null, null, null, null, uriObj );

      Map props = Key.getDefaultProperties();
      props.put( Key.NAMESPACE.getKey(), namespace );
      props.put( Key.TIMEOUT.getKey(), Integer.toString( timeout ) );
      props.put( Key.MEP.getKey(), Mep.OUT_IN.name() );
      props.put( Key.MIN_IDLE.getKey(), Integer.toString( minIdle ) );
      props.put( Key.MAX_IDLE.getKey(), Integer.toString( maxIdle ) );
      props.put( Key.CACHE_HTTP_CLIENT.getKey(), Boolean.TRUE.toString(  ));
      props.put( Key.WSSEC_POLICY.getKey(), BaseDirectory.CONF.toString() + File.separator + "cluster-policy.xml" );

      DefaultOutboundEndpoint endpoint = new DefaultOutboundEndpoint( connector, endpointUri, null, null, null, props, null, null, false, null, false, false, 0, null, null, null, null );
      return endpoint;
    }
    catch ( URISyntaxException e )
    {
      LOG.error( e, e );
      return null;
    }
  }

    @SuppressWarnings( "unchecked" )
  public static OutboundEndpoint getInsecureOutboundEndpoint( String uri, String namespace, int timeout, int minIdle, int maxIdle )
  {
    OutboundEndpoint insecEp = getDefaultOutboundEndpoint( uri, namespace, timeout, minIdle, maxIdle );
    insecEp.getProperties().remove( Key.WSSEC_POLICY.getKey() );
    insecEp.getProperties().put( Key.WSSEC_POLICY.getKey(), BaseDirectory.CONF.toString() + File.separator + "off-policy.xml" );
    return insecEp;
  }

  public static Axis2MessageDispatcher getMessageDispatcher( OutboundEndpoint outEp )
  {
    try
    {
      Axis2MessageDispatcherFactory clientFactory = new Axis2MessageDispatcherFactory();
      return (Axis2MessageDispatcher) clientFactory.create( outEp );
    }
    catch ( MuleException e )
    {
      LOG.error( e, e );
      return null;
    }
  }
}
