package com.eucalyptus.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class ConfigurationProperties {
  private static Logger LOG = Logger.getLogger( ConfigurationProperties.class );

  private static Multimap<String,Class> fileToClassMap = Multimaps.newHashMultimap( );
  private static Multimap<Class,String> classToFieldMap = Multimaps.newHashMultimap( );
  private static Map<String,TypeParser> fieldTypeMap = Maps.newHashMap( );
  private static Map<Class,TypeParser> typeParsers = Maps.newHashMap( );
  
  static abstract class TypeParser<T> {
    public abstract T parse( String property );
  }

  static {
    typeParsers.put( Integer.class, new TypeParser<Integer>( ) {
      @Override
      public Integer parse( String property ) {
        return Integer.parseInt( property );
      }
    } );
    typeParsers.put( String.class, new TypeParser<String>( ) {
      @Override
      public String parse( String property ) {
        return property;
      }
    } );
    typeParsers.put( Long.class, new TypeParser<Long>( ) {
      @Override
      public Long parse( String property ) {
        return Long.parseLong( property );
      }
    } );
    typeParsers.put( Float.class, new TypeParser<Float>( ) {
      @Override
      public Float parse( String property ) {
        return Float.parseFloat( property );
      }
    } );
    typeParsers.put( Float.class, new TypeParser<Float>( ) {
      @Override
      public Float parse( String property ) {
        return Float.parseFloat( property );
      }
    } );
    typeParsers.put( Boolean.class, new TypeParser<Boolean>( ) {
      @Override
      public Boolean parse( String property ) {
        return Boolean.parseBoolean( property );
      }
    } );
  }
  
  
  public static void prepareClass( Class c ) {
    ConfigurableClass a = ( ConfigurableClass ) c.getAnnotation( ConfigurableClass.class );
    if( a == null ) {
      return;
    } else {
      fileToClassMap.put( a.alias( ), c );
      for( Field f : c.getDeclaredFields( ) ) {
        Configurable fieldConf = f.getAnnotation( Configurable.class );
        if( fieldConf == null ) {
          continue;
        } else {
          for( Class other : fileToClassMap.get( a.alias( ) ) ) {
            if( classToFieldMap.get( other ).contains( f.getName( ) ) ) {
              throw new RuntimeException( "Duplicate configurable field in same config file: \n" +  
                                          "-> " + canonicalize( c, f ) + "\n" +
                                          "-> " + canonicalize( other, f ) + "\n" );
            }
          }
          classToFieldMap.put( c, f.getName( ) );
          fieldTypeMap.put( canonicalize( c, f ), typeParsers.get( f.getType( ) ) );
        }
      }
    }
  }
  
  public static void configure( Class c ) {
    ConfigurableClass a = ( ConfigurableClass ) c.getAnnotation( ConfigurableClass.class );
    if( a == null ) {
      throw new RuntimeException( "Attempt to configure a class which does not declare itself Configurable: " + c.getName( ) );
    }
    String propsFileName = SubDirectory.CONF + File.separator + a.alias( ) + ".properties";
    File propsFile = new File( propsFileName );
    if( !propsFile.exists( ) ) {
      ConfigurationProperties.reset( c );
    }
    Properties props = new Properties( );
    try {
      props.load( new FileReader( propsFile ) );
    } catch ( FileNotFoundException e ) {
      LOG.debug( e, e );
    } catch ( IOException e ) {
      LOG.debug( e, e );
    }
    Map<String,String> properties = Maps.fromProperties( props );
    List<String> missingFields = Lists.newArrayList( );
    for( String fieldName : properties.keySet( ) ) {
      try {
        Field f = c.getDeclaredField( fieldName );
        Configurable fieldConf = f.getAnnotation( Configurable.class );
        String value = properties.get( fieldName );
        if( value != null ) {
          try {
            f.set( null, fieldTypeMap.get( c.getName( )+f.getName( ) ).parse( value ) );
            LOG.debug( "-> " + canonicalize( c, f ) + " = " + f.get( null ) );
          } catch ( Exception e ) {
            LOG.warn( "Failed to parse " + propsFileName + " " + canonicalize( c, f ) + " = " + value + "as type " + fieldTypeMap.get( canonicalize( c, f ) ), e );
          }
        }
      } catch ( Exception e ) {
        missingFields.add( fieldName );
      }
    }
    if( !missingFields.isEmpty( ) ) {
      throw new RuntimeException( "Failed to parse values from " + propsFileName + ": " + missingFields );
    }
  }
  
  public static void store( Class c ) {
    
  }
  
  public static void reset( Class c ) {
    
  }
  
  private static String canonicalize( Class c, Field f) {
    return c.getName( ) + "." + f.getName( );
  }
}
