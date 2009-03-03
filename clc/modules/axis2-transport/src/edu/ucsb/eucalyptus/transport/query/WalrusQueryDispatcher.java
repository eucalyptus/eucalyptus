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
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.transport.query;


import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.keys.Hashes;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.util.*;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;
import org.apache.xml.dtm.ref.DTMNodeList;
import org.bouncycastle.util.encoders.Base64;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPInputStream;

public class WalrusQueryDispatcher extends GenericHttpDispatcher implements RESTfulDispatcher {

    public static final String NAME = "WalrusQueryDispatcher";
    public static final String IS_QUERY_REQUEST = NAME + "_IS_QUERY_REQUEST";
    public static final String BINDING_NAMESPACE = NAME + "_BINDING_NAMESPACE";
    private static Logger LOG = Logger.getLogger( WalrusQueryDispatcher.class );
    private static String HTTP_PARAM_SEPARATOR = "&";
    private static String HTTP_ASSIGNMENT_OPERATOR = "=";
    private static String S3_NAMESPACE =  "http://s3.amazonaws.com/doc/2006-03-01/";

    private static String walrusBaseAddress = "/services/Walrus/";
    private static String SERVICE = "service";
    private static String BUCKET = "bucket";
    private static String OBJECT = "object";
    private Map<String, String> operationMap = populateOperationMap();

    // Use getWriteMessenger and getReadMessenger to access these
    private static WalrusDataMessenger putMessenger;
    private static WalrusDataMessenger getMessenger;
    public static final int DATA_MESSAGE_SIZE = 102400;

    private Map<String, String> populateOperationMap() {
        Map<String, String> newMap = new HashMap<String, String>();
        //Service operations
        newMap.put(SERVICE + HTTPVerb.GET.toString(), "ListAllMyBuckets");

        //Bucket operations
        newMap.put(BUCKET + WalrusQueryDispatcher.HTTPVerb.GET.toString() + OperationParameter.acl.toString(), "GetBucketAccessControlPolicy");
        newMap.put(BUCKET + WalrusQueryDispatcher.HTTPVerb.PUT.toString() + OperationParameter.acl.toString(), "SetBucketAccessControlPolicy");

        newMap.put(BUCKET + WalrusQueryDispatcher.HTTPVerb.GET.toString(), "ListBucket");
        newMap.put(BUCKET + WalrusQueryDispatcher.HTTPVerb.GET.toString() + OperationParameter.prefix.toString(), "ListBucket");
        newMap.put(BUCKET + WalrusQueryDispatcher.HTTPVerb.GET.toString() + OperationParameter.maxkeys.toString(), "ListBucket");
        newMap.put(BUCKET + WalrusQueryDispatcher.HTTPVerb.GET.toString() + OperationParameter.marker.toString(), "ListBucket");
        newMap.put(BUCKET + WalrusQueryDispatcher.HTTPVerb.GET.toString() + OperationParameter.delimiter.toString(), "ListBucket");
        newMap.put(BUCKET + WalrusQueryDispatcher.HTTPVerb.PUT.toString(), "CreateBucket");
        newMap.put(BUCKET + HTTPVerb.DELETE.toString(), "DeleteBucket");

        newMap.put(BUCKET + WalrusQueryDispatcher.HTTPVerb.GET.toString() + OperationParameter.location.toString(), "GetBucketLocation");
        newMap.put(BUCKET + WalrusQueryDispatcher.HTTPVerb.GET.toString() + OperationParameter.logging.toString(), "GetBucketLoggingStatus");
        newMap.put(BUCKET + WalrusQueryDispatcher.HTTPVerb.PUT.toString() + OperationParameter.logging.toString(), "SetBucketLoggingStatus");

        //Object operations
        newMap.put(OBJECT + WalrusQueryDispatcher.HTTPVerb.GET.toString() + OperationParameter.acl.toString(), "GetObjectAccessControlPolicy");
        newMap.put(OBJECT + WalrusQueryDispatcher.HTTPVerb.PUT.toString() + OperationParameter.acl.toString(), "SetObjectAccessControlPolicy");

        newMap.put(BUCKET + WalrusQueryDispatcher.HTTPVerb.POST.toString(), "PostObject");

        newMap.put(OBJECT + WalrusQueryDispatcher.HTTPVerb.PUT.toString(), "PutObject");
        newMap.put(OBJECT + WalrusQueryDispatcher.HTTPVerb.PUT.toString() + WalrusProperties.COPY_SOURCE.toString(), "CopyObject");
        newMap.put(OBJECT + WalrusQueryDispatcher.HTTPVerb.GET.toString(), "GetObject");
        newMap.put(OBJECT + WalrusQueryDispatcher.HTTPVerb.DELETE.toString(), "DeleteObject");

        newMap.put(OBJECT + WalrusQueryDispatcher.HTTPVerb.HEAD.toString(), "GetObject");
        newMap.put(OBJECT + WalrusQueryDispatcher.HTTPVerb.GET.toString() + "extended", "GetObjectExtended");

        return newMap;
    }


