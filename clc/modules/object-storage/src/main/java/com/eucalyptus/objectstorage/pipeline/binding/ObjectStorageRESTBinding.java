/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.objectstorage.pipeline.binding;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.axiom.om.OMElement;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xml.dtm.ref.DTMNodeList;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.w3c.dom.Node;

import com.eucalyptus.auth.policy.key.Iso8601DateParser;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.objectstorage.ObjectStorageBucketLogger;
import com.eucalyptus.objectstorage.exceptions.s3.CorsConfigUnsupportedMethodException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidArgumentException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidTagErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.MalformedACLErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.MalformedPOSTRequestException;
import com.eucalyptus.objectstorage.exceptions.s3.MalformedXMLException;
import com.eucalyptus.objectstorage.exceptions.s3.MethodNotAllowedException;
import com.eucalyptus.objectstorage.exceptions.s3.NotImplementedException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataGetRequestType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataRequestType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageRequestType;
import com.eucalyptus.objectstorage.pipeline.ObjectStorageRESTPipeline;
import com.eucalyptus.objectstorage.util.AclUtils;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties.Permission;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties.X_AMZ_GRANT;
import com.eucalyptus.org.apache.tools.ant.util.DateUtils;
import com.eucalyptus.records.Logs;
import com.eucalyptus.storage.common.DateFormatter;
import com.eucalyptus.storage.msgs.BucketLogData;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.eucalyptus.storage.msgs.s3.AllowedCorsMethods;
import com.eucalyptus.storage.msgs.s3.BucketTag;
import com.eucalyptus.storage.msgs.s3.BucketTagSet;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.CorsConfiguration;
import com.eucalyptus.storage.msgs.s3.CorsRule;
import com.eucalyptus.storage.msgs.s3.DeleteMultipleObjectsEntry;
import com.eucalyptus.storage.msgs.s3.DeleteMultipleObjectsMessage;
import com.eucalyptus.storage.msgs.s3.Expiration;
import com.eucalyptus.storage.msgs.s3.Grant;
import com.eucalyptus.storage.msgs.s3.Grantee;
import com.eucalyptus.storage.msgs.s3.Group;
import com.eucalyptus.storage.msgs.s3.LifecycleConfiguration;
import com.eucalyptus.storage.msgs.s3.LifecycleRule;
import com.eucalyptus.storage.msgs.s3.LoggingEnabled;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;
import com.eucalyptus.storage.msgs.s3.Part;
import com.eucalyptus.storage.msgs.s3.PreflightRequest;
import com.eucalyptus.storage.msgs.s3.TaggingConfiguration;
import com.eucalyptus.storage.msgs.s3.TargetGrants;
import com.eucalyptus.storage.msgs.s3.Transition;
import com.eucalyptus.util.ChannelBufferStreamingInputStream;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.XMLParser;
import com.eucalyptus.ws.handlers.RestfulMarshallingHandler;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import groovy.lang.GroovyObject;
import javaslang.Tuple2;

public abstract class ObjectStorageRESTBinding extends RestfulMarshallingHandler {
  protected static Logger LOG = Logger.getLogger(ObjectStorageRESTBinding.class);
  protected static final String SERVICE = "service";
  protected static final String BUCKET = "bucket";
  protected static final String OBJECT = "object";
  protected Map<String, String> operationMap;
  protected Map<String, String> unsupportedOperationMap;
  protected String key;

  public enum SecurityParameter {
    AWSAccessKeyId, Timestamp, Expires, Signature, Authorization, Date, Content_MD5, Content_Type, SecurityToken,
  }

  public ObjectStorageRESTBinding() {
    super("http://s3.amazonaws.com/doc/" + ObjectStorageProperties.NAMESPACE_VERSION + "/");
    operationMap = populateOperationMap();
    unsupportedOperationMap = populateUnsupportedOperationMap();
  }

  @Override
  public void handleUpstream(final ChannelHandlerContext channelHandlerContext, final ChannelEvent channelEvent) throws Exception {
    if (Logs.isExtrrreeeme()) {
      Logs.extreme().trace(LogUtil.dumpObject(channelEvent));
    }
    if (channelEvent instanceof MessageEvent) {
      final MessageEvent msgEvent = (MessageEvent) channelEvent;
      try {
        this.incomingMessage(channelHandlerContext, msgEvent);
      } catch (Exception e) {
        Logs.extreme().trace("Error binding request", e);
        Channels.fireExceptionCaught(channelHandlerContext, e);
        return;
      }
    }
    channelHandlerContext.sendUpstream(channelEvent);
  }

  @Override
  public void incomingMessage(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
    if (event.getMessage() instanceof MappingHttpRequest) {
      MappingHttpRequest httpRequest = (MappingHttpRequest) event.getMessage();
      // TODO: get real user data here too
      // Auth is already done before binding, so binding here is just a validation. Then send 100-continue
      BaseMessage msg = (BaseMessage) this.bind(httpRequest);
      httpRequest.setMessage(msg);

      if (msg instanceof ObjectStorageDataGetRequestType) {
        ObjectStorageDataGetRequestType getObject = (ObjectStorageDataGetRequestType) msg;
        getObject.setChannel(ctx.getChannel());
      }
      if (msg instanceof ObjectStorageDataRequestType) {
        String expect = httpRequest.getHeader(HttpHeaders.Names.EXPECT);
        if (expect != null) {
          if (expect.toLowerCase().equals("100-continue")) {
            ObjectStorageDataRequestType request = (ObjectStorageDataRequestType) msg;
            request.setExpectHeader(true);
          }
        }

        // handle the content.
        ObjectStorageDataRequestType request = (ObjectStorageDataRequestType) msg;
        request.setIsChunked(httpRequest.isChunked());
        handleData(request, httpRequest.getContent());
      }
    }
  }

