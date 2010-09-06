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
package com.eucalyptus.ws.handlers;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.axiom.om.OMElement;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;
import org.apache.xml.dtm.ref.DTMNodeList;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.StorageProperties;
import com.eucalyptus.util.WalrusProperties;
import com.eucalyptus.util.WalrusUtil;
import com.eucalyptus.ws.InvalidOperationException;
import com.eucalyptus.ws.handlers.WalrusAuthenticationHandler.SecurityParameter;
import com.eucalyptus.ws.util.WalrusBucketLogger;
import com.eucalyptus.ws.util.XMLParser;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.cloud.BucketLogData;
import edu.ucsb.eucalyptus.msgs.AccessControlListType;
import edu.ucsb.eucalyptus.msgs.AccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.CanonicalUserType;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.Grant;
import edu.ucsb.eucalyptus.msgs.Grantee;
import edu.ucsb.eucalyptus.msgs.Group;
import edu.ucsb.eucalyptus.msgs.LoggingEnabled;
import edu.ucsb.eucalyptus.msgs.MetaDataEntry;
import edu.ucsb.eucalyptus.msgs.PutObjectResponseType;
import edu.ucsb.eucalyptus.msgs.TargetGrants;
import edu.ucsb.eucalyptus.msgs.WalrusDataGetRequestType;
import edu.ucsb.eucalyptus.msgs.WalrusDataRequestType;
import edu.ucsb.eucalyptus.msgs.WalrusRequestType;
import edu.ucsb.eucalyptus.msgs.PutObjectResponseType;
import edu.ucsb.eucalyptus.util.WalrusDataMessage;
import edu.ucsb.eucalyptus.util.WalrusDataMessenger;
import edu.ucsb.eucalyptus.util.WalrusDataQueue;
import groovy.lang.GroovyObject;

public class WalrusRESTBinding extends RestfulMarshallingHandler {
	private static Logger LOG = Logger.getLogger( WalrusRESTBinding.class );
	private static final String SERVICE = "service";
	private static final String BUCKET = "bucket";
	private static final String OBJECT = "object";
	private static final Map<String, String> operationMap = populateOperationMap();
	private static WalrusDataMessenger putMessenger;
	public static final int DATA_MESSAGE_SIZE = 102400;
	private String key;
	private String randomKey;
	private WalrusDataQueue<WalrusDataMessage> putQueue;
	private static final int PUT_ENTRY_TIMEOUT = 500;
	private static final int PUT_RETRY_COUNT = 30;

	@Override
	public void handleUpstream( final ChannelHandlerContext channelHandlerContext, final ChannelEvent channelEvent ) throws Exception {
		LOG.trace( LogUtil.dumpObject( channelEvent ) );
		if ( channelEvent instanceof MessageEvent ) {
			final MessageEvent msgEvent = ( MessageEvent ) channelEvent;
			try {
				this.incomingMessage( channelHandlerContext, msgEvent );
			} catch ( Throwable e ) {
				LOG.error( e, e );
				Channels.fireExceptionCaught( channelHandlerContext, e );
				return;
			} 
		} else if (channelEvent.toString().contains("DISCONNECTED") || 
				channelEvent.toString().contains("CLOSED")) {
			if(key != null && randomKey != null) {
				putMessenger.removeQueue(key, randomKey);
				putQueue = null;
			}
		}
		channelHandlerContext.sendUpstream( channelEvent );
	}