    public enum HTTPVerb {
        GET, PUT, DELETE, POST, HEAD;
    }

    public enum OperationParameter {

        acl, location, prefix, maxkeys, delimiter, marker, logging;

        private static String pattern = buildPattern();

        private static String buildPattern()
        {
            StringBuilder s = new StringBuilder();
            for ( OperationParameter op : OperationParameter.values() ) s.append( "(" ).append( op.name() ).append( ")|" );
            s.deleteCharAt( s.length() - 1 );
            return s.toString();
        }

        public static String toPattern()
        {
            return pattern;
        }

        public static String getParameter( Map<String,String> map )
        {
            for( OperationParameter op : OperationParameter.values() )
                if( map.containsKey( op.toString() ) )
                    return map.get( op.toString() );
            return null;
        }
    }

    private static String[] getTarget(String operationPath) {
        if(operationPath.startsWith("/"))
            operationPath = operationPath.substring(1);
        operationPath = operationPath.replaceAll("//", "/");
        return operationPath.split("/");
    }


    public enum RequiredQueryParams {
        Date
    }

    public boolean accepts( final HttpRequest httpRequest, final MessageContext messageContext )
    {
        //:: decide about whether or not to accept the request for processing :://
        if(httpRequest.getService().equals(WalrusProperties.SERVICE_NAME))
            return true;
        return false;
    }

