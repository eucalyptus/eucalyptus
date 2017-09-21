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

package com.eucalyptus.ws.protocol;

import java.beans.Beans;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingElementNotFoundException;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpEmbeddeds;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.binding.HttpParameterMappings;
import com.eucalyptus.binding.HttpValue;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.StackConfiguration;
import com.eucalyptus.ws.handlers.RestfulMarshallingHandler;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
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
  private final UnknownParameterStrategy unknownParameterStrategy;
  private final T       operationParam;
  private final List<T> altOperationParams;
  private final List<T> possibleParams;

  public enum UnknownParameterStrategy {
    /**
     * Ignore unknown parameters
     */
    IGNORE,

    /**
     * Fail with a binding error for unknown parameters
     */
    ERROR,
    ;
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
  @SafeVarargs
  public BaseQueryBinding( final String namespacePattern,
                           final String defaultVersion,
                           final T operationParam,
                           final T... alternativeOperationParam ) {
    this( namespacePattern, defaultVersion, UnknownParameterStrategy.IGNORE, operationParam, alternativeOperationParam );
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
  @SafeVarargs
  public BaseQueryBinding( final String namespacePattern,
                           final String defaultVersion,
                           final UnknownParameterStrategy unknownParameterStrategy,
                           final T operationParam,
                           final T... alternativeOperationParam ) {
    super( namespacePattern, defaultVersion );
    this.unknownParameterStrategy = unknownParameterStrategy;
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
        targetType = this.getBinding( ).getElementClass( operationName );
      }
      fieldMap = this.buildFieldMap( targetType );
      eucaMsg = ( BaseMessage ) targetType.newInstance( );
    } catch ( final BindingException e ) {
      LOG.debug(
          "Failed to construct message of type: " + operationName,
          e instanceof BindingElementNotFoundException ? null : e );
      throw e;
    } catch ( final Exception e ) {
      throw new BindingException( "Failed to construct message of type " + operationName, e );
    }
    
    final List<String> failedMappings = this.populateObject( eucaMsg, fieldMap, params );
    
    if ( isStrictBinding( ) && ( !failedMappings.isEmpty( ) || !params.isEmpty( ) ) ) {
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
    } else if ( this.getDefaultBinding() != null && this.getDefaultBinding().hasElementClass( operationName ) ) {
      binding = this.getDefaultBinding();
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
      if ( getDefaultBinding( ) != null ) {
        try {
          getDefaultBinding( ).toOM( eucaMsg, this.getNamespace( ) );
        } catch ( final RuntimeException ex ) {
          throw new BindingException( "Failed to build a valid message: " + ex.getMessage( ), ex );
        }
      } else {
        throw new BindingException( "Failed to build a valid message: " + e.getMessage( ), e );
      }
    }
  }

  private boolean isStrictBinding( ) {
    final String strategy = StackConfiguration.UNKNOWN_PARAMETER_HANDLING;
    return
        "error".equalsIgnoreCase( strategy ) ||
        ( !"ignore".equalsIgnoreCase( strategy ) &&
            unknownParameterStrategy == UnknownParameterStrategy.ERROR );
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
  
  private List<String> populateObject( final Object obj, final Map<String, String> paramFieldMap, final Map<String, String> params ) {
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
      Field field = null;
      Class<?> declaredType = null;
      try {
        field = getRecursiveField( obj.getClass( ), e.getValue( ) );
        declaredType = field.getType( );
      } catch ( final Exception e2 ) {
        LOG.debug( "Field not found: " + e.getValue(), e2 );
      }
      
      if ( params.containsKey( e.getKey( ) )
           && ( declaredType == null || !EucalyptusData.class.isAssignableFrom( declaredType ) )
           && !this.populateObjectField( obj, e, params ) ) {
        failedMappings.add( e.getKey( ) );
      } else if ( ( declaredType != null )
                  && EucalyptusData.class.isAssignableFrom( declaredType ) ) {
        try {
          final Map<String, String> fieldMap = this.buildFieldMap( declaredType );
          final Object newInstance = declaredType.newInstance( );
          Map<String, String> subParams = Maps.newHashMap( );

          HttpEmbedded httpEmbedded = null;
          if ( field != null && (
              field.isAnnotationPresent( HttpEmbedded.class ) ||
              field.isAnnotationPresent( HttpEmbeddeds.class ) ) ) {
            httpEmbedded = getHttpEmbeddedAnnotation( field );
          }
          if ( httpEmbedded != null && !httpEmbedded.multiple( ) ) {
            subParams = params;
          } else {
            for ( final String item : Sets.newHashSet( params.keySet( ) ) ) {
              if ( item.startsWith( e.getKey( ) + "." ) || (item.equals( e.getKey( ) ) && isValueObject( declaredType ) )) {
                subParams.put( replaceStringPrefixIfExists(item, e.getKey( ) + ".", "" ), params.remove( item ) );
              }
            }
          }
          if ( !subParams.isEmpty( ) ) {
            if ( httpEmbedded == null && subParams.size( ) == 1 && subParams.keySet( ).contains( e.getKey( ) ) ) {
              try {
                if ( populateValue( declaredType, newInstance, Iterables.getOnlyElement( subParams.values( ) ) ).isEmpty( ) ) {
                  setObjectProperty( obj, e.getValue(), newInstance );
                  subParams.clear( );
                }
              } catch ( final IllegalArgumentException e2 ) { /*param not bound error occurs for this failure*/ }
              if ( subParams != params ) for ( Map.Entry<String, String> entry : subParams.entrySet( ) ) {
                params.put( entry.getKey( ), entry.getValue( ) );
              }
            } else {
              this.populateObject( newInstance, fieldMap, subParams );
              setObjectProperty( obj, e.getValue(), newInstance );
              if ( subParams != params ) for ( Map.Entry<String, String> entry : subParams.entrySet( ) ) {
                params.put( e.getKey( ) + "." + entry.getKey( ), entry.getValue( ) );
              }
            }
          } else if ( params.containsKey( e.getKey( ) ) ) {
            setObjectProperty( obj, e.getValue(), newInstance );
          }
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
  private boolean populateObjectField( final Object obj, final Map.Entry<String, String> paramFieldPair, final Map<String, String> params ) {
    try {
      final Class<?> declaredType = getRecursiveField( obj.getClass( ), paramFieldPair.getValue( ) ).getType( );
      final Object value = convertToType( new Supplier<String>(){
        @Override
        public String get() {
          return params.remove( paramFieldPair.getKey() );
        }
      }, declaredType );

      if ( value != null )
        setObjectProperty( obj, paramFieldPair.getValue( ), value );

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
  private List<String> populateObjectList( final Object obj, final Map.Entry<String, String> paramFieldPair, final Map<String, String> params, final int paramSize ) {
    final List<String> failedMappings = new ArrayList<String>( );
    try {
      final Field declaredField = getRecursiveField( obj.getClass( ), paramFieldPair.getValue( ) );
      final ArrayList theList = ( ArrayList ) getObjectProperty( obj, paramFieldPair.getValue( ) );
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
          final Pattern paramPattern = Pattern.compile( Pattern.quote(paramFieldPair.getKey( )) + "\\.([0-9]{1,7})" );
          final Map<String,Object> indexToValueMap = new TreeMap<String,Object>( Ordering.natural().onResultOf( FunctionToInteger.INSTANCE ) );
          for ( final String k : keys ) {    
            final Matcher matcher = paramPattern.matcher( k );
            if ( matcher.matches() ) {
              indexToValueMap.put( matcher.group(1), convertToType( Suppliers.ofInstance(params.remove( k )), genericType )  );
            }
          }
          theList.addAll( indexToValueMap.values() );
        }
      } else if ( declaredField.isAnnotationPresent( HttpEmbedded.class ) ||
                  declaredField.isAnnotationPresent( HttpEmbeddeds.class )) {
        final HttpEmbedded annoteEmbedded = getHttpEmbeddedAnnotation( declaredField );
        // :: build the parameter map and call populate object recursively :://
        if ( annoteEmbedded.multiple( ) ) {
          final List<String> keys = Lists.newArrayList( params.keySet( ) );
          final Map<String,Map<String,String>> subParamMaps = new TreeMap<>( Ordering.natural().onResultOf( FunctionToInteger.INSTANCE ) );
          final Map<String,String> valueMap = new TreeMap<>( Ordering.natural().onResultOf( FunctionToInteger.INSTANCE ) );
          for ( final String k : keys ) {
            if ( k.matches( Pattern.quote( paramFieldPair.getKey( ) ) + "\\.[0-9]{1,7}\\..*" ) ) {
              final String currentValue = params.remove( k );
              final String setKey = k.replaceAll( "^"+ paramFieldPair.getKey( ) + "\\.([0-9]{1,7})\\..*", "$1" );
              if ( setKey.length() > 7 ) continue;
              final String subKey = k.replaceAll( "^"+ paramFieldPair.getKey( ) + "\\.[0-9]{1,7}\\." , "" );
              Map<String,String> subMap = subParamMaps.get( setKey );
              if ( subMap == null ) {
                subParamMaps.put( setKey, subMap = Maps.newHashMap() );
              }

              subMap.put( subKey, currentValue );
            } else if ( k.matches( Pattern.quote( paramFieldPair.getKey( ) ) + "\\.[0-9]{1,7}" ) ) {
              final String currentValue = params.remove( k );
              final String orderKey = k.replaceAll( "^"+ paramFieldPair.getKey( ) + "\\.([0-9]{1,7})", "$1" );
              if ( orderKey.length() > 7 ) continue;
              valueMap.put( orderKey, currentValue );
            }
          }

          if ( subParamMaps.isEmpty( ) ) {
            for ( final String value : valueMap.values() ) {
              failedMappings.addAll( this.populateEmbedded( genericType, value, theList ) );
            }
          } else {
            for ( final Map<String,String> subParams : subParamMaps.values() ) {
              failedMappings.addAll( this.populateEmbedded( genericType, subParams, theList ) );
            }
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
    final Object embedded = genericType.newInstance( );
    final Map<String, String> embeddedFields = this.buildFieldMap( genericType );
    final int startSize = params.size( );
    final List<String> embeddedFailures = this.populateObject( embedded, embeddedFields, params );
    if ( embeddedFailures.isEmpty( ) && !( params.size( ) - startSize == 0 ) )
      theList.add( embedded );
    
    return embeddedFailures;
  }

  private List<String> populateEmbedded( final Class<?> genericType, final String value, @SuppressWarnings( "rawtypes" ) final ArrayList theList ) throws InstantiationException, IllegalAccessException {
    final Object embedded = genericType.newInstance( );
    final List<String> embeddedFailures = populateValue( genericType, embedded, value );
    if ( embeddedFailures.isEmpty( ) ) {
      theList.add( embedded );
    }
    return embeddedFailures;
  }

  private List<String> populateValue( final Class<?> genericType, final Object targetObject, final String value ) throws InstantiationException, IllegalAccessException {
    final Field valueField = this.findValueField( genericType );
    if ( valueField == null ) {
      throw new IllegalArgumentException( "Simple type cannot be mapped for " + genericType.getSimpleName( ) );
    }
    final List<String> embeddedFailures = this.populateObject(
        targetObject,
        Maps.newHashMap( Collections.singletonMap( "value", valueField.getName() ) ),
        Maps.newHashMap( Collections.singletonMap( "value", value ) ) );

    return embeddedFailures;
  }

  @Nullable
  private Field findValueField( Class<?> targetType ) {
    while ( !BaseMessage.class.equals( targetType ) && !EucalyptusMessage.class.equals( targetType ) && !EucalyptusData.class.equals( targetType )
        && !BaseData.class.equals( targetType ) ) {
      final Field[] fields = targetType.getDeclaredFields( );
      for ( final Field f : fields ) {
        if ( Modifier.isStatic( f.getModifiers( ) ) ) {
          continue;
        } else if ( f.isAnnotationPresent( HttpValue.class ) ) {
          return f;
        }
      }
      targetType = targetType.getSuperclass( );
    }
    return null;
  }

  private boolean isValueObject( final Class<?> targetType ) {
    return findValueField( targetType ) != null;
  }

  private Map<String, String> buildFieldMap( Class<?> targetType ) {
    final Map<String, String> fieldMap = new HashMap<String, String>( );
    while ( !BaseMessage.class.equals( targetType ) && !EucalyptusMessage.class.equals( targetType ) && !EucalyptusData.class.equals( targetType )
            && !BaseData.class.equals( targetType ) ) {
      final Field[] fields = targetType.getDeclaredFields( );
      for ( final Field f : fields ) {
        if ( Modifier.isStatic( f.getModifiers( ) ) )
          continue;
        else if ( f.isAnnotationPresent( HttpParameterMapping.class ) || f.isAnnotationPresent( HttpParameterMappings.class ) ) {
          for ( String parameter : getHttpParameterMappingAnnotation( f ).parameter() ) {
            fieldMap.put( parameter, f.getName( ) );
          }
        } else {
          fieldMap.put( f.getName( ).substring( 0, 1 ).toUpperCase( ).concat( f.getName( ).substring( 1 ) ), f.getName( ) );
        }
      }
      targetType = targetType.getSuperclass( );
    }
    return fieldMap;
  }

  private void setObjectProperty( final Object target, final String property, final Object value ) {
    if ( target instanceof GroovyObject ) {
      ((GroovyObject)target).setProperty( property, value );
    } else {
      ReflectionUtils.invokeMethod(
          BeanUtils.getPropertyDescriptor( target.getClass( ), property ).getWriteMethod( ),
          target,
          value );
    }
  }

  private Object getObjectProperty( final Object target, final String property ) {
    if ( target instanceof GroovyObject ) {
      return ((GroovyObject)target).getProperty( property );
    } else {
      return ReflectionUtils.invokeMethod(
          BeanUtils.getPropertyDescriptor( target.getClass( ), property ).getReadMethod( ),
          target );
    }
  }

  private HttpEmbedded getHttpEmbeddedAnnotation( final Field field ) {
    if ( field.isAnnotationPresent( HttpEmbedded.class ) ) {
      return field.getAnnotation( HttpEmbedded.class );
    } else {
      return getVersionedAnnotation(
          field.getAnnotation( HttpEmbeddeds.class ).value(),
          HttpEmbeddedVersionExtractor.INSTANCE );
    }
  }

  private HttpParameterMapping getHttpParameterMappingAnnotation( final Field field ) {
    if ( field.isAnnotationPresent( HttpParameterMapping.class ) ) {
      return field.getAnnotation( HttpParameterMapping.class );
    } else {
      return getVersionedAnnotation(
          field.getAnnotation( HttpParameterMappings.class ).value(),
          HttpParameterMappingVersionExtractor.INSTANCE );
    }
  }

  private <T extends Annotation> T getVersionedAnnotation( final T[] values,
                                                           final Function<T,String> versionExtractor ) {
    for ( final T t : values ) {
      final String version = versionExtractor.apply( t );
      if ( Strings.isNullOrEmpty( version ) ) continue;
      if ( getNamespace().compareTo( getNamespaceForVersion( version ) ) < 1 ) {
        return t;
      }
    }
    return values[ values.length - 1 ];
  }

  private enum HttpEmbeddedVersionExtractor implements Function<HttpEmbedded,String> {
    INSTANCE;

    @Override
    public String apply( final HttpEmbedded httpEmbedded ) {
      return httpEmbedded.version();
    }
  }

  private enum HttpParameterMappingVersionExtractor implements Function<HttpParameterMapping,String> {
    INSTANCE;

    @Override
    public String apply( final HttpParameterMapping httpParameterMapping ) {
      return httpParameterMapping.version();
    }
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

  public static String replaceStringPrefixIfExists(String target, String oldPrefix, String newPrefix) {
    if (target == null) throw new NullPointerException("target can not be null");
    if (oldPrefix == null) throw new NullPointerException("oldPrefix can not be null");
    if (newPrefix == null) throw new NullPointerException("newPrefix can not be null");
    return target.startsWith(oldPrefix) ? newPrefix + target.substring(oldPrefix.length()) : target;
  }

}