	@Override
	public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
		if ( event.getMessage( ) instanceof MappingHttpRequest ) {
			MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
			namespace = "http://s3.amazonaws.com/doc/" + WalrusProperties.NAMESPACE_VERSION;
			// TODO: get real user data here too
			EucalyptusMessage msg = (EucalyptusMessage) this.bind( "admin", true, httpRequest );
			httpRequest.setMessage( msg );
			if(msg instanceof WalrusDataGetRequestType) {
				WalrusDataGetRequestType getObject = (WalrusDataGetRequestType) msg;
				getObject.setChannel(ctx.getChannel());
			}
			if(msg instanceof WalrusDataRequestType) {
				String expect = httpRequest.getHeader(HttpHeaders.Names.EXPECT);
				if(expect != null) {
					if(expect.equals("100-continue")) {
						HttpResponse response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE );
						DownstreamMessageEvent newEvent = new DownstreamMessageEvent( ctx.getChannel( ), event.getFuture(), response, null );
						ctx.sendDownstream( newEvent );
					}
				}
			}
		} else if(event.getMessage() instanceof HttpChunk) {
			if(putQueue != null) {
				HttpChunk httpChunk = (HttpChunk) event.getMessage();
				handleHttpChunk(httpChunk);
			}
		}
	}

	@Override
	public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
		if ( event.getMessage( ) instanceof MappingHttpResponse ) {
			MappingHttpResponse httpResponse = ( MappingHttpResponse ) event.getMessage( );
			EucalyptusMessage msg = (EucalyptusMessage) httpResponse.getMessage( );
			Binding binding;

			if(!(msg instanceof EucalyptusErrorMessageType)) {
				binding = BindingManager.getBinding( BindingManager.sanitizeNamespace( namespace ) );
				if(putQueue != null) {
					putQueue = null;
				}
			} else {
				binding = BindingManager.getBinding( BindingManager.sanitizeNamespace( "http://msgs.eucalyptus.com" ) );
				if(putQueue != null) {
					putQueue = null;
				}
			}
			if(msg != null) {
				OMElement omMsg = binding.toOM( msg );
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				HoldMe.canHas.lock(); 
				try {
					omMsg.serialize( byteOut );
				} finally {
					HoldMe.canHas.unlock();
				}
				byte[] req = byteOut.toByteArray();
				ChannelBuffer buffer = ChannelBuffers.copiedBuffer( req );
				httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(buffer.readableBytes() ) );
				httpResponse.addHeader( HttpHeaders.Names.CONTENT_TYPE, "application/xml" );
				httpResponse.setContent( buffer );
			}
		}
	}

	private static Map<String, String> populateOperationMap() {
		Map<String, String> newMap = new HashMap<String, String>();
		//Service operations
		newMap.put(SERVICE + WalrusProperties.HTTPVerb.GET.toString(), "ListAllMyBuckets");

		//Bucket operations
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.OperationParameter.acl.toString(), "GetBucketAccessControlPolicy");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.OperationParameter.acl.toString(), "SetRESTBucketAccessControlPolicy");

		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString(), "ListBucket");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.OperationParameter.prefix.toString(), "ListBucket");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.OperationParameter.maxkeys.toString(), "ListBucket");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.OperationParameter.marker.toString(), "ListBucket");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.OperationParameter.delimiter.toString(), "ListBucket");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.PUT.toString(), "CreateBucket");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.DELETE.toString(), "DeleteBucket");

		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.OperationParameter.location.toString(), "GetBucketLocation");

		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.OperationParameter.logging.toString(), "GetBucketLoggingStatus");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.OperationParameter.logging.toString(), "SetBucketLoggingStatus");

		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.OperationParameter.versions.toString(), "ListVersions");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.OperationParameter.versioning.toString(), "GetBucketVersioningStatus");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.OperationParameter.versioning.toString(), "SetBucketVersioningStatus");

		//Object operations
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.OperationParameter.acl.toString(), "GetObjectAccessControlPolicy");
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.OperationParameter.acl.toString(), "SetRESTObjectAccessControlPolicy");

		newMap.put(BUCKET + WalrusProperties.HTTPVerb.POST.toString(), "PostObject");

		newMap.put(OBJECT + WalrusProperties.HTTPVerb.PUT.toString(), "PutObject");
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.COPY_SOURCE.toString(), "CopyObject");
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.GET.toString(), "GetObject");
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.OperationParameter.torrent.toString(), "GetObject");
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.DELETE.toString(), "DeleteObject");

		newMap.put(OBJECT + WalrusProperties.HTTPVerb.HEAD.toString(), "GetObject");
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.GET.toString() + "extended", "GetObjectExtended");

		newMap.put(OBJECT + WalrusProperties.HTTPVerb.DELETE.toString() + WalrusProperties.OperationParameter.versionId.toString().toLowerCase(), "DeleteVersion");
		return newMap;
	}


	@Override
	public Object bind( final String userId, final boolean admin, final MappingHttpRequest httpRequest ) throws Exception {
		String servicePath = httpRequest.getServicePath();
		Map bindingArguments = new HashMap();
		final String operationName = getOperation(httpRequest, bindingArguments);
		if(operationName == null)
			throw new InvalidOperationException("Could not determine operation name for " + servicePath);

		Map<String, String> params = httpRequest.getParameters();

		OMElement msg;

		EucalyptusMessage eucaMsg;
		Map<String, String> fieldMap;
		Class targetType;
		try
		{
			//:: try to create the target class :://
			targetType = ClassLoader.getSystemClassLoader().loadClass( "edu.ucsb.eucalyptus.msgs.".concat( operationName ).concat( "Type" ) );
			//:: get the map of parameters to fields :://
			fieldMap = this.buildFieldMap( targetType );
			//:: get an instance of the message :://
			eucaMsg =  (EucalyptusMessage) targetType.newInstance();
		}
		catch ( Exception e )
		{
			throw new BindingException( "Failed to construct message of type " + operationName );
		}

		addLogData(eucaMsg, bindingArguments);

		//TODO: Refactor this to be more general
		List<String> failedMappings = populateObject( eucaMsg, fieldMap, params);
		populateObjectFromBindingMap(eucaMsg, fieldMap, httpRequest, bindingArguments);

		User user = Contexts.lookup( httpRequest.getCorrelationId( ) ).getUser();
		setRequiredParams (eucaMsg, user);

		if ( !failedMappings.isEmpty() || !params.isEmpty() )
		{
			StringBuilder errMsg = new StringBuilder( "Failed to bind the following fields:\n" );
			for ( String f : failedMappings )
				errMsg.append( f ).append( '\n' );
			for ( Map.Entry<String, String> f : params.entrySet() )
				errMsg.append( f.getKey() ).append( " = " ).append( f.getValue() ).append( '\n' );
			throw new BindingException( errMsg.toString() );
		}

		LOG.info(eucaMsg.toString());
		try
		{
			Binding binding = BindingManager.getBinding( BindingManager.sanitizeNamespace( "http://msgs.eucalyptus.com" ) );
			msg = binding.toOM( eucaMsg );
		}
		catch ( RuntimeException e )
		{
			throw new BindingException( "Failed to build a valid message: " + e.getMessage() );
		}

		return eucaMsg;

	}

	private void addLogData(EucalyptusMessage eucaMsg,
			Map bindingArguments) {
		if(eucaMsg instanceof WalrusRequestType) {
			String operation = (String) bindingArguments.remove("Operation");
			if(operation != null) {
				WalrusRequestType request = (WalrusRequestType) eucaMsg;
				BucketLogData logData = WalrusBucketLogger.getInstance().makeLogEntry(UUID.randomUUID().toString());
				logData.setOperation("REST." + operation);
				request.setLogData(logData);
			}
		}
	}

	private void setRequiredParams(final GroovyObject msg, User user) {
		if(user != null) {
			msg.setProperty("accessKeyID", user.getQueryId());
		}
		msg.setProperty("timeStamp", new Date());
	}

	private String getOperation(MappingHttpRequest httpRequest, Map operationParams) throws BindingException {
		String[] target = null;
		String path = getOperationPath(httpRequest);
		boolean walrusInternalOperation = false;

		String targetHost = httpRequest.getHeader(HttpHeaders.Names.HOST);
		if(targetHost.contains(".walrus")) {
			String bucket = targetHost.substring(0, targetHost.indexOf(".walrus"));
			path = "/" + bucket + path;
		}

		if(path.length() > 0) {
			target = WalrusUtil.getTarget(path);
		}

		String verb = httpRequest.getMethod().getName();
		String operationKey = "";
		Map<String, String> params = httpRequest.getParameters();
		String operationName = null;
		long contentLength = 0;
		String contentLengthString = httpRequest.getHeader(WalrusProperties.CONTENT_LEN);
		if(contentLengthString != null) {
			contentLength = Long.parseLong(contentLengthString);
		}
		if(httpRequest.containsHeader(StorageProperties.EUCALYPTUS_OPERATION)) {
			String value = httpRequest.getHeader(StorageProperties.EUCALYPTUS_OPERATION);
			for(WalrusProperties.WalrusInternalOperations operation: WalrusProperties.WalrusInternalOperations.values()) {
				if(value.toLowerCase().equals(operation.toString().toLowerCase())) {
					operationName = operation.toString();
					walrusInternalOperation = true;
					break;
				}
			}

			if(!walrusInternalOperation) {
				for(WalrusProperties.StorageOperations operation: WalrusProperties.StorageOperations.values()) {
					if(value.toLowerCase().equals(operation.toString().toLowerCase())) {
						operationName = operation.toString();
						walrusInternalOperation = true;
						if(httpRequest.containsHeader(StorageProperties.StorageParameters.EucaSnapSize.toString())) {
							operationParams.put("SnapshotSize", httpRequest.getAndRemoveHeader(StorageProperties.StorageParameters.EucaSnapSize.toString()));
						}
						break;
					}
				}
			}

		}

		if(target == null) {
			//target = service
			operationKey = SERVICE + verb;
		} else if(target.length < 2) {
			//target = bucket
			if(!target[0].equals("")) {
				operationKey = BUCKET + verb;
				operationParams.put("Bucket", target[0]);
				operationParams.put("Operation", verb.toUpperCase() + "." + "BUCKET");
				if(verb.equals(WalrusProperties.HTTPVerb.POST.toString())) {
					//TODO: handle POST.
					Map formFields = httpRequest.getFormFields();

					String objectKey = null;
					String file = (String) formFields.get(WalrusProperties.FormField.file.toString());
					String authenticationHeader = "";
					if(formFields.containsKey(WalrusProperties.FormField.key.toString())) {
						objectKey = (String) formFields.get(WalrusProperties.FormField.key.toString());
						objectKey = objectKey.replaceAll("\\$\\{filename\\}", file);
						operationParams.put("Key", objectKey);
					}
					if(formFields.containsKey(WalrusProperties.FormField.acl.toString())) {
						String acl = (String) formFields.get(WalrusProperties.FormField.acl.toString());
						httpRequest.addHeader(WalrusProperties.AMZ_ACL, acl);
					}
					if(formFields.containsKey(WalrusProperties.FormField.redirect.toString())) {
						String successActionRedirect = (String) formFields.get(WalrusProperties.FormField.redirect.toString());
						operationParams.put("SuccessActionRedirect", successActionRedirect);
					}
					if(formFields.containsKey(WalrusProperties.FormField.success_action_redirect.toString())) {
						String successActionRedirect = (String) formFields.get(WalrusProperties.FormField.success_action_redirect.toString());
						operationParams.put("SuccessActionRedirect", successActionRedirect);
					}
					if(formFields.containsKey(WalrusProperties.FormField.success_action_status.toString())) {
						Integer successActionStatus = Integer.parseInt((String)formFields.get(WalrusProperties.FormField.success_action_status.toString()));
						if(successActionStatus == 200 || successActionStatus == 201)
							operationParams.put("SuccessActionStatus", successActionStatus);
						else
							operationParams.put("SuccessActionStatus", 204);
					} else {
						operationParams.put("SuccessActionStatus", 204);
					}
					if(formFields.containsKey(WalrusProperties.CONTENT_TYPE)) {
						operationParams.put("ContentType", formFields.get(WalrusProperties.CONTENT_TYPE));
					}
					key = target[0] + "." + objectKey;
					randomKey = key + "." + Hashes.getRandom(10);
					if(contentLengthString != null)
						operationParams.put("ContentLength", (new Long(contentLength).toString()));
					operationParams.put(WalrusProperties.Headers.RandomKey.toString(), randomKey);
					putQueue = getWriteMessenger().interruptAllAndGetQueue(key, randomKey);
					handleFirstChunk(httpRequest, (ChannelBuffer)formFields.get(WalrusProperties.IGNORE_PREFIX + "FirstDataChunk"), contentLength);
				} else if(WalrusProperties.HTTPVerb.PUT.toString().equals(verb)) {  
					if(params.containsKey(WalrusProperties.OperationParameter.logging.toString())) {
						//read logging params
						getTargetBucketParams(operationParams, httpRequest);
					} else if(params.containsKey(WalrusProperties.OperationParameter.versioning.toString())) {
						getVersioningStatus(operationParams, httpRequest);
					}
				}
			} else {
				operationKey = SERVICE + verb;
			}
		} else {
			//target = object
			operationKey = OBJECT + verb;
			String objectKey="";
			String splitOn = "";
			for(int i = 1; i < target.length; ++i) {
				objectKey += splitOn + target[i];
				splitOn = "/";
			}
			try {
				objectKey = WalrusUtil.URLdecode(objectKey);
			} catch (UnsupportedEncodingException e) {
				throw new BindingException("Unable to get key: " + e.getMessage());
			}
			operationParams.put("Bucket", target[0]);
			operationParams.put("Key", objectKey);
			operationParams.put("Operation", verb.toUpperCase() + "." + "OBJECT");

			if(!params.containsKey(WalrusProperties.OperationParameter.acl.toString())) {
				if (verb.equals(WalrusProperties.HTTPVerb.PUT.toString())) {
					if(httpRequest.containsHeader(WalrusProperties.COPY_SOURCE.toString())) {
						String copySource = httpRequest.getHeader(WalrusProperties.COPY_SOURCE.toString());
						String[] sourceParts = copySource.split("\\?");
						if(sourceParts.length > 1) {
							operationParams.put("SourceVersionId", sourceParts[1].replaceFirst("versionId=", "").trim());
						}
						copySource = sourceParts[0];
						String[] sourceTarget = WalrusUtil.getTarget(copySource);
						String sourceObjectKey = "";
						String sourceSplitOn = "";
						if(sourceTarget.length > 1) {
							for(int i = 1; i < sourceTarget.length; ++i) {
								sourceObjectKey += sourceSplitOn + sourceTarget[i];
								sourceSplitOn = "/";
							}
							try {
								sourceObjectKey = WalrusUtil.URLdecode(sourceObjectKey);
							} catch (UnsupportedEncodingException e) {
								throw new BindingException("Unable to get source key: " + e.getMessage());
							}
							operationParams.put("SourceBucket", sourceTarget[0]);
							operationParams.put("SourceObject", sourceObjectKey);
							operationParams.put("DestinationBucket", operationParams.remove("Bucket"));
							operationParams.put("DestinationObject", operationParams.remove("Key"));

							String metaDataDirective = httpRequest.getHeader(WalrusProperties.METADATA_DIRECTIVE.toString());
							if(metaDataDirective != null) {
								operationParams.put("MetadataDirective", metaDataDirective);
							}
							AccessControlListType accessControlList;
							if(contentLength > 0) {
								accessControlList = null;
								accessControlList = getAccessControlList(httpRequest);
							} else {
								accessControlList = new AccessControlListType();
							}
							operationParams.put("AccessControlList", accessControlList);
							operationKey += WalrusProperties.COPY_SOURCE.toString();
							Set<String> headerNames = httpRequest.getHeaderNames();
							for(String key : headerNames) {
								for(WalrusProperties.CopyHeaders header: WalrusProperties.CopyHeaders.values()) {
									if(key.replaceAll("-", "").equals(header.toString().toLowerCase())) {
										String value = httpRequest.getHeader(key);
										parseExtendedHeaders(operationParams, header.toString(), value);
									}
								}
							}
						} else {
							throw new BindingException("Malformed COPY request");
						}

					} else {
						//handle PUTs
						key = target[0] + "." + objectKey;
						randomKey = key + "." + Hashes.getRandom(10);
						String contentType = httpRequest.getHeader(WalrusProperties.CONTENT_TYPE);
						if(contentType != null)
							operationParams.put("ContentType", contentType);
						String contentDisposition = httpRequest.getHeader("Content-Disposition");
						if(contentDisposition != null)
							operationParams.put("ContentDisposition", contentDisposition);
						if(contentLengthString != null)
							operationParams.put("ContentLength", (new Long(contentLength).toString()));
						operationParams.put(WalrusProperties.Headers.RandomKey.toString(), randomKey);
						putQueue = getWriteMessenger().interruptAllAndGetQueue(key, randomKey);
						handleFirstChunk(httpRequest, contentLength);
					}
				} else if(verb.equals(WalrusProperties.HTTPVerb.GET.toString())) {
					if(!walrusInternalOperation) {

						if(params.containsKey("torrent")) {
							operationParams.put("GetTorrent", Boolean.TRUE);
						} else {
							operationParams.put("GetData", Boolean.TRUE);
							operationParams.put("InlineData", Boolean.FALSE);
							operationParams.put("GetMetaData", Boolean.TRUE);
						}

						Set<String> headerNames = httpRequest.getHeaderNames();
						boolean isExtendedGet = false;
						for(String key : headerNames) {
							for(WalrusProperties.ExtendedGetHeaders header: WalrusProperties.ExtendedGetHeaders.values()) {
								if(key.replaceAll("-", "").equals(header.toString())) {
									String value = httpRequest.getHeader(key);
									isExtendedGet = true;
									parseExtendedHeaders(operationParams, header.toString(), value);
								}
							}

						}
						if(isExtendedGet) {
							operationKey += "extended";
							//only supported through SOAP
							operationParams.put("ReturnCompleteObjectOnConditionFailure", Boolean.FALSE);
						}
					} 
					if(params.containsKey(WalrusProperties.GetOptionalParameters.IsCompressed.toString())) {
						Boolean isCompressed = Boolean.parseBoolean(params.remove(WalrusProperties.GetOptionalParameters.IsCompressed.toString()));
						operationParams.put("IsCompressed", isCompressed);
					}

				} else if(verb.equals(WalrusProperties.HTTPVerb.HEAD.toString())) {
					if(!walrusInternalOperation) {
						operationParams.put("GetData", Boolean.FALSE);
						operationParams.put("InlineData", Boolean.FALSE);
						operationParams.put("GetMetaData", Boolean.TRUE);
					}
				}
			}
			if(params.containsKey(WalrusProperties.OperationParameter.versionId.toString())) {
				if(!verb.equals(WalrusProperties.HTTPVerb.DELETE.toString()))
					operationParams.put("VersionId", params.remove(WalrusProperties.OperationParameter.versionId.toString()));
			}
		}


		if (verb.equals(WalrusProperties.HTTPVerb.PUT.toString()) && params.containsKey(WalrusProperties.OperationParameter.acl.toString())) {
			operationParams.put("AccessControlPolicy", getAccessControlPolicy(httpRequest));
		}

		ArrayList paramsToRemove = new ArrayList();

		boolean addMore = true;
		Iterator iterator = params.keySet().iterator();
		while(iterator.hasNext()) {
			Object key = iterator.next();
			String keyString = key.toString().toLowerCase();
			boolean dontIncludeParam = false;
			for(WalrusAuthenticationHandler.SecurityParameter securityParam : WalrusAuthenticationHandler.SecurityParameter.values()) {
				if(keyString.equals(securityParam.toString().toLowerCase())) {
					dontIncludeParam = true;
					break;
				}
			}
			if(dontIncludeParam)
				continue;
			String value = params.get(key);
			if(value != null) {
				String[] keyStringParts = keyString.split("-");
				if(keyStringParts.length > 1) {
					keyString = "";
					for(int i=0; i < keyStringParts.length; ++i) {
						keyString += toUpperFirst(keyStringParts[i]);
					}
				} else {
					keyString = toUpperFirst(keyString);
				}
				operationParams.put(keyString, value);
			}
			if(addMore) {
				//just add the first one to the key
				operationKey += keyString.toLowerCase();
				addMore = false;
			}
			paramsToRemove.add(key);
		}

		for(Object key : paramsToRemove) {
			params.remove(key);
		}

		if(!walrusInternalOperation) {
			operationName = operationMap.get(operationKey);
		}

		if("CreateBucket".equals(operationName)) {
			String locationConstraint = getLocationConstraint(httpRequest);
			if(locationConstraint != null)
				operationParams.put("LocationConstraint", locationConstraint);
		}
		return operationName;	
	}

	private void getTargetBucketParams(Map operationParams,
			MappingHttpRequest httpRequest) throws BindingException {
		String message = getMessageString(httpRequest);
		if(message.length() > 0) {
			try {
				XMLParser xmlParser = new XMLParser(message);
				String targetBucket = xmlParser.getValue("//TargetBucket");
				if(targetBucket == null || targetBucket.length() == 0) 
					return;
				String targetPrefix = xmlParser.getValue("//TargetPrefix");
				ArrayList<Grant> grants = new ArrayList<Grant>();

				List<String> permissions = xmlParser.getValues("//TargetGrants/Grant/Permission");
				if(permissions == null)
					throw new BindingException("malformed access control list");

				DTMNodeList grantees = xmlParser.getNodes("//TargetGrants/Grant/Grantee");
				if(grantees == null)
					throw new BindingException("malformed access control list");

				for(int i = 0 ; i < grantees.getLength() ; ++i) {
					String id = xmlParser.getValue(grantees.item(i), "ID");
					if(id.length() > 0) {
						String canonicalUserName = xmlParser.getValue(grantees.item(i), "DisplayName");
						Grant grant = new Grant();
						Grantee grantee = new Grantee();
						grantee.setCanonicalUser(new CanonicalUserType(id, canonicalUserName));
						grant.setGrantee(grantee);
						grant.setPermission(permissions.get(i));
						grants.add(grant);
					} else {
						String groupUri = xmlParser.getValue(grantees.item(i), "URI");
						if(groupUri.length() == 0)
							throw new BindingException("malformed access control list");
						Grant grant = new Grant();
						Grantee grantee = new Grantee();
						grantee.setGroup(new Group(groupUri));
						grant.setGrantee(grantee);
						grant.setPermission(permissions.get(i));
						grants.add(grant);
					}
				}
				TargetGrants targetGrants = new TargetGrants(grants);
				LoggingEnabled loggingEnabled = new LoggingEnabled(targetBucket, targetPrefix, new TargetGrants(grants));
				operationParams.put("LoggingEnabled", loggingEnabled);
			} catch(Exception ex) {
				LOG.warn(ex);
				throw new BindingException("Unable to parse logging configuration " + ex.getMessage());
			}
		}
	}

	private void getVersioningStatus(Map operationParams,
			MappingHttpRequest httpRequest) throws BindingException {
		String message = getMessageString(httpRequest);
		if(message.length() > 0) {
			try {
				XMLParser xmlParser = new XMLParser(message);
				String status = xmlParser.getValue("//Status");
				if(status == null || status.length() == 0) 
					return;
				operationParams.put("VersioningStatus", status);
			} catch(Exception ex) {
				LOG.warn(ex);
				throw new BindingException("Unable to parse versioning status " + ex.getMessage());
			}
		}
	}

	private void parseExtendedHeaders(Map operationParams, String headerString, String value) throws BindingException {
		if(headerString.equals(WalrusProperties.ExtendedGetHeaders.Range.toString())) {
			String prefix = "bytes=";
			assert(value.startsWith(prefix));
			value = value.substring(prefix.length());
			String[]values = value.split("-");
			assert(values.length == 2);
			if(values[0].equals("")) {
				operationParams.put(WalrusProperties.ExtendedHeaderRangeTypes.ByteRangeStart.toString(), new Long(0));
			} else {
				operationParams.put(WalrusProperties.ExtendedHeaderRangeTypes.ByteRangeStart.toString(), Long.parseLong(values[0]));
			}
			assert(!values[1].equals(""));
			operationParams.put(WalrusProperties.ExtendedHeaderRangeTypes.ByteRangeEnd.toString(), Long.parseLong(values[1]));
		} else if(WalrusProperties.ExtendedHeaderDateTypes.contains(headerString)) {
			try {
				List<String> dateFormats = new ArrayList<String>();
				dateFormats.add(DateUtil.PATTERN_RFC1123);
				operationParams.put(headerString, DateUtil.parseDate(value, dateFormats));
			} catch(Exception ex) {
				try {
					operationParams.put(headerString, DateUtils.parseIso8601DateTime(value));
				} catch (ParseException e) {
					LOG.error(e);
					throw new BindingException(e);
				}
			}
		} else {
			operationParams.put(headerString, value);
		}
	}

	private AccessControlPolicyType getAccessControlPolicy(MappingHttpRequest httpRequest) throws BindingException {
		AccessControlPolicyType accessControlPolicy = new AccessControlPolicyType();
		AccessControlListType accessControlList = new AccessControlListType();
		ArrayList<Grant> grants = new ArrayList<Grant>();
		try {
			String aclString = getMessageString(httpRequest);
			if(aclString.length() > 0) {
				XMLParser xmlParser = new XMLParser(aclString);
				String ownerId = xmlParser.getValue("//Owner/ID");
				String displayName = xmlParser.getValue("//Owner/DisplayName");

				CanonicalUserType canonicalUser = new CanonicalUserType(ownerId, displayName);
				accessControlPolicy.setOwner(canonicalUser);

				List<String> permissions = xmlParser.getValues("//AccessControlList/Grant/Permission");
				if(permissions == null)
					throw new BindingException("malformed access control list");

				DTMNodeList grantees = xmlParser.getNodes("//AccessControlList/Grant/Grantee");
				if(grantees == null)
					throw new BindingException("malformed access control list");

				for(int i = 0 ; i < grantees.getLength() ; ++i) {
					String id = xmlParser.getValue(grantees.item(i), "ID");
					if(id.length() > 0) {
						String canonicalUserName = xmlParser.getValue(grantees.item(i), "DisplayName");
						Grant grant = new Grant();
						Grantee grantee = new Grantee();
						grantee.setCanonicalUser(new CanonicalUserType(id, canonicalUserName));
						grant.setGrantee(grantee);
						grant.setPermission(permissions.get(i));
						grants.add(grant);
					} else {
						String groupUri = xmlParser.getValue(grantees.item(i), "URI");
						if(groupUri.length() == 0)
							throw new BindingException("malformed access control list");
						Grant grant = new Grant();
						Grantee grantee = new Grantee();
						grantee.setGroup(new Group(groupUri));
						grant.setGrantee(grantee);
						grant.setPermission(permissions.get(i));
						grants.add(grant);
					}
				}
			}
		} catch(Exception ex) {
			LOG.warn(ex);
			throw new BindingException("Unable to parse access control policy " + ex.getMessage());
		}
		accessControlList.setGrants(grants);
		accessControlPolicy.setAccessControlList(accessControlList);
		return accessControlPolicy;
	}


	private AccessControlListType getAccessControlList(MappingHttpRequest httpRequest) throws BindingException {
		AccessControlListType accessControlList = new AccessControlListType();
		ArrayList<Grant> grants = new ArrayList<Grant>();
		try {
			String aclString = getMessageString(httpRequest);
			if(aclString.length() > 0) {
				XMLParser xmlParser = new XMLParser(aclString);
				String ownerId = xmlParser.getValue("//Owner/ID");
				String displayName = xmlParser.getValue("//Owner/DisplayName");

				List<String> permissions = xmlParser.getValues("/AccessControlList/Grant/Permission");
				if(permissions == null)
					throw new BindingException("malformed access control list");

				DTMNodeList grantees = xmlParser.getNodes("/AccessControlList/Grant/Grantee");
				if(grantees == null)
					throw new BindingException("malformed access control list");


				for(int i = 0 ; i < grantees.getLength() ; ++i) {
					String canonicalUserName = xmlParser.getValue(grantees.item(i), "DisplayName");
					if(canonicalUserName.length() > 0) {
						String id = xmlParser.getValue(grantees.item(i), "ID");
						Grant grant = new Grant();
						Grantee grantee = new Grantee();
						grantee.setCanonicalUser(new CanonicalUserType(id, canonicalUserName));
						grant.setGrantee(grantee);
						grant.setPermission(permissions.get(i));
						grants.add(grant);
					} else {
						String groupUri = xmlParser.getValue(grantees.item(i), "URI");
						if(groupUri.length() == 0)
							throw new BindingException("malformed access control list");
						Grant grant = new Grant();
						Grantee grantee = new Grantee();
						grantee.setGroup(new Group(groupUri));
						grant.setGrantee(grantee);
						grant.setPermission(permissions.get(i));
						grants.add(grant);
					}
				}
			}
		} catch(Exception ex) {
			LOG.warn(ex);
			throw new BindingException("Unable to parse access control list " + ex.getMessage());
		}
		accessControlList.setGrants(grants);
		return accessControlList;
	}

	private String getOperationPath(MappingHttpRequest httpRequest) {
		return httpRequest.getServicePath().replaceAll(WalrusProperties.walrusServicePath, "");
	}

	private String getLocationConstraint(MappingHttpRequest httpRequest) throws BindingException {
		String locationConstraint = null;
		try {
			String bucketConfiguration = getMessageString(httpRequest);
			if(bucketConfiguration.length() > 0) {
				XMLParser xmlParser = new XMLParser(bucketConfiguration);
				locationConstraint = xmlParser.getValue("/CreateBucketConfiguration/LocationConstraint");
			}
		} catch(Exception ex) {
			LOG.warn(ex);
			throw new BindingException(ex.getMessage());
		}
		return locationConstraint;
	}

	private List<String> populateObject( final GroovyObject obj, final Map<String, String> paramFieldMap, final Map<String, String> params ) {
		List<String> failedMappings = new ArrayList<String>( );
		for ( Map.Entry<String, String> e : paramFieldMap.entrySet( ) ) {
			try {
				if ( obj.getClass( ).getDeclaredField( e.getValue( ) ).getType( ).equals( ArrayList.class ) ) failedMappings.addAll( populateObjectList( obj, e, params, params.size( ) ) );
			} catch ( NoSuchFieldException e1 ) {
				failedMappings.add( e.getKey( ) );
			}
		}
		for ( Map.Entry<String, String> e : paramFieldMap.entrySet( ) ) {
			if ( params.containsKey( e.getKey( ) ) && !populateObjectField( obj, e, params ) ) failedMappings.add( e.getKey( ) );
		}
		return failedMappings;
	}

	private void populateObjectFromBindingMap( final GroovyObject obj, final Map<String, String> paramFieldMap, final MappingHttpRequest httpRequest, final Map bindingMap)
	{
		//process headers
		String aclString = httpRequest.getAndRemoveHeader(WalrusProperties.AMZ_ACL);
		if (aclString != null) {
			addAccessControlList(obj, paramFieldMap, bindingMap, aclString);
		}

		//add meta data
		String metaDataString = paramFieldMap.remove("MetaData");
		if(metaDataString != null) {
			Set<String> headerNames = httpRequest.getHeaderNames();
			ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
			for(String key : headerNames) {
				if(key.toLowerCase().startsWith(WalrusProperties.AMZ_META_HEADER_PREFIX)) {
					MetaDataEntry metaDataEntry = new MetaDataEntry();
					metaDataEntry.setName(key.substring(WalrusProperties.AMZ_META_HEADER_PREFIX.length()));
					metaDataEntry.setValue(httpRequest.getHeader(key));
					metaData.add(metaDataEntry);
				}
			}
			obj.setProperty(metaDataString, metaData);
		}

		//populate from binding map (required params)
		Iterator bindingMapIterator = bindingMap.keySet().iterator();
		while(bindingMapIterator.hasNext()) {
			String key = (String) bindingMapIterator.next();
			obj.setProperty(key.substring(0, 1).toLowerCase().concat(key.substring(1)), bindingMap.get(key));
		}
	}

	private boolean populateObjectField( final GroovyObject obj, final Map.Entry<String, String> paramFieldPair, final Map<String, String> params ) {
		try {
			Class declaredType = obj.getClass( ).getDeclaredField( paramFieldPair.getValue( ) ).getType( );
			if ( declaredType.equals( String.class ) ) obj.setProperty( paramFieldPair.getValue( ), params.remove( paramFieldPair.getKey( ) ) );
			else if ( declaredType.getName( ).equals( "int" ) ) obj.setProperty( paramFieldPair.getValue( ), Integer.parseInt( params.remove( paramFieldPair.getKey( ) ) ) );
			else if ( declaredType.equals( Integer.class ) ) obj.setProperty( paramFieldPair.getValue( ), new Integer( params.remove( paramFieldPair.getKey( ) ) ) );
			else if ( declaredType.getName( ).equals( "boolean" ) ) obj.setProperty( paramFieldPair.getValue( ), Boolean.parseBoolean( params.remove( paramFieldPair.getKey( ) ) ) );
			else if ( declaredType.equals( Boolean.class ) ) obj.setProperty( paramFieldPair.getValue( ), new Boolean( params.remove( paramFieldPair.getKey( ) ) ) );
			else if ( declaredType.getName( ).equals( "long" ) ) obj.setProperty( paramFieldPair.getValue( ), Long.parseLong( params.remove( paramFieldPair.getKey( ) ) ) );
			else if ( declaredType.equals( Long.class ) ) obj.setProperty( paramFieldPair.getValue( ), new Long( params.remove( paramFieldPair.getKey( ) ) ) );
			else return false;
			return true;
		} catch ( Exception e1 ) {
			return false;
		}
	}

	private List<String> populateObjectList( final GroovyObject obj, final Map.Entry<String, String> paramFieldPair, final Map<String, String> params, final int paramSize ) {
		List<String> failedMappings = new ArrayList<String>( );
		try {
			Field declaredField = obj.getClass( ).getDeclaredField( paramFieldPair.getValue( ) );
			ArrayList theList = ( ArrayList ) obj.getProperty( paramFieldPair.getValue( ) );
			Class genericType = ( Class ) ( ( ParameterizedType ) declaredField.getGenericType( ) ).getActualTypeArguments( )[0];
			// :: simple case: FieldName.# :://
			if ( String.class.equals( genericType ) ) {
				if ( params.containsKey( paramFieldPair.getKey( ) ) ) {
					theList.add( params.remove( paramFieldPair.getKey( ) ) );
				} else {
					List<String> keys = Lists.newArrayList( params.keySet( ) );
					for ( String k : keys ) {
						if ( k.matches( paramFieldPair.getKey( ) + "\\.\\d*" ) ) {
							theList.add( params.remove( k ) );
						}
					}
				}
			} else if ( declaredField.isAnnotationPresent( HttpEmbedded.class ) ) {
				HttpEmbedded annoteEmbedded = ( HttpEmbedded ) declaredField.getAnnotation( HttpEmbedded.class );
				// :: build the parameter map and call populate object recursively :://
				if ( annoteEmbedded.multiple( ) ) {
					String prefix = paramFieldPair.getKey( );
					List<String> embeddedListFieldNames = new ArrayList<String>( );
					for ( String actualParameterName : params.keySet( ) )
						if ( actualParameterName.matches( prefix + ".1.*" ) ) embeddedListFieldNames.add( actualParameterName.replaceAll( prefix + ".1.", "" ) );
					for ( int i = 0; i < paramSize + 1; i++ ) {
						boolean foundAll = true;
						Map<String, String> embeddedParams = new HashMap<String, String>( );
						for ( String fieldName : embeddedListFieldNames ) {
							String paramName = prefix + "." + i + "." + fieldName;
							if ( !params.containsKey( paramName ) ) {
								failedMappings.add( "Mismatched embedded field: " + paramName );
								foundAll = false;
							} else embeddedParams.put( fieldName, params.get( paramName ) );
						}
						if ( foundAll ) failedMappings.addAll( populateEmbedded( genericType, embeddedParams, theList ) );
						else break;
					}
				} else failedMappings.addAll( populateEmbedded( genericType, params, theList ) );
			}
		} catch ( Exception e1 ) {
			failedMappings.add( paramFieldPair.getKey( ) );
		}
		return failedMappings;
	}

	private List<String> populateEmbedded( final Class genericType, final Map<String, String> params, final ArrayList theList ) throws InstantiationException, IllegalAccessException {
		GroovyObject embedded = ( GroovyObject ) genericType.newInstance( );
		Map<String, String> embeddedFields = buildFieldMap( genericType );
		int startSize = params.size( );
		List<String> embeddedFailures = populateObject( embedded, embeddedFields, params );
		if ( embeddedFailures.isEmpty( ) && !( params.size( ) - startSize == 0 ) ) theList.add( embedded );
		return embeddedFailures;
	}

	private Map<String, String> buildFieldMap( final Class targetType ) {
		Map<String, String> fieldMap = new HashMap<String, String>( );
		Field[] fields = targetType.getDeclaredFields( );
		for ( Field f : fields )
			if ( Modifier.isStatic( f.getModifiers( ) ) ) continue;
			else if ( f.isAnnotationPresent( HttpParameterMapping.class ) ) {
				fieldMap.put( f.getAnnotation( HttpParameterMapping.class ).parameter( ), f.getName( ) );
				fieldMap.put( f.getName( ).substring( 0, 1 ).toUpperCase( ).concat( f.getName( ).substring( 1 ) ), f.getName( ) );
			} else fieldMap.put( f.getName( ).substring( 0, 1 ).toUpperCase( ).concat( f.getName( ).substring( 1 ) ), f.getName( ) );
		return fieldMap;
	}

	private static void addAccessControlList (final GroovyObject obj, final Map<String, String> paramFieldMap, Map bindingMap, String cannedACLString) {

		AccessControlListType accessControlList;
		ArrayList<Grant> grants;

		if(bindingMap.containsKey("AccessControlPolicy")) {
			AccessControlPolicyType accessControlPolicy = (AccessControlPolicyType) bindingMap.get("AccessControlPolicy");
			accessControlList = accessControlPolicy.getAccessControlList();
			grants = accessControlList.getGrants();
		} else {
			accessControlList = new AccessControlListType();
			grants = new ArrayList<Grant>();
		}

		CanonicalUserType aws = new CanonicalUserType();
		aws.setDisplayName("");
		Grant grant = new Grant(new Grantee(aws), cannedACLString);
		grants.add(grant);

		accessControlList.setGrants(grants);
		//set obj property
		String acl = paramFieldMap.remove("AccessControlList");
		if(acl != null) {
			obj.setProperty(acl, accessControlList );
		}
	}

	private String toUpperFirst(String string) {
		return string.substring(0, 1).toUpperCase().concat(string.substring(1));
	}

	private boolean exactMatch(JSONObject jsonObject, Map formFields, List<String> policyItemNames) {
		Iterator<String> iterator = jsonObject.keys();
		boolean returnValue = false;
		while(iterator.hasNext()) {
			String key = iterator.next();
			key = key.replaceAll("\\$", "");
			policyItemNames.add(key);
			try {
				if(jsonObject.get(key).equals(formFields.get(key)))
					returnValue = true;
				else
					returnValue = false;
			} catch(Exception ex) {
				ex.printStackTrace();
				return false;
			}
		}
		return returnValue;
	}

	private boolean partialMatch(JSONArray jsonArray, Map<String, String> formFields, List<String> policyItemNames) {
		boolean returnValue = false;
		if(jsonArray.size() != 3)
			return false;
		try {
			String condition = (String) jsonArray.get(0);
			String key = (String) jsonArray.get(1);
			key = key.replaceAll("\\$", "");
			policyItemNames.add(key);
			String value = (String) jsonArray.get(2);
			if(condition.contains("eq")) {
				if(value.equals(formFields.get(key)))
					returnValue = true;
			} else if(condition.contains("starts-with")) {
				if(!formFields.containsKey(key))
					return false;
				if(formFields.get(key).startsWith(value))
					returnValue = true;
			}
		} catch(Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return returnValue;
	}

	private String getMessageString(MappingHttpRequest httpRequest) {
		ChannelBuffer buffer = httpRequest.getContent( );
		buffer.markReaderIndex( );
		byte[] read = new byte[buffer.readableBytes( )];
		buffer.readBytes( read );
		return new String( read );
	}

	private void insertChunk(WalrusDataMessage message) throws InterruptedException {
		if(putQueue != null) {
			boolean success = false;
			int retry_count = 0;
			do {
				success = putQueue.offer(message, 500, TimeUnit.MILLISECONDS);
				if(++retry_count > PUT_RETRY_COUNT) {
					LOG.error("Tired of waiting for PUT. Giving up.");
					putQueue = null;
				}
			} while((putQueue != null) && (!success));
		}
	}

	private void handleHttpChunk(HttpChunk httpChunk) throws Exception {
		ChannelBuffer buffer = httpChunk.getContent();
		try {
			buffer.markReaderIndex( );
			byte[] read = new byte[buffer.readableBytes( )];
			buffer.readBytes( read );
			insertChunk(WalrusDataMessage.DataMessage(read));
			if(httpChunk.isLast()) {
				insertChunk(WalrusDataMessage.EOF());				
			}
		} catch (Exception ex) {
			LOG.error(ex, ex);
		}

	}

	private void handleFirstChunk(MappingHttpRequest httpRequest, long dataLength) {
		ChannelBuffer buffer = httpRequest.getContent();
		try {
			putQueue.put(WalrusDataMessage.StartOfData(dataLength));
			buffer.markReaderIndex( );
			byte[] read = new byte[buffer.readableBytes( )];
			buffer.readBytes( read );
			putQueue.put(WalrusDataMessage.DataMessage(read));
			if(!httpRequest.isChunked())
				putQueue.put(WalrusDataMessage.EOF());
		} catch (Exception ex) {
			LOG.error(ex, ex);
		}

	}

	private void handleFirstChunk(MappingHttpRequest httpRequest, ChannelBuffer firstChunk, long dataLength) {
		try {
			putQueue.put(WalrusDataMessage.StartOfData(dataLength));
			byte[] read = new byte[firstChunk.readableBytes( )];
			firstChunk.readBytes( read );
			putQueue.put(WalrusDataMessage.DataMessage(read));
			if(!httpRequest.isChunked())
				putQueue.put(WalrusDataMessage.EOF());
		} catch (Exception ex) {
			LOG.error(ex, ex);
		}

	}


	public static synchronized WalrusDataMessenger getWriteMessenger() {
		if (putMessenger == null) {
			putMessenger = new WalrusDataMessenger();
		}
		return putMessenger;
	}	

}
