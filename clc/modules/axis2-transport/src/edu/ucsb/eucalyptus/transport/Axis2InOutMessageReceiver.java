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

package edu.ucsb.eucalyptus.transport;

import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.transport.binding.Binding;
import edu.ucsb.eucalyptus.transport.binding.BindingManager;
import edu.ucsb.eucalyptus.transport.http.Axis2HttpWorker;
import edu.ucsb.eucalyptus.transport.query.GenericHttpDispatcher;
import edu.ucsb.eucalyptus.transport.query.HttpRequest;
import edu.ucsb.eucalyptus.util.BindingUtil;
import edu.ucsb.eucalyptus.util.WalrusProperties;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.receivers.AbstractInOutMessageReceiver;
import org.apache.axis2.transport.http.server.AxisHttpResponse;
import org.apache.axis2.util.JavaUtils;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;
import org.apache.neethi.Policy;
import org.apache.rampart.RampartMessageData;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.jibx.runtime.JiBXException;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.config.ExceptionHelper;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Vector;

public class Axis2InOutMessageReceiver extends AbstractInOutMessageReceiver {

  private static Logger LOG = Logger.getLogger( Axis2InOutMessageReceiver.class );
  private Class serviceClass;
  private Axis2MessageReceiver msgReceiver;

  public Axis2InOutMessageReceiver( Axis2MessageReceiver msgReceiver, Class serviceClass ) throws AxisFault {
    this.serviceClass = serviceClass;
    this.msgReceiver = msgReceiver;
  }

  public void invokeBusinessLogic( MessageContext msgContext, MessageContext newMsgContext ) throws AxisFault {
    String methodName = this.findOperation( msgContext );
    Class serviceMethodArgType = this.findArgumentClass( methodName );

    SOAPFactory factory = this.getSOAPFactory( msgContext );
    OMElement msgBodyOm = msgContext.getEnvelope().getBody().getFirstElement();

    String bindingName = this.findBindingName( msgBodyOm );
    EucalyptusMessage wrappedParam = this.bindMessage( methodName, serviceMethodArgType, msgBodyOm, bindingName );

    HttpRequest httprequest = ( HttpRequest ) msgContext.getProperty( GenericHttpDispatcher.HTTP_REQUEST );
    if ( httprequest == null ) {
      this.verifyUser( msgContext, wrappedParam );
    } else {
      bindingName = httprequest.getBindingName();
      Policy p = new Policy();
      newMsgContext.setProperty( RampartMessageData.KEY_RAMPART_POLICY, p );

    }
    MuleMessage message = this.invokeService( methodName, wrappedParam );

    if ( message == null )
      throw new AxisFault( "Received a NULL response. This is a bug -- it should NEVER happen." );

    this.checkException( message );


    if ( newMsgContext != null ) {
      SOAPEnvelope envelope = generateMessage( methodName, factory, bindingName, message.getPayload() );
      newMsgContext.setEnvelope( envelope );
    }

    newMsgContext.setProperty( Axis2HttpWorker.REAL_HTTP_REQUEST, msgContext.getProperty( Axis2HttpWorker.REAL_HTTP_REQUEST ) );
    newMsgContext.setProperty( Axis2HttpWorker.REAL_HTTP_RESPONSE, msgContext.getProperty( Axis2HttpWorker.REAL_HTTP_RESPONSE ) );

    if ( message.getPayload() instanceof WalrusErrorMessageType ) {
      WalrusErrorMessageType errorMessage = ( WalrusErrorMessageType ) message.getPayload();
      msgContext.setProperty( Axis2HttpWorker.HTTP_STATUS, errorMessage.getHttpCode() );
      newMsgContext.setProperty( Axis2HttpWorker.HTTP_STATUS, errorMessage.getHttpCode() );
      //This selects the data formatter
      newMsgContext.setProperty( "messageType", "application/walrus" );
      return;
    }

    /*     if(message.getPayload() instanceof WalrusDeleteResponseType) {
       msgContext.setProperty(Axis2HttpWorker.HTTP_STATUS, HttpStatus.SC_NO_CONTENT);
       newMsgContext.setProperty(Axis2HttpWorker.HTTP_STATUS, HttpStatus.SC_NO_CONTENT);
       return;
   } */

    Boolean putType = ( Boolean ) msgContext.getProperty( WalrusProperties.STREAMING_HTTP_PUT );
    Boolean getType = ( Boolean ) msgContext.getProperty( WalrusProperties.STREAMING_HTTP_GET );
    if ( getType != null || putType != null ) {
      WalrusDataResponseType reply = ( WalrusDataResponseType ) message.getPayload();
      AxisHttpResponse response = ( AxisHttpResponse ) msgContext.getProperty( Axis2HttpWorker.REAL_HTTP_RESPONSE );
      response.addHeader( new BasicHeader( "Last-Modified", reply.getLastModified() ) );
      response.addHeader( new BasicHeader( "ETag", reply.getEtag() ) );
      if ( getType != null ) {
        newMsgContext.setProperty( WalrusProperties.STREAMING_HTTP_GET, getType );
        Long contentLength = reply.getSize();
        response.addHeader( new BasicHeader( HTTP.CONTENT_LEN, String.valueOf( contentLength ) ) );
        List<MetaDataEntry> metaData = reply.getMetaData();
        for ( MetaDataEntry metaDataEntry : metaData ) {
          response.addHeader( new BasicHeader( WalrusProperties.AMZ_META_HEADER_PREFIX + metaDataEntry.getName(), metaDataEntry.getValue() ) );
        }
        if ( getType.equals( Boolean.TRUE ) ) {
          WalrusDataRequestType request = ( WalrusDataRequestType ) wrappedParam;
          newMsgContext.setProperty( "GET_KEY", request.getBucket() + "." + request.getKey() );
          newMsgContext.setProperty( "GET_RANDOM_KEY", request.getRandomKey() );
          Boolean isCompressed = request.getIsCompressed();
          if ( isCompressed != null ) {
            newMsgContext.setProperty( "GET_COMPRESSED", isCompressed );
          }
        }
        //This selects the data formatter
        newMsgContext.setProperty( "messageType", "application/walrus" );
      }
    }

  }

