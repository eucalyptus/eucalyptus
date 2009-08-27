package com.eucalyptus.ws.client;

import java.net.URI;
import java.security.GeneralSecurityException;

import org.apache.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.ServiceBootstrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.ws.client.pipeline.InternalClientPipeline;
import com.eucalyptus.ws.handlers.NioResponseHandler;

import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

public class ServiceProxy {
  private static Logger LOG = Logger.getLogger( ServiceProxy.class );
  private Component     component;
  private String        name;
  private MuleClient    muleClient;
  private NioClient     nioClient;
  private URI           address;

  public static ServiceProxy lookup( Component component, String name) {
    return (ServiceProxy) ServiceBootstrapper.getRegistry( ).lookupObject( component.name( ) + "/" + name );
  }
  
  private static MuleClient getMuleClient( ) throws Exception {
    return new MuleClient( );
  }

  private NioClient getNioClient( ) throws Exception {
    return new NioClient( this.address.getHost( ), this.address.getPort( ), this.address.getPath( ), new InternalClientPipeline( new NioResponseHandler( ) ) );
  }

  public ServiceProxy( Component component, String name, URI uri ) {
    super( );
    this.address = uri;
    this.component = component;
    this.name = name;
  }

  
  @SuppressWarnings( "static-access" )
  public void dispatch( EucalyptusMessage msg ) {
    MuleEvent context = RequestContext.getEvent( );
    try {
      if ( component.isLocal( ) ) {
        this.getMuleClient( ).dispatch( this.address.toASCIIString( ), msg, null );
      } else {
        this.getNioClient( ).dispatch( msg );
      }
    } catch ( Exception e ) {
      LOG.error( e );
    } finally {
      RequestContext.setEvent( context );
    }
  }

  @SuppressWarnings( "static-access" )
  public EucalyptusMessage send( EucalyptusMessage msg ) throws EucalyptusCloudException {
    MuleEvent context = RequestContext.getEvent( );
    try {
      if ( component.isLocal( ) ) {
        MuleMessage reply = this.getMuleClient( ).send( this.address.toASCIIString( ), msg, null );

        if ( reply.getExceptionPayload( ) != null ) {
          throw new EucalyptusCloudException( reply.getExceptionPayload( ).getRootException( ).getMessage( ), reply.getExceptionPayload( ).getRootException( ) );
        } else {
          return ( EucalyptusMessage ) reply.getPayload( );
        }
      } else {
        return this.getNioClient( ).send( msg );
      }
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new EucalyptusCloudException( e );
    } finally {
      RequestContext.setEvent( context );
    }
  }
}
