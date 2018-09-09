/**
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.binding;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Beans;
import com.eucalyptus.util.ThrowingFunction;
import com.eucalyptus.ws.protocol.BaseQueryBinding;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

/**
 *
 */
public class RestBinding {
  private static final Pattern PARAMETER_MATCHER = Pattern.compile( "\\{([A-Za-z0-9_-]{1,256})}" );
  private static final String PARAMETER_REPLACEMENT = "([A-Za-z0-9_-]{1,1024})";

  private final Map<Tuple2<String,String>, List<Class<?>>> httpToClassMap; // (method,path)->message classes
  private final ConcurrentMap<String,Pattern> regexMap = Maps.newConcurrentMap( );

  private RestBinding(
      final Map<Tuple2<String,String>, List<Class<?>>> httpToClassMap
  ) {
    this.httpToClassMap = ImmutableMap.copyOf( httpToClassMap );
  }

  public static RestBinding of( final Map<Tuple2<String,String>, List<Class<?>>> httpToClassMap ) {
    return new RestBinding( httpToClassMap );
  }

  public boolean hasRestClass( String method, String path ) {
    try {
      getRestClass( method, path );
    } catch ( BindingException e ) {
      return false;
    }
    return true;
  }

  public Class<?> getRestClass( String method, String path ) throws BindingException {
    final List<Class<?>> classes = getRestClasses( method, path );
    if ( classes.size() != 1 ) {
      throw new BindingException( "Failed to find unique class for method/path: " + method + ":" + path );
    }
    return classes.get( 0 );
  }

  public List<Class<?>> getRestClasses( String method, String path ) throws BindingException {
    // quick exact match
    for ( final Map.Entry<Tuple2<String,String>,List<Class<?>>> entry : this.httpToClassMap.entrySet( ) ) {
      if ( entry.getKey( )._1( ).equals( method ) && entry.getKey( )._2( ).equals( path ) ) {
        return entry.getValue( );
      }
    }
    // regex match
    for ( final Map.Entry<Tuple2<String,String>,List<Class<?>>> entry : this.httpToClassMap.entrySet( ) ) {
      if ( entry.getKey( )._1( ).equals( method ) && pathRegexMatch( entry.getKey( )._2( ), path ) ) {
        return entry.getValue( );
      }
    }
    throw new BindingException( "Failed to find corresponding class for method/path: " + method + ":" + path );
  }

  public boolean hasClass( final String simpleName ) {
    for ( final Class<?> boundClass : Stream.ofAll( this.httpToClassMap.values( ) ).flatMap( Function.identity( ) ) ) {
      if ( simpleName.equals( boundClass.getSimpleName( ) ) ) {
        return true;
      }
    }
    return false;
  }

  public Class<?> getClass( final String simpleName ) throws BindingException {
    for ( final Class<?> boundClass : Stream.ofAll( this.httpToClassMap.values( ) ).flatMap( Function.identity( ) ) ) {
      if ( simpleName.equals( boundClass.getSimpleName( ) ) ) {
        return boundClass;
      }
    }
    throw new BindingException( "Failed to find class: " + simpleName );
  }

