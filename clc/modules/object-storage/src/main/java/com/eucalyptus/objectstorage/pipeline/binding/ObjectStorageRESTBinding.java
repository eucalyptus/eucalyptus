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

package com.eucalyptus.objectstorage.pipeline.binding;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.policy.key.Iso8601DateParser;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.RoleUser;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.ObjectStorageBucketLogger;
import com.eucalyptus.objectstorage.exceptions.s3.MethodNotAllowedException;
import com.eucalyptus.objectstorage.exceptions.s3.NotImplementedException;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataGetRequestType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataRequestType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageRequestType;
import com.eucalyptus.objectstorage.pipeline.ObjectStorageRESTPipeline;
import com.eucalyptus.objectstorage.pipeline.handlers.ObjectStorageAuthenticationHandler;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.common.DateFormatter;
import com.eucalyptus.storage.msgs.BucketLogData;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.Expiration;
import com.eucalyptus.storage.msgs.s3.Grant;
import com.eucalyptus.storage.msgs.s3.Grantee;
import com.eucalyptus.storage.msgs.s3.Group;
import com.eucalyptus.storage.msgs.s3.LifecycleConfiguration;
import com.eucalyptus.storage.msgs.s3.LifecycleRule;
import com.eucalyptus.storage.msgs.s3.LoggingEnabled;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;
import com.eucalyptus.storage.msgs.s3.Part;
import com.eucalyptus.storage.msgs.s3.TargetGrants;
import com.eucalyptus.storage.msgs.s3.Transition;
import com.eucalyptus.util.ChannelBufferStreamingInputStream;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.XMLParser;
import com.eucalyptus.ws.handlers.RestfulMarshallingHandler;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;
import groovy.lang.GroovyObject;
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
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.w3c.dom.Node;

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

public class ObjectStorageRESTBinding extends RestfulMarshallingHandler {
    protected static Logger LOG = Logger.getLogger( ObjectStorageRESTBinding.class );
    protected static final String SERVICE = "service";
    protected static final String BUCKET = "bucket";
    protected static final String OBJECT = "object";
    protected static final Map<String, String> operationMap = populateOperationMap();
    private static final Map<String, String> unsupportedOperationMap = populateUnsupportedOperationMap();
    protected String key;
    protected String randomKey;

