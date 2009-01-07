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

import edu.ucsb.eucalyptus.annotation.HttpEmbedded;
import edu.ucsb.eucalyptus.annotation.HttpParameterMapping;
import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.msgs.AccessControlListType;
import edu.ucsb.eucalyptus.msgs.CanonicalUserType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.Grant;
import edu.ucsb.eucalyptus.transport.binding.Binding;
import edu.ucsb.eucalyptus.transport.binding.BindingManager;
import edu.ucsb.eucalyptus.util.BindingUtil;
import groovy.lang.GroovyObject;
import org.apache.axiom.om.OMElement;
import org.apache.log4j.Logger;
import org.apache.axis2.context.MessageContext;
import org.jibx.runtime.JiBXException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;

public class StorageQueryBinding implements QueryBinding {

    private static Logger LOG = Logger.getLogger( StorageQueryBinding.class );

    public static final String namespace = BindingUtil.sanitizeNamespace( "http://msgs.eucalyptus.ucsb.edu" );

    public String getName()
    {
        return "";
    }

    public QueryBinding getInstance()
    {
        return new StorageQueryBinding();
    }

    public OMElement bind(UserInfo userId, HttpRequest httpRequest, MessageContext messageContext) throws QueryBindingException {
        return bind (httpRequest.getOperation(), userId, httpRequest.getParameters(), httpRequest.getBindingArguments(), httpRequest.getHeaders());
    }

    public OMElement bind( final String operationName, final UserInfo user, final Map<String, String> params, Map bindingArguments, final Map<String, String> headers ) throws QueryBindingException
    {
        OMElement msg = null;

        EucalyptusMessage eucaMsg = null;
        Map<String, String> fieldMap = null;
        Class targetType = null;
        try
        {
            //:: try to create the target class :://
            targetType = Class.forName( "edu.ucsb.eucalyptus.msgs.".concat( operationName ).concat( "Type" ) );
            //:: get the map of parameters to fields :://
            fieldMap = this.buildFieldMap( targetType );
            //:: get an instance of the message :://
            eucaMsg = ( EucalyptusMessage ) targetType.newInstance();
        }
        catch ( Exception e ) {
            throw new QueryBindingException( "Failed to construct message of type " + operationName );
        }

        List<String> failedMappings = populateObject( eucaMsg, fieldMap, params, bindingArguments);

        if ( !failedMappings.isEmpty() || !params.isEmpty() )
        {
            StringBuilder errMsg = new StringBuilder( "Failed to bind the following fields:\n" );
            for ( String f : failedMappings )
                errMsg.append( f ).append( '\n' );
            for ( Map.Entry<String, String> f : params.entrySet() )
                errMsg.append( f.getKey() ).append( " = " ).append( f.getValue() ).append( '\n' );
            throw new QueryBindingException( errMsg.toString() );
        }

        eucaMsg.setUserId( user.getUserName() );
        eucaMsg.setEffectiveUserId( user.isAdministrator() ? "eucalyptus" : user.getUserName() );

        LOG.warn(eucaMsg.toString());
        try
        {
            Binding binding = BindingManager.getBinding( namespace );
            msg = binding.toOM( eucaMsg );
        }
        catch ( JiBXException e )
        {
            throw new QueryBindingException( "Failed to build a valid message: " + e.getMessage() );
        }

        return msg;
    }


    private List<String> populateObject( final GroovyObject obj, final Map<String, String> paramFieldMap, final Map<String, String> params, Map bindingMap) {
        List<String> failedMappings = new ArrayList<String>();
        for ( Map.Entry<String, String> e : paramFieldMap.entrySet() )
        {
            try
            {
                if ( obj.getClass().getDeclaredField( e.getValue() ).getType().equals( ArrayList.class ) )
                    failedMappings.addAll( populateObjectList( obj, e, params, params.size() ) );
                else if ( params.containsKey( e.getKey() ) && !populateObjectField( obj, e, params ) )
                    failedMappings.add( e.getKey() );
                else if (bindingMap != null && bindingMap.containsKey(e.getKey())) {
                    obj.setProperty(e.getValue(), bindingMap.get(e.getKey()));
                }

            }
            catch ( NoSuchFieldException ex )
            {
                failedMappings.add( e.getKey() );
            }
        }
        return failedMappings;
    }

