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

package edu.ucsb.eucalyptus.transport.http;

import org.apache.log4j.Logger;
import org.apache.axis2.context.*;
import org.apache.axis2.*;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.ListenerManager;
import org.apache.axis2.transport.TransportListener;
import org.apache.axis2.transport.http.server.*;
import org.apache.axis2.transport.http.*;

import java.util.*;
import java.io.IOException;

/**
 * User: decker
 * Date: Jun 25, 2008
 * Time: 12:05:28 AM
 */
public class Axis2HttpListener {

  private static Logger LOG = Logger.getLogger( Axis2HttpListener.class );

  private Map<Integer, HttpPort> serverMap;
  private ConfigurationContext axisConfigContext;

  public Axis2HttpListener( ConfigurationContext configurationContext )
  {
    this.serverMap = new HashMap<Integer, HttpPort>();
    this.axisConfigContext = configurationContext;
  }

  public void addHttpListener( String host, int port ) throws AxisFault
  {
    if ( this.serverMap.containsKey( port ) ) return;
    HttpPort newHttpListener = new HttpPort( this.axisConfigContext, host, port );
    newHttpListener.start();
    this.serverMap.put( port, newHttpListener );
  }

}

class HttpPort implements TransportListener {

  private static Logger LOG = Logger.getLogger( HttpPort.class );

  private ConfigurationContext axisConfigContext;
  private String address;
  private int port;
  private SessionManager sessionManager;
  private HttpFactory httpFactory;
  private SimpleHttpServer httpServer;

  public HttpPort( ConfigurationContext configurationContext, String address, int port ) throws AxisFault
  {
    this.address = address;
    this.port = port;
    this.axisConfigContext = configurationContext;
    ListenerManager listenerManager = this.axisConfigContext.getListenerManager();
    if ( listenerManager == null )
    {
      listenerManager = new ListenerManager();
      listenerManager.init( this.axisConfigContext );
    }
    TransportInDescription httpDescription = new TransportInDescription( Constants.TRANSPORT_HTTP );
    httpDescription.setReceiver( this );
//    httpDescription.addParameter( new Parameter( "port", this.port ) );
    listenerManager.addListener( httpDescription, true );
    this.httpFactory = new HttpFactory( this.axisConfigContext, this.port, new Axis2HttpWorkerFactory() );
    this.httpFactory.setRequestCoreThreadPoolSize( 64 );
    this.httpFactory.setRequestMaxThreadPoolSize( 4096 );
    this.httpFactory.setRequestSocketTimeout( 3600000 );
    this.sessionManager = new SessionManager();
  }

  public void init( ConfigurationContext configurationContext, TransportInDescription transportInDescription ) throws AxisFault
  {
  }

  public void start() throws AxisFault
  {
    try
    {
      this.httpServer = new SimpleHttpServer( httpFactory, port );
      this.httpServer.init();
      this.httpServer.start();
    }
    catch ( IOException e )
    {
      LOG.error( e.getMessage(), e );
      throw AxisFault.makeFault( e );
    }
  }

  public void stop()
  {
    LOG.warn( "stopping http server" );
    if ( this.httpServer != null )
      try
      {
        this.httpServer.destroy();
      }
      catch ( Exception e )
      {
        LOG.error( e.getMessage(), e );
      }

  }

  public EndpointReference getEPRForService( String serviceName, String ip ) throws AxisFault
  {
    return getEPRsForService( serviceName, ip )[ 0 ];
  }

  public EndpointReference[] getEPRsForService( String serviceName, String ip ) throws AxisFault
  {
    String endpointReference = this.address;
    endpointReference += ( this.axisConfigContext.getServiceContextPath().startsWith( "/" ) ? "" : '/' )
                         + this.axisConfigContext.getServiceContextPath() + "/" + serviceName;
    LOG.warn( "endpoint=" + endpointReference );
    return new EndpointReference[]{ new EndpointReference( endpointReference ) };
  }

  public SessionContext getSessionContext( MessageContext messageContext )
  {
    String sessionKey = ( String ) messageContext.getProperty( HTTPConstants.COOKIE_STRING );
    return this.sessionManager.getSessionContext( sessionKey );
  }

  public void destroy()
  {
    this.axisConfigContext = null;
  }

}

