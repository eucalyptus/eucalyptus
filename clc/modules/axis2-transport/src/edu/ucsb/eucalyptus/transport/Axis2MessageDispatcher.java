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

package edu.ucsb.eucalyptus.transport;

import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.transport.binding.BindingManager;
import edu.ucsb.eucalyptus.transport.client.BasicClient;
import edu.ucsb.eucalyptus.transport.config.Axis2OutProperties;
import edu.ucsb.eucalyptus.transport.config.Key;
import edu.ucsb.eucalyptus.transport.config.Mep;
import edu.ucsb.eucalyptus.util.BindingUtil;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.log4j.Logger;
import org.jibx.runtime.JiBXException;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transformer.TransformerException;
import org.mule.message.ExceptionMessage;
import org.mule.transport.AbstractMessageDispatcher;


public class Axis2MessageDispatcher extends AbstractMessageDispatcher {

  private static Logger LOG = Logger.getLogger( Axis2MessageDispatcher.class );
  private Axis2OutProperties properties;
  private BasicClient client;

  @SuppressWarnings("unchecked")
  public Axis2MessageDispatcher( OutboundEndpoint endpoint ) throws AxisFault, JiBXException
  {
    super( endpoint );
    if ( !endpoint.getProperties().containsKey( Key.WSSEC_POLICY.getKey() ) )
      endpoint.getProperties().put( Key.WSSEC_POLICY.getKey(), ( ( Axis2Connector ) endpoint.getConnector() ).getDefaultWssecPolicy() );
    ConfigurationContext ctx = (( Axis2Connector) endpoint.getConnector() ).getAxisConfig();
    Mep wssecFlow = Mep.valueOf( (String) endpoint.getProperty( Key.WSSEC_FLOW.getKey() ) );
    if( Mep.IN_ONLY.equals( wssecFlow ) ) endpoint.getProperties().put( Key.WSSEC_FLOW.getKey(), Mep.OUT_ONLY.name() );
    this.properties = new Axis2OutProperties( endpoint.getProperties(), ctx );
    System.out.println( "" );
  }

  public void sendAsync( Object msg ) throws EucalyptusCloudException
  {
    try
    {
      this.client.dispatch( (EucalyptusMessage) msg );
    }
    catch ( AxisFault e )
    {
      LOG.error( e, e );
      throw new EucalyptusCloudException( "Error dispatching the message", e );
    }
  }

  public void doDispatch( MuleEvent event ) throws EucalyptusCloudException
  {
    Object msg = getMessage( event );
    this.sendAsync( msg );
  }

  public MuleMessage doSend( MuleEvent event ) throws EucalyptusCloudException
  {
    Object msg = null;
    try
    {
      msg = event.transformMessage();
      if ( msg instanceof ExceptionMessage )
        msg = checkException( msg );
    }
    catch ( TransformerException e )
    {
      LOG.error( e, e );
      throw new EucalyptusCloudException( "Error handling message preparation for dispatch", e );
    }
    Object msgResponse = sendSync( msg );
    return new DefaultMuleMessage( msgResponse );
  }

  public Object sendSync( Object msg ) throws EucalyptusCloudException
  {
    Object msgResponse = null;
    try
    {
      msgResponse = this.client.send( ( EucalyptusMessage ) msg );
    }
    catch ( Exception e )
    {
      LOG.error( e, e );
      throw new EucalyptusCloudException( "Error dispatching the message", e );
    }
    return msgResponse;
  }

  private Object checkException( Object msg )
  {
    ExceptionMessage em = ( ExceptionMessage ) msg;
    try
    {
      String eucaMsgString = em.getPayloadAsString( "UTF-8" );

      EucalyptusMessage sourceMessage = ( EucalyptusMessage ) BindingManager.getBinding( BindingUtil.sanitizeNamespace( "http://msgs.eucalyptus.ucsb.edu/" ) ).fromOM( eucaMsgString );
      int i = 0;
      Throwable exception = em.getException().getCause();
      EucalyptusCloudException myEx = null;
      if ( exception instanceof EucalyptusCloudException )
        myEx = ( EucalyptusCloudException ) exception;
      else
        myEx = new EucalyptusCloudException( exception.getMessage(), exception);
      msg = new EucalyptusErrorMessageType( em.getEndpoint().getAddress(), sourceMessage, myEx.getMessage() );
    }
    catch ( Exception e )
    {
      LOG.error( e, e );
    }
    return msg;
  }

  private Object getMessage( final MuleEvent event ) throws EucalyptusCloudException
  {
    Object msg = null;
    try
    {
      msg = event.transformMessage();
      if ( msg instanceof ExceptionMessage )
        msg = checkException( msg );
    }
    catch ( TransformerException e )
    {
      LOG.error( e, e );
      throw new EucalyptusCloudException( "Error handling message preparation for dispatch", e );
    }
    return msg;
  }

  public BasicClient getClient()
  {
    return client;
  }

  public void setClient( final BasicClient client )
  {
    this.client = client;
  }

  public Axis2OutProperties getProperties()
  {
    return properties;
  }

  public void doConnect() throws Exception
  {

  }

  public void doDisconnect() throws Exception
  {

  }

  protected void doDispose() {}
}

