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

import edu.ucsb.eucalyptus.util.WalrusDataMessenger;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class StorageQueryDispatcher extends GenericHttpDispatcher implements RESTfulDispatcher {

    public static final String NAME = "StorageQueryDispatcher";
    public static final String IS_QUERY_REQUEST = NAME + "_IS_QUERY_REQUEST";
    public static final String BINDING_NAMESPACE = NAME + "_BINDING_NAMESPACE";
    private static Logger LOG = Logger.getLogger( StorageQueryDispatcher.class );
    private static String STORAGE_NAMESPACE =  "http://storagecontroller.eucalyptus.ucsb.edu";

    private static String walrusBaseAddress = "/services/StorageController/";
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

        newMap.put(OBJECT + StorageQueryDispatcher.HTTPVerb.GET.toString(), "GetDecryptedImage");

        return newMap;
    }


    public enum HTTPVerb {
        GET, PUT, DELETE, POST;
    }

    public enum OperationParameter {

        acl, location;

        private static String patterh = buildPattern();

        private static String buildPattern()
        {
            StringBuilder s = new StringBuilder();
            for ( OperationParameter op : OperationParameter.values() ) s.append( "(" ).append( op.name() ).append( ")|" );
            s.deleteCharAt( s.length() - 1 );
            return s.toString();
        }

        public static String toPattern()
        {
            return patterh;
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


  public boolean accepts( final HttpRequest httpRequest, final MessageContext messageContext )
    {
        //:: decide about whether or not to accept the request for processing :://
        for( StorageQuerySecurityHandler.StorageSecurityParameters p : StorageQuerySecurityHandler.StorageSecurityParameters.values() )
            if( !httpRequest.getHeaders().containsKey( p.toString() ) ) return false;
        return true;
    }

    public String getOperation( HttpRequest httpRequest, MessageContext messageContext )
    {
        //Figure out if it is an operation on the service, a bucket or an object
        Map operationParams = new HashMap();
        String[] target = null;
        String path = httpRequest.getOperationPath();
        if(path.length() > 0) {
            target = getTarget(path);
        }

        String verb = httpRequest.getHttpMethod();
        String operationKey = "";
        Map<String, String> params = httpRequest.getParameters();
        if(target == null) {
            //target = service
            operationKey = SERVICE + verb;
        } else if(target.length < 2) {
            //target = bucket
            operationKey = BUCKET + verb;
            operationParams.put("Bucket", target[0]);
        } else {
            //target = object
            operationKey = OBJECT + verb;
            operationParams.put("Bucket", target[0]);
            operationParams.put("Key", target[1]);
            messageContext.setProperty("STREAMING_HTTP_GET", Boolean.TRUE);
        }

        Iterator iterator = params.keySet().iterator();
        while(iterator.hasNext()) {
            operationKey += iterator.next().toString();
        }

        httpRequest.setBindingArguments(operationParams);
        String operationName = operationMap.get(operationKey);
        return operationName;
    }

    public QuerySecurityHandler getSecurityHandler()
    {
        return new StorageQuerySecurityHandler();
    }

    public QueryBinding getBinding()
    {
        return new StorageQueryBinding();
    }

    public String getNamespace() {
        return STORAGE_NAMESPACE;
    }

    public void initDispatcher()
    {
        init( new HandlerDescription( NAME ) );
    }

}