  private void checkException( final MuleMessage message ) throws AxisFault {
    if ( message.getPayload() != null && message.getPayload() instanceof EucalyptusErrorMessageType ) {
      throw new AxisFault( message.getPayload().toString() );
    } else if ( message.getExceptionPayload() != null ) {
      MuleException umoException = ExceptionHelper.getRootMuleException( message.getExceptionPayload().getException() );
      if ( umoException.getCause() != null )
        throw AxisFault.makeFault( umoException.getCause() );
      else
        throw AxisFault.makeFault( umoException );
    }
  }

  private SOAPEnvelope generateMessage( final String methodName, final SOAPFactory factory, final String bindingName, final Object response ) {
    SOAPEnvelope envelope = null;
    LOG.info( "[" + serviceClass.getSimpleName() + ":" + methodName + "] Got return type " + response.getClass().getSimpleName() );
    try {
      /** construct the response **/
      envelope = BindingManager.getBinding( bindingName, this.serviceClass ).toEnvelope( factory, response, false );
    }
    catch ( JiBXException e ) {
      LOG.error( e, e );
    }
    LOG.info( "[" + serviceClass.getSimpleName() + ":" + methodName + "] Returning message of type " + response.getClass().getSimpleName() );
    return envelope;
  }

  private MuleMessage invokeService( final String methodName, final EucalyptusMessage wrappedParam ) throws AxisFault {
    LOG.info( "[" + serviceClass.getSimpleName() + ":" + methodName + "] Invoking method " + methodName );
    MuleMessage message = null;
    try {
      message = this.msgReceiver.routeMessage( new DefaultMuleMessage( this.msgReceiver.getConnector().getMessageAdapter( wrappedParam ) ), true );
    }
    catch ( MuleException wsException ) {
      MuleException umoException = ExceptionHelper.getRootMuleException( wsException );
      if ( umoException.getCause() != null )
        throw AxisFault.makeFault( umoException.getCause() );
      else
        throw AxisFault.makeFault( umoException );
    }
    return message;
  }

  private EucalyptusMessage bindMessage( final String methodName, final Class serviceMethodArgType, final OMElement msgBodyOm, final String bindingName ) throws AxisFault {
    EucalyptusMessage wrappedParam = null;
    try {
      /** unmarshall the incoming message **/
      Binding msgBinding = BindingManager.getBinding( bindingName, this.serviceClass );
      wrappedParam = ( EucalyptusMessage ) msgBinding.fromOM( msgBodyOm, serviceMethodArgType );
      LOG.info( "[" + serviceClass.getSimpleName() + ":" + methodName + "] Unmarshalled parameter type " + wrappedParam.getClass() );
    }
    catch ( JiBXException e ) {
      LOG.error( e, e );
      throw AxisFault.makeFault( e );
    }
    return wrappedParam;
  }

  private String findBindingName( final OMElement msgBodyOm ) {
    String bindingName = null;
    String nsUri = msgBodyOm.getNamespace().getNamespaceURI();
    bindingName = BindingUtil.sanitizeNamespace( nsUri );
    return bindingName;
  }

  private Class findArgumentClass( final String methodName ) throws AxisFault {
    Class serviceMethodArgType = null;
    try {
      serviceMethodArgType = Class.forName( "edu.ucsb.eucalyptus.msgs." + methodName + "Type" );
    }
    catch ( ClassNotFoundException e ) {
      throw new AxisFault( "Argument type not found: edu.ucsb.eucalyptus.msgs." + methodName + "Type" );
    }
    return serviceMethodArgType;
  }

  private String findOperation( final MessageContext msgContext ) throws AxisFault { /**Find the axisOperation that has been set by the Dispatch phase.**/
    AxisOperation op = msgContext.getOperationContext().getAxisOperation();
    String methodName = null;
    if ( op == null || ( op.getName() == null ) || ( ( methodName = JavaUtils.xmlNameToJava( op.getName().getLocalPart() ) ) == null ) )
      throw new AxisFault( "Operation not found: " + op.getName() );
    return methodName;
  }

  private void verifyUser( MessageContext msgContext, EucalyptusMessage msg ) throws EucalyptusCloudException {
    Vector<WSHandlerResult> wsResults = ( Vector<WSHandlerResult> ) msgContext.getProperty( WSHandlerConstants.RECV_RESULTS );
    for ( WSHandlerResult wsResult : wsResults )
      if ( wsResult.getResults() != null )
        for ( WSSecurityEngineResult engResult : ( Vector<WSSecurityEngineResult> ) wsResult.getResults() )
          if ( engResult.containsKey( WSSecurityEngineResult.TAG_X509_CERTIFICATE ) ) {
            X509Certificate cert = ( X509Certificate ) engResult.get( WSSecurityEngineResult.TAG_X509_CERTIFICATE );
            msg = this.msgReceiver.getProperties().getAuthenticator().authenticate( cert, msg );
          }
  }

}