    public ObjectStorageRESTBinding( ) {
        super( "http://s3.amazonaws.com/doc/" + ObjectStorageProperties.NAMESPACE_VERSION );
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
                Channels.fireExceptionCaught( channelHandlerContext, e );
                return;
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

            if(msg instanceof ObjectStorageDataGetRequestType) {
                ObjectStorageDataGetRequestType getObject = (ObjectStorageDataGetRequestType) msg;
                getObject.setChannel(ctx.getChannel());
            }
            if(msg instanceof ObjectStorageDataRequestType) {
                String expect = httpRequest.getHeader(HttpHeaders.Names.EXPECT);
                if(expect != null) {
                    if(expect.toLowerCase().equals("100-continue")) {
                        ObjectStorageDataRequestType request = (ObjectStorageDataRequestType) msg;
                        request.setExpectHeader(true);
                        /*HttpResponse response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE );
                        DownstreamMessageEvent newEvent = new DownstreamMessageEvent( ctx.getChannel( ), event.getFuture(), response, null );
                        final Channel channel = ctx.getChannel();
                        if ( channel.isConnected( ) ) {
                            ChannelFuture writeFuture = Channels.future( ctx.getChannel( ) );
                            Channels.write(ctx, writeFuture, response);
                        }
                        ctx.sendDownstream( newEvent );*/
                    }
                }

                //handle the content.
                ObjectStorageDataRequestType request = (ObjectStorageDataRequestType) msg;
                request.setIsChunked(httpRequest.isChunked());
                handleData(request, httpRequest.getContent());
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
            } else {
                binding = BindingManager.getDefaultBinding( );
            }
            if(msg != null) {
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                byteOut.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>".getBytes("UTF-8"));
                binding.toStream( byteOut, msg );
                byte[] req = byteOut.toByteArray();
                ChannelBuffer buffer = ChannelBuffers.wrappedBuffer( req );
                httpResponse.setHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(buffer.readableBytes() ) );
                httpResponse.setHeader( HttpHeaders.Names.CONTENT_TYPE, "application/xml" );
                httpResponse.setHeader( HttpHeaders.Names.DATE, DateFormatter.dateToHeaderFormattedString(new Date()));
                httpResponse.setHeader( "x-amz-request-id", msg.getCorrelationId());
                httpResponse.setContent( buffer );
            }
        }
    }

    public void handleData(ObjectStorageDataRequestType dataRequest, ChannelBuffer content) {
        ChannelBufferStreamingInputStream stream = new ChannelBufferStreamingInputStream(content);
        dataRequest.setData(stream);
    }

    protected static Map<String, String> populateOperationMap() {
        Map<String, String> newMap = new HashMap<String, String>();
        //Service operations
        newMap.put(SERVICE + ObjectStorageProperties.HTTPVerb.GET.toString(), "ListAllMyBuckets");

        //Bucket operations
        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.HEAD.toString(), "HeadBucket");
        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.BucketParameter.acl.toString(), "GetBucketAccessControlPolicy");
        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.PUT.toString() + ObjectStorageProperties.BucketParameter.acl.toString(), "SetBucketAccessControlPolicy");

        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.GET.toString(), "ListBucket");
        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.BucketParameter.prefix.toString(), "ListBucket");
        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.BucketParameter.maxkeys.toString(), "ListBucket");
        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.BucketParameter.marker.toString(), "ListBucket");
        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.BucketParameter.delimiter.toString(), "ListBucket");
        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.PUT.toString(), "CreateBucket");
        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.DELETE.toString(), "DeleteBucket");

        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.BucketParameter.location.toString(), "GetBucketLocation");

        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.BucketParameter.logging.toString(), "GetBucketLoggingStatus");
        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.PUT.toString() + ObjectStorageProperties.BucketParameter.logging.toString(), "SetBucketLoggingStatus");

        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.BucketParameter.versions.toString(), "ListVersions");
        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.BucketParameter.versioning.toString(), "GetBucketVersioningStatus");
        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.PUT.toString() + ObjectStorageProperties.BucketParameter.versioning.toString(), "SetBucketVersioningStatus");

        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.BucketParameter.lifecycle.toString(), "GetBucketLifecycle");
        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.PUT.toString() + ObjectStorageProperties.BucketParameter.lifecycle.toString(), "SetBucketLifecycle");
        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.DELETE.toString() + ObjectStorageProperties.BucketParameter.lifecycle.toString(), "DeleteBucketLifecycle");
        // Multipart uploads
        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.BucketParameter.uploads.toString(), "ListMultipartUploads");


        //Object operations
        newMap.put(OBJECT + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.ObjectParameter.acl.toString(), "GetObjectAccessControlPolicy");
        newMap.put(OBJECT + ObjectStorageProperties.HTTPVerb.PUT.toString() + ObjectStorageProperties.ObjectParameter.acl.toString(), "SetObjectAccessControlPolicy");

        newMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.POST.toString(), "PostObject");

        newMap.put(OBJECT + ObjectStorageProperties.HTTPVerb.PUT.toString(), "PutObject");
        newMap.put(OBJECT + ObjectStorageProperties.HTTPVerb.PUT.toString() + ObjectStorageProperties.COPY_SOURCE.toString(), "CopyObject");
        newMap.put(OBJECT + ObjectStorageProperties.HTTPVerb.GET.toString(), "GetObject");
        newMap.put(OBJECT + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.ObjectParameter.torrent.toString(), "GetObject");
        newMap.put(OBJECT + ObjectStorageProperties.HTTPVerb.DELETE.toString(), "DeleteObject");

        newMap.put(OBJECT + ObjectStorageProperties.HTTPVerb.HEAD.toString(), "HeadObject");
        newMap.put(OBJECT + ObjectStorageProperties.HTTPVerb.GET.toString() + "extended", "GetObjectExtended");

        newMap.put(OBJECT + ObjectStorageProperties.HTTPVerb.DELETE.toString() + ObjectStorageProperties.ObjectParameter.versionId.toString().toLowerCase(), "DeleteVersion");

        // Multipart Uploads
        newMap.put(OBJECT + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.ObjectParameter.uploadId.toString().toLowerCase(), "ListParts");
        newMap.put(OBJECT + ObjectStorageProperties.HTTPVerb.POST.toString() + ObjectStorageProperties.ObjectParameter.uploads.toString(), "InitiateMultipartUpload");
        newMap.put(OBJECT + ObjectStorageProperties.HTTPVerb.PUT.toString() + ObjectStorageProperties.ObjectParameter.partNumber.toString().toLowerCase() + ObjectStorageProperties.ObjectParameter.uploadId.toString().toLowerCase(), "UploadPart");
        newMap.put(OBJECT + ObjectStorageProperties.HTTPVerb.PUT.toString() + ObjectStorageProperties.ObjectParameter.uploadId.toString().toLowerCase() + ObjectStorageProperties.ObjectParameter.partNumber.toString().toLowerCase(), "UploadPart");
        newMap.put(OBJECT + ObjectStorageProperties.HTTPVerb.POST.toString() + ObjectStorageProperties.ObjectParameter.uploadId.toString().toLowerCase(), "CompleteMultipartUpload");
        newMap.put(OBJECT + ObjectStorageProperties.HTTPVerb.DELETE.toString() + ObjectStorageProperties.ObjectParameter.uploadId.toString().toLowerCase(), "AbortMultipartUpload");

        return newMap;
    }

    private static Map<String, String> populateUnsupportedOperationMap() {
        Map<String, String> opsMap = new HashMap<String, String>();

        // Bucket operations
        // Cross-Origin Resource Sharing (cors)
        opsMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.BucketParameter.cors.toString(), "GET Bucket cors");
        opsMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.PUT.toString() + ObjectStorageProperties.BucketParameter.cors.toString(), "PUT Bucket cors");
        opsMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.DELETE.toString() + ObjectStorageProperties.BucketParameter.cors.toString(), "DELETE Bucket cors");
        // Policy
        opsMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.BucketParameter.policy.toString(), "GET Bucket policy");
        opsMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.PUT.toString() + ObjectStorageProperties.BucketParameter.policy.toString(), "PUT Bucket policy");
        opsMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.DELETE.toString() + ObjectStorageProperties.BucketParameter.policy.toString(), "DELETE Bucket policy");
        // Notification
        opsMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.BucketParameter.notification.toString(), "GET Bucket notification");
        opsMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.PUT.toString() + ObjectStorageProperties.BucketParameter.notification.toString(), "PUT Bucket notification");
        // Tagging
        opsMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.BucketParameter.tagging.toString(), "GET Bucket tagging");
        opsMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.PUT.toString() + ObjectStorageProperties.BucketParameter.tagging.toString(), "PUT Bucket tagging");
        opsMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.DELETE.toString() + ObjectStorageProperties.BucketParameter.tagging.toString(), "DELETE Bucket tagging");
        // Request Payments // TODO HACK! binding code converts parameters to lower case. Fix that issue!
        opsMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.BucketParameter.requestPayment.toString().toLowerCase(), "GET Bucket requestPayment");
        // Website
        opsMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.GET.toString() + ObjectStorageProperties.BucketParameter.website.toString(), "GET Bucket website");
        opsMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.PUT.toString() + ObjectStorageProperties.BucketParameter.website.toString(), "PUT Bucket website");
        opsMap.put(BUCKET + ObjectStorageProperties.HTTPVerb.DELETE.toString() + ObjectStorageProperties.BucketParameter.website.toString(), "DELETE Bucket website");

        // Object operations
        return opsMap;
    }

    @Override
    public Object bind( final MappingHttpRequest httpRequest ) throws Exception {
        String servicePath = httpRequest.getServicePath();
        Map bindingArguments = new HashMap();
        final String operationName = getOperation(httpRequest, bindingArguments);
        if(operationName == null)
            throw new MethodNotAllowedException(httpRequest.getMethod().toString() + " " + httpRequest.getUri());

        Map<String, String> params = httpRequest.getParameters();

        OMElement msg;

        GroovyObject groovyMsg;
        Map<String, String> fieldMap;
        Class targetType;
        try
        {
            //:: try to create the target class :://
            targetType = ClassLoader.getSystemClassLoader().loadClass( "com.eucalyptus.objectstorage.msgs.".concat( operationName ).concat( "Type" ) );
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

        User user = Contexts.lookup( httpRequest.getCorrelationId( ) ).getUser();
        setRequiredParams (groovyMsg, user);

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

        LOG.trace(groovyMsg.toString());
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

    protected void addLogData(BaseMessage eucaMsg,
                              Map bindingArguments) {
        if(eucaMsg instanceof ObjectStorageRequestType) {
            String operation = (String) bindingArguments.remove("Operation");
            if(operation != null) {
                ObjectStorageRequestType request = (ObjectStorageRequestType) eucaMsg;
                BucketLogData logData = ObjectStorageBucketLogger.getInstance().makeLogEntry(UUID.randomUUID().toString());
                logData.setOperation("REST." + operation);
                request.setLogData(logData);
            }
        }
    }

    protected void setRequiredParams(final GroovyObject msg, User user) throws Exception {
        if(user != null && !user.equals(Principals.nobodyUser())) {
            //Change to handle IAM roles, can't find the key this way
            if (user instanceof RoleUser) {
                RoleUser roleUser = (RoleUser) user;
                msg.setProperty("accessKeyID", roleUser.getUserId());
            } else {
                msg.setProperty("accessKeyID", Accounts.getFirstActiveAccessKeyId(user));
            }
        }
        msg.setProperty("timeStamp", new Date());
    }

    protected String getOperation(MappingHttpRequest httpRequest, Map operationParams) throws BindingException, NotImplementedException {
        String[] target = null;
        String path = getOperationPath(httpRequest);
        boolean objectstorageInternalOperation = false;

        String targetHost = httpRequest.getHeader(HttpHeaders.Names.HOST);
        if(targetHost.contains(".objectstorage")) {
            String bucket = targetHost.substring(0, targetHost.indexOf(".objectstorage"));
            path = "/" + bucket + path;
        }

        if(path.length() > 0) {
            target = OSGUtil.getTarget(path);
        }

        String verb = httpRequest.getMethod().getName();
        String operationKey = "";
        Map<String, String> params = httpRequest.getParameters();
        String operationName = null;
        long contentLength = 0;
        String contentLengthString = httpRequest.getHeader(HttpHeaders.Names.CONTENT_LENGTH);
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
                if(verb.equals(ObjectStorageProperties.HTTPVerb.POST.toString())) {
                    //TODO: handle POST.
                    Map formFields = httpRequest.getFormFields();

                    String objectKey = null;
                    String file = (String) formFields.get(ObjectStorageProperties.FormField.file.toString());
                    String authenticationHeader = "";
                    if(formFields.containsKey(ObjectStorageProperties.FormField.key.toString())) {
                        objectKey = (String) formFields.get(ObjectStorageProperties.FormField.key.toString());
                        objectKey = objectKey.replaceAll("\\$\\{filename\\}", file);
                        operationParams.put("Key", objectKey);
                    }
                    if(formFields.containsKey(ObjectStorageProperties.FormField.acl.toString())) {
                        String acl = (String) formFields.get(ObjectStorageProperties.FormField.acl.toString());
                        httpRequest.addHeader(ObjectStorageProperties.AMZ_ACL, acl);
                    }
                    if(formFields.containsKey(ObjectStorageProperties.FormField.redirect.toString())) {
                        String successActionRedirect = (String) formFields.get(ObjectStorageProperties.FormField.redirect.toString());
                        operationParams.put("SuccessActionRedirect", successActionRedirect);
                    }
                    if(formFields.containsKey(ObjectStorageProperties.FormField.success_action_redirect.toString())) {
                        String successActionRedirect = (String) formFields.get(ObjectStorageProperties.FormField.success_action_redirect.toString());
                        operationParams.put("SuccessActionRedirect", successActionRedirect);
                    }
                    if(formFields.containsKey(ObjectStorageProperties.FormField.success_action_status.toString())) {
                        Integer successActionStatus = Integer.parseInt((String)formFields.get(ObjectStorageProperties.FormField.success_action_status.toString()));
                        if(successActionStatus == 200 || successActionStatus == 201)
                            operationParams.put("SuccessActionStatus", successActionStatus);
                        else
                            operationParams.put("SuccessActionStatus", 204);
                    } else {
                        operationParams.put("SuccessActionStatus", 204);
                    }
                    if(formFields.containsKey(ObjectStorageProperties.CONTENT_TYPE)) {
                        operationParams.put("ContentType", formFields.get(ObjectStorageProperties.CONTENT_TYPE));
                    }
                    key = target[0] + "." + objectKey;
                    randomKey = key + "." + Hashes.getRandom(10);
                    if(contentLengthString != null)
                        operationParams.put("ContentLength", (new Long(contentLength).toString()));
                    operationParams.put(ObjectStorageProperties.Headers.RandomKey.toString(), randomKey);

                    //TODO: This is PostObject.
                    //handleFirstChunk(httpRequest, (ChannelBuffer)formFields.get(ObjectStorageProperties.IGNORE_PREFIX + "FirstDataChunk"), contentLength);

                    //Set the message content to the first-data chunk if found. This is used later for processing the message like a PUT request
                    httpRequest.setContent((ChannelBuffer)formFields.get(ObjectStorageProperties.IGNORE_PREFIX + "FirstDataChunk"));

                } else if(ObjectStorageProperties.HTTPVerb.PUT.toString().equals(verb)) {
                    if(params.containsKey(ObjectStorageProperties.BucketParameter.logging.toString())) {
                        //read logging params
                        getTargetBucketParams(operationParams, httpRequest);
                    } else if(params.containsKey(ObjectStorageProperties.BucketParameter.versioning.toString())) {
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
                objectKey = OSGUtil.URLdecode(objectKey);
            } catch (UnsupportedEncodingException e) {
                throw new BindingException("Unable to get key: " + e.getMessage());
            }

            operationParams.put("Bucket", target[0]);
            operationParams.put("Key", objectKey);
            operationParams.put("Operation", verb.toUpperCase() + "." + "OBJECT");

            if(!params.containsKey(ObjectStorageProperties.BucketParameter.acl.toString())) {
                if (verb.equals(ObjectStorageProperties.HTTPVerb.PUT.toString())) {
                    if(httpRequest.containsHeader(ObjectStorageProperties.COPY_SOURCE.toString())) {
                        String copySource = httpRequest.getHeader(ObjectStorageProperties.COPY_SOURCE.toString());
                        try {
                            copySource = OSGUtil.URLdecode(copySource);
                        } catch(UnsupportedEncodingException ex) {
                            throw new BindingException("Unable to decode copy source: " + copySource);
                        }
                        String[] sourceParts = copySource.split("\\?");
                        if(sourceParts.length > 1) {
                            operationParams.put("SourceVersionId", sourceParts[1].replaceFirst("versionId=", "").trim());
                        }
                        copySource = sourceParts[0];
                        String[] sourceTarget = OSGUtil.getTarget(copySource);
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

                            String metaDataDirective = httpRequest.getHeader(ObjectStorageProperties.METADATA_DIRECTIVE.toString());
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
                            operationKey += ObjectStorageProperties.COPY_SOURCE.toString();
                            Set<String> headerNames = httpRequest.getHeaderNames();
                            for(String key : headerNames) {
                                for(ObjectStorageProperties.CopyHeaders header: ObjectStorageProperties.CopyHeaders.values()) {
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
                        String contentType = httpRequest.getHeader(ObjectStorageProperties.CONTENT_TYPE);
                        if(contentType != null)
                            operationParams.put("ContentType", contentType);
                        String contentDisposition = httpRequest.getHeader("Content-Disposition");
                        if(contentDisposition != null)
                            operationParams.put("ContentDisposition", contentDisposition);
                        String contentMD5 = httpRequest.getHeader(ObjectStorageProperties.CONTENT_MD5);
                        if(contentMD5 != null)
                            operationParams.put("ContentMD5", contentMD5);
                        if(contentLengthString != null)
                            operationParams.put("ContentLength", (new Long(contentLength).toString()));

                        //Not used in this pipeline
                        //operationParams.put(ObjectStorageProperties.Headers.RandomKey.toString(), randomKey);
                        //putQueue = getWriteMessenger().interruptAllAndGetQueue(key, randomKey);
                        //handleFirstChunk(httpRequest, contentLength);

                        //Not needed for binding anymore
						/*try {
							operationParams.put("Data", getFirstChunk(httpRequest, contentLength));
						} catch (Exception ex) {
							throw new BindingException("Unable to get data from PUT request for: " + key, ex);
						}*/
                    }
                } else if(verb.equals(ObjectStorageProperties.HTTPVerb.GET.toString())) {
                    if(!objectstorageInternalOperation) {

                        if(params.containsKey("torrent")) {
                            operationParams.put("GetTorrent", Boolean.TRUE);
                        } else {
                            if (!params.containsKey("uploadId")) {
                                operationParams.put("InlineData", Boolean.FALSE);
                                operationParams.put("GetMetaData", Boolean.TRUE);
                            }
                        }

                        Set<String> headerNames = httpRequest.getHeaderNames();
                        boolean isExtendedGet = false;
                        for(String key : headerNames) {
                            for(ObjectStorageProperties.ExtendedGetHeaders header: ObjectStorageProperties.ExtendedGetHeaders.values()) {
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
                    if(params.containsKey(ObjectStorageProperties.GetOptionalParameters.IsCompressed.toString())) {
                        Boolean isCompressed = Boolean.parseBoolean(params.remove(ObjectStorageProperties.GetOptionalParameters.IsCompressed.toString()));
                        operationParams.put("IsCompressed", isCompressed);
                    }

                } else if(verb.equals(ObjectStorageProperties.HTTPVerb.POST.toString())) {
                    if(params.containsKey("uploadId")) {
                        operationParams.put("Parts", getPartsList(httpRequest));
                    }
                }
            }
            if(params.containsKey(ObjectStorageProperties.ObjectParameter.versionId.toString())) {
                if(!verb.equals(ObjectStorageProperties.HTTPVerb.DELETE.toString()))
                    operationParams.put("VersionId", params.remove(ObjectStorageProperties.ObjectParameter.versionId.toString()));
            }
        }


        if (verb.equals(ObjectStorageProperties.HTTPVerb.PUT.toString()) && params.containsKey(ObjectStorageProperties.BucketParameter.acl.toString())) {
            operationParams.put("AccessControlPolicy", getAccessControlPolicy(httpRequest));
        }

        if (verb.equals(ObjectStorageProperties.HTTPVerb.PUT.toString()) && params.containsKey(ObjectStorageProperties.BucketParameter.lifecycle.toString())) {
            operationParams.put("lifecycleConfiguration", getLifecycle(httpRequest));
        }

        ArrayList paramsToRemove = new ArrayList();

        boolean addMore = true;
        Iterator iterator = params.keySet().iterator();
        while(iterator.hasNext()) {
            Object key = iterator.next();
            String keyString = key.toString();
            boolean dontIncludeParam = false;
            for(ObjectStorageAuthenticationHandler.SecurityParameter securityParam : ObjectStorageAuthenticationHandler.SecurityParameter.values()) {
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
                    for(ObjectStorageProperties.ServiceParameter param : ObjectStorageProperties.ServiceParameter.values()) {
                        if(keyString.toLowerCase().equals(param.toString().toLowerCase())) {
                            dontIncludeParam = false;
                            break;
                        }
                    }
                } else if(operationKey.startsWith(BUCKET)) {
                    for(ObjectStorageProperties.BucketParameter param : ObjectStorageProperties.BucketParameter.values()) {
                        if(keyString.toLowerCase().equals(param.toString().toLowerCase())) {
                            dontIncludeParam = false;
                            break;
                        }
                    }
                } else if(operationKey.startsWith(OBJECT)) {
                    for(ObjectStorageProperties.ObjectParameter param : ObjectStorageProperties.ObjectParameter.values()) {
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
            for(ObjectStorageProperties.SubResource subResource : ObjectStorageProperties.SubResource.values()) {
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

        if(!objectstorageInternalOperation) {
            operationName = operationMap.get(operationKey);
        }

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
                NotImplementedException e = new NotImplementedException();
                e.setResource(resource);
                e.setResourceType(resourceType);
                e.setMessage(unsupportedOp + " is not implemented");
                throw e;
            }
        }

        if("CreateBucket".equals(operationName)) {
            String locationConstraint = getLocationConstraint(httpRequest);
            if(locationConstraint != null)
                operationParams.put("LocationConstraint", locationConstraint);
        }
        return operationName;
    }

    protected void getTargetBucketParams(Map operationParams,
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

    protected void getVersioningStatus(Map operationParams, MappingHttpRequest httpRequest) throws BindingException {
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

    protected void parseExtendedHeaders(Map operationParams, String headerString, String value) throws BindingException {
        if(headerString.equals(ObjectStorageProperties.ExtendedGetHeaders.Range.toString())) {
            String prefix = "bytes=";
            assert(value.startsWith(prefix));
            value = value.substring(prefix.length());
            String[]values = value.split("-");
            if(values[0].equals("")) {
                operationParams.put(ObjectStorageProperties.ExtendedHeaderRangeTypes.ByteRangeStart.toString(), new Long(0));
            } else {
                operationParams.put(ObjectStorageProperties.ExtendedHeaderRangeTypes.ByteRangeStart.toString(), Long.parseLong(values[0]));
            }
            if((values.length < 2) || (values[1].equals(""))) {
                //-1 is treated by the back end as end of object
                operationParams.put(ObjectStorageProperties.ExtendedHeaderRangeTypes.ByteRangeEnd.toString(), new Long(-1));
            } else {
                operationParams.put(ObjectStorageProperties.ExtendedHeaderRangeTypes.ByteRangeEnd.toString(), Long.parseLong(values[1]));
            }
        } else if(ObjectStorageProperties.ExtendedHeaderDateTypes.contains(headerString)) {
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

    protected AccessControlPolicy getAccessControlPolicy(MappingHttpRequest httpRequest) throws BindingException {
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


    protected AccessControlList getAccessControlList(MappingHttpRequest httpRequest) throws BindingException {
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
        for(String pathCandidate : ObjectStorageRESTPipeline.getServicePaths()) {
            if(httpRequest.getServicePath().startsWith(pathCandidate)) {
                String opPath = httpRequest.getServicePath().replaceFirst(pathCandidate, "");
                if(!Strings.isNullOrEmpty(opPath) && !opPath.startsWith("/")) {
                    //The service path was not demarked with a /, e.g. /services/objectstorageblahblah -> blahblah
                    //So, don't remove the service path because that changes the semantics.
                    break;
                } else {
                    return opPath;
                }
            }
        }

        return httpRequest.getServicePath();
    }


    protected String getLocationConstraint(MappingHttpRequest httpRequest) throws BindingException {
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

    protected List<String> populateObject( final GroovyObject obj, final Map<String, String> paramFieldMap, final Map<String, String> params ) {
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

    protected void populateObjectFromBindingMap( final GroovyObject obj, final Map<String, String> paramFieldMap, final MappingHttpRequest httpRequest, final Map bindingMap) throws BindingException
    {
        //process headers
        String aclString = httpRequest.getAndRemoveHeader(ObjectStorageProperties.AMZ_ACL);
        if (aclString != null) {
            addAccessControlList(obj, paramFieldMap, bindingMap, aclString);
        }

        //add meta data
        String metaDataString = paramFieldMap.remove("MetaData");
        if(metaDataString != null) {
            Set<String> headerNames = httpRequest.getHeaderNames();
            ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
            for(String key : headerNames) {
                if(key.toLowerCase().startsWith(ObjectStorageProperties.AMZ_META_HEADER_PREFIX)) {
                    MetaDataEntry metaDataEntry = new MetaDataEntry();
                    metaDataEntry.setName(key.substring(ObjectStorageProperties.AMZ_META_HEADER_PREFIX.length()));
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

    protected boolean populateObjectField( final GroovyObject obj, final Map.Entry<String, String> paramFieldPair, final Map<String, String> params ) {
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

    protected List<String> populateObjectList( final GroovyObject obj, final Map.Entry<String, String> paramFieldPair, final Map<String, String> params, final int paramSize ) {
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

    protected List<String> populateEmbedded( final Class genericType, final Map<String, String> params, final ArrayList theList ) throws InstantiationException, IllegalAccessException {
        GroovyObject embedded = ( GroovyObject ) genericType.newInstance( );
        Map<String, String> embeddedFields = buildFieldMap(genericType);
        int startSize = params.size( );
        List<String> embeddedFailures = populateObject( embedded, embeddedFields, params );
        if ( embeddedFailures.isEmpty( ) && !( params.size( ) - startSize == 0 ) ) theList.add( embedded );
        return embeddedFailures;
    }

    protected Map<String, String> buildFieldMap( final Class targetType ) {
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

    protected static void addAccessControlList (final GroovyObject obj, final Map<String, String> paramFieldMap, Map bindingMap, String cannedACLString) {

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

    protected String toUpperFirst(String string) {
        return string.substring(0, 1).toUpperCase().concat(string.substring(1));
    }

    protected boolean exactMatch(JSONObject jsonObject, Map formFields, List<String> policyItemNames) {
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

    protected boolean partialMatch(JSONArray jsonArray, Map<String, String> formFields, List<String> policyItemNames) {
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

    protected String getMessageString(MappingHttpRequest httpRequest) {
        ChannelBuffer buffer = httpRequest.getContent( );
        buffer.markReaderIndex( );
        byte[] read = new byte[buffer.readableBytes( )];
        buffer.readBytes( read );
        return new String( read );
    }

    private LifecycleConfiguration getLifecycle(MappingHttpRequest httpRequest) throws BindingException {
        LifecycleConfiguration lifecycleConfigurationType = new LifecycleConfiguration();
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

        lifecycleRule.setId(id);
        lifecycleRule.setPrefix(prefix);
        lifecycleRule.setStatus(status);

        try {
            Transition transition = null;
            String transitionDays = parser.getValue(node, "Transition/Days");
            String transitionDate = parser.getValue(node, "Transition/Date");
            if ( (transitionDays != null && !transitionDays.equals("")) ||
                    ( transitionDate != null && !transitionDate.equals("") )) {
                String storageClass = parser.getValue(node, "Transition/StorageClass");
                if (transitionDays != null && !transitionDays.equals("")) {
                    transition = new Transition();
                    Integer transitionDaysInt = new Integer( Integer.parseInt(transitionDays) );
                    transition.setCreationDelayDays(transitionDaysInt.intValue());
                }
                else if (transitionDate != null && !transitionDate.equals("")) {
                    transition = new Transition();
                    Date transitionDateAsDate = Iso8601DateParser.parse(transitionDate);
                    transition.setEffectiveDate(transitionDateAsDate);
                }
                if (transition != null) {
                    transition.setDestinationClass(storageClass);
                }
            }
            if (transition != null) {
                lifecycleRule.setTransition(transition);
            }

            Expiration expiration = null;
            String expirationDays = parser.getValue(node, "Expiration/Days");
            String expirationDate = parser.getValue(node, "Expiration/Date");
            if ( (expirationDays != null && !expirationDays.equals("")) ||
                    ( expirationDate != null && !expirationDate.equals("") )) {
                if ( expirationDays != null && !expirationDays.equals("") ) {
                    expiration = new Expiration();
                    Integer expirationDaysInt = new Integer( Integer.parseInt(expirationDays) );
                    expiration.setCreationDelayDays(expirationDaysInt.intValue());
                }
                else if ( expirationDate != null && !expirationDate.equals("") ) {
                    expiration = new Expiration();
                    Date expirationDateAsDate = Iso8601DateParser.parse(expirationDate);
                    expiration.setEffectiveDate(expirationDateAsDate);
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
}
