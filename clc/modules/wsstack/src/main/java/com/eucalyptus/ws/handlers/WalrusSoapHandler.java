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
 * Author: Neil Soman neil@eucalyptus.com
 */

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

import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
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
				EucalyptusMessage errMsg = WalrusUtil.convertErrorMessage(errorMessage);
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
