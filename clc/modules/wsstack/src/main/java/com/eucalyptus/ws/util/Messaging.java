package com.eucalyptus.ws.util;

import org.apache.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import com.eucalyptus.util.EucalyptusCloudException;

public class Messaging {

  private static Logger LOG = Logger.getLogger( Messaging.class );

  private static MuleClient getClient( ) throws MuleException {
    return new MuleClient( );
  }

  public static void dispatch( String dest, Object msg ) {
    MuleEvent context = RequestContext.getEvent( );
    try {
      getClient( ).dispatch( dest, msg, null );
    } catch ( MuleException e ) {
      LOG.error( e );
    } finally {
      RequestContext.setEvent( context );
    }
  }

  public static Object send( String dest, Object msg ) throws EucalyptusCloudException {
    MuleEvent context = RequestContext.getEvent( );
    try {
      MuleMessage reply = getClient( ).send( dest, msg, null );

      if ( reply.getExceptionPayload( ) != null ) throw new EucalyptusCloudException( reply.getExceptionPayload( ).getRootException( ).getMessage( ), reply.getExceptionPayload( ).getRootException( ) );
      else return reply.getPayload( );
    } catch ( MuleException e ) {
      LOG.error( e, e );
      throw new EucalyptusCloudException( e );
    } finally {
      RequestContext.setEvent( context );
    }
  }

}