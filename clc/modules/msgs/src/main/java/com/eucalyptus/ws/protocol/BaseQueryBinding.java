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

package com.eucalyptus.ws.protocol;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.handlers.RestfulMarshallingHandler;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.BaseData;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import groovy.lang.GroovyObject;

public class BaseQueryBinding<T extends Enum<T>> extends RestfulMarshallingHandler {
  private static Logger LOG = Logger.getLogger( BaseQueryBinding.class );
  private final T       operationParam;
  private final List<T> altOperationParams;
  private final List<T> possibleParams;
  
  /**
   * @param namespacePattern - the format string to be used when constructing the namespace. this
   *          can be a fully formed namespace.
   * @param operationParam - this argument is used to determine the list of possible operation
   *          parameters
   * @param alternativeOperationParam - these arguments are treated as alternatives to
   *          <tt>operationParam</tt> (e.g., <tt>Action</tt> is an alternative to <tt>Operation</tt>
   *          ).
   */
  public BaseQueryBinding( final String namespacePattern, final T operationParam, final T... alternativeOperationParam ) {
    super( namespacePattern );
    this.operationParam = operationParam;
    this.altOperationParams = Arrays.asList( alternativeOperationParam );
    this.possibleParams = Arrays.asList( operationParam.getDeclaringClass( ).getEnumConstants( ) );
  }
  
  /**
   * @param namespacePattern - the format string to be used when constructing the namespace. this
   *          can be a fully formed namespace.
   * @param defaultVersion - default version to use if binding problems are encountered (e.g.,
   *          unknown request namespace).
   * @param operationParam - this argument is used to determine the list of possible operation
   *          parameters
   * @param alternativeOperationParam - these arguments are treated as alternatives to
   *          <tt>operationParam</tt> (e.g., <tt>Action</tt> is an alternative to <tt>Operation</tt>
   *          ).
   */
  public BaseQueryBinding( final String namespacePattern, final String defaultVersion, final T operationParam, final T... alternativeOperationParam ) {
    super( namespacePattern, defaultVersion );
    this.operationParam = operationParam;
    this.altOperationParams = Arrays.asList( alternativeOperationParam );
    this.possibleParams = Arrays.asList( operationParam.getDeclaringClass( ).getEnumConstants( ) );
  }
  
  private String extractOperationName( final MappingHttpRequest httpRequest ) {
    if ( httpRequest.getParameters( ).containsKey( this.operationParam.toString( ) ) ) {
      return httpRequest.getParameters( ).get( this.operationParam.toString( ) );
    } else {
      for ( final T param : this.altOperationParams ) {
        if ( httpRequest.getParameters( ).containsKey( param.toString( ) ) ) {
          return httpRequest.getParameters( ).get( param.toString( ) );
        }
      }
    }
    LOG.error( "Failed to find operation parameter an " + Lists.asList( this.operationParam, this.altOperationParams.toArray( ) ).toString( )
               + " in HTTP request: " + httpRequest );
    return null;
  }
  
  @Override
  public Object bind( final MappingHttpRequest httpRequest ) throws BindingException {
    final String operationName = this.extractOperationName( httpRequest );
    final String operationNameType = operationName + "Type";
    for ( final T op : this.possibleParams )
      httpRequest.getParameters( ).remove( op.name( ) );
    final Map<String, String> params = httpRequest.getParameters( );
    
    BaseMessage eucaMsg;
    Map<String, String> fieldMap;
    Binding currentBinding;
    try {
      currentBinding = getBindingWithElementClass( operationName );
      Class<?> targetType = currentBinding==null ? null : currentBinding.getElementClass( operationName );
      if ( currentBinding == null ) {
        currentBinding = getBindingWithElementClass( operationNameType );
        targetType = currentBinding==null ? null : currentBinding.getElementClass( operationNameType );
      }
      if ( currentBinding == null ) {
        //this will necessarily fault.
        try {
          targetType = this.getBinding( ).getElementClass( operationName );
        } catch ( final BindingException ex ) {
          LOG.error( ex, ex );
          throw ex;
        }
      }
      fieldMap = this.buildFieldMap( targetType );
      eucaMsg = ( BaseMessage ) targetType.newInstance( );
    } catch ( final BindingException e ) {
      LOG.debug( "Failed to construct message of type: " + operationName, e );
      LOG.error( e, e );
      throw e;
    } catch ( final Exception e ) {
      throw new BindingException( "Failed to construct message of type " + operationName, e );
    }
    
    final List<String> failedMappings = this.populateObject( ( GroovyObject ) eucaMsg, fieldMap, params );
    
    if ( !failedMappings.isEmpty( ) || !params.isEmpty( ) ) {
      final StringBuilder errMsg = new StringBuilder( "Failed to bind the following fields:\n" );
      for ( final String f : failedMappings )
        errMsg.append( f ).append( '\n' );
      for ( final Map.Entry<String, String> f : params.entrySet( ) )
        errMsg.append( f.getKey( ) ).append( " = " ).append( f.getValue( ) ).append( '\n' );
      throw new BindingException( errMsg.toString( ) );
    }

    validateBinding( currentBinding, operationName, params, eucaMsg );
    
    return eucaMsg;
  }