  public Object fromRest(
      final String method,
      final String path,
      final Function<String,String> headerLookup,
      final Function<String,String> parameterLookup,
      final String content,
      final ThrowingFunction<Tuple2<Class<?>,String>,Object,Exception> contentHandler
      ) throws Exception {
    Object object;
    final Class<?> targetType = getRestClass( method, path );
    final Ats targetTypeAts = Ats.from( targetType );
    final Option<HttpNoContent> httpNoContentOption = targetTypeAts.getOption( HttpNoContent.class );
    if ( httpNoContentOption.isDefined( ) || content.isEmpty( ) ) {
      object = targetType.newInstance( );
    } else {
      final Object payload = contentHandler.apply( Tuple.of(targetType, content) );
      if ( targetType.isInstance( payload ) ) {
        object = payload;
      } else if ( targetTypeAts.has( HttpContent.class ) ) {
        object = targetType.newInstance( );
        Beans.setObjectProperty( object, targetTypeAts.get( HttpContent.class ).payload( ), payload );
      } else {
        throw new BindingException( "Invalid content " + payload.getClass( ).getSimpleName( ) +
            " for " + targetType.getSimpleName( ) );
      }
    }
    final Field[] fields = targetType.getDeclaredFields( );
    for ( final Field field : fields ) {
      final Ats ats = Ats.from( field );
      if ( ats.has( HttpParameterMapping.class ) ) {
        final HttpParameterMapping paramMapping = ats.get( HttpParameterMapping.class );
        final String parameterValue = parameterLookup.apply( paramMapping.parameter( )[ 0 ] );
        if ( parameterValue != null ) {
          final Object value = BaseQueryBinding.convertToType(
              ( ) -> parameterValue,
              field.getType( ) );
          Beans.setObjectProperty( object, field.getName( ), value );
        }
      } else if ( ats.has( HttpHeaderMapping.class ) ) {
          final HttpHeaderMapping headerMapping = ats.get( HttpHeaderMapping.class);
          final String headerValue = headerLookup.apply( headerMapping.header( ) );
          if ( headerValue != null ) {
            final Object value = BaseQueryBinding.convertToType(
                () -> headerValue,
                field.getType( ) );
            Beans.setObjectProperty( object, field.getName( ), value );
          }
      } else if ( ats.has( HttpUriMapping.class ) ) {
        final HttpUriMapping uriMapping = ats.get( HttpUriMapping.class);
        final HttpRequestMapping requestMapping = targetTypeAts.get( HttpRequestMapping.class );
        final Matcher matcher = PARAMETER_MATCHER.matcher( requestMapping.uri() );
        final Map<String,Integer> nameToGroupMap = Maps.newHashMap( );
        int count = 0;
        while ( matcher.find( ) ) {
          count++;
          nameToGroupMap.put( matcher.group(1), count );
        }
        matcher.reset();
        final Pattern uriPattern =
            regexMap.computeIfAbsent( matcher.replaceAll( PARAMETER_REPLACEMENT ), Pattern::compile );
        final Matcher uriMatcher = uriPattern.matcher( path );
        if ( uriMatcher.matches( ) ) {
          final Object value = BaseQueryBinding.convertToType(
              () -> uriMatcher.group( nameToGroupMap.get( uriMapping.uri( ) ) ),
              field.getType( ) );
          Beans.setObjectProperty( object, field.getName( ), value );
        } else {
          throw new BindingException( "Failed to construct message of type " + targetType.getSimpleName( ) +
              ", invalid path: " + path );
        }
      }
    }
    return object;
  }

  public static RestResponse toRest( final Object object ) {
    final Field[] fields = object.getClass().getDeclaredFields( );
    final Map<String,String> headers = Maps.newHashMap( );
    int status = 200;
    boolean body = true;
    for ( final Field field : fields ) {
      final Ats ats = Ats.from( field );
      if ( ats.has( HttpHeaderMapping.class ) ) {
        final HttpHeaderMapping headerMapping = ats.get( HttpHeaderMapping.class);
        final Object value = Beans.getObjectProperty( object, field.getName( ) );
        if ( value != null ) {
          headers.put( headerMapping.header( ), String.valueOf( value ) );
        }
      }
    }
    final Option<HttpNoContent> httpNoContentOption = Ats.from(object).getOption( HttpNoContent.class );
    if ( httpNoContentOption.isDefined( ) ) {
      status = httpNoContentOption.get( ).statusCode( );
      body = false;
    }
    return new RestResponse( headers, status, body );
  }

  private boolean pathRegexMatch( final String pathRegex, final String path ) {
    return isPathRegex( pathRegex ) && getPathRegex( pathRegex ).matcher( path ).matches( );
  }

  private boolean isPathRegex( final String path ) {
    return path.indexOf( '{' ) > -1;
  }

  private Pattern getPathRegex( final String path ) {
    return regexMap.computeIfAbsent(
        PARAMETER_MATCHER.matcher( path ).replaceAll( PARAMETER_REPLACEMENT ),
        Pattern::compile );
  }

  public static class RestResponse {
    private final Map<String,String> headers;
    private final int status;
    private final boolean body;

    public RestResponse( final Map<String, String> headers, final int status, final boolean body ) {
      this.headers = ImmutableMap.copyOf( headers );
      this.status = status;
      this.body = body;
    }

    public Map<String, String> getHeaders( ) {
      return headers;
    }

    public int getStatus( ) {
      return status;
    }

    public boolean hasBody( ) {
      return body;
    }
  }
}
