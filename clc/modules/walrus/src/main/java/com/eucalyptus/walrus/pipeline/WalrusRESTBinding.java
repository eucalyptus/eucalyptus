/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.walrus.pipeline;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
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

import com.eucalyptus.auth.policy.key.Iso8601DateParser;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.walrus.WalrusBackend;
import com.eucalyptus.walrus.msgs.LifecycleConfigurationType;
import com.eucalyptus.walrus.msgs.LifecycleExpiration;
import com.eucalyptus.walrus.msgs.LifecycleRule;
import com.eucalyptus.walrus.msgs.LifecycleTransition;

import com.google.common.base.Strings;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.axiom.om.OMElement;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;
import org.apache.xml.dtm.ref.DTMNodeList;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
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

import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.XMLParser;
import com.eucalyptus.walrus.WalrusBucketLogger;
import com.eucalyptus.storage.msgs.BucketLogData;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.Grant;
import com.eucalyptus.storage.msgs.s3.Grantee;
import com.eucalyptus.storage.msgs.s3.Group;
import com.eucalyptus.storage.msgs.s3.LoggingEnabled;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;
import com.eucalyptus.storage.msgs.s3.TargetGrants;
import com.eucalyptus.walrus.exceptions.NotImplementedException;
import com.eucalyptus.walrus.msgs.WalrusDataGetRequestType;
import com.eucalyptus.walrus.msgs.WalrusDataMessage;
import com.eucalyptus.walrus.msgs.WalrusDataMessenger;
import com.eucalyptus.walrus.msgs.WalrusDataQueue;
import com.eucalyptus.walrus.msgs.WalrusDataRequestType;
import com.eucalyptus.walrus.msgs.WalrusRequestType;
import com.eucalyptus.walrus.util.WalrusProperties;
import com.eucalyptus.walrus.util.WalrusUtil;
import com.eucalyptus.ws.MethodNotAllowedException;
import com.eucalyptus.ws.handlers.RestfulMarshallingHandler;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;
import groovy.lang.GroovyObject;

import org.w3c.dom.Node;

import com.eucalyptus.storage.msgs.s3.Part;

public class WalrusRESTBinding extends RestfulMarshallingHandler {
	private static Logger LOG = Logger.getLogger( WalrusRESTBinding.class );
	private static final String SERVICE = "service";
	private static final String BUCKET = "bucket";
	private static final String OBJECT = "object";
	private static final Map<String, String> operationMap = populateOperationMap();
	private static final Map<String, String> unsupportedOperationMap = populateUnsupportedOperationMap();
	private static WalrusDataMessenger putMessenger;
	public static final int DATA_MESSAGE_SIZE = 102400;
	private String key;
	private String randomKey;
	private WalrusDataQueue<WalrusDataMessage> putQueue;
    private final String walrusServicePath;

	public WalrusRESTBinding( ) {
		super("http://walrus.s3.amazonaws.com/doc/" + WalrusProperties.NAMESPACE_VERSION);
        walrusServicePath = ComponentIds.lookup(WalrusBackend.class).getServicePath();
	}