  protected Binding getBindingWithElementClass( final String operationName ) throws BindingException {
    Binding binding = null;
    if ( this.getBinding( ).hasElementClass( operationName ) ) {
      binding = this.getBinding( );
    } else if ( this.getDefaultBinding().hasElementClass( operationName ) ) {
      binding = this.getDefaultBinding();
    } else if ( BindingManager.getDefaultBinding().hasElementClass( operationName ) ) {
      binding = BindingManager.getDefaultBinding();
    }
    return binding;
  }

  protected void validateBinding( final Binding currentBinding, 
                                  final String operationName,
                                  final Map<String, String> params, 
                                  final BaseMessage eucaMsg ) throws BindingException {
    try {
      currentBinding.toOM( eucaMsg, this.getNamespace( ) );
    } catch ( final RuntimeException e ) {
      LOG.error( "Falling back to default (unvalidated) binding for: " + operationName + " with params=" + params );
      LOG.error( "Failed to build a valid message: " + e.getMessage( ), e );
      try {
        BindingManager.getDefaultBinding().toOM( eucaMsg, BindingManager.defaultBindingNamespace( ) );
      } catch ( final RuntimeException ex ) {
        throw new BindingException( "Default binding failed to build a valid message: " + ex.getMessage( ), ex );
      }
    }
  }

  private static Field getRecursiveField( Class<?> clazz, final String fieldName ) throws Exception {
    Exception e = null;
    while ( !BaseMessage.class.equals( clazz ) && !Object.class.equals( clazz ) ) {
      try {
        return clazz.getDeclaredField( fieldName );
      } catch ( final Exception e1 ) {
        e = e1;        
      }
      clazz = clazz.getSuperclass( );
    }
    if ( e == null ) throw new Exception("Class not supported: " + clazz);
    throw e;
  }
  
  private List<String> populateObject( final GroovyObject obj, final Map<String, String> paramFieldMap, final Map<String, String> params ) {
    final List<String> failedMappings = new ArrayList<String>( );
    for ( final Map.Entry<String, String> e : paramFieldMap.entrySet( ) ) {
      try {
        if ( getRecursiveField( obj.getClass( ), e.getValue( ) ).getType( ).equals( ArrayList.class ) ) {
          failedMappings.addAll( this.populateObjectList( obj, e, params, params.size( ) ) );
        }
      } catch ( final Exception e1 ) {
        LOG.debug( "Failed mapping : ", e1 );
        failedMappings.add( e.getKey( ) );
      }
    }
    
    for ( final Map.Entry<String, String> e : paramFieldMap.entrySet( ) ) {
      Class<?> declaredType = null;
      try {
        declaredType = getRecursiveField( obj.getClass( ), e.getValue( ) ).getType( );
      } catch ( final Exception e2 ) {
        LOG.debug( "Field not found: " + e.getValue(), e2 );
      }
      
      if ( params.containsKey( e.getKey( ) )
           && !this.populateObjectField( obj, e, params ) ) {
        failedMappings.add( e.getKey( ) );
      } else if ( ( declaredType != null )
                  && EucalyptusData.class.isAssignableFrom( declaredType ) ) {
        try {
          final Map<String, String> fieldMap = this.buildFieldMap( declaredType );
          final Object newInstance = declaredType.newInstance( );
          final Map<String, String> subParams = Maps.newHashMap( );
          
          for ( final String item : Sets.newHashSet( params.keySet( ) ) ) {
            if ( item.startsWith( e.getKey( ) ) ) {
              params.get( item );
              subParams.put( item.replace( e.getKey( ) + ".", "" ), params.remove( item ) );
            }
          }
          this.populateObject( ( GroovyObject ) newInstance, fieldMap, subParams );
          obj.setProperty( e.getValue( ), newInstance );
        } catch ( final Exception e1 ) {
          LOG.debug( "Error binding object", e1 );
        }
      } else {
        failedMappings.remove( e.getKey( ) );
      }
      
    }
    return failedMappings;
  }
  