  @Override
  public void outgoingMessage(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
    if (event.getMessage() instanceof MappingHttpResponse) {
      MappingHttpResponse httpResponse = (MappingHttpResponse) event.getMessage();
      BaseMessage msg = (BaseMessage) httpResponse.getMessage();
      Binding binding = BindingManager.getBinding(super.getNamespace());
      if (msg != null) {
        final Tuple2<String,byte[]> encoded = S3ResponseEncoders.encode( msg );
        String contentType;
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream( );
        if ( encoded != null ) {
          contentType = encoded._1( );
          byteOut.write( encoded._2( ) );
        } else {
          contentType = "application/xml";
          byteOut.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>".getBytes( StandardCharsets.UTF_8 ));
          binding.toStream(byteOut, msg);
        }
        byte[] req = byteOut.toByteArray();
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(req);
        httpResponse.setHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(buffer.readableBytes()));
        httpResponse.setHeader(HttpHeaders.Names.CONTENT_TYPE, contentType);
        httpResponse.setHeader(HttpHeaders.Names.DATE, DateFormatter.dateToHeaderFormattedString(new Date()));
        httpResponse.setHeader("x-amz-request-id", msg.getCorrelationId());
        httpResponse.setContent(buffer);

      }
    }
  }

  public void handleData(ObjectStorageDataRequestType dataRequest, ChannelBuffer content) {
    ChannelBufferStreamingInputStream stream = new ChannelBufferStreamingInputStream(content);
    dataRequest.setData(stream);
  }

  protected abstract Map<String, String> populateOperationMap();

  protected abstract Map<String, String> populateUnsupportedOperationMap();

  @Override
  public Object bind(final MappingHttpRequest httpRequest) throws Exception {
    Map bindingArguments = new HashMap();
    final String operationName = getOperation(httpRequest, bindingArguments);
    if (operationName == null)
      throw new MethodNotAllowedException(httpRequest.getMethod().toString() + " " + httpRequest.getUri());

    Map<String, String> params = httpRequest.getParameters();

    OMElement msg;

    GroovyObject groovyMsg;
    Map<String, String> fieldMap;
    Class targetType;
    try {
      // :: try to create the target class :://
      targetType = ClassLoader.getSystemClassLoader().loadClass("com.eucalyptus.objectstorage.msgs.".concat(operationName).concat("Type"));
      if (!GroovyObject.class.isAssignableFrom(targetType)) {
        throw new Exception();
      }
      // :: get the map of parameters to fields :://
      fieldMap = this.buildFieldMap(targetType);
      // :: get an instance of the message :://
      groovyMsg = (GroovyObject) targetType.newInstance();
    } catch (Exception e) {
      throw new BindingException("Failed to construct message of type " + operationName);
    }

    addLogData((BaseMessage) groovyMsg, bindingArguments);

    // TODO: Refactor this to be more general
    List<String> failedMappings = populateObject(groovyMsg, fieldMap, params);
    populateObjectFromBindingMap(groovyMsg, fieldMap, httpRequest, bindingArguments);

    User user = Contexts.lookup(httpRequest.getCorrelationId()).getUser();
    setRequiredParams(groovyMsg, user);

    if (!params.isEmpty()) {
      // ignore params that are not consumed, EUCA-4840
      params.clear();
    }

    if (!failedMappings.isEmpty()) {
      StringBuilder errMsg = new StringBuilder("Failed to bind the following fields:\n");
      for (String f : failedMappings)
        errMsg.append(f).append('\n');
      for (Map.Entry<String, String> f : params.entrySet())
        errMsg.append(f.getKey()).append(" = ").append(f.getValue()).append('\n');
      throw new BindingException(errMsg.toString());
    }

    if (Logs.extreme().isTraceEnabled()) {
      Logs.extreme().trace(groovyMsg.toString());
    }
    try {
      Binding binding = BindingManager.getDefaultBinding();
      msg = binding.toOM(groovyMsg);
    } catch (RuntimeException e) {
      throw new BindingException("Failed to build a valid message: " + e.getMessage());
    }

    return groovyMsg;

  }

  protected void addLogData(BaseMessage eucaMsg, Map bindingArguments) {
    if (eucaMsg instanceof ObjectStorageRequestType) {
      String operation = (String) bindingArguments.remove("Operation");
      if (operation != null) {
        ObjectStorageRequestType request = (ObjectStorageRequestType) eucaMsg;
        BucketLogData logData = ObjectStorageBucketLogger.getInstance().makeLogEntry(UUID.randomUUID().toString());
        logData.setOperation("REST." + operation);
        request.setLogData(logData);
      }
    }
  }

  protected void setRequiredParams(final GroovyObject msg, User user) throws Exception {
    msg.setProperty("timeStamp", new Date());
  }

  protected String getOperation(MappingHttpRequest httpRequest, Map operationParams) throws Exception {
    String[] target = null;
    String path = OSGUtil.getOperationPath(httpRequest, ObjectStorageRESTPipeline.getServicePaths());
    boolean objectstorageInternalOperation = false;

    String hostBucket = null;
    if ((hostBucket = OSGUtil.getBucketFromHostHeader(httpRequest)) != null) {
      path = "/" + hostBucket + path;
    }

    if (path.length() > 0) {
      target = OSGUtil.getTarget(path);
    }

    String verb = httpRequest.getMethod().getName();
    String operationKey = "";
    Map<String, String> params = httpRequest.getParameters();
    String operationName = null;
    long contentLength = 0;
    String contentLengthString = httpRequest.getHeader(HttpHeaders.Names.CONTENT_LENGTH);
    if (contentLengthString != null) {
      contentLength = Long.parseLong(contentLengthString);
    }

    if (target == null) {
      // target = service
      operationKey = SERVICE + verb;
    } else if (target.length == 1) {
      // target = bucket
      if (!target[0].equals("")) {
        operationKey = BUCKET + verb;
        operationParams.put("Bucket", target[0]);
        operationParams.put("Operation", verb.toUpperCase() + "." + "BUCKET");
        if (AllowedCorsMethods.methodList.contains(HttpMethod.valueOf(verb)) &&
            httpRequest.getHeader(HttpHeaders.Names.ORIGIN) != null) {
          operationParams.put("Origin", httpRequest.getHeader(HttpHeaders.Names.ORIGIN));
          operationParams.put("HttpMethod", httpRequest.getMethod().getName());
        }
        if (verb.equals(ObjectStorageProperties.HTTPVerb.POST.toString())) {
          if (params.containsKey(ObjectStorageProperties.BucketParameter.delete.toString())) {
            operationParams.put("delete", getMultiObjectDeleteMessage(httpRequest));
          } else {
            // Handle POST form upload.
            /*
             * For multipart-form uploads we get the metadata from the form fields and the first data chunk from the "file" form field
             */
            Map formFields = httpRequest.getFormFields();
            String objectKey = null;
            String file = (String) formFields.get(ObjectStorageProperties.FormField.file.toString());
            if (formFields.containsKey(ObjectStorageProperties.FormField.key.toString())) {
              objectKey = (String) formFields.get(ObjectStorageProperties.FormField.key.toString());
              objectKey = objectKey.replaceAll("\\$\\{filename\\}", file);
              operationParams.put("Key", objectKey);
            }
            if (formFields.containsKey(ObjectStorageProperties.FormField.acl.toString())) {
              String acl = (String) formFields.get(ObjectStorageProperties.FormField.acl.toString());
              httpRequest.addHeader(ObjectStorageProperties.AMZ_ACL, acl);
            }
            if (formFields.containsKey(ObjectStorageProperties.FormField.redirect.toString())) {
              String successActionRedirect = (String) formFields.get(ObjectStorageProperties.FormField.redirect.toString());
              operationParams.put("SuccessActionRedirect", successActionRedirect);
            }
            if (formFields.containsKey(ObjectStorageProperties.FormField.success_action_redirect.toString())) {
              String successActionRedirect = (String) formFields.get(ObjectStorageProperties.FormField.success_action_redirect.toString());
              operationParams.put("SuccessActionRedirect", successActionRedirect);
            }
            if (formFields.containsKey(ObjectStorageProperties.FormField.success_action_status.toString())) {
              Integer successActionStatus =
                  Integer.parseInt((String) formFields.get(ObjectStorageProperties.FormField.success_action_status.toString()));
              if (successActionStatus == 200 || successActionStatus == 201) {
                operationParams.put("SuccessActionStatus", successActionStatus);
              } else {
                // Default is 204, as per S3 spec
                operationParams.put("SuccessActionStatus", 204);
              }
            } else {
              operationParams.put("SuccessActionStatus", 204);
            }

            // Get the content-type of the upload content, not the form itself
            if (formFields.get(ObjectStorageProperties.FormField.Content_Type.toString()) != null) {
              operationParams.put("ContentType", formFields.get(ObjectStorageProperties.FormField.Content_Type.toString()));
            }

            if (formFields.get(ObjectStorageProperties.FormField.x_ignore_filecontentlength.toString()) != null) {
              operationParams.put("ContentLength", (long) formFields.get(ObjectStorageProperties.FormField.x_ignore_filecontentlength.toString()));
            } else {
              throw new MalformedPOSTRequestException(null, "Could not calculate upload content length from request");
              // if(contentLengthString != null) {
              // operationParams.put("ContentLength", (new Long(contentLength).toString()));
              // }
            }
            key = target[0] + "." + objectKey;

            // Verify the message content is found.
            ChannelBuffer buffer = (ChannelBuffer) formFields.get(ObjectStorageProperties.FormField.x_ignore_firstdatachunk.toString());
            if (buffer == null) {
              // No content found.
              throw new MalformedPOSTRequestException(null, "No upload content found");
            }
          }
        } else if (ObjectStorageProperties.HTTPVerb.PUT.toString().equals(verb)) {
          if (params.containsKey(ObjectStorageProperties.BucketParameter.logging.toString())) {
            // read logging params
            getTargetBucketParams(operationParams, httpRequest);
          } else if (params.containsKey(ObjectStorageProperties.BucketParameter.versioning.toString())) {
            getVersioningStatus(operationParams, httpRequest);
          }
        } else if (ObjectStorageProperties.HTTPVerb.OPTIONS.toString().equals(verb)) {
            operationParams.put("preflightRequest", processPreflightRequest(httpRequest));
        }
      } else {
        operationKey = SERVICE + verb;
      }
    } else {
      // target = object
      operationKey = OBJECT + verb;
      String objectKey = target[1];

      try {
        objectKey = OSGUtil.URLdecode(objectKey);
      } catch (UnsupportedEncodingException e) {
        throw new BindingException("Unable to get key: " + e.getMessage());
      }

      operationParams.put("Bucket", target[0]);
      operationParams.put("Key", objectKey);
      operationParams.put("Operation", verb.toUpperCase() + "." + "OBJECT");

      if (AllowedCorsMethods.methodList.contains(HttpMethod.valueOf(verb)) &&
          httpRequest.getHeader(HttpHeaders.Names.ORIGIN) != null) {
        operationParams.put("Origin", httpRequest.getHeader(HttpHeaders.Names.ORIGIN));
        operationParams.put("HttpMethod", httpRequest.getMethod().getName());
      }

      if (!params.containsKey(ObjectStorageProperties.BucketParameter.acl.toString())) {
        if (verb.equals(ObjectStorageProperties.HTTPVerb.PUT.toString())) {
          if (httpRequest.containsHeader(ObjectStorageProperties.COPY_SOURCE.toString())) {
            String copySource = httpRequest.getHeader(ObjectStorageProperties.COPY_SOURCE.toString());
            try {
              copySource = OSGUtil.URLdecode(copySource);
            } catch (UnsupportedEncodingException ex) {
              throw new BindingException("Unable to decode copy source: " + copySource);
            }
            String[] sourceParts = copySource.split("\\?");
            if (sourceParts.length > 1) {
              operationParams.put("SourceVersionId", sourceParts[1].replaceFirst("versionId=", "").trim());
            }
            copySource = sourceParts[0];
            String[] sourceTarget = OSGUtil.getTarget(copySource);
            String sourceObjectKey = "";
            if (sourceTarget != null && sourceTarget.length > 1) {
              sourceObjectKey = sourceTarget[1];

              operationParams.put("SourceBucket", sourceTarget[0]);
              operationParams.put("SourceObject", sourceObjectKey);
              operationParams.put("DestinationBucket", operationParams.remove("Bucket"));
              operationParams.put("DestinationObject", operationParams.remove("Key"));

              String metaDataDirective = httpRequest.getHeader(ObjectStorageProperties.METADATA_DIRECTIVE.toString());
              if (metaDataDirective != null) {
                operationParams.put("MetadataDirective", metaDataDirective);
              }

              operationKey += ObjectStorageProperties.COPY_SOURCE.toString();
              Set<String> headerNames = httpRequest.getHeaderNames();
              for (String key : headerNames) {
                if (key.startsWith("x-amz-")) {
                  String stripped = key.replaceFirst("x-amz-", "");
                  for (ObjectStorageProperties.CopyHeaders header : ObjectStorageProperties.CopyHeaders.values()) {
                    if (stripped.replaceAll("-", "").equals(header.toString().toLowerCase())) {
                      String value = httpRequest.getHeader(key);
                      parseExtendedHeaders(operationParams, header.toString(), value);
                    }
                  }
                }
              }
            } else {
              throw new BindingException("Malformed COPY request");
            }

          } else {
            // handle PUTs
            key = target[0] + "." + objectKey;
            String contentType = httpRequest.getHeader(HttpHeaders.Names.CONTENT_TYPE);
            if (contentType != null)
              operationParams.put("ContentType", contentType);
            String contentDisposition = httpRequest.getHeader("Content-Disposition");
            if (contentDisposition != null)
              operationParams.put("ContentDisposition", contentDisposition);
            String contentMD5 = httpRequest.getHeader(HttpHeaders.Names.CONTENT_MD5);
            if (contentMD5 != null)
              operationParams.put("ContentMD5", contentMD5);
            if (contentLengthString != null)
              operationParams.put("ContentLength", (new Long(contentLength).toString()));
          }
          copyHeadersForStoring(operationParams, httpRequest);
        } else if (verb.equals(ObjectStorageProperties.HTTPVerb.GET.toString())) {
          if (!objectstorageInternalOperation) {

            if (params.containsKey("torrent")) {
              operationParams.put("GetTorrent", Boolean.TRUE);
            } else {
              if (!params.containsKey("uploadId")) {
                operationParams.put("InlineData", Boolean.FALSE);
                operationParams.put("GetMetaData", Boolean.TRUE);
              }
            }

            Set<String> headerNames = httpRequest.getHeaderNames();
            boolean isExtendedGet = false;
            for (String key : headerNames) {
              for (ObjectStorageProperties.ExtendedGetHeaders header : ObjectStorageProperties.ExtendedGetHeaders.values()) {
                if (key.replaceAll("-", "").equals(header.toString())) {
                  String value = httpRequest.getHeader(key);
                  isExtendedGet = true;
                  parseExtendedHeaders(operationParams, header.toString(), value);
                }
              }

            }
            if (isExtendedGet) {
              operationKey += "extended";
              // only supported through SOAP
              operationParams.put("ReturnCompleteObjectOnConditionFailure", Boolean.FALSE);
            }
          }
          if (params.containsKey(ObjectStorageProperties.GetOptionalParameters.IsCompressed.toString())) {
            Boolean isCompressed = Boolean.parseBoolean(params.remove(ObjectStorageProperties.GetOptionalParameters.IsCompressed.toString()));
            operationParams.put("IsCompressed", isCompressed);
          }
          Map<String, String> responseHeaderOverrides = Maps.newHashMap();
          for (String paramName : params.keySet()) {
            if (paramName != null && !"".equals(paramName) && paramName.startsWith("response-")) {
              String paramValue = params.get(paramName);
              if (paramValue != null && !"".equals(paramValue)) {
                responseHeaderOverrides.put(paramName, params.get(paramName));
              }
            }
          }
          if (responseHeaderOverrides.size() > 0) {
            operationParams.put("ResponseHeaderOverrides", responseHeaderOverrides);
          }
        } else if (verb.equals(ObjectStorageProperties.HTTPVerb.POST.toString())) {
          if (params.containsKey("uploadId")) {
            operationParams.put("Parts", getPartsList(httpRequest));
          }
        }
      }
      if (params.containsKey(ObjectStorageProperties.ObjectParameter.versionId.toString())) {
        if (!verb.equals(ObjectStorageProperties.HTTPVerb.DELETE.toString()))
          operationParams.put("VersionId", params.remove(ObjectStorageProperties.ObjectParameter.versionId.toString()));
      }
    }

    if (verb.equals(ObjectStorageProperties.HTTPVerb.PUT.toString()) && params.containsKey(ObjectStorageProperties.BucketParameter.acl.toString())) {
      operationParams.put("AccessControlPolicy", getAccessControlPolicy(httpRequest));
    }

    if (verb.equals(ObjectStorageProperties.HTTPVerb.PUT.toString())
        && params.containsKey(ObjectStorageProperties.BucketParameter.lifecycle.toString())) {
      operationParams.put("lifecycleConfiguration", getLifecycle(httpRequest));
    }

    if (verb.equals(ObjectStorageProperties.HTTPVerb.PUT.toString())
        && params.containsKey(ObjectStorageProperties.BucketParameter.tagging.toString())) {
      operationParams.put("taggingConfiguration", getTagging(httpRequest));
    }

    if (verb.equals(ObjectStorageProperties.HTTPVerb.PUT.toString()) && params.containsKey(ObjectStorageProperties.BucketParameter.cors.toString())) {
      operationParams.put("corsConfiguration", getCors(httpRequest));
    }

    if (verb.equals(ObjectStorageProperties.HTTPVerb.OPTIONS.toString())) {
        operationParams.put("preflightRequest", processPreflightRequest(httpRequest));
      }

    if (verb.equals(ObjectStorageProperties.HTTPVerb.PUT.toString())
        && params.containsKey(ObjectStorageProperties.BucketParameter.policy.toString())) {
      operationParams.put("policy", getMessageString(httpRequest));
    }

    ArrayList paramsToRemove = new ArrayList();

    params:
    for ( final Map.Entry<String,String> parameterEntry : params.entrySet( ) ) {
      final String key = parameterEntry.getKey( );
      final String value = parameterEntry.getValue( );
      String keyString = key;
      boolean dontIncludeParam = false;
      for (SecurityParameter securityParam : SecurityParameter.values()) {
        if (keyString.equals(securityParam.toString().toLowerCase())) {
          dontIncludeParam = true;
          break;
        }
      }
      if (!dontIncludeParam) {
        if (value != null) {
          String[] keyStringParts = keyString.split("-");
          if (keyStringParts.length > 1) {
            keyString = "";
            for (int i = 0; i < keyStringParts.length; ++i) {
              keyString += toUpperFirst(keyStringParts[i]);
            }
          } else {
            keyString = toUpperFirst(keyString);
          }
        }
        dontIncludeParam = true;
        if (operationKey.startsWith(SERVICE)) {
          for (ObjectStorageProperties.ServiceParameter param : ObjectStorageProperties.ServiceParameter.values()) {
            if (keyString.toLowerCase().equals(param.toString().toLowerCase())) {
              dontIncludeParam = false;
              break;
            }
          }
        } else if (operationKey.startsWith(BUCKET)) {
          for (ObjectStorageProperties.BucketParameter param : ObjectStorageProperties.BucketParameter.values()) {
            if (keyString.toLowerCase().equals(param.toString().toLowerCase())) {
              dontIncludeParam = false;
              break;
            }
          }
        } else if (operationKey.startsWith(OBJECT)) {
          for (ObjectStorageProperties.ObjectParameter param : ObjectStorageProperties.ObjectParameter.values()) {
            if (keyString.toLowerCase().equals(param.toString().toLowerCase())) {
              dontIncludeParam = false;
              break;
            }
          }
        }
        if (dontIncludeParam) {
          paramsToRemove.add(key);
        }
      }
      if (dontIncludeParam)
        continue;
      // Add subresource params to the operationKey
      for (ObjectStorageProperties.SubResource subResource : ObjectStorageProperties.SubResource.values()) {
        if (keyString.toLowerCase().equals(subResource.toString().toLowerCase())) {
          operationKey += keyString.toLowerCase();
          if ( Strings.isNullOrEmpty( value ) ) {
            paramsToRemove.add(key);
            continue params;
          }
        }
      }

      if (value != null) {
        operationParams.put(keyString, value);
      }

      /*
       * if(addMore) { //just add the first one to the key operationKey += keyString.toLowerCase(); addMore = false; }
       */

      paramsToRemove.add(key);
    }

    for (Object key : paramsToRemove) {
      params.remove(key);
    }

    if (!objectstorageInternalOperation) {
      operationName = operationMap.get(operationKey);
    }

    if (operationName == null) {
      String unsupportedOp = unsupportedOperationMap.get(operationKey);
      if (unsupportedOp != null) {
        String resourceType = null;
        String resource = null;
        if (target.length < 2) {
          resourceType = BUCKET;
          resource = target[0];
        } else {
          resourceType = OBJECT;
          String delimiter = new String();
          for (int i = 1; i < target.length; ++i) {
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

    if ("CreateBucket".equals(operationName)) {
      String locationConstraint = getLocationConstraint(httpRequest);
      if (locationConstraint != null)
        operationParams.put("LocationConstraint", locationConstraint);
    }
    return operationName;
  }

  private static final Ordering<String> STRING_COMPARATOR = Ordering.natural();

  protected List<String> responseHeadersForStoring = Collections.unmodifiableList(STRING_COMPARATOR.sortedCopy(Lists.newArrayList(
      // per REST API PUT Object docs as of 10/13/2014
      HttpHeaders.Names.CACHE_CONTROL, "Content-Disposition", // strangely not included
      HttpHeaders.Names.CONTENT_ENCODING, HttpHeaders.Names.CONTENT_LENGTH,
      // HttpHeaders.Names.CONTENT_MD5, // handled elsewhere
      HttpHeaders.Names.CONTENT_TYPE,
      // HttpHeaders.Names.EXPECT, // handled elsewhere
      HttpHeaders.Names.EXPIRES)));

  protected void copyHeadersForStoring(Map operationParams, MappingHttpRequest httpRequest) {
    Map<String, String> headersToStore = Maps.newHashMap();
    List<Map.Entry<String, String>> headersInRequest = httpRequest.getHeaders();
    for (Map.Entry<String, String> entry : headersInRequest) {
      int foundIdx = Collections.binarySearch(responseHeadersForStoring, entry.getKey(), STRING_COMPARATOR);
      if (foundIdx >= 0) {
        headersToStore.put(entry.getKey(), entry.getValue());
      }
    }
    if (headersToStore != null && headersToStore.size() > 0) {
      operationParams.put("copiedHeaders", headersToStore);
    }
  }

  protected void getTargetBucketParams(Map operationParams, MappingHttpRequest httpRequest) throws S3Exception, BindingException {
    String message = getMessageString(httpRequest);
    if (message.length() > 0) {
      try {
        XMLParser xmlParser = new XMLParser(message);
        String targetBucket = xmlParser.getValue("//TargetBucket");
        if (targetBucket == null || targetBucket.length() == 0)
          return;
        String targetPrefix = xmlParser.getValue("//TargetPrefix");
        ArrayList<Grant> grants = new ArrayList<Grant>();

        List<String> permissions = xmlParser.getValues("//TargetGrants/Grant/Permission");
        if (permissions == null)
          throw new MalformedACLErrorException("/TargetGrants/Grant/Permission");

        DTMNodeList grantees = xmlParser.getNodes("//TargetGrants/Grant/Grantee");
        if (grantees == null)
          throw new MalformedACLErrorException("//TargetGrants/Grant/Grantee");

        for (int i = 0; i < grantees.getLength(); ++i) {
          String id = xmlParser.getValue(grantees.item(i), "ID");
          if (id.length() > 0) {
            String canonicalUserName = xmlParser.getValue(grantees.item(i), "DisplayName");
            Grant grant = new Grant();
            Grantee grantee = new Grantee();
            grantee.setCanonicalUser(new CanonicalUser(id, canonicalUserName));
            grant.setGrantee(grantee);
            grant.setPermission(permissions.get(i));
            grants.add(grant);
          } else {
            String groupUri = xmlParser.getValue(grantees.item(i), "URI");
            if (groupUri.length() == 0)
              throw new MalformedACLErrorException("ACL group URI");
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
      } catch (S3Exception e) {
        throw e;
      } catch (Exception ex) {
        MalformedXMLException malEx = new MalformedXMLException("//TargetBucket");
        malEx.initCause(ex);
        throw malEx;
      }
    }
  }

  protected void getVersioningStatus(Map operationParams, MappingHttpRequest httpRequest) throws S3Exception, BindingException {
    String message = getMessageString(httpRequest);
    if (message.length() > 0) {
      try {
        XMLParser xmlParser = new XMLParser(message);
        String status = xmlParser.getValue("//Status");
        if (status == null || status.length() == 0)
          return;
        operationParams.put("VersioningStatus", status);
      } catch (Exception ex) {
        MalformedXMLException malEx = new MalformedXMLException(message);
        malEx.initCause(ex);
        throw malEx;
      }
    }
  }

  protected void parseExtendedHeaders(Map operationParams, String headerString, String value) throws BindingException {
    if (headerString.equals(ObjectStorageProperties.ExtendedGetHeaders.Range.toString())) {
      // Examples: bytes=0-499, bytes=-500, bytes=500-, bytes=1000-500, bytes=5--1, bytes=-5-1, bytes=-5--1
      Matcher matcher = ObjectStorageProperties.RANGE_HEADER_PATTERN.matcher(value);
      if (matcher.matches()) {
        if (matcher.groupCount() > 0) {
          Long start = null;
          Long end = null;
          boolean parseError = false;
          for (int i = 1; i <= matcher.groupCount(); i++) {
            String position = matcher.group(i);
            if (StringUtils.isNotBlank(position)) {
              try {
                if (i == 1) { // begin position
                  start = Long.parseLong(position);
                } else { // end position
                  end = Long.parseLong(position);
                }
              } catch (NumberFormatException e) {
                LOG.debug("Cannot parse string \"" + position + "\" to long ");
                parseError = true;
                break;
              }
            }
          }
          if (!parseError) { // if there was an error parsing, don't populate headers
            if (start != null) {
              operationParams.put(ObjectStorageProperties.ExtendedHeaderRangeTypes.ByteRangeStart.toString(), start);
            }
            if (end != null) {
              operationParams.put(ObjectStorageProperties.ExtendedHeaderRangeTypes.ByteRangeEnd.toString(), end);
            }
          } else {
            LOG.debug("Dropping the Range header since its value: " + value + " cannot be parsed");
          }
        } else {
          LOG.debug("Dropping the Range header since its value: " + value + " does not contain any matching groups against the pattern: "
              + ObjectStorageProperties.RANGE_HEADER_PATTERN.toString());
        }
      } else {
        LOG.debug("Dropping the Range header since its value: " + value + " does not match pattern: "
            + ObjectStorageProperties.RANGE_HEADER_PATTERN.toString());
      }
    } else if (ObjectStorageProperties.ExtendedHeaderDateTypes.contains(headerString)) {
      try {
        List<String> dateFormats = new ArrayList<String>();
        dateFormats.add(DateUtil.PATTERN_RFC1123);
        operationParams.put(headerString, DateUtil.parseDate(value, dateFormats));
      } catch (Exception ex) {
        try {
          operationParams.put(headerString, DateUtils.parseIso8601DateTime(value));
        } catch (ParseException e) {
          throw new BindingException("Error parsing date: " + value, e);
        }
      }
    } else {
      operationParams.put(headerString, StringUtils.strip(value, "\""));
    }
  }

  protected AccessControlPolicy getAccessControlPolicy(MappingHttpRequest httpRequest) throws S3Exception, BindingException {
    AccessControlPolicy accessControlPolicy = new AccessControlPolicy();
    AccessControlList accessControlList = new AccessControlList();
    ArrayList<Grant> grants = new ArrayList<Grant>();
    String aclString = "";
    try {
      aclString = getMessageString(httpRequest);
      if (aclString.length() > 0) {
        XMLParser xmlParser = new XMLParser(aclString);
        String ownerId = xmlParser.getValue("//Owner/ID");
        String displayName = xmlParser.getValue("//Owner/DisplayName");

        CanonicalUser canonicalUser = new CanonicalUser(ownerId, displayName);
        accessControlPolicy.setOwner(canonicalUser);

        List<String> permissions = xmlParser.getValues("//AccessControlList/Grant/Permission");
        if (permissions == null) {
          throw new MalformedACLErrorException("//AccessControlList/Grant/Permission");
        }
        DTMNodeList grantees = xmlParser.getNodes("//AccessControlList/Grant/Grantee");
        if (grantees == null) {
          throw new MalformedACLErrorException("//AccessControlList/Grant/Grantee");
        }
        for (int i = 0; i < grantees.getLength(); ++i) {
          String id = xmlParser.getValue(grantees.item(i), "ID");
          if (id.length() > 0) {
            String canonicalUserName = xmlParser.getValue(grantees.item(i), "DisplayName");
            Grant grant = new Grant();
            Grantee grantee = new Grantee();
            grantee.setCanonicalUser(new CanonicalUser(id, canonicalUserName));
            grant.setGrantee(grantee);
            grant.setPermission(permissions.get(i));
            grants.add(grant);
          } else if (!"".equals(xmlParser.getValue(grantees.item(i), "EmailAddress"))) {
            String canonicalUserName = xmlParser.getValue(grantees.item(i), "DisplayName");
            Grant grant = new Grant();
            Grantee grantee = new Grantee();
            String email = xmlParser.getValue(grantees.item(i), "EmailAddress");
            grantee.setEmailAddress(email);
            grant.setGrantee(grantee);
            grant.setPermission(permissions.get(i));
            grants.add(grant);
          } else {
            String groupUri = xmlParser.getValue(grantees.item(i), "URI");
            if (groupUri.length() == 0) {
              throw new MalformedACLErrorException("Group-URI");
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
    } catch (S3Exception e) {
      // pass it up
      throw e;
    } catch (Exception ex) {
      throw new MalformedACLErrorException(aclString);
    }
    accessControlList.setGrants(grants);
    accessControlPolicy.setAccessControlList(accessControlList);
    return accessControlPolicy;
  }

  private ArrayList<Part> getPartsList(MappingHttpRequest httpRequest) throws BindingException {
    ArrayList<Part> partList = new ArrayList<Part>();
    try {
      String partsString = getMessageString(httpRequest);
      if (partsString.length() > 0) {
        XMLParser xmlParser = new XMLParser(partsString);
        DTMNodeList partNodes = xmlParser.getNodes("/CompleteMultipartUpload/Part");
        if (partNodes == null) {
          throw new BindingException("Malformed part list");
        }

        for (int i = 0; i < partNodes.getLength(); i++) {
          Part part = new Part(Integer.parseInt(xmlParser.getValue(partNodes.item(i), "PartNumber")), xmlParser.getValue(partNodes.item(i), "ETag"));
          partList.add(part);
        }
      }
    } catch (Exception ex) {
      throw new BindingException("Unable to parse part list " + ex.getMessage());
    }
    return partList;
  }

  protected String getLocationConstraint(MappingHttpRequest httpRequest) throws S3Exception {
    String locationConstraint = null;
    try {
      String bucketConfiguration = getMessageString(httpRequest);
      if (bucketConfiguration.length() > 0) {
        XMLParser xmlParser = new XMLParser(bucketConfiguration);
        locationConstraint = xmlParser.getValue("/CreateBucketConfiguration/LocationConstraint");
      }
    } catch (Exception ex) {
      MalformedXMLException malEx = new MalformedXMLException("/CreateBucketConfiguration/LocationConstraint");
      malEx.initCause(ex);
      throw malEx;

    }
    return locationConstraint;
  }

  protected List<String> populateObject(final GroovyObject obj, final Map<String, String> paramFieldMap, final Map<String, String> params) {
    List<String> failedMappings = new ArrayList<String>();
    for (Map.Entry<String, String> e : paramFieldMap.entrySet()) {
      try {
        if (obj.getClass().getDeclaredField(e.getValue()).getType().equals(ArrayList.class))
          failedMappings.addAll(populateObjectList(obj, e, params, params.size()));
      } catch (NoSuchFieldException e1) {
        failedMappings.add(e.getKey());
      }
    }
    for (Map.Entry<String, String> e : paramFieldMap.entrySet()) {
      if (params.containsKey(e.getKey()) && !populateObjectField(obj, e, params))
        failedMappings.add(e.getKey());
    }
    return failedMappings;
  }

  protected void populateObjectFromBindingMap(final GroovyObject obj, final Map<String, String> paramFieldMap, final MappingHttpRequest httpRequest,
      final Map bindingMap) throws S3Exception, BindingException {
    // process headers
    // String aclString = httpRequest.getAndRemoveHeader(ObjectStorageProperties.AMZ_ACL);
    // if (aclString != null) {
    // addAccessControlList(obj, paramFieldMap, bindingMap, aclString);
    // }
    // above logic only accounts for x-amz-acl. x-amz-grant-* headers are dropped
    processHeaderGrants(obj, paramFieldMap, bindingMap, httpRequest);

    // add meta data
    String metaDataString = paramFieldMap.remove("MetaData");
    if (metaDataString != null) {
      Set<String> headerNames = httpRequest.getHeaderNames();
      ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
      for (String key : headerNames) {
        if (key.toLowerCase().startsWith(ObjectStorageProperties.AMZ_META_HEADER_PREFIX)) {
          MetaDataEntry metaDataEntry = new MetaDataEntry();
          metaDataEntry.setName(key.substring(ObjectStorageProperties.AMZ_META_HEADER_PREFIX.length()));
          metaDataEntry.setValue(httpRequest.getHeader(key));
          metaData.add(metaDataEntry);
        }
      }
      obj.setProperty(metaDataString, metaData);
    }

    // populate from binding map (required params)
    Iterator bindingMapIterator = bindingMap.keySet().iterator();
    while (bindingMapIterator.hasNext()) {
      String key = (String) bindingMapIterator.next();
      obj.setProperty(key.substring(0, 1).toLowerCase().concat(key.substring(1)), bindingMap.get(key));
    }
  }

  protected boolean populateObjectField(final GroovyObject obj, final Map.Entry<String, String> paramFieldPair, final Map<String, String> params) {
    try {
      Class declaredType = obj.getClass().getDeclaredField(paramFieldPair.getValue()).getType();
      if (declaredType.equals(String.class))
        obj.setProperty(paramFieldPair.getValue(), params.remove(paramFieldPair.getKey()));
      else if (declaredType.getName().equals("int"))
        obj.setProperty(paramFieldPair.getValue(), Integer.parseInt(params.remove(paramFieldPair.getKey())));
      else if (declaredType.equals(Integer.class))
        obj.setProperty(paramFieldPair.getValue(), new Integer(params.remove(paramFieldPair.getKey())));
      else if (declaredType.getName().equals("boolean"))
        obj.setProperty(paramFieldPair.getValue(), Boolean.parseBoolean(params.remove(paramFieldPair.getKey())));
      else if (declaredType.equals(Boolean.class))
        obj.setProperty(paramFieldPair.getValue(), new Boolean(params.remove(paramFieldPair.getKey())));
      else if (declaredType.getName().equals("long"))
        obj.setProperty(paramFieldPair.getValue(), Long.parseLong(params.remove(paramFieldPair.getKey())));
      else if (declaredType.equals(Long.class))
        obj.setProperty(paramFieldPair.getValue(), new Long(params.remove(paramFieldPair.getKey())));
      else
        return false;
      return true;
    } catch (Exception e1) {
      return false;
    }
  }

  protected List<String> populateObjectList(final GroovyObject obj, final Map.Entry<String, String> paramFieldPair, final Map<String, String> params,
      final int paramSize) {
    List<String> failedMappings = new ArrayList<String>();
    try {
      Field declaredField = obj.getClass().getDeclaredField(paramFieldPair.getValue());
      ArrayList theList = (ArrayList) obj.getProperty(paramFieldPair.getValue());
      Class genericType = (Class) ((ParameterizedType) declaredField.getGenericType()).getActualTypeArguments()[0];
      // :: simple case: FieldName.# :://
      if (String.class.equals(genericType)) {
        if (params.containsKey(paramFieldPair.getKey())) {
          theList.add(params.remove(paramFieldPair.getKey()));
        } else {
          List<String> keys = Lists.newArrayList(params.keySet());
          for (String k : keys) {
            if (k.matches(paramFieldPair.getKey() + "\\.\\d*")) {
              theList.add(params.remove(k));
            }
          }
        }
      } else if (declaredField.isAnnotationPresent(HttpEmbedded.class)) {
        HttpEmbedded annoteEmbedded = (HttpEmbedded) declaredField.getAnnotation(HttpEmbedded.class);
        // :: build the parameter map and call populate object recursively :://
        if (annoteEmbedded.multiple()) {
          String prefix = paramFieldPair.getKey();
          List<String> embeddedListFieldNames = new ArrayList<String>();
          for (String actualParameterName : params.keySet())
            if (actualParameterName.matches(prefix + ".1.*"))
              embeddedListFieldNames.add(actualParameterName.replaceAll(prefix + ".1.", ""));
          for (int i = 0; i < paramSize + 1; i++) {
            boolean foundAll = true;
            Map<String, String> embeddedParams = new HashMap<String, String>();
            for (String fieldName : embeddedListFieldNames) {
              String paramName = prefix + "." + i + "." + fieldName;
              if (!params.containsKey(paramName)) {
                failedMappings.add("Mismatched embedded field: " + paramName);
                foundAll = false;
              } else
                embeddedParams.put(fieldName, params.get(paramName));
            }
            if (foundAll)
              failedMappings.addAll(populateEmbedded(genericType, embeddedParams, theList));
            else
              break;
          }
        } else
          failedMappings.addAll(populateEmbedded(genericType, params, theList));
      }
    } catch (Exception e1) {
      failedMappings.add(paramFieldPair.getKey());
    }
    return failedMappings;
  }

  protected List<String> populateEmbedded(final Class genericType, final Map<String, String> params, final ArrayList theList)
      throws InstantiationException, IllegalAccessException {
    GroovyObject embedded = (GroovyObject) genericType.newInstance();
    Map<String, String> embeddedFields = buildFieldMap(genericType);
    int startSize = params.size();
    List<String> embeddedFailures = populateObject(embedded, embeddedFields, params);
    if (embeddedFailures.isEmpty() && !(params.size() - startSize == 0))
      theList.add(embedded);
    return embeddedFailures;
  }

  protected Map<String, String> buildFieldMap(final Class targetType) {
    Map<String, String> fieldMap = new HashMap<String, String>();
    Field[] fields = targetType.getDeclaredFields();
    for (Field f : fields)
      if (Modifier.isStatic(f.getModifiers()))
        continue;
      else if (f.isAnnotationPresent(HttpParameterMapping.class)) {
        for (final String parameter : f.getAnnotation(HttpParameterMapping.class).parameter())
          fieldMap.put(parameter, f.getName());
        fieldMap.put(f.getName().substring(0, 1).toUpperCase().concat(f.getName().substring(1)), f.getName());
      } else
        fieldMap.put(f.getName().substring(0, 1).toUpperCase().concat(f.getName().substring(1)), f.getName());
    return fieldMap;
  }

  protected static void validateCannedAcl(String cannedAcl) throws InvalidArgumentException {
    if (!AclUtils.isValidCannedAcl(cannedAcl)) {
      throw new InvalidArgumentException(cannedAcl);
    }
  }

  protected static void processHeaderGrants(final GroovyObject obj, final Map<String, String> paramFieldMap, Map bindingMap,
      MappingHttpRequest httpRequest) throws S3Exception {

    if (paramFieldMap.containsKey("AccessControlList") || paramFieldMap.containsKey("AccessControlPolicy")) {

      ArrayList<Grant> grants = new ArrayList<Grant>();

      // Parse and construct grant from x-amz-acl in header
      String cannedACLString = httpRequest.getAndRemoveHeader(ObjectStorageProperties.AMZ_ACL);
      if (!Strings.isNullOrEmpty(cannedACLString)) {
        validateCannedAcl(cannedACLString);
        grants.add(new Grant(new Grantee(new CanonicalUser("", "")), cannedACLString));
      }

      // Parse and construct grants from x-amz-grant-* headers, examples:
      // x-amz-grant-read: emailAddress="xyz@amazon.com", uri="http://some-uri", id="canonical-id"
      // x-amz-grant-write: emailAddress="xyz@amazon.com", uri="http://some-uri", id="canonical-id"

      for (Map.Entry<X_AMZ_GRANT, Permission> mapEntry : ObjectStorageProperties.HEADER_PERMISSION_MAP.entrySet()) {

        String headerValue = httpRequest.getAndRemoveHeader(mapEntry.getKey().toString());

        if (StringUtils.isNotBlank(headerValue)) {

          if (ObjectStorageProperties.GRANT_HEADER_PATTERN.matcher(headerValue).matches()) {

            List<String[]> gpList = null;

            try {
              gpList = ObjectStorageProperties.GRANT_HEADER_PARSER.apply(headerValue);
            } catch (Exception e) {
              LOG.debug("Ignoring header: " + mapEntry.getKey().toString() + ". Unable to parse value: " + headerValue, e);
              throw new InvalidArgumentException().withArgumentName(mapEntry.getKey().toString()).withArgumentValue(headerValue);
            }

            if (gpList != null && !gpList.isEmpty()) {
              for (String[] gp : gpList) {
                switch (gp[0]) {
                  case "emailAddress":
                    grants.add(new Grant(new Grantee(gp[1]), mapEntry.getValue().toString()));
                    break;
                  case "id":
                    grants.add(new Grant(new Grantee(new CanonicalUser(gp[1], "")), mapEntry.getValue().toString()));
                    break;
                  case "uri":
                    grants.add(new Grant(new Grantee(new Group(gp[1])), mapEntry.getValue().toString()));
                    break;
                  default:
                    throw new InvalidArgumentException().withArgumentName(mapEntry.getKey().toString()).withArgumentValue(headerValue);
                }
              }
            } else {
              LOG.debug("Ignoring header: " + mapEntry.getKey().toString() + ". Value is invalid: " + headerValue);
              throw new InvalidArgumentException().withArgumentName(mapEntry.getKey().toString()).withArgumentValue(headerValue);
            }
          } else {
            LOG.debug("Cannot parse header: " + mapEntry.getKey().toString() + ", value:  " + headerValue + ". Value does not match pattern: "
                + ObjectStorageProperties.GRANT_HEADER_PATTERN.toString());
            throw new InvalidArgumentException().withArgumentName(mapEntry.getKey().toString()).withArgumentValue(headerValue);
          }
        } else {
          // header not included, nothing to do here
        }
      }

      if (!grants.isEmpty()) {

        // Objects can either contain ACL or ACP
        if (paramFieldMap.containsKey("AccessControlList")) { // Object has ACL

          AccessControlList accessControlList;

          // Set up the AccessControlList in the binding map
          if (bindingMap.containsKey("AccessControlList")) {// ACL only comes from the headers, nevertheless check
            accessControlList = (AccessControlList) bindingMap.get("AccessControlList");
          } else {
            accessControlList = new AccessControlList();
            bindingMap.put("AccessControlList", accessControlList);
          }

          accessControlList.getGrants().addAll(grants);

        } else { // Object has ACP

          AccessControlPolicy accessControlPolicy;

          // Set up the AccessControlPolicy in the binding map
          if (bindingMap.containsKey("AccessControlPolicy")) { // ACP could come from request body
            accessControlPolicy = (AccessControlPolicy) bindingMap.get("AccessControlPolicy");
          } else {
            accessControlPolicy = new AccessControlPolicy(new CanonicalUser("", ""), new AccessControlList());
            bindingMap.put("AccessControlPolicy", accessControlPolicy);
          }

          accessControlPolicy.getAccessControlList().getGrants().addAll(grants);
        }
      } else {
        // no new grants to add, let the ACL and ACP be
      }
    } else {
      // nothing to do here as the result class definition does not contain ACL or ACP
    }
  }

  protected String toUpperFirst(String string) {
    return string.substring(0, 1).toUpperCase().concat(string.substring(1));
  }

  protected String getMessageString(MappingHttpRequest httpRequest) {
    ChannelBuffer buffer = httpRequest.getContent();
    buffer.markReaderIndex();
    byte[] read = new byte[buffer.readableBytes()];
    buffer.readBytes(read);
    return new String(read);
  }

  private TaggingConfiguration getTagging(MappingHttpRequest httpRequest) throws S3Exception {
    TaggingConfiguration taggingConfiguration = new TaggingConfiguration();
    BucketTagSet tagSet = new BucketTagSet();
    tagSet.setBucketTags(new ArrayList<BucketTag>());
    taggingConfiguration.setBucketTagSet(tagSet);

    String message = getMessageString(httpRequest);

    if (message.length() > 0) {
      try {
        XMLParser xmlParser = new XMLParser(message);
        DTMNodeList bucketTagSets = xmlParser.getNodes("//Tagging/TagSet");

        if (bucketTagSets == null || bucketTagSets.getLength() != 1) {
          throw new MalformedXMLException("/Tagging/TagSet");
        }
        bucketTagSets = xmlParser.getNodes("//Tagging/TagSet/Tag");

        for (int i = 0; i < bucketTagSets.getLength(); i++) {
          taggingConfiguration.getBucketTagSet().getBucketTags().add(extractBucketTag(xmlParser, bucketTagSets.item(i)));
        }
      } catch (Exception e) {
        throw e;
      }
    }
    return taggingConfiguration;
  }

  private BucketTag extractBucketTag(XMLParser parser, Node node) throws InvalidTagErrorException {
    BucketTag bucketTag = new BucketTag();
    String key = parser.getValue(node, "Key");
    String value = parser.getValue(node, "Value");

    if (isInValidTagSet(key, value)) {
      throw new InvalidTagErrorException();
    }

    bucketTag.setKey(key);
    bucketTag.setValue(value);

    return bucketTag;
  }

  private boolean isInValidTagSet(String key, String value) {
    final Pattern pattern = Pattern.compile("[a-zA-Z0-9\\s+-=._:]+");

    if (key == null || key.equals("") || value == null || value.equals("")) {
      return true;
    } else if (key.equals(" ") || key.charAt(0) == ' ' || value.charAt(0) == ' ' || value.equals(" ")) {
      return true;
    } else if (key.length() > 128 || value.length() > 256) {
      return true;
    } else if (!pattern.matcher(key).matches() || !pattern.matcher(value).matches()) {
      return true;
    }
    return false;
  }

  private LifecycleConfiguration getLifecycle(MappingHttpRequest httpRequest) throws S3Exception {
    LifecycleConfiguration lifecycleConfigurationType = new LifecycleConfiguration();
    lifecycleConfigurationType.setRules(new ArrayList<LifecycleRule>());
    String message = getMessageString(httpRequest);
    if (message.length() > 0) {
      try {
        XMLParser xmlParser = new XMLParser(message);
        DTMNodeList rules = xmlParser.getNodes("//LifecycleConfiguration/Rule");
        if (rules == null) {
          throw new MalformedXMLException("/LifecycleConfiguration/Rule");
        }
        for (int idx = 0; idx < rules.getLength(); idx++) {
          lifecycleConfigurationType.getRules().add(extractLifecycleRule(xmlParser, rules.item(idx)));
        }
      } catch (S3Exception e) {
        throw e;
      } catch (Exception ex) {
        MalformedXMLException e = new MalformedXMLException("/LifecycleConfiguration");
        ex.initCause(ex);
        throw e;
      }
    }
    return lifecycleConfigurationType;
  }

  private LifecycleRule extractLifecycleRule(XMLParser parser, Node node) throws S3Exception {
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
      if ((transitionDays != null && !transitionDays.equals("")) || (transitionDate != null && !transitionDate.equals(""))) {
        String storageClass = parser.getValue(node, "Transition/StorageClass");
        if (transitionDays != null && !transitionDays.equals("")) {
          transition = new Transition();
          Integer transitionDaysInt = new Integer(Integer.parseInt(transitionDays));
          transition.setCreationDelayDays(transitionDaysInt.intValue());
        } else if (transitionDate != null && !transitionDate.equals("")) {
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
      if ((expirationDays != null && !expirationDays.equals("")) || (expirationDate != null && !expirationDate.equals(""))) {
        if (expirationDays != null && !expirationDays.equals("")) {
          expiration = new Expiration();
          Integer expirationDaysInt = new Integer(Integer.parseInt(expirationDays));
          expiration.setCreationDelayDays(expirationDaysInt.intValue());
        } else if (expirationDate != null && !expirationDate.equals("")) {
          expiration = new Expiration();
          Date expirationDateAsDate = Iso8601DateParser.parse(expirationDate);
          expiration.setEffectiveDate(expirationDateAsDate);
        }
      }
      if (expiration != null) {
        lifecycleRule.setExpiration(expiration);
      }
    } catch (ParseException e) {
      MalformedXMLException ex = new MalformedXMLException("Expiration|Transition Date");
      ex.initCause(e);
      throw ex;
    } catch (Exception ex) {
      MalformedXMLException e = new MalformedXMLException("LifecycleRule");
      ex.initCause(ex);
      throw e;
    }

    return lifecycleRule;
  }

  private CorsConfiguration getCors(MappingHttpRequest httpRequest) throws S3Exception {
    CorsConfiguration corsConfigurationType = new CorsConfiguration();
    corsConfigurationType.setRules(new ArrayList<CorsRule>());
    String message = getMessageString(httpRequest);
    if (message.length() > 0) {
      try {
        XMLParser xmlParser = new XMLParser(message);
        DTMNodeList rules = xmlParser.getNodes("//CORSConfiguration/CORSRule");
        if (rules == null) {
          throw new MalformedXMLException("/CORSConfiguration/CORSRule");
        }
        for (int idx = 0; idx < rules.getLength(); idx++) {
          CorsRule extractedCorsRule = extractCorsRule(xmlParser, rules.item(idx));
          extractedCorsRule.setSequence(idx);
          corsConfigurationType.getRules().add(extractedCorsRule);
        }
      } catch (S3Exception e) {
        throw e;
      } catch (Exception ex) {
        MalformedXMLException e = new MalformedXMLException("/CORSConfiguration");
        e.initCause(ex);
        throw e;
      }
    }
    return corsConfigurationType;
  }

  private CorsRule extractCorsRule(XMLParser parser, Node node) throws S3Exception {
    CorsRule corsRule = new CorsRule();

    try {

      String id = parser.getValue(node, "ID");
      // Don't populate the ID if it's empty
      if (id != null && id.length() > 0) {
        corsRule.setId(id);
      }

      String maxAgeSecondsString = parser.getValue(node, "MaxAgeSeconds");
      if (maxAgeSecondsString != null) {
        try {
          int maxAgeSeconds = Integer.parseInt(maxAgeSecondsString);
          // Don't populate the max age if it's negative (invalid) or zero (the default)
          if (maxAgeSeconds > 0) {
            corsRule.setMaxAgeSeconds(maxAgeSeconds);
          }
        } catch (NumberFormatException nfe) {
          // Should only get here on an empty (but not null) string
        }
      }

      List<String> corsAllowedMethods = extractCorsElementList(parser, node, "AllowedMethod");
      if (corsAllowedMethods != null) {
        for (String corsAllowedMethod : corsAllowedMethods) {
          if (!AllowedCorsMethods.methodList.contains(HttpMethod.valueOf(corsAllowedMethod))) {
            CorsConfigUnsupportedMethodException s3e = new CorsConfigUnsupportedMethodException(corsAllowedMethod);
            throw s3e;
          }
        }
      }
      corsRule.setAllowedMethods(corsAllowedMethods);

      corsRule.setAllowedOrigins(extractCorsElementList(parser, node, "AllowedOrigin"));

      corsRule.setAllowedHeaders(extractCorsElementList(parser, node, "AllowedHeader"));

      corsRule.setExposeHeaders(extractCorsElementList(parser, node, "ExposeHeader"));

    } catch (S3Exception e) {
      throw e;
    } catch (Exception ex) {
      MalformedXMLException e = new MalformedXMLException("/CORSConfiguration/CORSRule");
      e.initCause(ex);
      throw e;
    }

    return corsRule;
  }

  private List<String> extractCorsElementList(XMLParser parser, Node node, String element) throws S3Exception {
      List<String> elementList = new ArrayList<String>();
      try {

        DTMNodeList elementNodes = parser.getNodes(node, element);
        if (elementNodes == null) {
          throw new MalformedXMLException("/CORSConfiguration/CORSRule/" + element);
        }
        int elementNodesSize = elementNodes.getLength();

        if (elementNodesSize > 0) {
          for (int idx = 0; idx < elementNodes.getLength(); idx++) {
            Node elementNode = elementNodes.item(idx);
            elementList.add(elementNode.getFirstChild().getNodeValue());
          }
        }
      } catch (S3Exception e) {
        throw e;
      } catch (Exception ex) {
        MalformedXMLException e = new MalformedXMLException("/CORSConfiguration/CORSRule");
        e.initCause(ex);
        throw e;
      }

      return elementList;
    }

    private PreflightRequest processPreflightRequest(MappingHttpRequest httpRequest) throws S3Exception {
      PreflightRequest preflightRequest = new PreflightRequest();

      preflightRequest.setOrigin(httpRequest.getHeader(HttpHeaders.Names.ORIGIN));
      preflightRequest.setMethod(httpRequest.getHeader(HttpHeaders.Names.ACCESS_CONTROL_REQUEST_METHOD));
      String requestHeadersFromRequest = httpRequest.getHeader(HttpHeaders.Names.ACCESS_CONTROL_REQUEST_HEADERS);
      if (requestHeadersFromRequest != null) {
        String[] requestHeadersArrayFromRequest = requestHeadersFromRequest.split(",");
        List<String> requestHeaders = new ArrayList<String>();
        for (int idx = 0; idx < requestHeadersArrayFromRequest.length; idx++) {
          requestHeaders.add(requestHeadersArrayFromRequest[idx].trim());
        }
        preflightRequest.setRequestHeaders(requestHeaders);
      }
      return preflightRequest;
    }
    
  private DeleteMultipleObjectsMessage getMultiObjectDeleteMessage(MappingHttpRequest httpRequest) throws S3Exception {
    DeleteMultipleObjectsMessage message = new DeleteMultipleObjectsMessage();
    String rawMessage = httpRequest.getContent().toString(StandardCharsets.UTF_8);
    if (rawMessage.length() > 0) {
      try {
        XMLParser xmlParser = new XMLParser(rawMessage);
        String quietVal = xmlParser.getValue("//Delete/Quiet");
        if (quietVal != null && !"".equals(quietVal) && quietVal.trim().startsWith("true")) {
          message.setQuiet(Boolean.TRUE);
        } else {
          message.setQuiet(Boolean.FALSE);
        }
        DTMNodeList deletes = xmlParser.getNodes("//Delete/Object");
        if (deletes == null) {
          throw new MalformedXMLException("/Delete/Object");
        }
        List<DeleteMultipleObjectsEntry> deleteObjList = Lists.newArrayList();
        for (int idx = 0; idx < deletes.getLength(); idx++) {
          // lifecycleConfigurationType.getRules().add( extractLifecycleRule( xmlParser, deletes.item(idx) ) );
          deleteObjList.add(extractDeleteObjectEntry(xmlParser, deletes.item(idx)));
        }
        message.setObjects(deleteObjList);
      } catch (S3Exception e) {
        throw e;
      } catch (Exception ex) {
        MalformedXMLException e = new MalformedXMLException("/LifecycleConfiguration");
        ex.initCause(ex);
        throw e;
      }
    }
    return message;
  }

  private DeleteMultipleObjectsEntry extractDeleteObjectEntry(XMLParser parser, Node node) {
    DeleteMultipleObjectsEntry entry = new DeleteMultipleObjectsEntry();
    String objectKey = parser.getValue(node, "Key");
    String objectVersionId = parser.getValue(node, "VersionId");
    entry.setKey(objectKey);
    entry.setVersionId(objectVersionId);
    return entry;
  }
}