    private boolean populateObjectField( final GroovyObject obj, final Map.Entry<String, String> paramFieldPair, final Map<String, String> params )
    {
        try
        {
            Class declaredType = obj.getClass().getDeclaredField( paramFieldPair.getValue() ).getType();
            if ( declaredType.equals( String.class ) )
                obj.setProperty( paramFieldPair.getValue(), params.remove( paramFieldPair.getKey() ) );
            else if ( declaredType.getName().equals( "int" ) )
                obj.setProperty( paramFieldPair.getValue(), Integer.parseInt( params.remove( paramFieldPair.getKey() ) ) );
            else
                return false;
            return true;
        }
        catch ( Exception e1 )
        {
            return false;
        }
    }

    private List<String> populateObjectList( final GroovyObject obj, final Map.Entry<String, String> paramFieldPair, final Map<String, String> params, final int paramSize )
    {
        List<String> failedMappings = new ArrayList<String>();
        try
        {
            Field declaredField = obj.getClass().getDeclaredField( paramFieldPair.getValue() );
            ArrayList theList = ( ArrayList ) obj.getProperty( paramFieldPair.getValue() );
            Class genericType = ( Class ) ( ( ParameterizedType ) declaredField.getGenericType() ).getActualTypeArguments()[ 0 ];
            //:: simple case: FieldName.# :://
            if ( String.class.equals( genericType ) )
            {
                if ( params.containsKey( paramFieldPair.getKey() ) )
                    theList.add( params.remove( paramFieldPair.getKey() ) );
                else
                    for ( int i = 0; i < paramSize + 1; i++ )
                        if ( params.containsKey( paramFieldPair.getKey() + "." + i ) )
                            theList.add( params.remove( paramFieldPair.getKey() + "." + i ) );
            }
            else if ( declaredField.isAnnotationPresent( HttpEmbedded.class ) )
            {
                HttpEmbedded annoteEmbedded = ( HttpEmbedded ) declaredField.getAnnotation( HttpEmbedded.class );
                //:: build the parameter map and call populate object recursively :://
                if ( annoteEmbedded.multiple() )
                {
                    String prefix = paramFieldPair.getKey();
                    List<String> embeddedListFieldNames = new ArrayList<String>();
                    for ( String actualParameterName : params.keySet() )
                        if ( actualParameterName.matches( prefix + ".1.*" ) )
                            embeddedListFieldNames.add( actualParameterName.replaceAll( prefix + ".1.", "" ) );
                    for ( int i = 0; i < paramSize + 1; i++ )
                    {
                        boolean foundAll = true;
                        Map<String, String> embeddedParams = new HashMap<String, String>();
                        for ( String fieldName : embeddedListFieldNames )
                        {
                            String paramName = prefix + "." + i + "." + fieldName;
                            if ( !params.containsKey( paramName ) )
                            {
                                failedMappings.add( "Mismatched embedded field: " + paramName );
                                foundAll = false;
                            }
                            else
                                embeddedParams.put( fieldName, params.get( paramName ) );
                        }
                        if ( foundAll )
                            failedMappings.addAll( populateEmbedded( genericType, embeddedParams, theList ) );
                        else
                            break;
                    }
                }
                else
                    failedMappings.addAll( populateEmbedded( genericType, params, theList ) );
            }
        }
        catch ( Exception e1 )
        {
            failedMappings.add( paramFieldPair.getKey() );
        }
        return failedMappings;
    }

    private List<String> populateEmbedded( final Class genericType, final Map<String, String> params, final ArrayList theList ) throws InstantiationException, IllegalAccessException
    {
        GroovyObject embedded = ( GroovyObject ) genericType.newInstance();
        Map<String, String> embeddedFields = buildFieldMap( genericType );
        int startSize = params.size();
        List<String> embeddedFailures = populateObject( embedded, embeddedFields, params, null );
        if ( embeddedFailures.isEmpty() && !(params.size() - startSize == 0) )
            theList.add( embedded );
        return embeddedFailures;
    }

    private Map<String, String> buildFieldMap( final Class targetType )
    {
        Map<String, String> fieldMap = new HashMap<String, String>();
        Field[] fields = targetType.getDeclaredFields();
        for ( Field f : fields )
            if ( Modifier.isStatic( f.getModifiers() ) ) continue;
            else if ( f.isAnnotationPresent( HttpParameterMapping.class ) )
                fieldMap.put( f.getAnnotation( HttpParameterMapping.class ).parameter(), f.getName() );
            else
                fieldMap.put( f.getName().substring( 0, 1 ).toUpperCase().concat( f.getName().substring( 1 ) ), f.getName() );
        return fieldMap;
    }
}