/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.ws.handlers;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPFaultCode;
import org.apache.axiom.soap.SOAPFaultDetail;
import org.apache.axiom.soap.SOAPFaultReason;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.util.WalrusUtil;
import com.eucalyptus.ws.EucalyptusRemoteFault;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;
import edu.ucsb.eucalyptus.msgs.WalrusErrorMessageType;

@ChannelPipelineCoverage("one")
public class WalrusSoapHandler extends MessageStackHandler {
	private static Logger LOG = Logger.getLogger( WalrusSoapHandler.class );

	@Override
	public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
		if ( event.getMessage( ) instanceof MappingHttpMessage ) {
			final MappingHttpMessage message = ( MappingHttpMessage ) event.getMessage( );
			String content = message.getContent( ).toString( "UTF-8" );
			StAXSOAPModelBuilder soapBuilder = new StAXSOAPModelBuilder( HoldMe.getXMLStreamReader( content ), HoldMe.getOMSOAP11Factory( ), null );
			SOAPEnvelope env = ( SOAPEnvelope ) soapBuilder.getDocumentElement( );
			message.setSoapEnvelope( env );
			message.setMessageString( content );
			if ( !env.hasFault( ) ) {
				message.setOmMessage( env.getBody( ).getFirstElement( ) );
			} else {
				final SOAPHeader header = env.getHeader( );
				if(header != null) {
				final List<SOAPHeaderBlock> headers = Lists.newArrayList( header.examineAllHeaderBlocks( ) );
					// :: try to get the fault info from the soap header -- hello there? :://
					String action = "ProblemAction";
					String relatesTo = "RelatesTo";
					for ( final SOAPHeaderBlock headerBlock : headers ) {
						if ( action.equals( headerBlock.getLocalName( ) ) ) {
							action = headerBlock.getText( );
						} else if ( relatesTo.equals( headerBlock.getLocalName( ) ) ) {
							relatesTo = headerBlock.getText( );
						}
					}
					// :: process the real fault :://
					final SOAPFault fault = env.getBody( ).getFault( );
					if(fault != null) {
						String faultReason = "";
						final Iterator children = fault.getChildElements( );
						while ( children.hasNext( ) ) {
							final OMElement child = ( OMElement ) children.next( );
							faultReason += child.getText( );
						}
						final String faultCode = fault.getCode( ).getText( );
						faultReason = faultReason.replaceAll( faultCode, "" );
						throw new EucalyptusRemoteFault( action, relatesTo, faultCode, faultReason );
					}
				}
			}
		}
	}

	@Override
	public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
		if ( event.getMessage( ) instanceof MappingHttpMessage ) {
			final MappingHttpMessage httpMessage = ( MappingHttpMessage ) event.getMessage( );
			if( httpMessage.getMessage( ) instanceof EucalyptusErrorMessageType ) {
				EucalyptusErrorMessageType errorMessage = (EucalyptusErrorMessageType) httpMessage.getMessage( );
				BaseMessage errMsg = WalrusUtil.convertErrorMessage(errorMessage);
				if(errMsg instanceof WalrusErrorMessageType) {
					WalrusErrorMessageType walrusErrMsg = (WalrusErrorMessageType) errMsg;
					httpMessage.setSoapEnvelope( createFault( walrusErrMsg.getCode(), 
							walrusErrMsg.getMessage(), 
							walrusErrMsg.getStatus().getReasonPhrase(), 
							walrusErrMsg.getResourceType(), 
							walrusErrMsg.getResource()));
				} else {
					httpMessage.setSoapEnvelope( Binding.createFault( errorMessage.getSource( ), errorMessage.getMessage( ), errorMessage.getStatusMessage( ) ) );
				}
			} else if( httpMessage.getMessage( ) instanceof ExceptionResponseType ) {
			  ExceptionResponseType errorMessage = (ExceptionResponseType) httpMessage.getMessage( );
        BaseMessage errMsg = WalrusUtil.convertErrorMessage(errorMessage);
        if(errMsg instanceof WalrusErrorMessageType) {
          WalrusErrorMessageType walrusErrMsg = (WalrusErrorMessageType) errMsg;
          httpMessage.setSoapEnvelope( createFault( walrusErrMsg.getCode(), 
              walrusErrMsg.getMessage(), 
              walrusErrMsg.getStatus().getReasonPhrase(), 
              walrusErrMsg.getResourceType(), 
              walrusErrMsg.getResource()));
        }
			} else {
				// :: assert sourceElem != null :://
				httpMessage.setSoapEnvelope( HoldMe.getOMSOAP11Factory( ).getDefaultEnvelope( ) );
				httpMessage.getSoapEnvelope( ).getBody( ).addChild( httpMessage.getOmMessage( ) );
			}
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream( );

			HoldMe.canHas.lock( );
			try {
				httpMessage.getSoapEnvelope( ).serialize( byteOut );//HACK: xml breakage?
			} finally {
				HoldMe.canHas.unlock();
			}

			ChannelBuffer buffer = ChannelBuffers.wrappedBuffer( byteOut.toByteArray( ) );
			httpMessage.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes( ) ) );
			httpMessage.addHeader( HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=UTF-8" );
			httpMessage.setContent( buffer );
		}
	}

	private static SOAPEnvelope createFault(  String faultCode, String faultReason, String faultDetails, 
			String resourceType, String resource )  {
		HoldMe.canHas.lock( );
		try {
			SOAPFactory soapFactory = HoldMe.getOMSOAP11Factory();

			SOAPFaultCode soapFaultCode = soapFactory.createSOAPFaultCode();
			soapFaultCode.setText( SOAP11Constants.FAULT_CODE_SENDER + "." + faultCode );

			SOAPFaultReason soapFaultReason = soapFactory.createSOAPFaultReason();
			soapFaultReason.setText( faultReason );

			SOAPFaultDetail soapFaultDetail = soapFactory.createSOAPFaultDetail();

			if(resource != null) {
				OMElement detail = soapFactory.createOMElement(new QName(resourceType));
				detail.setText(resource);
				soapFaultDetail.addDetailEntry(detail);
			} else {
				soapFaultDetail.setText(faultDetails);
			}

			SOAPEnvelope soapEnv = soapFactory.getDefaultEnvelope( );
			SOAPFault soapFault = soapFactory.createSOAPFault( );
			soapFault.setCode( soapFaultCode );
			soapFault.setDetail( soapFaultDetail );
			soapFault.setReason( soapFaultReason );
			soapEnv.getBody( ).addFault( soapFault );
			return soapEnv;
		} finally {
			HoldMe.canHas.unlock();
		}
	}

}