    public String getOperation( HttpRequest httpRequest, MessageContext messageContext ) throws EucalyptusCloudException
    {
        //Figure out if it is an operation on the service, a bucket or an object
        Map operationParams = new HashMap();
        String[] target = null;
        String path = httpRequest.getOperationPath();
        boolean walrusInternalOperation = false;
        if(path.length() > 0) {
            target = getTarget(path);
        }

        String verb = httpRequest.getHttpMethod();
        Map<String, String> headers = httpRequest.getHeaders();
        CaseInsensitiveMap caseInsensitiveHeaders = new CaseInsensitiveMap(headers);
        String operationKey = "";
        Map<String, String> params = httpRequest.getParameters();
        String operationName = null;
        long contentLength = 0;
        String contentLengthString = (String) messageContext.getProperty(HTTP.CONTENT_LEN);
        if(contentLengthString != null) {
            contentLength = Long.parseLong(contentLengthString);
        }
        if(caseInsensitiveHeaders.containsKey(StorageProperties.EUCALYPTUS_OPERATION)) {
            String value = caseInsensitiveHeaders.get(StorageProperties.EUCALYPTUS_OPERATION);
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

                if(verb.equals(HTTPVerb.POST.toString())) {
                    InputStream in = (InputStream) messageContext.getProperty("TRANSPORT_IN");
                    messageContext.setProperty(WalrusProperties.STREAMING_HTTP_PUT, Boolean.TRUE);
                    String contentType = caseInsensitiveHeaders.get(HTTP.CONTENT_TYPE);
                    int postContentLength = Integer.parseInt(caseInsensitiveHeaders.get(HTTP.CONTENT_LEN));
                    POSTRequestContext postRequestContext = new POSTRequestContext(in, contentType, postContentLength);
                    FileUpload fileUpload = new FileUpload(new WalrusFileItemFactory());
                    InputStream formDataIn = null;
                    String objectKey = null;
                    String file = null;
                    String key;
                    Map<String, String> formFields = new HashMap<String, String>();
                    try {
                        List<FileItem> parts = fileUpload.parseRequest(postRequestContext);
                        for(FileItem part : parts) {
                            if(part.isFormField()) {
                                String fieldName = part.getFieldName().toString().toLowerCase();
                                InputStream formFieldIn = part.getInputStream();
                                int bytesRead;
                                String fieldValue = "";
                                byte[] bytes = new byte[512];
                                while((bytesRead = formFieldIn.read(bytes)) > 0) {
                                    fieldValue += new String(bytes, 0, bytesRead);
                                }
                                formFields.put(fieldName, fieldValue);
                            } else {
                                formDataIn = part.getInputStream();
                            }
                        }
                    } catch (Exception ex) {
                        LOG.warn(ex, ex);
                        throw new EucalyptusCloudException("could not process form request");
                    }

                    String authenticationHeader = "";
                    formFields.put(WalrusProperties.FormField.bucket.toString(), target[0]);
                    if(formFields.containsKey(WalrusProperties.FormField.file.toString())) {
                        file = formFields.get(WalrusProperties.FormField.file.toString());
                    }
                    if(formFields.containsKey(WalrusProperties.FormField.key.toString())) {
                        objectKey = formFields.get(WalrusProperties.FormField.key.toString());
                        if(file != null) {
                            StringBuilder builder = new StringBuilder();
                            builder.append('$');
                            builder.append('{');
                            builder.append("filename");
                            builder.append('}');
                            objectKey = objectKey.replaceAll(builder.toString(), file);
                        }
                    }
                    if(formFields.containsKey(WalrusProperties.FormField.acl.toString())) {
                        String acl = formFields.get(WalrusProperties.FormField.acl.toString());
                        headers.put(WalrusProperties.AMZ_ACL, acl);
                    }
                    if(formFields.containsKey(WalrusProperties.FormField.success_action_redirect.toString())) {
                        String successActionRedirect = formFields.get(WalrusProperties.FormField.success_action_redirect.toString());
                        operationParams.put("SuccessActionRedirect", successActionRedirect);
                    }
                    if(formFields.containsKey(WalrusProperties.FormField.success_action_status.toString())) {
                        Integer successActionStatus = Integer.parseInt(formFields.get(WalrusProperties.FormField.success_action_status.toString()));
                        if(successActionStatus == 200 || successActionStatus == 201)
                            operationParams.put("SuccessActionStatus", successActionStatus);
                        else
                            operationParams.put("SuccessActionStatus", 204);
                    } else {
                        operationParams.put("SuccessActionStatus", 204);
                    }
                    if(formFields.containsKey(WalrusProperties.FormField.policy.toString())) {
                        String policy = new String(Base64.decode(formFields.remove(WalrusProperties.FormField.policy.toString())));
                        String policyData;
                        try {
                            policyData = new String(Base64.encode(policy.getBytes()));
                        } catch (Exception ex) {
                            LOG.warn(ex, ex);
                            throw new EucalyptusCloudException("error reading policy data.");
                        }
                        //parse policy
                        try {
                            JSONObject policyObject = new JSONObject(policy);
                            String expiration = (String) policyObject.get(WalrusProperties.PolicyHeaders.expiration.toString());
                            if(expiration != null) {
                                Date expirationDate = DateUtils.parseIso8601DateTimeOrDate(expiration);
                                if((new Date()).getTime() > expirationDate.getTime()) {
                                    LOG.warn("Policy has expired.");
                                    //TODO: currently this will be reported as an invalid operation
                                    //Fix this to report a security exception
                                    throw new EucalyptusCloudException("Policy has expired.");
                                }
                            }
                            List<String> policyItemNames = new ArrayList<String>();

                            JSONArray conditions = (JSONArray) policyObject.get(WalrusProperties.PolicyHeaders.conditions.toString());
                            for (int i = 0 ; i < conditions.length() ; ++i) {
                                Object policyItem = conditions.get(i);
                                if(policyItem instanceof JSONObject) {
                                    JSONObject jsonObject = (JSONObject) policyItem;
                                    if(!exactMatch(jsonObject, formFields, policyItemNames)) {
                                        LOG.warn("Policy verification failed. ");
                                        throw new EucalyptusCloudException("Policy verification failed.");
                                    }
                                } else if(policyItem instanceof  JSONArray) {
                                    JSONArray jsonArray = (JSONArray) policyItem;
                                    if(!partialMatch(jsonArray, formFields, policyItemNames)) {
                                        LOG.warn("Policy verification failed. ");
                                        throw new EucalyptusCloudException("Policy verification failed.");
                                    }
                                }
                            }

                            Set<String> formFieldsKeys = formFields.keySet();
                            for(String formKey : formFieldsKeys) {
                                if(formKey.startsWith(WalrusProperties.IGNORE_PREFIX))
                                    continue;
                                boolean fieldOkay = false;
                                for(WalrusProperties.IgnoredFields field : WalrusProperties.IgnoredFields.values()) {
                                    if(formKey.equals(field.toString().toLowerCase())) {
                                        fieldOkay = true;
                                        break;
                                    }
                                }
                                if(fieldOkay)
                                    continue;
                                if(policyItemNames.contains(formKey))
                                    continue;
                                LOG.warn("All fields except those marked with x-ignore- should be in policy.");
                                throw new EucalyptusCloudException("All fields except those marked with x-ignore- should be in policy.");
                            }
                        } catch(Exception ex) {
                            //rethrow
                            if(ex instanceof EucalyptusCloudException)
                                throw (EucalyptusCloudException)ex;
                            LOG.warn(ex);
                        }
                        //all form uploads without a policy are anonymous
                        if(formFields.containsKey(WalrusProperties.FormField.AWSAccessKeyId.toString().toLowerCase())) {
                            String accessKeyId = formFields.remove(WalrusProperties.FormField.AWSAccessKeyId.toString().toLowerCase());
                            authenticationHeader += "AWS" + " " + accessKeyId + ":";
                        }
                        if(formFields.containsKey(WalrusProperties.FormField.signature.toString())) {
                            String signature = formFields.remove(WalrusProperties.FormField.signature.toString());
                            authenticationHeader += signature;
                            headers.put(HMACQuerySecurityHandler.SecurityParameter.Authorization.toString(), authenticationHeader);
                        }
                        headers.put(WalrusProperties.FormField.FormUploadPolicyData.toString(), policyData);
                    }
                    operationParams.put("Key", objectKey);
                    key = target[0] + "." + objectKey;
                    String randomKey = key + "." + Hashes.getRandom(10);
                    LinkedBlockingQueue<WalrusDataMessage> putQueue = getWriteMessenger().interruptAllAndGetQueue(key, randomKey);

                    Writer writer = new Writer(formDataIn, postContentLength, putQueue);
                    writer.start();

                    operationParams.put("ContentLength", (new Long(postContentLength).toString()));
                    operationParams.put(WalrusProperties.Headers.RandomKey.toString(), randomKey);
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
            operationParams.put("Bucket", target[0]);
            operationParams.put("Key", objectKey);


            if(!params.containsKey(OperationParameter.acl.toString())) {
                if (verb.equals(HTTPVerb.PUT.toString())) {
                    if(caseInsensitiveHeaders.containsKey(WalrusProperties.COPY_SOURCE.toString())) {
                        String copySource = caseInsensitiveHeaders.get(WalrusProperties.COPY_SOURCE.toString());
                        String[] sourceTarget = getTarget(copySource);
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

                            String metaDataDirective = caseInsensitiveHeaders.get(WalrusProperties.METADATA_DIRECTIVE.toString());
                            if(metaDataDirective != null) {
                                operationParams.put("MetadataDirective", metaDataDirective);
                            }
                            AccessControlListType accessControlList;
                            if(contentLength > 0) {
                                InputStream in = (InputStream) messageContext.getProperty("TRANSPORT_IN");
                                accessControlList = getAccessControlList(in);
                            } else {
                                accessControlList = new AccessControlListType();
                                ArrayList<Grant> grant = new ArrayList<Grant>();
                                accessControlList.setGrants(grant);
                            }
                            operationParams.put("AccessControlList", accessControlList);
                            operationKey += WalrusProperties.COPY_SOURCE.toString();
                            Iterator<String> iterator = caseInsensitiveHeaders.keySet().iterator();
                            while(iterator.hasNext()) {
                                String key = iterator.next();
                                for(WalrusProperties.CopyHeaders header: WalrusProperties.CopyHeaders.values()) {
                                    if(key.replaceAll("-", "").equals(header.toString().toLowerCase())) {
                                        String value = caseInsensitiveHeaders.get(key);
                                        parseExtendedHeaders(operationParams, header.toString(), value);
                                    }
                                }
                            }
                        } else {
                            throw new EucalyptusCloudException("Malformed COPY request");
                        }

                    } else {
                        messageContext.setProperty(WalrusProperties.STREAMING_HTTP_PUT, Boolean.TRUE);
                        InputStream in = (InputStream) messageContext.getProperty("TRANSPORT_IN");
                        InputStream inStream = in;
                        if((!walrusInternalOperation) || (!WalrusProperties.StorageOperations.StoreSnapshot.toString().equals(operationName))) {
                            inStream = new BufferedInputStream(in);
                        } else {
                            try {
                                inStream = new GZIPInputStream(in);
                            } catch(Exception ex) {
                                LOG.warn(ex, ex);
                                throw new EucalyptusCloudException("cannot process input");
                            }
                        }
                        String key = target[0] + "." + objectKey;
                        String randomKey = key + "." + Hashes.getRandom(10);
                        LinkedBlockingQueue<WalrusDataMessage> putQueue = getWriteMessenger().interruptAllAndGetQueue(key, randomKey);

                        Writer writer = new Writer(inStream, contentLength, putQueue);
                        writer.start();

                        operationParams.put("ContentLength", (new Long(contentLength).toString()));
                        operationParams.put(WalrusProperties.Headers.RandomKey.toString(), randomKey);
                    }
                } else if(verb.equals(HTTPVerb.GET.toString())) {
                    messageContext.setProperty(WalrusProperties.STREAMING_HTTP_GET, Boolean.TRUE);
                    if(!walrusInternalOperation) {

                        operationParams.put("GetData", Boolean.TRUE);
                        operationParams.put("InlineData", Boolean.FALSE);
                        operationParams.put("GetMetaData", Boolean.TRUE);

                        Iterator<String> iterator = caseInsensitiveHeaders.keySet().iterator();
                        boolean isExtendedGet = false;
                        while(iterator.hasNext()) {
                            String key = iterator.next();
                            for(WalrusProperties.ExtendedGetHeaders header: WalrusProperties.ExtendedGetHeaders.values()) {
                                if(key.replaceAll("-", "").equals(header.toString().toLowerCase())) {
                                    String value = caseInsensitiveHeaders.get(key);
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
                    } else {
                        for(WalrusProperties.InfoOperations operation : WalrusProperties.InfoOperations.values()) {
                            if(operation.toString().equals(operationName)) {
                                messageContext.removeProperty(WalrusProperties.STREAMING_HTTP_GET);
                                break;
                            }
                        }
                    }
                    if(params.containsKey(WalrusProperties.GetOptionalParameters.IsCompressed.toString())) {
                        Boolean isCompressed = Boolean.parseBoolean(params.remove(WalrusProperties.GetOptionalParameters.IsCompressed.toString()));
                        operationParams.put("IsCompressed", isCompressed);
                    }

                } else if(verb.equals(HTTPVerb.HEAD.toString())) {
                    messageContext.setProperty(WalrusProperties.STREAMING_HTTP_GET, Boolean.FALSE);
                    if(!walrusInternalOperation) {
                        operationParams.put("GetData", Boolean.FALSE);
                        operationParams.put("InlineData", Boolean.FALSE);
                        operationParams.put("GetMetaData", Boolean.TRUE);
                    }
                }
            }

        }


        if (verb.equals(HTTPVerb.PUT.toString()) && params.containsKey(OperationParameter.acl.toString())) {
            //read ACL
            InputStream in = (InputStream) messageContext.getProperty("TRANSPORT_IN");
            operationParams.put("AccessControlPolicy", getAccessControlPolicy(in));
        }

        ArrayList paramsToRemove = new ArrayList();

        boolean addMore = true;
        Iterator iterator = params.keySet().iterator();
        while(iterator.hasNext()) {
            Object key = iterator.next();
            String keyString = key.toString().toLowerCase();
            boolean dontIncludeParam = false;
            for(HMACQuerySecurityHandler.SecurityParameter securityParam : HMACQuerySecurityHandler.SecurityParameter.values()) {
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
        httpRequest.setBindingArguments(operationParams);
        messageContext.setProperty(WalrusProperties.WALRUS_OPERATION, operationName);
        return operationName;
    }

    private boolean exactMatch(JSONObject jsonObject, Map formFields, List<String> policyItemNames) {
        Iterator<String> iterator = jsonObject.keys();
        boolean returnValue = false;
        while(iterator.hasNext()) {
            String key = iterator.next();
            key = key.replaceAll("\\$", "");
            policyItemNames.add(key.toLowerCase());
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
        if(jsonArray.length() != 3)
            return false;
        try {
            String condition = (String) jsonArray.get(0);
            String key = (String) jsonArray.get(1);
            key = key.replaceAll("\\$", "");
            policyItemNames.add(key.toLowerCase());
            String value = (String) jsonArray.get(2);
            if(condition.contains("eq")) {
                if(value.equals(formFields.get(key)))
                    returnValue = true;
            } else if(condition.contains("starts-with")) {
                if(!formFields.containsKey(key.toLowerCase()))
                    return false;
                if(formFields.get(key.toLowerCase()).startsWith(value))
                    returnValue = true;
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return returnValue;
    }

    private void parseExtendedHeaders(Map operationParams, String headerString, String value) {
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
                operationParams.put(headerString, DateUtils.parseIso8601DateTimeOrDate(value));
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        } else {
            operationParams.put(headerString, value);
        }
    }

    private AccessControlPolicyType getAccessControlPolicy(InputStream in) throws EucalyptusCloudException {
        AccessControlPolicyType accessControlPolicy = new AccessControlPolicyType();
        try {
            BufferedInputStream bufferedIn = new BufferedInputStream(in);
            byte[] bytes = new byte[DATA_MESSAGE_SIZE];

            int bytesRead;
            String aclString = "";
            while ((bytesRead = bufferedIn.read(bytes)) > 0) {
                aclString += new String(bytes, 0, bytesRead);
            }
            if(aclString.length() > 0) {
                XMLParser xmlParser = new XMLParser(aclString);
                String ownerId = xmlParser.getValue("//Owner/ID");
                String displayName = xmlParser.getValue("//Owner/DisplayName");

                CanonicalUserType canonicalUser = new CanonicalUserType(ownerId, displayName);
                accessControlPolicy.setOwner(canonicalUser);

                AccessControlListType accessControlList = new AccessControlListType();
                ArrayList<Grant> grants = new ArrayList<Grant>();

                List<String> permissions = xmlParser.getValues("//AccessControlList/Grant/Permission");

                DTMNodeList grantees = xmlParser.getNodes("//AccessControlList/Grant/Grantee");


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
                            throw new EucalyptusCloudException("malformed access control list");
                        Grant grant = new Grant();
                        Grantee grantee = new Grantee();
                        grantee.setGroup(new Group(groupUri));
                        grant.setGrantee(grantee);
                        grant.setPermission(permissions.get(i));
                        grants.add(grant);
                    }
                }

                accessControlList.setGrants(grants);
                accessControlPolicy.setAccessControlList(accessControlList);
            }
        } catch(Exception ex) {
            LOG.warn(ex);
            throw new EucalyptusCloudException(ex.getMessage());
        }
        return accessControlPolicy;
    }

    private AccessControlListType getAccessControlList(InputStream in) throws EucalyptusCloudException {
        AccessControlListType accessControlList = new AccessControlListType();
        try {
            BufferedInputStream bufferedIn = new BufferedInputStream(in);
            byte[] bytes = new byte[DATA_MESSAGE_SIZE];

            int bytesRead;
            String aclString = "";
            while ((bytesRead = bufferedIn.read(bytes)) > 0) {
                aclString += new String(bytes, 0, bytesRead);
            }
            if(aclString.length() > 0) {
                XMLParser xmlParser = new XMLParser(aclString);
                String ownerId = xmlParser.getValue("//Owner/ID");
                String displayName = xmlParser.getValue("//Owner/DisplayName");


                ArrayList<Grant> grants = new ArrayList<Grant>();

                List<String> permissions = xmlParser.getValues("/AccessControlList/Grant/Permission");

                DTMNodeList grantees = xmlParser.getNodes("/AccessControlList/Grant/Grantee");


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
                            throw new EucalyptusCloudException("malformed access control list");
                        Grant grant = new Grant();
                        Grantee grantee = new Grantee();
                        grantee.setGroup(new Group(groupUri));
                        grant.setGrantee(grantee);
                        grant.setPermission(permissions.get(i));
                        grants.add(grant);
                    }
                }
                accessControlList.setGrants(grants);
            }
        } catch(Exception ex) {
            LOG.warn(ex);
            throw new EucalyptusCloudException(ex.getMessage());
        }
        return accessControlList;
    }

    public QuerySecurityHandler getSecurityHandler()
    {
        return new WalrusQuerySecurityHandler();
    }

    public QueryBinding getBinding()
    {
        return new WalrusQueryBinding();
    }

    public String getNamespace() {
        return S3_NAMESPACE;
    }

    public void initDispatcher()
    {
        init( new HandlerDescription( NAME ) );
    }

    public static synchronized WalrusDataMessenger getReadMessenger() {
        if (getMessenger == null) {
            getMessenger = new WalrusDataMessenger();
        }
        return getMessenger;
    }

    public static synchronized WalrusDataMessenger getWriteMessenger() {
        if (putMessenger == null) {
            putMessenger = new WalrusDataMessenger();
        }
        return putMessenger;
    }

    class Writer extends Thread {

        private InputStream in;
        private long dataLength;
        private LinkedBlockingQueue<WalrusDataMessage> putQueue;
        public Writer(InputStream in, long dataLength, LinkedBlockingQueue<WalrusDataMessage> putQueue) {
            this.in = in;
            this.dataLength = dataLength;
            this.putQueue = putQueue;
        }

        public void run() {
            byte[] bytes = new byte[DATA_MESSAGE_SIZE];


            try {
                putQueue.put(WalrusDataMessage.StartOfData(dataLength));

                int bytesRead;
                while ((bytesRead = in.read(bytes)) > 0) {
                    putQueue.put(WalrusDataMessage.DataMessage(bytes, bytesRead));
                }

                putQueue.put(WalrusDataMessage.EOF());
            } catch (Exception ex) {
                LOG.warn(ex, ex);
            }
        }

    }

    private String toUpperFirst(String string) {
        return string.substring(0, 1).toUpperCase().concat(string.substring(1));
    }
}