  @SuppressWarnings( "unchecked" )
  private boolean populateObjectField( final GroovyObject obj, final Map.Entry<String, String> paramFieldPair, final Map<String, String> params ) {
    try {
      final Class<?> declaredType = getRecursiveField( obj.getClass( ), paramFieldPair.getValue( ) ).getType( );
      final Object value = convertToType( new Supplier<String>(){
        @Override
        public String get() {
          return params.remove( paramFieldPair.getKey() );
        }
      }, declaredType );

      if ( value != null )
        obj.setProperty( paramFieldPair.getValue( ), value );
      
      return !params.containsKey( paramFieldPair.getKey() );
    } catch ( final Exception e1 ) {
      return false;
    }
  }
  
  private Object convertToType( final Supplier<String> value, final Class<?> targetType ) throws Exception {
    if ( targetType.equals( String.class ) )
      return value.get();
    else if ( targetType.getName( ).equals( "int" ) )
      return Integer.parseInt( value.get() );
    else if ( targetType.equals( Integer.class ) )
      return Integer.valueOf( value.get() );
    else if ( targetType.getName( ).equals( "boolean" ) )
      return Boolean.parseBoolean( value.get() );
    else if ( targetType.equals( Boolean.class ) )
      return Boolean.valueOf( value.get() );
    else if ( targetType.getName( ).equals( "long" ) )
      return Long.parseLong( value.get() );
    else if ( targetType.equals( Long.class ) )
      return Long.valueOf( value.get() );
    else if ( targetType.getName( ).equals( "double" ) )
      return Double.parseDouble( value.get() );
    else if ( targetType.equals( Double.class ) )
      return Double.valueOf( value.get() );
    else if ( targetType.equals( Date.class ) )
      return Timestamps.parseIso8601Timestamp( value.get() );
    else 
      return null;
  }
  
