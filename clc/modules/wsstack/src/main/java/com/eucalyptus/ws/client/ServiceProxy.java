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
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.client;

import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.List;

import org.apache.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.registry.Registry;
import org.mule.module.client.MuleClient;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.ServiceBootstrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.NetworkUtil;
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
  private boolean isLocal = false;
  
  public static ServiceProxy lookup( String registryKey ) {
    Registry registry = ServiceBootstrapper.getRegistry( );
    return (ServiceProxy) registry.lookupObject( registryKey );
  }
  
  private static MuleClient getMuleClient( ) throws Exception {
    return new MuleClient( );
  }

  private NioClient getNioClient( ) throws Exception {
    return new NioClient( this.address.getHost( ), this.address.getPort( ), this.address.getPath( ), new InternalClientPipeline( new NioResponseHandler( ) ) );
  }

  public ServiceProxy( Component component, String name, URI uri, boolean isLocal ) {
    super( );
    this.address = uri;
    this.component = component;
    this.name = name;
    this.isLocal = isLocal;
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
      if( this.isLocal || NetworkUtil.testLocal( this.address.getHost( ) ) ) {
        this.getMuleClient( ).dispatch( this.component.getLocalUri( ), msg, null );
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
      if( this.isLocal || NetworkUtil.testLocal( this.address.getHost( ) ) ) {
        MuleMessage reply = this.getMuleClient( ).send( this.component.getLocalUri( ), msg, null );
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
