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

import edu.ucsb.eucalyptus.keys.Hashes;
import edu.ucsb.eucalyptus.util.*;
import edu.ucsb.eucalyptus.msgs.AccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.CanonicalUserType;
import edu.ucsb.eucalyptus.msgs.Grant;
import edu.ucsb.eucalyptus.msgs.AccessControlListType;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.log4j.Logger;
import org.apache.commons.lang.time.DateUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

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

        newMap.put(OBJECT + WalrusQueryDispatcher.HTTPVerb.PUT.toString(), "PutObject");
        //TODO: newMap.put(OBJECT + WalrusQueryDispatcher.HTTPVerb.POST.toString(), "PutObject");
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
        operationPath = operationPath.substring(1);
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

    public String getOperation( HttpRequest httpRequest, MessageContext messageContext )
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
        String operationKey = "";
        Map<String, String> params = httpRequest.getParameters();
        if(target == null) {
            //target = service
            operationKey = SERVICE + verb;
        } else if(target.length < 2) {
            //target = bucket
            if(!target[0].equals("")) {
                operationKey = BUCKET + verb;
                operationParams.put("Bucket", target[0]);
            } else {
                operationKey = SERVICE + verb;
            }
        } else {
            //target = object
            operationKey = OBJECT + verb;
            operationParams.put("Bucket", target[0]);
            operationParams.put("Key", target[1]);

            if(headers.containsKey(StorageProperties.EUCALYPTUS_OPERATION)) {
                String value = headers.get(StorageProperties.EUCALYPTUS_OPERATION);
                for(WalrusProperties.WalrusInternalOperations operation: WalrusProperties.WalrusInternalOperations.values()) {
                    if(value.toLowerCase().equals(operation.toString().toLowerCase())) {
                        walrusInternalOperation = true;
                        break;
                    }
                }
            }

            if(!params.containsKey(OperationParameter.acl.toString())) {
                if (verb.equals(HTTPVerb.PUT.toString())) {
                    messageContext.setProperty(WalrusProperties.STREAMING_HTTP_PUT, Boolean.TRUE);
                    InputStream in = (InputStream) messageContext.getProperty("TRANSPORT_IN");
                    BufferedInputStream bufferedIn = new BufferedInputStream(in);
                    String key = target[0] + "." + target[1];
                    String randomKey = key + "." + Hashes.getRandom(10);
                    LinkedBlockingQueue<WalrusDataMessage> putQueue = getWriteMessenger().interruptAllAndGetQueue(key, randomKey);
                    int dataLength = 0;
                    try {
                        dataLength = bufferedIn.available();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                    Writer writer = new Writer(bufferedIn, dataLength, putQueue);
                    writer.start();

                    operationParams.put("ContentLength", (new Long(dataLength).toString()));
                    operationParams.put(WalrusProperties.Headers.RandomKey.toString(), randomKey);
                } else if(verb.equals(HTTPVerb.GET.toString())) {
                    messageContext.setProperty(WalrusProperties.STREAMING_HTTP_GET, Boolean.TRUE);
                    if(!walrusInternalOperation) {
                        operationParams.put("GetData", Boolean.TRUE);
                        operationParams.put("InlineData", Boolean.FALSE);
                        operationParams.put("GetMetaData", Boolean.FALSE);

                        Iterator<String> iterator = headers.keySet().iterator();
                        boolean isExtendedGet = false;
                        while(iterator.hasNext()) {
                            String key = iterator.next();
                            for(WalrusProperties.ExtendedGetHeaders header: WalrusProperties.ExtendedGetHeaders.values()) {
                                if(key.toLowerCase().equals(header.toString().toLowerCase())) {
                                    String value = headers.get(key);
                                    isExtendedGet = true;
                                    parseExtendedGetHeaders(operationParams, header.toString(), value);
                                }
                            }

                        }
                        if(isExtendedGet) {
                            operationKey += "extended";
                            //only supported through SOAP
                            operationParams.put("ReturnCompleteObjectOnConditionFailure", Boolean.FALSE);
                        }
                    }
                } else if(verb.equals(HTTPVerb.HEAD.toString())) {
                    messageContext.setProperty(WalrusProperties.STREAMING_HTTP_GET, Boolean.FALSE);
                    operationParams.put("GetData", Boolean.FALSE);
                    operationParams.put("InlineData", Boolean.FALSE);
                    operationParams.put("GetMetaData", Boolean.FALSE);
                }
            }
        }

        if (verb.equals(HTTPVerb.PUT.toString()) && params.containsKey(OperationParameter.acl.toString())) {
            //read ACL
            try {
                InputStream in = (InputStream) messageContext.getProperty("TRANSPORT_IN");
                BufferedInputStream bufferedIn = new BufferedInputStream(in);

                byte[] bytes = new byte[DATA_MESSAGE_SIZE];

                int bytesRead;
                String aclString = "";
                while ((bytesRead = bufferedIn.read(bytes)) > 0) {
                    aclString += new String(bytes, 0, bytesRead);
                }
                if(aclString.length() > 0) {
                    XMLParser xmlParser = new XMLParser(aclString);
                    AccessControlPolicyType accessControlPolicy = new AccessControlPolicyType();
                    String ownerId = xmlParser.getValue("//Owner/ID");
                    String displayName = xmlParser.getValue("//Owner/DisplayName");

                    CanonicalUserType canonicalUser = new CanonicalUserType(ownerId, displayName);
                    accessControlPolicy.setOwner(canonicalUser);

                    AccessControlListType accessControlList = new AccessControlListType();
                    ArrayList<Grant> grants = new ArrayList<Grant>();

                    List<String> displayNames = xmlParser.getValues("//AccessControlList/Grant/Grantee/DisplayName");
                    List<String> ids = xmlParser.getValues("//AccessControlList/Grant/Grantee/ID");
                    List<String> permissions = xmlParser.getValues("//AccessControlList/Grant/Permission");

                    if((ids.size() == permissions.size()) && (ids.size() == displayNames.size())) {
                        for(int i=0; i < ids.size(); ++i) {
                            Grant grant = new Grant();
                            grant.setGrantee(new CanonicalUserType(ids.get(i), displayNames.get(i)));
                            grant.setPermission(permissions.get(i));
                            grants.add(grant);
                        }
                    }

                    accessControlList.setGrants(grants);
                    accessControlPolicy.setAccessControlList(accessControlList);

                    operationParams.put("AccessControlPolicy", accessControlPolicy);
                }
            } catch(Exception ex) {
                LOG.warn(ex, ex);
            }

        }

        ArrayList paramsToRemove = new ArrayList();

        boolean addMore = true;
        Iterator iterator = params.keySet().iterator();
        while(iterator.hasNext()) {
            Object key = iterator.next();
            String keyString = key.toString().toLowerCase();
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

        String operationName;
        if(headers.containsKey(StorageProperties.EUCALYPTUS_OPERATION)) {
            operationName = headers.get(StorageProperties.EUCALYPTUS_OPERATION);
            if(operationName.equals(WalrusProperties.StorageOperations.StoreSnapshot.toString())) {
                //get http params and add that to snapshot values
                ArrayList<String> snapshotValues = new ArrayList<String>();
                Set<String> paramKeySet = params.keySet();
                for(String paramKey : paramKeySet) {
                    if(paramKey.equals(WalrusProperties.StorageParameters.SnapshotVgName.toString()) ||
                            paramKey.equals(WalrusProperties.StorageParameters.SnapshotLvName.toString()))
                                snapshotValues.add(params.get(paramKey));
                }
                operationParams.put("SnapshotValues", snapshotValues);
            }
        } else {
            operationName = operationMap.get(operationKey);
        }
        httpRequest.setBindingArguments(operationParams);
        messageContext.setProperty(WalrusProperties.WALRUS_OPERATION, operationName);
        return operationName;
    }

    private void parseExtendedGetHeaders(Map operationParams, String headerString, String value) {
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
                operationParams.put(headerString, DateUtils.parseDate(value, null));
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        } else {
            operationParams.put(headerString, value);
        }
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

        private BufferedInputStream in;
        private long dataLength;
        private LinkedBlockingQueue<WalrusDataMessage> putQueue;
        public Writer(BufferedInputStream in, long dataLength, LinkedBlockingQueue<WalrusDataMessage> putQueue) {
            this.in = in;
            this.dataLength = dataLength;
            this.putQueue = putQueue;
        }

        public void run() {
            byte[] bytes = new byte[DATA_MESSAGE_SIZE];


            try {
                putQueue.put(WalrusDataMessage.StartOfData(dataLength));

                int bytesRead = 0;
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