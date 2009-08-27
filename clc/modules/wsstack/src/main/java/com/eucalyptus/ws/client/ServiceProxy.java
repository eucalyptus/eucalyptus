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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.client;

import java.net.URI;
import java.security.GeneralSecurityException;

import org.apache.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.registry.Registry;
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
    Registry registry = ServiceBootstrapper.getRegistry( );
    String key = component.name( ) + "/" + name ;
    return (ServiceProxy) registry.lookupObject( key );
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