  @SuppressWarnings( "rawtypes" )
  private List<String> populateObjectList( final GroovyObject obj, final Map.Entry<String, String> paramFieldPair, final Map<String, String> params, final int paramSize ) {
    final List<String> failedMappings = new ArrayList<String>( );
    try {
      final Field declaredField = getRecursiveField( obj.getClass( ), paramFieldPair.getValue( ) );
      final ArrayList theList = ( ArrayList ) obj.getProperty( paramFieldPair.getValue( ) );
      final Class genericType = ( Class ) ( ( ParameterizedType ) declaredField.getGenericType( ) ).getActualTypeArguments( )[0];
      // :: simple case: FieldName.# :://
      if ( String.class.equals( genericType ) ||
           Boolean.class.equals( genericType ) ||
           Integer.class.equals( genericType ) ||
           Long.class.equals( genericType ) ||
           Double.class.equals( genericType ) ||
           Date.class.equals( genericType ) ) {
        if ( params.containsKey( paramFieldPair.getKey( ) ) ) {
          theList.add( convertToType( Suppliers.ofInstance(params.remove( paramFieldPair.getKey() )), genericType ) );
        } else {
          final List<String> keys = Lists.newArrayList( params.keySet( ) );
          final Pattern paramPattern = Pattern.compile( Pattern.quote(paramFieldPair.getKey( )) + "\\.([1-9][0-9]*)" );
          final Map<String,Object> indexToValueMap = new TreeMap<String,Object>( Ordering.natural().onResultOf( FunctionToInteger.INSTANCE ) );
          for ( final String k : keys ) {    
            final Matcher matcher = paramPattern.matcher( k );
            if ( matcher.matches() ) {
              indexToValueMap.put( matcher.group(1), convertToType( Suppliers.ofInstance(params.remove( k )), genericType )  );
            }
          }
          theList.addAll( indexToValueMap.values() );
        }
      } else if ( declaredField.isAnnotationPresent( HttpEmbedded.class ) ) {
        final HttpEmbedded annoteEmbedded = declaredField.getAnnotation( HttpEmbedded.class );
        // :: build the parameter map and call populate object recursively :://
        if ( annoteEmbedded.multiple( ) ) {
          final List<String> keys = Lists.newArrayList( params.keySet( ) );
          final Map<String,Map<String,String>> subParamMaps = new TreeMap<String,Map<String,String>>( Ordering.natural().onResultOf( FunctionToInteger.INSTANCE ) );
          for ( final String k : keys ) {            
            if ( k.startsWith( paramFieldPair.getKey( ) + "." ) ) {
              final String currentValue = params.remove( k );
              final String setKey = k.replaceAll( "^"+ paramFieldPair.getKey( ) + "\\.(\\d+)\\..*", "$1" );
              final String subKey = k.replaceAll( "^"+ paramFieldPair.getKey( ) + "\\.\\d+\\." , "" );
              Map<String,String> subMap = subParamMaps.get( setKey );
              if ( subMap == null ) {
                subParamMaps.put( setKey, subMap = Maps.newHashMap() );  
              }

              subMap.put( subKey, currentValue );
            }
          }
          
          for ( final Map<String,String> subParams : subParamMaps.values() ) {
            failedMappings.addAll( this.populateEmbedded( genericType, subParams, theList ) );
          }
        } else {
          failedMappings.addAll( this.populateEmbedded( genericType, params, theList ) );
        }
      }
    } catch ( final Exception e1 ) {
      LOG.debug( "FAILED HERE : ", e1 );
      failedMappings.add( paramFieldPair.getKey( ) );
    }
    return failedMappings;
  }
  
  private List<String> populateEmbedded( final Class<?> genericType, final Map<String, String> params, @SuppressWarnings( "rawtypes" ) final ArrayList theList ) throws InstantiationException, IllegalAccessException {
    final GroovyObject embedded = ( GroovyObject ) genericType.newInstance( );
    final Map<String, String> embeddedFields = this.buildFieldMap( genericType );
    final int startSize = params.size( );
    final List<String> embeddedFailures = this.populateObject( embedded, embeddedFields, params );
    if ( embeddedFailures.isEmpty( ) && !( params.size( ) - startSize == 0 ) )
      theList.add( embedded );
    
    return embeddedFailures;
  }
  
  private Map<String, String> buildFieldMap( Class<?> targetType ) {
    final Map<String, String> fieldMap = new HashMap<String, String>( );
    while ( !BaseMessage.class.equals( targetType ) && !EucalyptusMessage.class.equals( targetType ) && !EucalyptusData.class.equals( targetType )
            && !BaseData.class.equals( targetType ) ) {
      final Field[] fields = targetType.getDeclaredFields( );
      for ( final Field f : fields ) {
        if ( Modifier.isStatic( f.getModifiers( ) ) )
          continue;
        else if ( f.isAnnotationPresent( HttpParameterMapping.class ) ) {
          fieldMap.put( f.getAnnotation( HttpParameterMapping.class ).parameter( ), f.getName( ) );
          fieldMap.put( f.getName( ).substring( 0, 1 ).toUpperCase( ).concat( f.getName( ).substring( 1 ) ), f.getName( ) );
        } else {
          fieldMap.put( f.getName( ).substring( 0, 1 ).toUpperCase( ).concat( f.getName( ).substring( 1 ) ), f.getName( ) );
        }
      }
      targetType = targetType.getSuperclass( );
    }
    return fieldMap;
  }
  
  private enum FunctionToInteger implements Function<String,Integer> {
    INSTANCE {
      @Override
      public Integer apply(final String parameterIndex ) {
        Integer result = Integer.MAX_VALUE;
        try {
          result = Integer.valueOf( parameterIndex );
        } catch ( NumberFormatException nfe ) {
          // use default
        }
        return result;
      }
    }
  }
}