	@Override
	public void handleUpstream( final ChannelHandlerContext channelHandlerContext, final ChannelEvent channelEvent ) throws Exception {
		LOG.trace( LogUtil.dumpObject( channelEvent ) );
		if ( channelEvent instanceof MessageEvent ) {
			final MessageEvent msgEvent = ( MessageEvent ) channelEvent;
			try {
				this.incomingMessage( channelHandlerContext, msgEvent );
			} catch ( Exception e ) {
				LOG.error( e, e );
				//throw e;
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
			// TODO: get real user data here too			
			//Auth is already done before binding, so binding here is just a validation. Then send 100-continue
			BaseMessage msg = (BaseMessage) this.bind( httpRequest );
			httpRequest.setMessage( msg );
			if(msg instanceof WalrusDataGetRequestType) {
				WalrusDataGetRequestType getObject = (WalrusDataGetRequestType) msg;
				getObject.setChannel(ctx.getChannel());
			}
			if(msg instanceof WalrusDataRequestType) {
				String expect = httpRequest.getHeader(HttpHeaders.Names.EXPECT);
				if(expect != null) {
					if(expect.toLowerCase().equals("100-continue")) {
						HttpResponse response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE );
						DownstreamMessageEvent newEvent = new DownstreamMessageEvent( ctx.getChannel( ), event.getFuture(), response, null );
						final Channel channel = ctx.getChannel();		
						if ( channel.isConnected( ) ) {
							ChannelFuture writeFuture = Channels.future( ctx.getChannel( ) );
							Channels.write(ctx, writeFuture, response);
						}
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
			BaseMessage msg = (BaseMessage) httpResponse.getMessage( );
			Binding binding;

			if(!(msg instanceof EucalyptusErrorMessageType)&&!(msg instanceof ExceptionResponseType)) {
				binding = BindingManager.getBinding( super.getNamespace( ) );
				if(putQueue != null) {
					putQueue = null;
				}
			} else {
				binding = BindingManager.getDefaultBinding( );
				if(putQueue != null) {
					putQueue = null;
				}
			}
			if(msg != null) {
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				binding.toStream( byteOut, msg );
				byte[] req = byteOut.toByteArray();
				ChannelBuffer buffer = ChannelBuffers.wrappedBuffer( req );
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
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.HEAD.toString(), "HeadBucket");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.BucketParameter.acl.toString(), "GetBucketAccessControlPolicy");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.BucketParameter.acl.toString(), "SetRESTBucketAccessControlPolicy");

		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString(), "ListBucket");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.BucketParameter.prefix.toString(), "ListBucket");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.BucketParameter.maxkeys.toString(), "ListBucket");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.BucketParameter.marker.toString(), "ListBucket");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.BucketParameter.delimiter.toString(), "ListBucket");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.PUT.toString(), "CreateBucket");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.DELETE.toString(), "DeleteBucket");

		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.BucketParameter.location.toString(), "GetBucketLocation");

		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.BucketParameter.logging.toString(), "GetBucketLoggingStatus");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.BucketParameter.logging.toString(), "SetBucketLoggingStatus");

		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.BucketParameter.versions.toString(), "ListVersions");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.BucketParameter.versioning.toString(), "GetBucketVersioningStatus");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.BucketParameter.versioning.toString(), "SetBucketVersioningStatus");

		newMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.BucketParameter.lifecycle.toString(), "GetLifecycle");
		newMap.put(BUCKET + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.BucketParameter.lifecycle.toString(), "PutLifecycle");

		//Object operations
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.ObjectParameter.acl.toString(), "GetObjectAccessControlPolicy");
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.ObjectParameter.acl.toString(), "SetRESTObjectAccessControlPolicy");

		newMap.put(BUCKET + WalrusProperties.HTTPVerb.POST.toString(), "PostObject");

		newMap.put(OBJECT + WalrusProperties.HTTPVerb.PUT.toString(), "PutObject");
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.COPY_SOURCE.toString(), "CopyObject");
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.GET.toString(), "GetObject");
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.ObjectParameter.torrent.toString(), "GetObject");
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.DELETE.toString(), "DeleteObject");

		newMap.put(OBJECT + WalrusProperties.HTTPVerb.HEAD.toString(), "GetObject");
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.GET.toString() + "extended", "GetObjectExtended");

		newMap.put(OBJECT + WalrusProperties.HTTPVerb.DELETE.toString() + WalrusProperties.ObjectParameter.versionId.toString().toLowerCase(), "DeleteVersion");

		// For multipart uploads
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.POST.toString() + WalrusProperties.ObjectParameter.uploads.toString(), "InitiateMultipartUpload");
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.ObjectParameter.partNumber.toString().toLowerCase() + WalrusProperties.ObjectParameter.uploadId.toString().toLowerCase(), "UploadPart");
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.ObjectParameter.uploadId.toString().toLowerCase() + WalrusProperties.ObjectParameter.partNumber.toString().toLowerCase(), "UploadPart");
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.POST.toString() + WalrusProperties.ObjectParameter.uploadId.toString().toLowerCase(), "CompleteMultipartUpload");
		newMap.put(OBJECT + WalrusProperties.HTTPVerb.DELETE.toString() + WalrusProperties.ObjectParameter.uploadId.toString().toLowerCase(), "AbortMultipartUpload");

		return newMap;
	}

	private static Map<String, String> populateUnsupportedOperationMap() {
		Map<String, String> opsMap = new HashMap<String, String>();

		// Bucket operations
		// Cross-Origin Resource Sharing (cors) 
		opsMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.BucketParameter.cors.toString(), "GET Bucket cors");
		opsMap.put(BUCKET + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.BucketParameter.cors.toString(), "PUT Bucket cors");
		opsMap.put(BUCKET + WalrusProperties.HTTPVerb.DELETE.toString() + WalrusProperties.BucketParameter.cors.toString(), "DELETE Bucket cors");

		// Lifecycle
		//opsMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.BucketParameter.lifecycle.toString(), "GET Bucket lifecycle");
		//opsMap.put(BUCKET + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.BucketParameter.lifecycle.toString(), "PUT Bucket lifecycle");
		opsMap.put(BUCKET + WalrusProperties.HTTPVerb.DELETE.toString() + WalrusProperties.BucketParameter.lifecycle.toString(),"DELETE Bucket lifecycle");
		// Policy
		opsMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.BucketParameter.policy.toString(), "GET Bucket policy");
		opsMap.put(BUCKET + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.BucketParameter.policy.toString(), "PUT Bucket policy");
		opsMap.put(BUCKET + WalrusProperties.HTTPVerb.DELETE.toString() + WalrusProperties.BucketParameter.policy.toString(), "DELETE Bucket policy");
		// Notification
		opsMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.BucketParameter.notification.toString(), "GET Bucket notification");
		opsMap.put(BUCKET + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.BucketParameter.notification.toString(), "PUT Bucket notification");
		// Tagging
		opsMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.BucketParameter.tagging.toString(), "GET Bucket tagging");
		opsMap.put(BUCKET + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.BucketParameter.tagging.toString(), "PUT Bucket tagging");
		opsMap.put(BUCKET + WalrusProperties.HTTPVerb.DELETE.toString() + WalrusProperties.BucketParameter.tagging.toString(), "DELETE Bucket tagging");
		// Request Payments // TODO HACK! binding code converts parameters to lower case. Fix that issue!
		opsMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.BucketParameter.requestPayment.toString().toLowerCase(), "GET Bucket requestPayment");
		// Website
		opsMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.BucketParameter.website.toString(), "GET Bucket website");
		opsMap.put(BUCKET + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.BucketParameter.website.toString(), "PUT Bucket website");
		opsMap.put(BUCKET + WalrusProperties.HTTPVerb.DELETE.toString() + WalrusProperties.BucketParameter.website.toString(), "DELETE Bucket website");
		// Multipart uploads
		opsMap.put(BUCKET + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.BucketParameter.uploads.toString(), "List Multipart Uploads");

		// Object operations
		// Multipart Uploads
		opsMap.put(OBJECT + WalrusProperties.HTTPVerb.GET.toString() + WalrusProperties.ObjectParameter.uploadId.toString().toLowerCase(), "List Parts");
		opsMap.put(OBJECT + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.ObjectParameter.uploadId.toString().toLowerCase() 
				+ WalrusProperties.ObjectParameter.partNumber.toString().toLowerCase(), "Upload Part");
		opsMap.put(OBJECT + WalrusProperties.HTTPVerb.PUT.toString() + WalrusProperties.ObjectParameter.partNumber.toString().toLowerCase() 
				+ WalrusProperties.ObjectParameter.uploadId.toString().toLowerCase(), "Upload Part");
		opsMap.put(OBJECT + WalrusProperties.HTTPVerb.POST.toString() + WalrusProperties.ObjectParameter.uploads.toString(), "Initiate Multipart Upload");
		opsMap.put(OBJECT + WalrusProperties.HTTPVerb.POST.toString() + WalrusProperties.ObjectParameter.uploadId.toString().toLowerCase(), "Complete Multipart Upload");
		opsMap.put(OBJECT + WalrusProperties.HTTPVerb.DELETE.toString() + WalrusProperties.ObjectParameter.uploadId.toString().toLowerCase(), "Abort Multipart Upload");

		return opsMap;
	}

	@Override
	public Object bind( final MappingHttpRequest httpRequest ) throws Exception {
		String servicePath = httpRequest.getServicePath();
		Map bindingArguments = new HashMap();
		final String operationName = getOperation(httpRequest, bindingArguments);
		if(operationName == null)
			throw new MethodNotAllowedException("Could not determine operation name for " + servicePath);

		Map<String, String> params = httpRequest.getParameters();

		OMElement msg;

		GroovyObject groovyMsg;
		Map<String, String> fieldMap;
		Class targetType;
		try
		{
			//:: try to create the target class :://
			targetType = ClassLoader.getSystemClassLoader().loadClass( "com.eucalyptus.walrus.msgs.".concat( operationName ).concat( "Type" ) );
			if( !GroovyObject.class.isAssignableFrom( targetType ) ) {
				throw new Exception( );
			}
			//:: get the map of parameters to fields :://
			fieldMap = this.buildFieldMap( targetType );
			//:: get an instance of the message :://
			groovyMsg =  (GroovyObject) targetType.newInstance();
		}
		catch ( Exception e )
		{
			throw new BindingException( "Failed to construct message of type " + operationName );
		}

		addLogData((BaseMessage)groovyMsg, bindingArguments);

		//TODO: Refactor this to be more general
		List<String> failedMappings = populateObject( groovyMsg, fieldMap, params);
		populateObjectFromBindingMap(groovyMsg, fieldMap, httpRequest, bindingArguments);

		final Context context = Contexts.lookup( httpRequest.getCorrelationId( ) );
		setRequiredParams (groovyMsg, context);

		if ( !params.isEmpty()) {
			//ignore params that are not consumed, EUCA-4840
			params.clear();
		}

		if ( !failedMappings.isEmpty() )
		{
			StringBuilder errMsg = new StringBuilder( "Failed to bind the following fields:\n" );
			for ( String f : failedMappings )
				errMsg.append( f ).append( '\n' );
			for ( Map.Entry<String, String> f : params.entrySet() )
				errMsg.append( f.getKey() ).append( " = " ).append( f.getValue() ).append( '\n' );
			throw new BindingException( errMsg.toString() );
		}

		LOG.debug(groovyMsg.toString());
		try
		{
			Binding binding = BindingManager.getDefaultBinding( );
			msg = binding.toOM( groovyMsg );
		}
		catch ( RuntimeException e )
		{
			throw new BindingException( "Failed to build a valid message: " + e.getMessage() );
		}

		return groovyMsg;

	}

	private void addLogData(BaseMessage eucaMsg,
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

	private void setRequiredParams(final GroovyObject msg, Context context) throws Exception {
		msg.setProperty("timeStamp", new Date());
	}

	protected String getOperation(MappingHttpRequest httpRequest, Map operationParams) throws BindingException, NotImplementedException {
		String[] target = null;
		String path = getOperationPath(httpRequest);

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
					if(params.containsKey(WalrusProperties.BucketParameter.logging.toString())) {
						//read logging params
						getTargetBucketParams(operationParams, httpRequest);
					} else if(params.containsKey(WalrusProperties.BucketParameter.versioning.toString())) {
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

			if(!params.containsKey(WalrusProperties.BucketParameter.acl.toString())) {
				if (verb.equals(WalrusProperties.HTTPVerb.PUT.toString())) {
					if(httpRequest.containsHeader(WalrusProperties.COPY_SOURCE.toString())) {
						String copySource = httpRequest.getHeader(WalrusProperties.COPY_SOURCE.toString());
						try {
							copySource = WalrusUtil.URLdecode(copySource);
						} catch(UnsupportedEncodingException ex) {
							throw new BindingException("Unable to decode copy source: " + copySource);
						}
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

							operationParams.put("SourceBucket", sourceTarget[0]);
							operationParams.put("SourceObject", sourceObjectKey);
							operationParams.put("DestinationBucket", operationParams.remove("Bucket"));
							operationParams.put("DestinationObject", operationParams.remove("Key"));

							String metaDataDirective = httpRequest.getHeader(WalrusProperties.METADATA_DIRECTIVE.toString());
							if(metaDataDirective != null) {
								operationParams.put("MetadataDirective", metaDataDirective);
							}
							AccessControlList accessControlList;
							if(contentLength > 0) {
								accessControlList = null;
								accessControlList = getAccessControlList(httpRequest);
							} else {
								accessControlList = new AccessControlList();
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
						String contentMD5 = httpRequest.getHeader(WalrusProperties.CONTENT_MD5);
						if(contentMD5 != null)
							operationParams.put("ContentMD5", contentMD5);
						if(contentLengthString != null)
							operationParams.put("ContentLength", (new Long(contentLength).toString()));
						operationParams.put(WalrusProperties.Headers.RandomKey.toString(), randomKey);
						putQueue = getWriteMessenger().interruptAllAndGetQueue(key, randomKey);
						handleFirstChunk(httpRequest, contentLength);
					}
				} else if(verb.equals(WalrusProperties.HTTPVerb.GET.toString())) {
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
                    if(params.containsKey(WalrusProperties.GetOptionalParameters.IsCompressed.toString())) {
                        Boolean isCompressed = Boolean.parseBoolean(params.remove(WalrusProperties.GetOptionalParameters.IsCompressed.toString()));
                        operationParams.put("IsCompressed", isCompressed);
                    }

				} else if(verb.equals(WalrusProperties.HTTPVerb.HEAD.toString())) {
                    operationParams.put("GetData", Boolean.FALSE);
                    operationParams.put("InlineData", Boolean.FALSE);
                    operationParams.put("GetMetaData", Boolean.TRUE);
                } else if(verb.equals(WalrusProperties.HTTPVerb.POST.toString())) {
					LOG.debug("Not sure what to do here");
					if(params.containsKey("uploadId")) {
						operationParams.put("Parts", getPartsList(httpRequest));
					}
				}
			}
			if(params.containsKey(WalrusProperties.ObjectParameter.versionId.toString())) {
				if(!verb.equals(WalrusProperties.HTTPVerb.DELETE.toString()))
					operationParams.put("VersionId", params.remove(WalrusProperties.ObjectParameter.versionId.toString()));
			}
		}


		if (verb.equals(WalrusProperties.HTTPVerb.PUT.toString()) && params.containsKey(WalrusProperties.BucketParameter.acl.toString())) {
			operationParams.put("AccessControlPolicy", getAccessControlPolicy(httpRequest));
		}

		if (verb.equals(WalrusProperties.HTTPVerb.PUT.toString()) && params.containsKey(WalrusProperties.BucketParameter.lifecycle.toString())) {
			operationParams.put("lifecycle", getLifecycle(httpRequest));
		}

		ArrayList paramsToRemove = new ArrayList();

        boolean addMore = true;
        Iterator iterator = params.keySet().iterator();
        while(iterator.hasNext()) {
            Object key = iterator.next();
            String keyString = key.toString();
            boolean dontIncludeParam = false;
            for(WalrusAuthenticationHandler.SecurityParameter securityParam : WalrusAuthenticationHandler.SecurityParameter.values()) {
                if(keyString.equals(securityParam.toString().toLowerCase())) {
                    dontIncludeParam = true;
                    break;
                }
            }
            if(!dontIncludeParam) {
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
                }
                dontIncludeParam = true;
                if(operationKey.startsWith(SERVICE)) {
                    for(WalrusProperties.ServiceParameter param : WalrusProperties.ServiceParameter.values()) {
                        if(keyString.toLowerCase().equals(param.toString().toLowerCase())) {
                            dontIncludeParam = false;
                            break;
                        }
                    }
                } else if(operationKey.startsWith(BUCKET)) {
                    for(WalrusProperties.BucketParameter param : WalrusProperties.BucketParameter.values()) {
                        if(keyString.toLowerCase().equals(param.toString().toLowerCase())) {
                            dontIncludeParam = false;
                            break;
                        }
                    }
                } else if(operationKey.startsWith(OBJECT)) {
                    for(WalrusProperties.ObjectParameter param : WalrusProperties.ObjectParameter.values()) {
                        if(keyString.toLowerCase().equals(param.toString().toLowerCase())) {
                            dontIncludeParam = false;
                            break;
                        }
                    }
                }
                if(dontIncludeParam) {
                    paramsToRemove.add(key);
                }
            }
            if(dontIncludeParam)
                continue;
            String value = params.get(key);
            if(value != null) {
                operationParams.put(keyString, value);
            }

            //Add subresource params to the operationKey
            for(WalrusProperties.SubResource subResource : WalrusProperties.SubResource.values()) {
                if(keyString.toLowerCase().equals(subResource.toString().toLowerCase())) {
                    operationKey+=keyString.toLowerCase();
                }
            }

			/*if(addMore) {
				//just add the first one to the key
				operationKey += keyString.toLowerCase();
				addMore = false;
			}*/


            paramsToRemove.add(key);
        }

        for(Object key : paramsToRemove) {
            params.remove(key);
        }

        operationName = operationMap.get(operationKey);

		if(operationName == null) {
			String unsupportedOp = unsupportedOperationMap.get(operationKey);
			if(unsupportedOp != null){
				String resourceType = null;
				String resource = null;
				if(target.length < 2) {
					resourceType = BUCKET;
					resource = target[0];
				} else {
					resourceType = OBJECT;
					String delimiter = new String();
					for(int i = 1; i < target.length; ++i) {
						resource += delimiter + target[i];
						delimiter = "/";
					}
				}
				throw new NotImplementedException(unsupportedOp + " is not implemented", resourceType, resource);
			}
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
						grantee.setCanonicalUser(new CanonicalUser(id, canonicalUserName));
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

	private void getVersioningStatus(Map operationParams, MappingHttpRequest httpRequest) throws BindingException {
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

	private LifecycleConfigurationType getLifecycle(MappingHttpRequest httpRequest) throws BindingException {
		LifecycleConfigurationType lifecycleConfigurationType = new LifecycleConfigurationType();
		lifecycleConfigurationType.setRules( new ArrayList<LifecycleRule>() );
		String message = getMessageString(httpRequest);
		if (message.length() > 0) {
			try {
				XMLParser xmlParser = new XMLParser(message);
				DTMNodeList rules = xmlParser.getNodes("//LifecycleConfiguration/Rule");
				if (rules == null) {
					throw new BindingException("malformed lifecycle configuration");
				}
				for (int idx = 0; idx < rules.getLength(); idx++) {
					lifecycleConfigurationType.getRules().add( extractLifecycleRule( xmlParser, rules.item(idx) ) );
				}
			}
			catch (Exception ex) {
				LOG.warn(ex);
				throw new BindingException("Unable to parse lifecycle " + ex.getMessage());
			}
		}
		return lifecycleConfigurationType;
	}

	private LifecycleRule extractLifecycleRule(XMLParser parser, Node node) throws BindingException{
		LifecycleRule lifecycleRule = new LifecycleRule();
		String id = parser.getValue(node, "ID");
		String prefix = parser.getValue(node, "Prefix");
		String status = parser.getValue(node, "Status");

		lifecycleRule.setID(id);
		lifecycleRule.setPrefix(prefix);
		lifecycleRule.setStatus(status);

		try {
			LifecycleTransition transition = null;
			String transitionDays = parser.getValue(node, "Transition/Days");
			String transitionDate = parser.getValue(node, "Transition/Date");
			if ( (transitionDays != null && !transitionDays.equals("")) ||
					( transitionDate != null && !transitionDate.equals("") )) {
				String storageClass = parser.getValue(node, "Transition/StorageClass");
				if (transitionDays != null && !transitionDays.equals("")) {
					transition = new LifecycleTransition();
					Integer transitionDaysInt = new Integer( Integer.parseInt(transitionDays) );
					transition.setDays(transitionDaysInt);
				}
				else if (transitionDate != null && !transitionDate.equals("")) {
					transition = new LifecycleTransition();
					Date transitionDateAsDate = Iso8601DateParser.parse(transitionDate);
					transition.setDate(transitionDateAsDate);
				}
				if (transition != null) {
					transition.setStorageClass(storageClass);
				}
			}
			if (transition != null) {
				lifecycleRule.setTransition(transition);
			}

			LifecycleExpiration expiration = null;
			String expirationDays = parser.getValue(node, "Expiration/Days");
			String expirationDate = parser.getValue(node, "Expiration/Date");
			if ( (expirationDays != null && !expirationDays.equals("")) ||
					( expirationDate != null && !expirationDate.equals("") )) {
				if ( expirationDays != null && !expirationDays.equals("") ) {
					expiration = new LifecycleExpiration();
					Integer expirationDaysInt = new Integer( Integer.parseInt(expirationDays) );
					expiration.setDays(expirationDaysInt);
				}
				else if ( expirationDate != null && !expirationDate.equals("") ) {
					expiration = new LifecycleExpiration();
					Date expirationDateAsDate = Iso8601DateParser.parse(expirationDate);
					expiration.setDate(expirationDateAsDate);
				}
			}
			if (expiration != null) {
				lifecycleRule.setExpiration(expiration);
			}
		}
		catch (ParseException e) {
			throw new BindingException("caught a parsing exception while translating the transition or expiration date - " + e.getMessage());
		}
		catch (Exception ex) {
			throw new BindingException("caught general exception while parsing lifecycle input - " + ex.getMessage());
		}

		return lifecycleRule;
	}

	private void parseExtendedHeaders(Map operationParams, String headerString, String value) throws BindingException {
		if(headerString.equals(WalrusProperties.ExtendedGetHeaders.Range.toString())) {
			String prefix = "bytes=";
			assert(value.startsWith(prefix));
			value = value.substring(prefix.length());
			String[]values = value.split("-");
			if(values[0].equals("")) {
				operationParams.put(WalrusProperties.ExtendedHeaderRangeTypes.ByteRangeStart.toString(), new Long(0));
			} else {
				operationParams.put(WalrusProperties.ExtendedHeaderRangeTypes.ByteRangeStart.toString(), Long.parseLong(values[0]));
			}
			if((values.length < 2) || (values[1].equals(""))) {
				//-1 is treated by the back end as end of object
				operationParams.put(WalrusProperties.ExtendedHeaderRangeTypes.ByteRangeEnd.toString(), new Long(-1));
			} else {
				operationParams.put(WalrusProperties.ExtendedHeaderRangeTypes.ByteRangeEnd.toString(), Long.parseLong(values[1]));
			}
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

	private AccessControlPolicy getAccessControlPolicy(MappingHttpRequest httpRequest) throws BindingException {
		AccessControlPolicy accessControlPolicy = new AccessControlPolicy();
		AccessControlList accessControlList = new AccessControlList();
		ArrayList<Grant> grants = new ArrayList<Grant>();
		try {
			String aclString = getMessageString(httpRequest);
			if(aclString.length() > 0) {
				XMLParser xmlParser = new XMLParser(aclString);
				String ownerId = xmlParser.getValue("//Owner/ID");
				String displayName = xmlParser.getValue("//Owner/DisplayName");

				CanonicalUser canonicalUser = new CanonicalUser(ownerId, displayName);
				accessControlPolicy.setOwner(canonicalUser);

				List<String> permissions = xmlParser.getValues("//AccessControlList/Grant/Permission");
				if(permissions == null) {
					throw new BindingException("malformed access control list");
				}
				DTMNodeList grantees = xmlParser.getNodes("//AccessControlList/Grant/Grantee");
				if(grantees == null){
					throw new BindingException("malformed access control list");
				}
				for(int i = 0 ; i < grantees.getLength() ; ++i) {
					String id = xmlParser.getValue(grantees.item(i), "ID");					
					if(id.length() > 0) {
						String canonicalUserName = xmlParser.getValue(grantees.item(i), "DisplayName");
						Grant grant = new Grant();
						Grantee grantee = new Grantee();
						grantee.setCanonicalUser(new CanonicalUser(id, canonicalUserName));
						grant.setGrantee(grantee);
						grant.setPermission(permissions.get(i));
						grants.add(grant);
					} else if (! "".equals(xmlParser.getValue(grantees.item(i), "EmailAddress"))) {
						String canonicalUserName = xmlParser.getValue(grantees.item(i), "DisplayName");
						Grant grant = new Grant();
						Grantee grantee = new Grantee();
						String email = xmlParser.getValue(grantees.item(i), "EmailAddress");
						grantee.setCanonicalUser(new CanonicalUser(email, canonicalUserName));
						grant.setGrantee(grantee);
						grant.setPermission(permissions.get(i));
						grants.add(grant);
					} else {
						String groupUri = xmlParser.getValue(grantees.item(i), "URI");
						if(groupUri.length() == 0) {
							throw new BindingException("malformed access control list");
						}
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


	private AccessControlList getAccessControlList(MappingHttpRequest httpRequest) throws BindingException {
		AccessControlList accessControlList = new AccessControlList();
		ArrayList<Grant> grants = new ArrayList<Grant>();
		try {
			String aclString = getMessageString(httpRequest);
			if(aclString.length() > 0) {
				XMLParser xmlParser = new XMLParser(aclString);
				String ownerId = xmlParser.getValue("//Owner/ID");
				String displayName = xmlParser.getValue("//Owner/DisplayName");

				List<String> permissions = xmlParser.getValues("/AccessControlList/Grant/Permission");
				if(permissions == null) {
					throw new BindingException("malformed access control list");
				}
				DTMNodeList grantees = xmlParser.getNodes("/AccessControlList/Grant/Grantee");
				if(grantees == null) {
					throw new BindingException("malformed access control list");
				}

				for(int i = 0 ; i < grantees.getLength() ; ++i) {
					String canonicalUserName = xmlParser.getValue(grantees.item(i), "DisplayName");
					if(canonicalUserName.length() > 0) {
						String id = xmlParser.getValue(grantees.item(i), "ID");
						Grant grant = new Grant();
						Grantee grantee = new Grantee();
						grantee.setCanonicalUser(new CanonicalUser(id, canonicalUserName));
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

	private ArrayList<Part> getPartsList(MappingHttpRequest httpRequest) throws BindingException {
		ArrayList<Part> partList = new ArrayList<Part>();
		try {
			String partsString = getMessageString(httpRequest);
			if(partsString.length() > 0) {
				XMLParser xmlParser = new XMLParser(partsString);
				DTMNodeList partNodes = xmlParser.getNodes("/CompleteMultipartUpload/Part");
				if(partNodes == null) {
					throw new BindingException("Malformed part list");
				}

				for(int i = 0; i < partNodes.getLength(); i++) {
					Part part = new Part(Integer.parseInt(xmlParser.getValue(partNodes.item(i), "PartNumber")), xmlParser.getValue(partNodes.item(i), "ETag"));
					partList.add(part);
				}
			}
		}catch(Exception ex) {
			LOG.warn(ex);
			throw new BindingException("Unable to parse part list " + ex.getMessage());
		}
		return partList;
	}

    /**
     * Removes the service path for processing the bucket/key split.
     * @param httpRequest
     * @return
     */
    protected String getOperationPath(MappingHttpRequest httpRequest) {
        if(httpRequest.getServicePath().startsWith(walrusServicePath)) {
            String opPath = httpRequest.getServicePath().replaceFirst(walrusServicePath, "");
            if(!Strings.isNullOrEmpty(opPath) && !opPath.startsWith("/")) {
                //The service path was not demarked with a /, e.g. /services/WalrusBackendblahblah -> blahblah
                //So, don't remove the service path because that changes the semantics.
            } else {
                return opPath;
            }
        }
        return httpRequest.getServicePath();
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

	private void populateObjectFromBindingMap( final GroovyObject obj, final Map<String, String> paramFieldMap, final MappingHttpRequest httpRequest, final Map bindingMap) throws BindingException
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
				for( final String parameter : f.getAnnotation( HttpParameterMapping.class ).parameter( ) )
					fieldMap.put( parameter, f.getName( ) );
				fieldMap.put( f.getName( ).substring( 0, 1 ).toUpperCase( ).concat( f.getName( ).substring( 1 ) ), f.getName( ) );
			} else fieldMap.put( f.getName( ).substring( 0, 1 ).toUpperCase( ).concat( f.getName( ).substring( 1 ) ), f.getName( ) );
		return fieldMap;
	}

	private static void addAccessControlList (final GroovyObject obj, final Map<String, String> paramFieldMap, Map bindingMap, String cannedACLString) {

		AccessControlList accessControlList;
		ArrayList<Grant> grants;

		if(bindingMap.containsKey("AccessControlPolicy")) {
			AccessControlPolicy accessControlPolicy = (AccessControlPolicy) bindingMap.get("AccessControlPolicy");
			accessControlList = accessControlPolicy.getAccessControlList();
			grants = accessControlList.getGrants();
		} else {
			accessControlList = new AccessControlList();
			grants = new ArrayList<Grant>();
		}


		CanonicalUser aws = new CanonicalUser();
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
		ChannelBuffer buffer = httpRequest.getContent();
		buffer.markReaderIndex( );
		byte[] read = new byte[buffer.readableBytes( )];
		buffer.readBytes( read );
		return new String( read );
	}

	private void handleHttpChunk(HttpChunk httpChunk) throws Exception {
		ChannelBuffer buffer = httpChunk.getContent();
		try {
			buffer.markReaderIndex( );
			byte[] read = new byte[buffer.readableBytes( )];
			buffer.readBytes( read );
			while((putQueue != null) && (!putQueue.offer(WalrusDataMessage.DataMessage(read), 500, TimeUnit.MILLISECONDS)));
			if(httpChunk.isLast()) {
				while((putQueue != null) && (!putQueue.offer(WalrusDataMessage.EOF(), 1000, TimeUnit.MILLISECONDS)));
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

	class Writer extends Thread {

		private ChannelBuffer firstBuffer;
		private long dataLength;
		public Writer(ChannelBuffer firstBuffer, long dataLength) {
			this.firstBuffer = firstBuffer;
			this.dataLength = dataLength;
		}

		public void run() {
			byte[] bytes = new byte[DATA_MESSAGE_SIZE];

			try {
				LOG.info("Starting upload");                
				putQueue.put(WalrusDataMessage.StartOfData(dataLength));

				firstBuffer.markReaderIndex( );
				byte[] read = new byte[firstBuffer.readableBytes( )];
				firstBuffer.readBytes( read );
				putQueue.put(WalrusDataMessage.DataMessage(read));
				//putQueue.put(WalrusDataMessage.EOF());

			} catch (Exception ex) {
				LOG.error(ex, ex);
			}
		}

	}

}
