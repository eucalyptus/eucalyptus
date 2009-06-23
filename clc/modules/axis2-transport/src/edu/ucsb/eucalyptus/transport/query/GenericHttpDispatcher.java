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

package edu.ucsb.eucalyptus.transport.query;

import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.util.BindingUtil;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.dispatchers.RequestURIBasedDispatcher;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.log4j.Logger;
import org.apache.neethi.Policy;
import org.apache.rampart.RampartMessageData;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class GenericHttpDispatcher extends RequestURIBasedDispatcher {

  public static final String NAME = "GenericHttpDispatcher";
  public static final String HTTP_REQUEST = NAME + "_HTTP_REQUEST";
  private static Logger LOG = Logger.getLogger( GenericHttpDispatcher.class );

  public AxisService findService( final MessageContext messageContext ) throws AxisFault {
    if ( messageContext.isDoingREST() && messageContext.getExecutionChain() != null )
      prepareRequest( messageContext );
    return super.findService( messageContext );
  }

  public AxisOperation findOperation( AxisService service, MessageContext messageContext ) throws AxisFault {
    if ( !( this instanceof RESTfulDispatcher ) ) return null;

    RESTfulDispatcher dispatcher = ( RESTfulDispatcher ) this;

    HttpRequest httpRequest = ( HttpRequest ) messageContext.getProperty( HTTP_REQUEST );
    if ( httpRequest == null ) return null; // bail out to avoid messing with the message context.

    //:: test if this dispatcher accepts this request type :://
    if ( !dispatcher.accepts( httpRequest, messageContext ) ) return null;

    String maybeVersion = httpRequest.getParameters().get( Axis2QueryDispatcher.RequiredQueryParams.Version.toString() );
    String nameSpace = dispatcher.getNamespace();
    if ( maybeVersion != null )
      nameSpace = nameSpace.replaceAll( dispatcher.getBinding().getName(), maybeVersion );
    httpRequest.setOriginalNamespace( nameSpace );
    //:: set the operation name... this looks ugly... sigh :://
      String operationName;
      try {
        operationName = dispatcher.getOperation( httpRequest, messageContext );
      } catch(Exception ex) {
          throw new AxisFault("Could not process operation\n" + ex.getMessage());
      }
    httpRequest.setOperation( operationName );
    httpRequest.setBindingName( BindingUtil.sanitizeNamespace( dispatcher.getNamespace() ) );

    QuerySecurityHandler securityHandler = dispatcher.getSecurityHandler();
    UserInfo user = securityHandler.authenticate( httpRequest );

    //:: setup & verify the operation  :://
    if ( httpRequest.getOperation() == null )
      throw new AxisFault( "Protocol failure: Could not identify the operation component of the request." ); //this is a dispatcher failure, shouldn't have "accept()"ed the request

    //:: find the operation :://
    AxisOperation operation = service.getOperationByAction( httpRequest.getOperation().replaceAll("/*","") );
    if ( operation == null )
      throw new AxisFault( "Failed to process the request: Operation doesnt exist: " + httpRequest.getOperation() ); //this is a user failure, incorrectly specified Operation perhaps?

    //:: consume the request and turn it into a SOAP envelope :://
    QueryBinding binding = dispatcher.getBinding();
    OMElement msg = binding.bind( user, httpRequest, messageContext );
    messageContext.getEnvelope().getBody().addChild( msg );

    //:: massage rampart so it doesnt interfere :://
    Policy p = new Policy();
    messageContext.setProperty( RampartMessageData.KEY_RAMPART_POLICY, p );

    return operation;
  }

  private void prepareRequest( final MessageContext messageContext ) throws AxisFault {
    try {
      HttpRequest httpRequest = new HttpRequest();
      EndpointReference endpoint = messageContext.getTo();
      //:: fixes trailing '/' added by some clients :://
      if( endpoint.getAddress().endsWith( "Eucalyptus/" ) ) {
        endpoint.setAddress( endpoint.getAddress().replaceAll( "Eucalyptus/", "Eucalyptus" ) );
        httpRequest.setPureClient( true );
      }

      //:: fixes handling of arguments in POST :://
      if( (messageContext.getProperty( HTTPConstants.HTTP_METHOD )).equals( HTTPConstants.HTTP_METHOD_POST )) {
        BufferedReader in = new BufferedReader( new InputStreamReader( ( InputStream ) messageContext.getProperty( MessageContext.TRANSPORT_IN ) ) );
        String postLine = in.readLine();
        endpoint.setAddress( endpoint.getAddress() + (endpoint.getAddress().contains( "?" ) ? "&" : "?" ) + postLine );
      }
      URL url = new URL( "http://my.flavourite.host.com" + endpoint.getAddress() );
      httpRequest.setHostAddr( (String) messageContext.getProperty( MessageContext.TRANSPORT_ADDR ) );
      Map transportHeaders = (Map) messageContext.getProperty( MessageContext.TRANSPORT_HEADERS);
      if( transportHeaders != null && transportHeaders.get("Host") != null ) {
        String hostHeader = (String) transportHeaders.get("Host");
        if( hostHeader.indexOf( ':' ) >= 0 ) {
          hostHeader = hostHeader.split( ":" )[0];
        }
        httpRequest.setHostName( hostHeader );
      }
      httpRequest.setRequestURL( messageContext.getTo().getAddress() );

      //:: mangle the service path :://
      String serviceContextPath = messageContext.getConfigurationContext().getServiceContextPath();
      String serviceBasePath = url.toURI().getPath().substring( serviceContextPath.length() );

      String serviceName = serviceBasePath.split( "/" )[ 1 ];
      httpRequest.setService( serviceName );

      String serviceAddress = serviceContextPath + "/" + serviceName;
      httpRequest.setServicePath( serviceAddress );

      messageContext.setTo( endpoint );

      //:: extract query parameters :://
      String restQuery = url.toURI().getQuery();
      Map<String, String> httpParams = new HashMap<String, String>();
      if ( restQuery != null )
        for ( String p : restQuery.split( "&" ) ) {
          String[] splitParam = p.split( "=" );
          httpParams.put( splitParam[ 0 ], splitParam.length == 2 ? splitParam[ 1 ] : null );
        }
      httpRequest.setParameters( httpParams );

      //:: extract the additional path components :://
      String restPath = url.toURI().getPath().replaceAll( serviceAddress, "" );
      httpRequest.setOperationPath( restPath );

      //:: set the input stream for later use :://
      httpRequest.setInStream( ( InputStream ) messageContext.getProperty( MessageContext.TRANSPORT_IN ) );

      //:: get the HTTP headers :://
      httpRequest.setHeaders( ( Map<String, String> ) messageContext.getProperty( MessageContext.TRANSPORT_HEADERS ) );

      //:: and set the HTTP method type of the request :://
      httpRequest.setHttpMethod( ( String ) messageContext.getProperty( HTTPConstants.HTTP_METHOD ) );

      //:: remove the security phase since we arent doing WS-Security :://
      if ( messageContext.getExecutionChain().size() > 2 )
        messageContext.getExecutionChain().remove( 2 );

      //:: store the httprequest in the msgctx :://
      messageContext.setProperty( HTTP_REQUEST, httpRequest );
    }
    catch ( Exception e ) {
      LOG.error( e, e );
      throw AxisFault.makeFault( e );
    }
  }

  public void initDispatcher() {
    init( new HandlerDescription( NAME ) );
  }
}
