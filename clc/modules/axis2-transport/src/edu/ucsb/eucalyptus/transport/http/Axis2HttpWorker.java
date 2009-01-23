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
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.transport.http;

import edu.ucsb.eucalyptus.util.Messaging;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HTTPTransportReceiver;
import org.apache.axis2.transport.http.HTTPTransportUtils;
import org.apache.axis2.transport.http.server.AxisHttpRequest;
import org.apache.axis2.transport.http.server.AxisHttpResponse;
import org.apache.axis2.transport.http.server.HttpUtils;
import org.apache.axis2.transport.http.server.Worker;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EncodingUtils;
import org.apache.log4j.Logger;
import org.mule.transport.NullPayload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Axis2HttpWorker implements Worker {

  private static Logger LOG = Logger.getLogger( Axis2HttpWorker.class );

  public static String REAL_HTTP_REQUEST = "HAI_IS_REAL_HTTP_REQUEST";
  public static String REAL_HTTP_RESPONSE = "YHALO_I_ARE_TEH_HTTP_RESPONSE";
  public static String HTTP_STATUS = "IS_IT_CAN_BE_HTTP_STATUS";

  public Axis2HttpWorker() {}

  public void service( final AxisHttpRequest request, final AxisHttpResponse response, final MessageContext msgContext ) throws HttpException, IOException {

    ConfigurationContext configurationContext = msgContext.getConfigurationContext();
    final String servicePath = configurationContext.getServiceContextPath();
    final String contextPath = ( servicePath.startsWith( "/" ) ? servicePath : "/" + servicePath ) + "/";

    String uri = request.getRequestURI();
    String method = request.getMethod();
    String soapAction = HttpUtils.getSoapAction( request );
    Handler.InvocationResponse pi = null;
    msgContext.setProperty( REAL_HTTP_REQUEST, request );
    msgContext.setProperty( REAL_HTTP_RESPONSE, response );

    if ( method.equals( HTTPConstants.HEADER_GET ) ) {
      if ( ( uri.startsWith( "/latest/" ) || uri.matches("/\\d\\d\\d\\d-\\d\\d-\\d\\d/.*") ) && handleMetaData( response, msgContext, uri ) )
        return;
      if ( !uri.startsWith( contextPath ) ) {
        response.setStatus( HttpStatus.SC_MOVED_PERMANENTLY );
        response.addHeader( new BasicHeader( "Location", ( contextPath + uri ).replaceAll( "//", "/" ) ) );
        return;
      }
      if ( uri.endsWith( "services/" ) ) {
        handleServicesList( response, configurationContext );
        return;
      }
      pi = handleGet( request, response, msgContext );
    }
    else if ( method.equals( HTTPConstants.HEADER_POST ) ) {
      String contentType = request.getContentType();
      if ( HTTPTransportUtils.isRESTRequest( contentType ) )
        pi = Axis2HttpWorker.processXMLRequest( msgContext, request.getInputStream(), response.getOutputStream(), contentType );
      else {
        String ip = ( String ) msgContext.getProperty( MessageContext.TRANSPORT_ADDR );
        if ( ip != null )
          uri = ip + uri;
        pi = HTTPTransportUtils.processHTTPPostRequest( msgContext, request.getInputStream(), response.getOutputStream(), contentType, soapAction, uri );
      }
    }
    else if ( method.equals( HTTPConstants.HEADER_PUT ) ) {
      String contentType = request.getContentType();
      msgContext.setProperty( Constants.Configuration.CONTENT_TYPE, contentType );

      pi = Axis2HttpWorker.processXMLRequest( msgContext, request.getInputStream(), response.getOutputStream(), contentType );
    }
    else if ( method.equals( HTTPConstants.HEADER_DELETE ) )
      pi = Axis2HttpWorker.processURLRequest( msgContext, response.getOutputStream(), null );
    else if ( method.equals( "HEAD" ) )
      pi = Axis2HttpWorker.processURLRequest( msgContext, response.getOutputStream(), null );
    else
      throw new MethodNotSupportedException( method + " method not supported" );

    Boolean holdResponse = ( Boolean ) msgContext.getProperty( RequestResponseTransport.HOLD_RESPONSE );
    if ( pi.equals( Handler.InvocationResponse.SUSPEND ) || ( holdResponse != null && Boolean.TRUE.equals( holdResponse ) ) )
      try {
        ( ( RequestResponseTransport ) msgContext.getProperty( RequestResponseTransport.TRANSPORT_CONTROL ) ).awaitResponse();
      }
      catch ( InterruptedException e ) {
        throw new IOException( "We were interrupted, so this may not function correctly:" + e.getMessage() );
      }
    RequestResponseTransport requestResponseTransportControl = ( RequestResponseTransport ) msgContext.getProperty( RequestResponseTransport.TRANSPORT_CONTROL );
    if ( TransportUtils.isResponseWritten( msgContext )  || ( ( requestResponseTransportControl != null ) && requestResponseTransportControl.getStatus().equals( RequestResponseTransport.RequestResponseTransportStatus.SIGNALLED ) ) );
    else
      response.setStatus( HttpStatus.SC_ACCEPTED );
    Integer status = ( Integer ) msgContext.getProperty( HTTP_STATUS );
    if ( status != null ) response.setStatus( status );
  }

  private void handleServicesList( final AxisHttpResponse response, final ConfigurationContext configurationContext ) throws IOException {
    String s = HTTPTransportReceiver.getServicesHTML( configurationContext );
    response.setStatus( HttpStatus.SC_OK );
    response.setContentType( "text/html" );
    OutputStream out = response.getOutputStream();
    out.write( EncodingUtils.getBytes( s, HTTP.ISO_8859_1 ) );
  }

  private boolean handleMetaData( final AxisHttpResponse response, final MessageContext msgContext, final String uri ) {
    try {
      String newUri = null;
      if( uri.startsWith( "/latest/" ) )
        newUri = uri.replaceAll( "/latest/", msgContext.getProperty( MessageContext.REMOTE_ADDR ) + ":" );
      else
        newUri = uri.replaceAll( "/\\d\\d\\d\\d-\\d\\d-\\d\\d/", msgContext.getProperty( MessageContext.REMOTE_ADDR ) + ":" );
      LOG.debug( "Metadata request: " + newUri );
      Object reply = Messaging.send( "vm://VmMetadata", newUri );
      if ( !( reply instanceof NullPayload ) ) {
        response.setStatus( HttpStatus.SC_OK );
        response.setContentType( "text/html" );
        OutputStream out = response.getOutputStream();
        out.write( EncodingUtils.getBytes( ( ( String ) reply ) + "\n", HTTP.ISO_8859_1 ) );
      }
      else
        response.setStatus( HttpStatus.SC_NOT_FOUND );
      return true;
    }
    catch ( Exception e ) {
      LOG.error( e, e );
    }
    return false;
  }

  private Handler.InvocationResponse handleGet( final AxisHttpRequest request, final AxisHttpResponse response, final MessageContext msgContext ) throws AxisFault {
    Handler.InvocationResponse pi;
    String contentType = null;
    Header[] headers = request.getHeaders( HTTPConstants.HEADER_CONTENT_TYPE );
    if ( headers != null && headers.length > 0 ) {
      contentType = headers[ 0 ].getValue();
      int index = contentType.indexOf( ';' );
      if ( index > 0 ) {
        contentType = contentType.substring( 0, index );
      }
    }
    pi = Axis2HttpWorker.processURLRequest( msgContext, response.getOutputStream(), contentType );
    return pi;
  }

  public static Handler.InvocationResponse processURLRequest( MessageContext msgContext, OutputStream out, String contentType ) throws AxisFault {
    try {
      if ( contentType == null || "".equals( contentType ) ) {
        contentType = HTTPConstants.MEDIA_TYPE_X_WWW_FORM;
      }
      msgContext.setDoingREST( true );
      msgContext.setProperty( MessageContext.TRANSPORT_OUT, out );
      String charSetEncoding = BuilderUtil.getCharSetEncoding( contentType );
      msgContext.setProperty( Constants.Configuration.CHARACTER_SET_ENCODING, charSetEncoding );
      SOAPEnvelope soapEnvelope = TransportUtils.createSOAPMessage( msgContext, null, contentType );
      msgContext.setEnvelope( soapEnvelope );
    }
    catch ( Exception e ) {
      throw e instanceof AxisFault ? ( AxisFault ) e : AxisFault.makeFault( e );
    }
    finally {
      String messageType = ( String ) msgContext.getProperty( Constants.Configuration.MESSAGE_TYPE );
      if ( HTTPConstants.MEDIA_TYPE_X_WWW_FORM.equals( messageType ) || HTTPConstants.MEDIA_TYPE_MULTIPART_FORM_DATA.equals( messageType ) )
        msgContext.setProperty( Constants.Configuration.MESSAGE_TYPE, HTTPConstants.MEDIA_TYPE_APPLICATION_XML );
    }
    AxisEngine axisEngine = new AxisEngine( msgContext.getConfigurationContext() );
    return axisEngine.receive( msgContext );
  }

  public static Handler.InvocationResponse processXMLRequest( MessageContext msgContext, InputStream in, OutputStream out, String contentType ) throws AxisFault {
    try {
      msgContext.setDoingREST( true );
      String charSetEncoding = BuilderUtil.getCharSetEncoding( contentType );
      msgContext.setProperty( Constants.Configuration.CHARACTER_SET_ENCODING, charSetEncoding );
      SOAPFactory soapFactory = new SOAP11Factory();
      SOAPEnvelope soapEnvelope = soapFactory.getDefaultEnvelope();
      msgContext.setEnvelope( soapEnvelope );
      in = HTTPTransportUtils.handleGZip( msgContext, in );
      msgContext.setProperty( Constants.Configuration.CONTENT_TYPE, contentType );
      msgContext.setProperty( MessageContext.TRANSPORT_OUT, out );
      msgContext.setProperty( MessageContext.TRANSPORT_IN, in );
    }
    catch ( Exception e ) {
      throw e instanceof AxisFault ? ( AxisFault ) e : AxisFault.makeFault( e );
    }
    finally {
      String messageType = ( String ) msgContext.getProperty( Constants.Configuration.MESSAGE_TYPE );
      if ( HTTPConstants.MEDIA_TYPE_X_WWW_FORM.equals( messageType ) || HTTPConstants.MEDIA_TYPE_MULTIPART_FORM_DATA.equals( messageType ) )
        msgContext.setProperty( Constants.Configuration.MESSAGE_TYPE, HTTPConstants.MEDIA_TYPE_APPLICATION_XML );
    }
    AxisEngine axisEngine = new AxisEngine( msgContext.getConfigurationContext() );
    return axisEngine.receive( msgContext );
  }

}
